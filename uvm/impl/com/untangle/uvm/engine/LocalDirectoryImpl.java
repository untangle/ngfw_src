/*
 * $Id: LocalDirectoryImpl.java,v 1.00 2011/08/11 13:35:15 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.util.LinkedList;
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
    private static final String LOCAL_DIRECTORY_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/untangle-vm-convert-local-directory.py";

    private LinkedList<LocalDirectoryUser> currentList;
    
    public LocalDirectoryImpl()
    {
        loadUsersList();
    }
    
    public boolean authenticate(String uid, String pwd)
    {
        /* FIXME */
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
        this.currentList = users;
        saveUsersList();
    }
    
    private void saveUsersList()
    {
        SettingsManager settingsManager = LocalUvmContextFactory.context().settingsManager();

        if (this.currentList == null)
            this.currentList = new LinkedList<LocalDirectoryUser>();

        /**
         * Remove cleartext password before saving
         */
        for (LocalDirectoryUser user : this.currentList) {
            user.removeCleartextPassword();
        }
        
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
        }

        // Still no settings, just initialize new ones
        if (users == null) {
            logger.warn("No settings found - Initializing new settings.");
            this.currentList = new LinkedList<LocalDirectoryUser>();
            saveUsersList();
        } else {
            this.currentList = users;
        }
    }
    
}
