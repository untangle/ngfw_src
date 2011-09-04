/*
 * $Id: LocalDirectoryImpl.java,v 1.00 2011/08/11 13:35:15 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.util.LinkedList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.LocalDirectory;
import com.untangle.uvm.LocalDirectoryUser;
import com.untangle.node.util.SimpleExec;

/**
 * Local Directory stores a local list of users
 */
class LocalDirectoryImpl implements LocalDirectory
{
    private final Logger logger = Logger.getLogger(getClass());

    private final static String LOCAL_DIRECTORY_SETTINGS_FILE = System.getProperty( "uvm.settings.dir" ) + "/untangle-vm/local_directory";
    private final static String LOCAL_DIRECTORY_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/untangle-vm-convert-local-directory.py";

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
        }
        if ( password == null ) {
            logger.warn("Invalid arguments: password is null");
        }
            
        for (LocalDirectoryUser user : this.currentList) {
            if (username.equals(user.getUsername())) {
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
        /**
         * Remove cleartext password before saving
         */
        for (LocalDirectoryUser user : users) {
            /**
             * If password hasn't change - copy the hashes from the previous settings
             * We must do this because the UI does not send the same hashes back
             */
            if (UNCHANGED_PASSWORD.equals(user.getPassword())) {
                for (LocalDirectoryUser currentUser : this.currentList) {
                    if (currentUser.equals(user)) {
                        user.setPasswordShaHash(currentUser.getPasswordShaHash());
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

        this.currentList = users;
        saveUsersList();
    }
    
    private void saveUsersList()
    {
        SettingsManager settingsManager = LocalUvmContextFactory.context().settingsManager();

        if (this.currentList == null)
            this.currentList = new LinkedList<LocalDirectoryUser>();

        try {
            settingsManager.save(LinkedList.class, LOCAL_DIRECTORY_SETTINGS_FILE, this.currentList);
        } catch (SettingsManager.SettingsException e) {
            logger.error("Unable to save localDirectory file: ", e );
        }

        return;
    }
        
    @SuppressWarnings("unchecked")
    private void loadUsersList()
    {
        SettingsManager settingsManager = LocalUvmContextFactory.context().settingsManager();
        LinkedList<LocalDirectoryUser> users = null;
        
        // Read the settings from file
        try {
            users = (LinkedList<LocalDirectoryUser>) settingsManager.load(LinkedList.class, LOCAL_DIRECTORY_SETTINGS_FILE);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Unable to read localDirectory file: ", e );
        }

        // Failed to read settings - try the conversion script
        if (users == null) {
            logger.warn("No settings found - Running conversion script to check DB");
            try {
                SimpleExec.SimpleExecResult result = null;
                logger.warn("Running: " + LOCAL_DIRECTORY_CONVERSION_SCRIPT + " " + LOCAL_DIRECTORY_SETTINGS_FILE + ".js");
                result = SimpleExec.exec( LOCAL_DIRECTORY_CONVERSION_SCRIPT, new String[] { LOCAL_DIRECTORY_SETTINGS_FILE + ".js"}, null, null, true, true, 1000*60, logger, true);
            } catch ( Exception e ) {
                logger.warn( "Conversion script failed.", e );
            } 

            try {
                users = (LinkedList<LocalDirectoryUser>) settingsManager.load(LinkedList.class, LOCAL_DIRECTORY_SETTINGS_FILE);
            } catch (SettingsManager.SettingsException e) {
                logger.warn("Unable to read localDirectory file: ", e );
            }

            // Call the save function to calculate the hashes
            if (users != null)
                this.setUsers(users);
        }

        // Still no settings, just initialize new ones
        if (users == null) {
            logger.warn("No settings found - Initializing new settings.");
            this.currentList = new LinkedList<LocalDirectoryUser>();
            this.saveUsersList();
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
