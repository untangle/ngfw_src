/*
 * $Id: LocalDirectoryImpl.java,v 1.00 2011/08/11 13:35:15 dmorris Exp $
 */
package com.untangle.uvm.engine;

import com.untangle.uvm.LocalDirectory;
import com.untangle.uvm.LocalDirectoryUser;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Local Directory stores a local list of users
 */
public class LocalDirectoryImpl implements LocalDirectory
{
    private final Logger logger = Logger.getLogger(getClass());

    private final static String LOCAL_DIRECTORY_SETTINGS_FILE = System.getProperty( "uvm.settings.dir" ) + "/untangle-vm/local_directory";

    private final static String UNCHANGED_PASSWORD = "***UNCHANGED***";
    
    private LinkedList<LocalDirectoryUser> currentList;
    
    public LocalDirectoryImpl()
    {
        loadUsersList();
    }


    private boolean accountExpired(LocalDirectoryUser user)
    {
        return user.getExpirationTime() > 0 && System.currentTimeMillis() >= user.getExpirationTime();
    }

    public boolean authenticate( String username, String password )
    {
        if ( username == null ) {
            logger.warn("Invalid arguments: username is null");
            return false;
        }
        if ( password == null ) {
            logger.warn("Invalid arguments: password is null or empty");
            return false;
        }
        if ( "".equals(password) ) {
            logger.info("Blank passwords not allowed");
            return false;
        }

        for (LocalDirectoryUser user : this.currentList) {
            if (username.equals(user.getUsername())) {
                if (password.equals(user.getPassword()) && !accountExpired(user))
                    return true;
                String base64 = calculateBase64Hash(password);
                if (base64 != null && base64.equals(user.getPasswordBase64Hash()) && !accountExpired(user))
                    return true;
                String md5 = calculateMd5Hash(password);
                if (md5 != null && md5.equals(user.getPasswordMd5Hash()) && !accountExpired(user))
                    return true;
                String sha = calculateShaHash(password);
                if (sha != null && sha.equals(user.getPasswordShaHash()) && !accountExpired(user))
                    return true;
            }
        }

        return false;
    }

    public LinkedList<LocalDirectoryUser> getUsers()
    {
        if (this.currentList == null)
            return new LinkedList<LocalDirectoryUser>();

        return this.currentList;
    }

    public void setUsers(LinkedList<LocalDirectoryUser> users)
    {
        HashSet<String> usersSeen = new HashSet<String>();
        
        /**
         * Remove cleartext password before saving
         */
        for (LocalDirectoryUser user : users) {

            /**
             * Check for dupes
             */
            if (usersSeen.contains(user.getUsername()))
                throw new RuntimeException("Duplicate user: " + user.getUsername());
            else
                usersSeen.add(user.getUsername());

            /**
             * If password hasn't changed - copy the hashes from the previous settings
             * We must do this because the UI does not send the same hashes back
             */
            if (UNCHANGED_PASSWORD.equals(user.getPassword())) {
                if (this.currentList != null) {
                    for (LocalDirectoryUser currentUser : this.currentList) {
                        if (currentUser.equals(user)) {
                            user.setPasswordShaHash( currentUser.getPasswordShaHash());
                            user.setPasswordMd5Hash( currentUser.getPasswordMd5Hash());
                            user.setPasswordBase64Hash( currentUser.getPasswordBase64Hash());
                        }
                    }
                }
            }
            /**
             * Otherwise the password has changed and we must recalculate the hashes
             */
            else {
                user.setPasswordShaHash(calculateShaHash(user.getPassword()));
                user.setPasswordMd5Hash(calculateMd5Hash(user.getPassword()));
                user.setPasswordBase64Hash(calculateBase64Hash(user.getPassword()));
            }

            /* clear the cleartext before saving */
            user.removeCleartextPassword();
        }

        saveUsersList(users);
    }

    public void addUser(LocalDirectoryUser user)
    {
        LinkedList<LocalDirectoryUser> users = this.getUsers();
        user.setPasswordShaHash(calculateShaHash(user.getPassword()));
        user.setPasswordMd5Hash(calculateMd5Hash(user.getPassword()));
        user.setPasswordBase64Hash(calculateBase64Hash(user.getPassword()));
        user.removeCleartextPassword();
        users.add(user);
        this.saveUsersList(users);
    }

    private boolean userNameMatch(LocalDirectoryUser u1, LocalDirectoryUser u2)
    {
        return u1.getUsername() != null && u1.getUsername().equals(u2.getUsername());
    }

    private boolean emailMatch(LocalDirectoryUser u1, LocalDirectoryUser u2)
    {
        return u1.getEmail() != null && u1.getEmail().equals(u2.getEmail());
    }


    public boolean userExists(LocalDirectoryUser user)
    {
        LinkedList<LocalDirectoryUser> users = this.getUsers();
        for (LocalDirectoryUser u: users) {
            if ( userNameMatch(u, user) || emailMatch(u, user)) {
                return true;
            }
        }
        return false;
    }

    public boolean updateUser(LocalDirectoryUser user)
    {
        LinkedList<LocalDirectoryUser> users = this.getUsers();
        boolean retVal = false;
        for (LocalDirectoryUser u: users) {
            if ( userNameMatch(u, user) || emailMatch( u, user)) {
                if ( user.getPassword() != null && !user.getPassword().equals( UNCHANGED_PASSWORD )) {
                    u.setPasswordShaHash(calculateShaHash(user.getPassword()));
                    u.setPasswordMd5Hash(calculateMd5Hash(user.getPassword()));
                    u.setPasswordBase64Hash(calculateBase64Hash(user.getPassword()));
                    u.removeCleartextPassword();
                }
                if ( user.getFirstName() != null && user.getFirstName().trim().length() > 0 ) {
                    u.setFirstName( user.getFirstName());
                }
                if ( user.getLastName() != null && user.getLastName().trim().length() > 0 ) {
                    u.setLastName( user.getLastName());
                }
                if ( user.getExpirationTime() > 0) {
                    u.setExpirationTime(user.getExpirationTime());
                }
                retVal=true;
                break;
            }
        }
        if ( retVal) {
            this.saveUsersList(users);
        }
        return retVal;
    }


    public boolean userExpired(LocalDirectoryUser user)
    {
        LinkedList<LocalDirectoryUser> users = this.getUsers();
        for (LocalDirectoryUser u: users) {
            if ( userNameMatch(u, user) || emailMatch( u, user)) {
                if (accountExpired(u)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void cleanupExpiredUsers()
    {
        Iterator<LocalDirectoryUser> iter = this.getUsers().iterator();
        boolean entriesDeleted = false;
        while (iter.hasNext()) {
            LocalDirectoryUser u = iter.next();
            if ( accountExpired(u)) {
                iter.remove();
                entriesDeleted = true;
            }
        }
        if ( entriesDeleted) {
            saveUsersList( this.getUsers());
        }
    }


    
    private synchronized void saveUsersList(LinkedList<LocalDirectoryUser> list)
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();

        if (list == null)
            list = new LinkedList<LocalDirectoryUser>();

        try {
            settingsManager.save(LinkedList.class, LOCAL_DIRECTORY_SETTINGS_FILE, list);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        this.currentList = list;
    }
        
    @SuppressWarnings("unchecked")
    private void loadUsersList()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        LinkedList<LocalDirectoryUser> users = null;
        
        // Read the settings from file
        try {
            users = (LinkedList<LocalDirectoryUser>) settingsManager.load(LinkedList.class, LOCAL_DIRECTORY_SETTINGS_FILE);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Unable to read localDirectory file: ", e );
        }

        // no settings? just initialize new ones
        if (users == null) {
            logger.warn("No settings found - Initializing new settings.");
            this.saveUsersList(new LinkedList<LocalDirectoryUser>());
        }

        this.currentList = users;
    }

    private String calculateShaHash( String password )
    {
        return calculateHash( password, "SHA" );
    }

    private String calculateMd5Hash( String password )
    {
        return calculateHash( password, "MD5" );
    }

    private String calculateBase64Hash( String password )
    {
            String hashString;
            byte[] digest = Base64.encodeBase64(password.getBytes());
            return new String(digest);
    }
    
    private String calculateHash( String password, String hashAlgo)
    {
        try {
            MessageDigest hasher = MessageDigest.getInstance( hashAlgo );
            String hashString;
            hasher.update(password.getBytes());
            byte[] digest = hasher.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < digest.length; i++) {
                hashString = Integer.toHexString(0xFF & digest[i]);
                if (hashString.length() < 2)
                    hashString = "0" + hashString;
                hexString.append(hashString);
            }
            String result = hexString.toString();
            return result;
        } catch (NoSuchAlgorithmException e) {
            logger.warn("Unable to find " + hashAlgo + " Algorithm", e);
        }

        return null;
    }
    
}
