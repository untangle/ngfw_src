/**
 * $Id$
 */

package com.untangle.uvm;

import com.untangle.uvm.LocalDirectory;
import com.untangle.uvm.LocalDirectoryUser;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Formatter;
import java.util.FormatterClosedException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Local Directory stores a local list of users
 */
public class LocalDirectoryImpl implements LocalDirectory
{
    private final static String LOCAL_DIRECTORY_SETTINGS_FILE = System.getProperty("uvm.settings.dir") + "/untangle-vm/local_directory.js";
    private final static String XAUTH_SECRETS_FILE = "/etc/xauth.secrets";
    private final static String IPSEC_RELOAD_SECRETS = "/usr/sbin/ipsec rereadsecrets";

    private final static String FREERADIUS_LOGFILE_SCRIPT = System.getProperty("uvm.home") + "/bin/ut-radius-logfile";
    private final static String FREERADIUS_LOCAL_SECRETS = "/etc/freeradius/3.0/mods-config/files/untangle.local";
    private final static String FREERADIUS_AUTHORIZE = "/etc/freeradius/3.0/mods-config/files/authorize";
    private final static String FREERADIUS_RADIUSD = "/etc/freeradius/3.0/radiusd.conf";

    private final static String UNCHANGED_PASSWORD = "***UNCHANGED***";
    private final static String FILE_DISCLAIMER = "# This file is created and maintained by the Untangle Local Directory.\n" + "# If you modify this file manually, your changes will be overwritten!\n\n";

    private final Logger logger = Logger.getLogger(getClass());

    private LinkedList<LocalDirectoryUser> currentList;
    private NetworkSaveHookCallback networkSaveHookCallback = new NetworkSaveHookCallback();

    /**
     * Constructor
     */
    public LocalDirectoryImpl()
    {
        loadUsersList();

        // install a callback for network settings changes
        UvmContextFactory.context().hookManager().registerCallback(com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, networkSaveHookCallback);
    }

    /**
     * Gets the contents of the radius server log file
     *
     * @return The contents of the radius server log file
     */
    public String getRadiusLogFile()
    {
        logger.debug("getRadiusLogFile()");
        return UvmContextFactory.context().execManager().execOutput(FREERADIUS_LOGFILE_SCRIPT);
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
                // if (password.equals(user.getPassword()) && !accountExpired(user))
                //     return true;
                String base64 = calculateBase64Hash(password);
                if (base64 != null && base64.equals(user.getPasswordBase64Hash()) && !accountExpired(user)) return true;
                String md5 = calculateMd5Hash(password);
                if (md5 != null && md5.equals(user.getPasswordMd5Hash()) && !accountExpired(user)) return true;
                String sha = calculateShaHash(password);
                if (sha != null && sha.equals(user.getPasswordShaHash()) && !accountExpired(user)) return true;
            }
        }

        return false;
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
             * Set the other hashes has changed and we must recalculate the
             */
            String password = getPasswordFromBase64Hash(user.getPasswordBase64Hash());
            user.setPasswordShaHash(calculateShaHash(password));
            user.setPasswordMd5Hash(calculateMd5Hash(password));
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
        user.setPasswordBase64Hash(calculateBase64Hash(user.getPassword()));
        user.setPassword(""); //remove cleartext
        users.add(user);

        this.saveUsersList(users);
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
        updateChapSecrets(list);
        updateRadiusSecrets(list);
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
            this.saveUsersList(new LinkedList<LocalDirectoryUser>());
        }

        // settings loaded so assign to currentList and write IPsec and RADIUS secrets
        else {
            updateXauthSecrets(users);
            updateChapSecrets(users);
            updateRadiusSecrets(users);
            this.currentList = users;
        }
    }

    /**
     * This takes the local directory user information and appends it to
     * /etc/ppp/chap-secrets
     * 
     * The IPsec L2TP feature uses LocalDirectory for login credentials so any
     * time we load or save the list we'll call this function which will export
     * all of the user/pass info to the chap-secrets file
     * 
     * This is necessary because L2TP (xl2tpd) uses pppd to authenticate users
     * So the passwords need to be written to chap-secrets.
     * 
     * XXX: Unfortunately, /etc/ppp/chap-secrets is managed by sync-settings
     * Modifying this file directly outside of sync-settings will break the
     * change detection in sync-settings. sync-settings will write its version,
     * then we will append the info here and then next time sync-settings runs
     * it will see a difference. This will cause unnecssary network restarts if:
     * 1) There is PPPoE on a WAN interface 2) L2TP is enabled 3) Local
     * Directory is non empty
     * 
     * Unfortunately there is no easy fix to this because there is no way to
     * tell xl2tpd/pppd to use a separate secretes file. The solution would be
     * to move local directory into network settings or change sync-settings to
     * read local directory settings (or ideally all settings)
     * 
     * @param list
     *        The list of LocalDirectoryUsers
     */
    private void updateChapSecrets(LinkedList<LocalDirectoryUser> list)
    {
        if (list == null) return;
        if (list.size() == 0) return;
        String chapFile = "/etc/ppp/chap-secrets";
        List<String> chapData;
        FileWriter chap = null;

        try {
            // read all the lines from the existing file
            chapData = Files.readAllLines(Paths.get(chapFile), Charset.forName("UTF-8"));

            // now that we have the contents write to the file without append
            chap = new FileWriter(chapFile, false);

            // start by writing everything except our local directory users to the file 
            for (String line : chapData) {
                String[] fields = line.split("\\s+");
                // if this is one of our user entries just ignore it
                if ((fields.length > 3) && (fields[1].equals("untangle-l2tp"))) continue;
                // not one of ours so write to the file as-is
                chap.write(line + "\n");
            }

            // now append all the username/password pairs to the file
            for (LocalDirectoryUser user : list) {
                if (user.getUsername() == null || user.getPasswordBase64Hash() == null) continue;
                byte[] rawPassword = Base64.decodeBase64(user.getPasswordBase64Hash().getBytes());
                String userPassword = new String(rawPassword);
                chap.write(user.getUsername() + "\t\t");
                chap.write("untangle-l2tp\t\t");
                chap.write("\"" + userPassword + "\"\t\t");
                chap.write("*\n");
            }

            chap.flush();
            chap.close();
        } catch (Exception exn) {
            logger.error("Exception creating L2TP chap-secrets file", exn);
        } finally {
            try {
                chap.close();
            } catch (IOException ex) {
                logger.error("Exception closing L2TP chap-secrets file", ex);
            }
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
                byte[] rawPassword = Base64.decodeBase64(user.getPasswordBase64Hash().getBytes());
                String userPassword = new String(rawPassword);
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
     * We now support WPA Enterprise which allows connecting to a WiFi network
     * using a username and password which is authenticated via RADIUS. To make
     * this work we put all of our credentials in file that will work with
     * freeradius and update the other configuration files required to make this
     * work.
     *
     * @param list
     *        The list of LocalDirectoryUsers
     */
    private void updateRadiusSecrets(LinkedList<LocalDirectoryUser> list)
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
                    byte[] rawPassword = Base64.decodeBase64(user.getPasswordBase64Hash().getBytes());
                    String userPassword = new String(rawPassword);
                    fw.write(user.getUsername() + " Cleartext-Password := \"" + userPassword + "\"\n");
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
         * If server is enabled restart the freeradius service
         */
        if (systemSettings.getRadiusServerEnabled()) {
            UvmContextFactory.context().execManager().exec("systemctl restart freeradius.service");
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
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < digest.length; i++) {
                hashString = Integer.toHexString(0xFF & digest[i]);
                if (hashString.length() < 2) hashString = "0" + hashString;
                hexString.append(hashString);
            }
            String result = hexString.toString();
            return result;
        } catch (NoSuchAlgorithmException e) {
            logger.warn("Unable to find " + hashAlgo + " Algorithm", e);
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
     * This hook is called when network settings are changed This is necessary
     * becaus saving network setttings writes /etc/ppp/chap-secrets and we need
     * to append local directory information onto that file
     */
    private class NetworkSaveHookCallback implements HookCallback
    {
        /**
         * Constructor
         */
        NetworkSaveHookCallback()
        {
        }

        /**
         * Get the name of our callback hook
         * 
         * @return The name
         */
        public String getName()
        {
            return "local-directory-network-settings-change-hook";
        }

        /**
         * Callback function
         * 
         * @param args
         *        Callback arguments
         */
        public void callback(Object... args)
        {
            updateChapSecrets(currentList);
        }
    }
}
