/*
 * $Id: LocalDirectoryImpl.java,v 1.00 2011/08/11 13:35:15 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.util.LinkedList;
import java.util.HashSet;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.LocalDirectory;
import com.untangle.uvm.LocalDirectoryUser;

/**
 * Local Directory stores a local list of users
 */
class LocalDirectoryImpl implements LocalDirectory
{
    private final Logger logger = Logger.getLogger(getClass());

    private final static String LOCAL_DIRECTORY_SETTINGS_FILE = System.getProperty( "uvm.settings.dir" ) + "/untangle-vm/local_directory";

    private final static String UNCHANGED_PASSWORD = "***UNCHANGED***";
    
    private LinkedList<LocalDirectoryUser> currentList;
    
    public LocalDirectoryImpl()
    {
        loadUsersList();
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
                if (password.equals(user.getPassword()))
                    return true;
                String base64 = calculateBase64Hash(password);
                if (base64 != null && base64.equals(user.getPasswordBase64Hash()))
                    return true;
                String md5 = calculateMd5Hash(password);
                if (md5 != null && md5.equals(user.getPasswordMd5Hash()))
                    return true;
                String sha = calculateShaHash(password);
                if (sha != null && sha.equals(user.getPasswordShaHash()))
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
    
    private void saveUsersList(LinkedList<LocalDirectoryUser> list)
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

        return;
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
