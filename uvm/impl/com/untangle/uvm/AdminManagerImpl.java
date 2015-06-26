/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Set;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.LanguageSettings;
import com.untangle.uvm.AdminManager;
import com.untangle.uvm.AdminSettings;
import com.untangle.uvm.AdminUserSettings;
import com.untangle.uvm.ExecManagerResult;

/**
 * Remote interface for administrative user management.
 */
public class AdminManagerImpl implements AdminManager
{
    private static final String INITIAL_USER_DESCRIPTION = "System Administrator";
    private static final String INITIAL_USER_LOGIN = "admin";
    private static final String INITIAL_USER_PASSWORD = "passwd";

    private static final String KERNEL_VERSION_SCRIPT = "/bin/uname -r";
    private static final String REBOOT_COUNT_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-reboot-count.sh";
    
    private final Logger logger = Logger.getLogger(this.getClass());

    private AdminSettings settings;

    protected AdminManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        AdminSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "admin.js";

        try {
            readSettings = settingsManager.load( AdminSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            AdminSettings newSettings = new AdminSettings();
            newSettings.addUser(new AdminUserSettings(INITIAL_USER_LOGIN, INITIAL_USER_PASSWORD, INITIAL_USER_DESCRIPTION, ""));
            this.setSettings(newSettings);
        }
        else {
            logger.debug("Loading Settings...");

            this.settings = readSettings;
            this.applyToSystem();
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        /**
         * If the settings file date is newer than the system files, re-sync them
         */
        if ( ! UvmContextFactory.context().isDevel() ) {
            File settingsFile = new File( settingsFileName );
            File shadowFile = new File("/etc/shadow");
            if (settingsFile.lastModified() > shadowFile.lastModified() ) {
                applyToSystem();
            }
        }
        
        logger.info("Initialized AdminManager");
    }

    public AdminSettings getSettings()
    {
        return this.settings;
    }

    public void setSettings( final AdminSettings newSettings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "admin.js", newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

        this.applyToSystem();
    }

    public String getFullVersionAndRevision()
    {
        try {
            String version = UvmContextImpl.context().execManager().execOutput("dpkg-query -f '${Version}\\n' -W untangle-vm");

            if (version == null)
                return "";
            else
                return version.replaceAll("(\\r|\\n)", "");
        } catch (Exception e) {
            logger.warn("Unable to fetch version",e);
        }

        /**
         * that method probably timed out
         * fall back to this method
         */
        return UvmContextImpl.context().getFullVersion();
    }

    public String getModificationState()
    {
        File zshHistoryFile = new File("/root/.zsh_history");
        File blessedFile = new File(System.getProperty("uvm.conf.dir") + "/mods-blessed-flag");

        /* if there is no zsh_history file it obviously hasn't been modified */
        if (!zshHistoryFile.exists())
            return "none";

        /* if there is a zsh_history, but the blessed flag is newer these changes have been approved */
        if (blessedFile.exists() && blessedFile.lastModified() > zshHistoryFile.lastModified())
            return "blessed";

        ExecManagerResult result = UvmContextImpl.context().execManager().exec("cat /root/.zsh_history | /usr/bin/wc -l");
        int exitCode = result.getResult();
        String output = result.getOutput();

        output = output.replaceAll("(\\r|\\n)", "");
            
        if( exitCode == 0 ) {
            return new String("yes (" + output + ")");
        }

        return "UNKNOWN";
    }

    public String getRebootCount()
    {
        try {
            String count = UvmContextImpl.context().execManager().execOutput(REBOOT_COUNT_SCRIPT);
        
            if (count == null)
                return "";
            else
                return count.replaceAll("(\\r|\\n)", "");
        } catch (Exception e) {
            logger.warn("Unable to fetch version",e);
        }

        return "Unknown";
    }
    
    public String getKernelVersion()
    {
        try {
            String version = UvmContextImpl.context().execManager().execOutput(KERNEL_VERSION_SCRIPT);
        
            if (version == null)
                return "";
            else
                return version.replaceAll("(\\r|\\n)", "");
        } catch (Exception e) {
            logger.warn("Unable to fetch version",e);
        }

        return "Unknown";
    }
    
    public String getAdminEmail()
    {
        try {
            for ( AdminUserSettings user : getSettings().getUsers() ) {
                if ( "admin".equals( user.getUsername() ) )
                    return user.getEmailAddress();
            }
        } catch ( Exception e ) {
            logger.warn("Failed to find admin email", e);
        }
        return null;
    }
    
    private void applyToSystem() 
    {
        setRootPasswordToAdminPassword();
    }

    private void setRootPasswordToAdminPassword()
    {
        // Set root password to "admin" password
        for ( AdminUserSettings user : this.settings.getUsers() ) {
            if ( "admin".equals( user.getUsername() ) ) {
                String pass = user.trans_getPassword();
                if ( pass != null ) {
                    logger.info("Setting root password");
                    String cmd = "echo 'root:" + pass + "' | sudo chpasswd";
                    
                    // turn down logging so we dont log password
                    UvmContextImpl.context().execManager().setLevel(  org.apache.log4j.Level.DEBUG );

                    ExecManagerResult result = UvmContextImpl.context().execManager().exec( cmd );

                    // turn logging back up
                    UvmContextImpl.context().execManager().setLevel(  org.apache.log4j.Level.INFO );
                    
                    int exitCode = result.getResult();
                    if ( exitCode != 0 ) {
                        logger.warn( "Setting root password returned non-zero exit code: " + exitCode );
                    }
                }
            }
        }
    }
}
