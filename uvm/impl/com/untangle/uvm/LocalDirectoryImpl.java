/**
 * $Id$
 */

package com.untangle.uvm;

import com.google.common.io.BaseEncoding;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Base32;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.FormatterClosedException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.io.File;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Local Directory stores a local list of users
 */
public class LocalDirectoryImpl implements LocalDirectory
{
    private static final String LOCAL_DIRECTORY_SETTINGS_FILE = System.getProperty("uvm.settings.dir") + "/untangle-vm/local_directory.js";
    private static final String XAUTH_SECRETS_FILE = "/etc/xauth.secrets";
    private static final String IPSEC_RELOAD_SECRETS = "/usr/sbin/ipsec rereadsecrets";

    private static final String FREERADIUS_LOGFILE_SCRIPT = System.getProperty("uvm.home") + "/bin/ut-radius-logfile";
    private static final String FREERADIUS_PROXY_SCRIPT = System.getProperty("uvm.home") + "/bin/ut-radius-proxy";
    private static final String FREERADIUS_LOCAL_SECRETS = "/etc/freeradius/3.0/mods-config/files/untangle.local";
    private static final String FREERADIUS_AUTHORIZE = "/etc/freeradius/3.0/mods-config/files/authorize";
    private static final String FREERADIUS_RADIUSD = "/etc/freeradius/3.0/radiusd.conf";
    private static final String FREERADIUS_SAMBA_DIRECTORY = "/etc/samba";
    private static final String FREERADIUS_SAMBA_CONFIG = "/etc/samba/smb.conf";
    private static final String FREERADIUS_KRB5_CONFIG = "/etc/krb5.conf";
    private static final String FREERADIUS_MSCHAP_CONFIG = "/etc/freeradius/3.0/mods-enabled/mschap";
    private static final String FREERADIUS_NTLM_CONFIG = "/etc/freeradius/3.0/mods-enabled/ntlm_auth";

    private final static String FILE_DISCLAIMER = "# This file is created and maintained by the Untangle Local Directory.\n# If you modify this file manually, your changes will be overwritten!\n\n";
    public static final Random random = new Random();

    private final Logger logger = LogManager.getLogger(getClass());

    private LinkedList<LocalDirectoryUser> currentList;

    private boolean radiusProxyComputerAccountExists = false;

    /**
     * Constructor
     */
    public LocalDirectoryImpl()
    {
        loadUsersList();
    }

    /**
     * Gets the contents of the radius server log file
     *
     * @return The contents of the radius server log file
     */
    public String getRadiusLogFile()
    {
        return UvmContextFactory.context().execManager().execOutput(FREERADIUS_LOGFILE_SCRIPT);
    }

    /**
     * Gets the status of the account status in Active Directory
     *
     * @return The status returned by the script
     */
    public String getRadiusProxyStatus()
    {
        SystemSettings systemSettings = UvmContextFactory.context().systemManager().getSettings();
        String userPassword;
        try{
            userPassword = getRadiusProxyOriginalPassword(systemSettings);
        }catch (PasswordUtil.CryptoProcessException exn) { 
            logger.error("Exception occured while fetching original password", exn);
            return "Unable to decrypt the encrypted password for AD server" + systemSettings.getRadiusProxyServer();
        }
        String command = String.format("%s \"%s\" \"%s\"", FREERADIUS_PROXY_SCRIPT, systemSettings.getRadiusProxyUsername(), userPassword);

        return UvmContextFactory.context().execManager().execOutput(false, command);
    }

    /**
     * Adds a computer account to the configured AD domain controller using the
     * configured credentials.
     * 
     * @return The result of the join attempt
     */
    public ExecManagerResult addRadiusComputerAccount()
    {
        SystemSettings systemSettings = UvmContextFactory.context().systemManager().getSettings();
        String userPassword;

        // The hostname and workgroup must NOT be the same or the samba tools
        // will throw all kinds of obscure errors about memory allocation when
        // the real problem is the name conflict.
        String groupName = systemSettings.getRadiusProxyWorkgroup();
        String machineName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName();
        if (groupName.equalsIgnoreCase(machineName)) {
            return new ExecManagerResult(1, "Unable to create computer account because the System Hostname and AD Workgroup are the same");
        }

        // Make sure the proxy server resolves to a valid address
        try {
            InetAddress checker = InetAddress.getByName(systemSettings.getRadiusProxyServer());
        } catch (java.net.UnknownHostException exn) {
            return new ExecManagerResult(1, "Unable to resolve the IP address of the AD Server " + systemSettings.getRadiusProxyServer());
        } catch (Exception exn) { }
        try{
            userPassword = getRadiusProxyOriginalPassword(systemSettings);
        }catch (PasswordUtil.CryptoProcessException exn) { 
            logger.error("Exception occured while fetching original password", exn);
            return new ExecManagerResult(1, "Unable to decrypt the encrypted password of AD server " + systemSettings.getRadiusProxyServer());
        }
        String command = ("/usr/bin/net ads --no-dns-updates join");
        command += String.format(" -U \"%s%%%s\"", systemSettings.getRadiusProxyUsername(), userPassword);
        command += (" -S " + systemSettings.getRadiusProxyServer());
        command += (" osName=\"Untangle NG Firewall\"");
        command += (" osVer=\"" + UvmContextFactory.context().getFullVersion() + "\"");

        ExecManagerResult result =  UvmContextFactory.context().execManager().exec(command, false, false, false);

        // NGFW-13595 The winbind service must be restarted after we create
        // the computer account because it requires the SID that gets created
        // by the "net ads" call we made above.
        if (result.result == 0) {
            UvmContextFactory.context().execManager().exec("systemctl restart winbind.service");
        }

        return result;
    }
    /**
     * This method used to fetch the radius proxy original ppassword
     *
     * @param systemSettings
     *        The system Settings 
     * @return original Radius Proxy password
     * @throws PasswordUtil.CryptoProcessException if the decrypted password is `null`, indicating a decryption failure.
     */
    private String getRadiusProxyOriginalPassword(SystemSettings systemSettings) throws PasswordUtil.CryptoProcessException{
        String userPassword = PasswordUtil.getDecryptPassword(systemSettings.getRadiusProxyEncryptedPassword());
        if (userPassword == null) {
            throw new PasswordUtil.CryptoProcessException("Got null as decryptedPassword");
        }
        return userPassword;
    }

    /**
     * Called to test Active Directory authentication
     *
     * @param userName
     *        The username for the test
     * @param userPass
     *        The password for the test
     * @param userDomain
     *        The domain for the test
     * @return The result of the test
     */
    public String testRadiusProxyLogin(String userName, String userPass, String userDomain)
    {
        if (userName.isBlank()) {
            return "Missing Username for authentication test";
        }

        if (userPass.isBlank()) {
            return "Missing Password for authentication test";
        }

        String command = ("/usr/bin/ntlm_auth --request-nt-key");
        if (StringUtils.isNotEmpty(userDomain)) {
            command += (" --domain=\"" + userDomain + "\"");
        }
        command += (" --username=\"" + userName + "\"");
        command += (" --password=\"" + userPass + "\"");

        return UvmContextFactory.context().execManager().execOutput(command);
    }

    /**
     * Called to generate new random key for two factor auth.
     *
     * @return key 160bit in base 32.
     */
    public String generateSecret() {
        byte[] buffer = new byte[20];

        random.nextBytes(buffer);
        byte[] secretKey = Arrays.copyOf(buffer, 10);

        return BaseEncoding.base32().encode(secretKey);
    }

    /**
     * Called to obtain secret and QR code for a particular user.
     *
     * @param username
     *        The username used in the new key.
     * @param issuer
     *        The issuer or hostname.
     * @param secret
     *        The secret in Base32 encoding.
     * 
     * @return Message text and SVG image. (String)
     */
    public String showSecretQR(String username, String issuer, String secret) {
        String url = new StringBuilder("otpauth://totp/").append(username.toLowerCase().trim())
        .append("@")
        .append("openvpn".trim())
        .append("?secret=")
        .append(secret.toUpperCase().trim())
        .append("&issuer=")
        .append(issuer.trim())
        .toString();

        String command = new StringBuilder("/usr/bin/qrencode -s 4 -t SVG -o - ").append(url).toString();
        String QrSvg =  UvmContextFactory.context().execManager().execOutput(command);
        return ("Manual entry: " + secret + "<BR>" + QrSvg);
    }

    /**
     * Checks to see if user is expired
     *
     * @param user
     *        The user to check
     * @return True if expired, otherwise false
     */
    private boolean accountExpired(LocalDirectoryUser user)
    {
        return user.getExpirationTime() > 0 && System.currentTimeMillis() >= user.getExpirationTime();
    }

    /**
     * Authenticate a user
     * 
     * @param username
     *        The username
     * @param password
     *        The password
     * @return True for successful authentication, otherwise false
     */
    public boolean authenticate(String username, String password)
    {
        if (username == null) {
            logger.warn("Invalid arguments: username is null");
            return false;
        }
        if (password == null) {
            logger.warn("Invalid arguments: password is null or empty");
            return false;
        }
        if ("".equals(password)) {
            logger.info("Blank passwords not allowed");
            return false;
        }

        for (LocalDirectoryUser user : this.currentList) {
            if (username.equals(user.getUsername())) {
                //Check for the records which still contains PasswordBase64Hash
                String base64 = calculateBase64Hash(password);
                if(StringUtils.isNotBlank(user.getPasswordBase64Hash()) &&
                        base64 != null && base64.equals(user.getPasswordBase64Hash()) && !accountExpired(user)) return true;

                String encryptedPassword = PasswordUtil.getEncryptPassword(password);
                if (encryptedPassword != null && encryptedPassword.equals(user.getEncryptedPassword()) && !accountExpired(user)) return true;
                String md5 = calculateMd5Hash(password);
                if (md5 != null && md5.equals(user.getPasswordMd5Hash()) && !accountExpired(user)) return true;
                String sha = calculateShaHash(password);
                if (sha != null && sha.equals(user.getPasswordShaHash()) && !accountExpired(user)) return true;
            }
        }

        return false;
    }

    /**
     * Authenticate a user. Only being used by OpenVPN.
     * 
     * @param username
     *        The username
     * @param password
     *        The password
     * @param code
     *        The TOTP code provided by the user
     * @return True for successful authentication, otherwise false
     */
    public boolean authenticate(String username, String password, long code)
    {
        // First check if username/password is correct. If not we don't need to do more work.
        if (authenticate(username, password) == false) return false;

        // Look up the users shared secret.
        String secret = null;
        for (LocalDirectoryUser user : this.currentList) {
            if (username.equals(user.getUsername())) {
                if (!user.getMfaEnabled())
                    return true; 
                secret = user.getTwofactorSecretKey();
                break;
            }
        }

        // Get current time slot. 
        Long offset = Instant.now().getEpochSecond() / 30;

        // We check time slots +/- 1 to account for slight time skew and user slowness.
        // TODO: Consider making #slots a admin setting.
        if (verifyTOTPcode(secret, code, offset) == false)
            if (verifyTOTPcode(secret, code, offset - 1) == false)
                return verifyTOTPcode(secret, code, offset + 1);
        return true;
    }

    /**
     * This function is called from the RADIUS server for successful user
     * authentication. We handle MAC addresses in the following formats:
     * AA:BB:CC:DD:EE:FF, AA-BB-CC-DD-EE-FF, and AABBCCDDEEFF
     * because I've seen all three across different access points.
     *
     * @param userName - The name of the user
     * @param macAddress - The mac address of the device
     */
    public void notifyRadiusUserLogin(String userName, String macAddress)
    {
        String[] checker;

        logger.debug("RADIUS Login Notification - USER:{} MAC:{}", userName , macAddress);

        // first try AA:BB:CC:DD:EE:FF
        checker = macAddress.split(":");
        if (checker.length == 6) {
            handleRadiusUserLogin(userName, macAddress);
            return;
        }

        // next try AA-BB-CC-DD-EE-FF
        checker = macAddress.split("-");
        if (checker.length == 6) {
            handleRadiusUserLogin(userName, macAddress.replace('-', ':'));
            return;
        }

        // format the first 12 bytes as the MAC address
        if (macAddress.length() >= 12) {
            StringBuilder builder = new StringBuilder();
            byte[] list = macAddress.getBytes();
            builder.append((char)list[0]);
            builder.append((char)list[1]);
            builder.append(':');
            builder.append((char)list[2]);
            builder.append((char)list[3]);
            builder.append(':');
            builder.append((char)list[4]);
            builder.append((char)list[5]);
            builder.append(':');
            builder.append((char)list[6]);
            builder.append((char)list[7]);
            builder.append(':');
            builder.append((char)list[8]);
            builder.append((char)list[9]);
            builder.append(':');
            builder.append((char)list[10]);
            builder.append((char)list[11]);
            handleRadiusUserLogin(userName, builder.toString());
        }

        logger.warn("RADIUS Invalid MAC Address: {}", macAddress);
    }

    /**
     * Adds or gets the existing device entry from the system table and
     * adds the RADIUS username.
     *
     * @param userName - The name of the user
     * @param macAddress - The mac address of the device
     */
    private void handleRadiusUserLogin(String userName, String macAddress)
    {
        DeviceTableEntry device = UvmContextFactory.context().deviceTable().addDevice(macAddress.toLowerCase());
        device.setUsername(userName);
    }

    /**
     * Get the list of local directory users
     * 
     * @return The list of users
     */
    public LinkedList<LocalDirectoryUser> getUsers()
    {
        if (this.currentList == null) return new LinkedList<>();

        return this.currentList;
    }

    /**
     * Set the list of local directory users
     * 
     * @param users
     *        The list of users
     */
    public void setUsers(LinkedList<LocalDirectoryUser> users)
    {
        HashSet<String> usersSeen = new HashSet<>();

        /**
         * Remove cleartext password before saving
         */
        for (LocalDirectoryUser user : users) {

            /**
             * Check for dupes
             */
            if (usersSeen.contains(user.getUsername())) throw new RuntimeException("Duplicate user: " + user.getUsername());
            else usersSeen.add(user.getUsername());

            /**
             * Set the encrypted passowrd and other hashes has changed and we must recalculate the values
             */
            if(StringUtils.isNotBlank(user.getPasswordBase64Hash())){ 
                String password = getPasswordFromBase64Hash(user.getPasswordBase64Hash());
                user.setPasswordShaHash(calculateShaHash(password));
                user.setPasswordMd5Hash(calculateMd5Hash(password));
                user.setEncryptedPassword(PasswordUtil.getEncryptPassword(password));
                user.setPasswordBase64Hash(null);
                user.setPassword(null);
            }
        }
        saveUsersList(users);
    }

    /**
     * Add a user to the local directory
     * 
     * @param user
     *        The user to add
     */
    public void addUser(LocalDirectoryUser user)
    {
        LinkedList<LocalDirectoryUser> users = this.getUsers();
        user.setEncryptedPassword(PasswordUtil.getEncryptPassword(user.getPassword()));
        user.setPassword(null); //remove password
        users.add(user);

        this.saveUsersList(users);
    }

    /**
     * Sets if radius computer account has been added successfully
     * @param radiusProxyComputerAccountExists
     *          Boolean for if the computer account exists
     */
    public void setRadiusProxyComputerAccountExists(boolean radiusProxyComputerAccountExists) {
        this.radiusProxyComputerAccountExists = radiusProxyComputerAccountExists;
    }

    /**
     * Gets if radius computer account exists
     * @return boolean for if the radius computer account exists
     */
    public boolean getRadiusProxyComputerAccountExists() {
        return this.radiusProxyComputerAccountExists;
    }

    /**
     * Username matcher
     * 
     * @param u1
     *        Username one
     * @param u2
     *        Username two
     * @return True if they match, otherwise false
     */
    private boolean userNameMatch(LocalDirectoryUser u1, LocalDirectoryUser u2)
    {
        return u1.getUsername() != null && u1.getUsername().equals(u2.getUsername());
    }

    /**
     * Email matcher
     * 
     * @param u1
     *        Email one
     * @param u2
     *        Email two
     * @return True if they match, otherwise false
     */
    private boolean emailMatch(LocalDirectoryUser u1, LocalDirectoryUser u2)
    {
        return u1.getEmail() != null && u1.getEmail().equals(u2.getEmail());
    }

    /**
     * Check to see if a local directory user exists
     * 
     * @param user
     *        The user
     * @return True if the user is found, otherwise false
     */
    public boolean userExists(LocalDirectoryUser user)
    {
        LinkedList<LocalDirectoryUser> users = this.getUsers();
        for (LocalDirectoryUser u : users) {
            if (userNameMatch(u, user) || emailMatch(u, user)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check to see if a user is expired
     * 
     * @param user
     *        The user
     * @return True if the user is expired, otherwise false
     */
    public boolean userExpired(LocalDirectoryUser user)
    {
        LinkedList<LocalDirectoryUser> users = this.getUsers();
        for (LocalDirectoryUser u : users) {
            if (userNameMatch(u, user) || emailMatch(u, user)) {
                if (accountExpired(u)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Cleanup expired users
     */
    public void cleanupExpiredUsers()
    {
        Iterator<LocalDirectoryUser> iter = this.getUsers().iterator();
        boolean entriesDeleted = false;
        while (iter.hasNext()) {
            LocalDirectoryUser u = iter.next();
            if (accountExpired(u)) {
                iter.remove();
                entriesDeleted = true;
            }
        }
        if (entriesDeleted) {
            saveUsersList(this.getUsers());
        }
    }

    /**
     * Save the user list to a file
     * 
     * @param list
     *        The user list
     */
    private synchronized void saveUsersList(LinkedList<LocalDirectoryUser> list)
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();

        if (list == null) list = new LinkedList<>();

        try {
            settingsManager.save(LOCAL_DIRECTORY_SETTINGS_FILE, list);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.", e);
            return;
        }

        // update xauth.secrets and chap-secrets for IPsec and untangle.local for RADIUS
        updateXauthSecrets(list);
        // Update chap-secrets
        UvmContextFactory.context().syncSettings().run(LOCAL_DIRECTORY_SETTINGS_FILE);
        updateRadiusConfiguration(list);
        this.currentList = list;
    }

    /**
     * Load the userlist from a file
     */
    @SuppressWarnings("unchecked")
    private void loadUsersList()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        LinkedList<LocalDirectoryUser> users = null;

        // Read the settings from file
        try {
            users = (LinkedList<LocalDirectoryUser>) settingsManager.load(LinkedList.class, LOCAL_DIRECTORY_SETTINGS_FILE);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Unable to read localDirectory file: ", e);
        }

        // no settings? just initialize new ones
        if (users == null) {
            logger.warn("No settings found - Initializing new settings.");
            this.saveUsersList(new LinkedList<>());
        }

        // settings loaded so assign to currentList and write IPsec and RADIUS secrets
        else {
            //For backup restore, user record needs to be set with encrypted password only
            setUsers(users);

            updateXauthSecrets(users);
            // Update chap-secrets
            UvmContextFactory.context().syncSettings().run(LOCAL_DIRECTORY_SETTINGS_FILE);
            updateRadiusConfiguration(users);
            this.currentList = users;
        }
    }

    /**
     * IPsec Xauth and IKEv2 can use LocalDirectory for login credentials so any
     * time we load or save the list we'll call this function which will export
     * all of the user/pass info to the xauth.secrets file
     * 
     * @param list
     *        The list of LocalDirectoryUsers
     */
    private void updateXauthSecrets(LinkedList<LocalDirectoryUser> list)
    {
        FileWriter auth = null;
        try {
            // put all the username/password pairs into a file for IPsec Xauth and IKEv2
            auth = new FileWriter(XAUTH_SECRETS_FILE, false);

            auth.write(FILE_DISCLAIMER);

            for (LocalDirectoryUser user : list) {
                String userPassword = getLocalUserOriginalPass(user);
                if (userPassword == null) {
                    logger.warn("Error while creating entry in XAUTH secrets file for user : {}", user.getUsername());
                    continue;
                }
                auth.write(user.getUsername() + " : XAUTH 0x" + stringHexify(userPassword) + "\n");
                auth.write(user.getUsername() + " : EAP 0x" + stringHexify(userPassword) + "\n");
            }

            auth.flush();
            auth.close();

            /*
             * Tell IPsec to reload the secrets
             */
            UvmContextFactory.context().execManager().exec(IPSEC_RELOAD_SECRETS);
        } catch (Exception exn) {
            logger.error("Exception creating IPsec xauth.secrets file", exn);
        } finally {
            if (auth != null) {
                try {
                    auth.close();
                } catch (IOException ex) {
                    logger.error("Exception closing IPsec xauth.secrets file", ex);
                }
            }
        }
    }
    /**
     * This method is used to fetch the original local user password form base64 and encrypted password
     *
     * @param user
     *        The local directory user
     * @return
     *        Original password
     */
    private String getLocalUserOriginalPass(LocalDirectoryUser user){
        String userPassword;
        if(StringUtils.isNotBlank(user.getPasswordBase64Hash())){
            byte[] rawPassword = Base64.decodeBase64(user.getPasswordBase64Hash().getBytes());
            userPassword = new String(rawPassword);
        }else{
            userPassword = PasswordUtil.getDecryptPassword(user.getEncryptedPassword());
        }
        return userPassword;
    }

    /**
     * We now support WPA Enterprise which allows connecting to a WiFi network
     * using a username and password which is authenticated via RADIUS. To make
     * this work we put all of our credentials in file that will work with
     * freeradius and update the other configuration files required to make this
     * work.
     *
     * @param list
     *        The list of LocalDirectoryUsers
     */
    private void updateRadiusConfiguration(LinkedList<LocalDirectoryUser> list)
    {
        SystemSettings systemSettings = UvmContextFactory.context().systemManager().getSettings();
        FileWriter fw = null;

        /*
         * Put all of the username/password pairs into a file for the RADIUS
         * server. Unfortunately these have to be in plaintext as it is the only
         * format that will work with all of the radius authentication
         * mechanisms.
         */
        try {
            fw = new FileWriter(FREERADIUS_LOCAL_SECRETS, false);
            fw.write(FILE_DISCLAIMER);

            if (systemSettings.getRadiusServerEnabled()) {
                for (LocalDirectoryUser user : list) {
                    String userPassword = getLocalUserOriginalPass(user);
                    if (userPassword == null) {
                        logger.warn("Error while creating entry in RADIUS secrets file for user : {}", user.getUsername());
                        continue;
                    }
                    fw.write(user.getUsername() + " Cleartext-Password := \"" + userPassword + "\", MS-CHAP-Use-NTLM-Auth := 0\n");
                }
            }
            fw.flush();
            fw.close();
        } catch (Exception exn) {
            logger.error("Exception creating RADIUS secrets file", exn);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ex) {
                    logger.error("Exception closing RADIUS secrets file", ex);
                }
            }
        }

        /*
         * Create the authorize file with the freeradius package defaults and a
         * line to include our local credentials.
         */
        try {
            fw = new FileWriter(FREERADIUS_AUTHORIZE, false);
            fw.write(FILE_DISCLAIMER);
            if (systemSettings.getRadiusServerEnabled()) {
                fw.write("DEFAULT Framed-Protocol == PPP\n\tFramed-Protocol = PPP,\n\tFramed-Compression = Van-Jacobson-TCP-IP\n\n");
                fw.write("DEFAULT Hint == \"CSLIP\"\n\tFramed-Protocol = SLIP,\n\tFramed-Compression = Van-Jacobson-TCP-IP\n\n");
                fw.write("DEFAULT Hint == \"SLIP\"\n\tFramed-Protocol = SLIP\n\n");
                fw.write("$INCLUDE untangle.local\n");
            }
            fw.flush();
            fw.close();
        } catch (Exception exn) {
            logger.error("Exception creating RADIUS authorize file", exn);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ex) {
                    logger.error("Exception closing RADIUS authorize file", ex);
                }
            }
        }

        /*
         * Create the radiusd.conf file with most of the freeradius package
         * defaults. The main change here is we create our client definition
         * inline here rather than including and maintaining a separate
         * clients.conf as would be the case with the default configuration. We
         * also enable the logging stuff for reporting purposes.
         */
        try {
            fw = new FileWriter(FREERADIUS_RADIUSD, false);
            fw.write(FILE_DISCLAIMER);
            if (systemSettings.getRadiusServerEnabled()) {
                fw.write("prefix = /usr\n");
                fw.write("exec_prefix = /usr\n");
                fw.write("sysconfdir = /etc\n");
                fw.write("localstatedir = /var\n");
                fw.write("sbindir = ${exec_prefix}/sbin\n");
                fw.write("logdir = /var/log/freeradius\n");
                fw.write("raddbdir = /etc/freeradius/3.0\n");
                fw.write("radacctdir = ${logdir}/radacct\n");
                fw.write("name = freeradius\n");
                fw.write("confdir = ${raddbdir}\n");
                fw.write("modconfdir = ${confdir}/mods-config\n");
                fw.write("certdir = ${confdir}/certs\n");
                fw.write("cadir = ${confdir}/certs\n");
                fw.write("run_dir = ${localstatedir}/run/${name}\n");
                fw.write("db_dir = ${raddbdir}\n");
                fw.write("libdir = /usr/lib/freeradius\n");
                fw.write("pidfile = ${run_dir}/${name}.pid\n");
                fw.write("correct_escapes = true\n");
                fw.write("max_request_time = 30\n");
                fw.write("cleanup_delay = 5\n");
                fw.write("max_requests = 16384\n");
                fw.write("hostname_lookups = no\n");
                fw.write("log {\n");
                fw.write("\tdestination = files\n");
                fw.write("\tcolourise = yes\n");
                fw.write("\tfile = ${logdir}/radius.log\n");
                fw.write("\tsyslog_facility = daemon\n");
                fw.write("\tstripped_names = no\n");
                fw.write("\tauth = yes\n");
                fw.write("\tauth_badpass = no\n");
                fw.write("\tauth_goodpass = no\n");
                fw.write("\tmsg_goodpass = \"RADIUS_ACCEPT\"\n");
                fw.write("\tmsg_badpass = \"RADIUS_REJECT\"\n");
                fw.write("\tmsg_denied = \"Access Denied\"\n");
                fw.write("}\n");
                fw.write("checkrad = ${sbindir}/checkrad\n");
                fw.write("security {\n");
                fw.write("\tuser = freerad\n");
                fw.write("\tgroup = freerad\n");
                fw.write("\tallow_core_dumps = no\n");
                fw.write("\tmax_attributes = 200\n");
                fw.write("\treject_delay = 1\n");
                fw.write("\tstatus_server = yes\n");
                fw.write("}\n");
                fw.write("proxy_requests = yes\n");
                fw.write("$INCLUDE proxy.conf\n");
                fw.write("client untangle {\n");
                fw.write("\tipaddr = 0.0.0.0/0\n");
                fw.write("\tproto = *\n");
                fw.write("\tsecret = " + systemSettings.getRadiusServerSecret() + "\n");
                fw.write("}\n");
                fw.write("thread pool {\n");
                fw.write("\tstart_servers = 5\n");
                fw.write("\tmax_servers = 32\n");
                fw.write("\tmin_spare_servers = 3\n");
                fw.write("\tmax_spare_servers = 10\n");
                fw.write("\tmax_requests_per_server = 0\n");
                fw.write("\tauto_limit_acct = no\n");
                fw.write("}\n");
                fw.write("modules {\n");
                fw.write("$INCLUDE mods-enabled/\n");
                fw.write("}\n");
                fw.write("instantiate {\n");
                fw.write("}\n");
                fw.write("policy {\n");
                fw.write("$INCLUDE policy.d/\n");
                fw.write("}\n");
                fw.write("$INCLUDE sites-enabled/\n");
            }
            fw.flush();
            fw.close();
        } catch (Exception exn) {
            logger.error("Exception creating RADIUS radiusd file", exn);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ex) {
                    logger.error("Exception closing RADIUS radiusd file", ex);
                }
            }
        }

        /*
         * Create the smb.conf file if /etc/samba exists
         */
        File sambaTarget = new File(FREERADIUS_SAMBA_DIRECTORY);
        if (sambaTarget.isDirectory()) {
            try {
                fw = new FileWriter(FREERADIUS_SAMBA_CONFIG, false);
                fw.write(FILE_DISCLAIMER);
                if (systemSettings.getRadiusProxyEnabled()) {
                    fw.write("[global]\n");
                    fw.write("\tworkgroup = " + systemSettings.getRadiusProxyWorkgroup() + "\n");
                    fw.write("\tsecurity = ads\n");
                    fw.write("\tpassword server = " + systemSettings.getRadiusProxyServer() + "\n");
                    fw.write("\trealm = " + systemSettings.getRadiusProxyRealm() + "\n");
                    fw.write("\twinbind use default domain = yes\n");
                    fw.write("\tserver role = standalone server\n");
                    fw.write("\tbind interfaces only = no\n");
                    fw.write("\tload printers = no\n");
                    fw.write("\tprinting = bsd\n");
                    fw.write("\tprintcap name = /dev/null\n");
                    fw.write("\tdisable spoolss = yes\n");
                    fw.write("\tlocal master = no\n");
                }
                fw.flush();
                fw.close();
            } catch (Exception exn) {
                logger.error("Exception creating SAMBA configuration file", exn);
            } finally {
                if (fw != null) {
                    try {
                        fw.close();
                    } catch (IOException ex) {
                        logger.error("Exception closing SAMBA configuration file", ex);
                    }
                }
            }
        }

        /*
         * Create the krb5.conf file
         */
        try {
            fw = new FileWriter(FREERADIUS_KRB5_CONFIG, false);
            fw.write(FILE_DISCLAIMER);
            if (systemSettings.getRadiusProxyEnabled()) {
                fw.write("[libdefaults]\n");
                fw.write("\tdefault_realm = " + systemSettings.getRadiusProxyRealm().toUpperCase() + "\n");
                fw.write("\tdns_lookup_realm = false\n");
                fw.write("\tdns_lookup_kdc = false\n");
                fw.write("\n");
                fw.write("[realms]\n");
                fw.write("\t" + systemSettings.getRadiusProxyRealm().toUpperCase() + " {\n");
                fw.write("\t\tkdc = " + systemSettings.getRadiusProxyServer() + "\n");
                fw.write("\t\tadmin_server = " + systemSettings.getRadiusProxyServer() + "\n");
                fw.write("\t\tdefault_domain = " + systemSettings.getRadiusProxyRealm() + "\n");
                fw.write("\t}\n");
                fw.write("[domain_realm]\n");
                fw.write("\t." + systemSettings.getRadiusProxyRealm().toLowerCase() + " = " + systemSettings.getRadiusProxyRealm().toUpperCase() + "\n");
                fw.write("\t" + systemSettings.getRadiusProxyRealm().toLowerCase() + " = " + systemSettings.getRadiusProxyRealm().toUpperCase() + "\n");
            }
            fw.flush();
            fw.close();
        } catch (Exception exn) {
            logger.error("Exception creating KRB5 configuration file", exn);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ex) {
                    logger.error("Exception closing KRB5 configuration file", ex);
                }
            }
        }

        /*
         * Create the mschap config file
         */
        try {
            fw = new FileWriter(FREERADIUS_MSCHAP_CONFIG, false);
            fw.write(FILE_DISCLAIMER);
            fw.write("mschap {\n");
            if (systemSettings.getRadiusProxyEnabled()) {
                fw.write("\twith_ntdomain_hack = yes\n");
                fw.write("\tntlm_auth = \"/usr/bin/ntlm_auth --request-nt-key --username=%{%{Stripped-User-Name}:-%{%{User-Name}:-None}} --challenge=%{%{mschap:Challenge}:-00} --nt-response=%{%{mschap:NT-Response}:-00}\"\n");
            }
            fw.write("}\n");
            fw.flush();
            fw.close();
        } catch (Exception exn) {
            logger.error("Exception creating MSCHAP configuration file", exn);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ex) {
                    logger.error("Exception closing MSCHAP configuration file", ex);
                }
            }
        }

        /*
         * Create the ntlm_auth config file
         */
        try {
            fw = new FileWriter(FREERADIUS_NTLM_CONFIG, false);
            fw.write(FILE_DISCLAIMER);
            if (systemSettings.getRadiusProxyEnabled()) {
                fw.write("exec ntlm_auth {\n");
                fw.write("\twait = yes\n");
                fw.write("\tprogram = \"/usr/bin/ntlm_auth --request-nt-key --domain=MYDOMAIN --username=%{mschap:User-Name} --password=%{User-Password}\"\n");
                fw.write("}\n");
            }
            fw.flush();
            fw.close();
        } catch (Exception exn) {
            logger.error("Exception creating NTLM configuration file", exn);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException ex) {
                    logger.error("Exception closing NTLM configuration file", ex);
                }
            }
        }

        /*
         * If server is enabled restart the freeradius service
         */
        if (systemSettings.getRadiusServerEnabled()) {
            UvmContextFactory.context().execManager().exec("systemctl restart freeradius.service");
        }

        /*
         * If proxy is enabled restart the smbd and winbind services
         */
        if (systemSettings.getRadiusProxyEnabled()) {
            UvmContextFactory.context().execManager().exec("systemctl restart smbd.service");
            UvmContextFactory.context().execManager().exec("systemctl restart winbind.service");
        }
    }

    /**
     * Calculate the SHA hash for a password
     * 
     * @param password
     *        The password
     * @return The hash
     */
    private String calculateShaHash(String password)
    {
        return calculateHash(password, "SHA");
    }

    /**
     * Calculate the MD5 hash for a password
     * 
     * @param password
     *        The password
     * @return The hash
     */
    private String calculateMd5Hash(String password)
    {
        return calculateHash(password, "MD5");
    }

    /**
     * Calculate the base64 hash for a password
     * 
     * @param password
     *        The password
     * @return The hash
     */
    private String calculateBase64Hash(String password)
    {
        byte[] digest = Base64.encodeBase64(password.getBytes());
        return new String(digest);
    }

    /**
     * Get the password from the base64 hash
     * 
     * @param base64
     *        The base64 hash
     * @return The password
     */
    private String getPasswordFromBase64Hash(String base64)
    {
        byte[] pass = Base64.decodeBase64(base64.getBytes());
        return new String(pass);
    }

    /**
     * Calculate the hash for a password
     * 
     * @param password
     *        The password
     * @param hashAlgo
     *        The hash algorighm
     * @return The hash
     */
    private String calculateHash(String password, String hashAlgo)
    {
        try {
            MessageDigest hasher = MessageDigest.getInstance(hashAlgo);
            String hashString;
            hasher.update(password.getBytes());
            byte[] digest = hasher.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hashString = Integer.toHexString(0xFF & b);
                if (hashString.length() < 2) hashString = "0" + hashString;
                hexString.append(hashString);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.warn("Unable to find {} Algorithm", hashAlgo , e);
        }

        return null;
    }

    /**
     * We convert the source to a hex string that can handle CR, LF, and other
     * characters and symbols that would otherwise break parsing of the
     * xauth.secrets file. This has the added benefit that the plaintext
     * passwords aren't directly visible in the file.
     * 
     * @param source
     *        The string to hexify
     * @return The string in hex format (ie: AABBCCDD)
     */
    private String stringHexify(String source)
    {
        StringBuilder secbuff = new StringBuilder();
        Formatter secform = null;
        try {
            secform = new Formatter(secbuff);
            int val = 0;

            for (int l = 0; l < source.length(); l++) {
                // get the char as an integer and mask the sign bit
                // so we get character values between 0 and 255
                val = (source.charAt(l) & 0xff);
                secform.format("%02X", val);
            }
        } catch (Exception e) {
            logger.warn("Unable to access formatter", e);
        } finally {
            if (secform != null) {
                try {
                    secform.close();
                } catch (FormatterClosedException ex) {
                    logger.error("Unable to close formatter", ex);
                }
            }
        }

        return (secbuff.toString());
    }

/**
 * verify users TOTP code.
 *
 * @param secret
 *        Shared secret
 * @param code
 *        User provided code
 * @param timeslot
 *        Current time slot. epoch seconds divided by time delta.
 *
 * @return true if code is good, otherwise false.
 */
    private boolean verifyTOTPcode(String secret, long code, long timeslot)
    {
        Base32 codec = new Base32();
        byte[] data = new byte[8];
        long value = timeslot;
        byte[] decodedKey = codec.decode(secret);
        byte[] hash;

        for (int i = 8; i-- > 0; value >>>= 8) {
            data[i] = (byte) value;
        }
        try {
            SecretKeySpec signKey = new SecretKeySpec(decodedKey, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signKey);
            hash = mac.doFinal(data);
        } catch(Exception e) {
            logger.warn("Not able to validate TOTP (ignoring) :", e);
            return false;
        }
 
        int offset = hash[20 - 1] & 0xF;
   
        long truncatedHash = 0;
        for (int i = 0; i < 4; ++i) {
            truncatedHash <<= 8;
            // We are dealing with signed bytes:
            // we just keep the first byte.
            truncatedHash |= (hash[offset + i] & 0xFF);
        }

        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= 1000000;

        return (truncatedHash == code);
    }
}
