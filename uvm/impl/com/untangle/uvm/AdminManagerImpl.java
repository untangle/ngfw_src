/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.commons.io.FileUtils;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.AdminManager;
import com.untangle.uvm.AdminSettings;
import com.untangle.uvm.AdminUserSettings;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.event.AdminLoginEvent;

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
    
    private final Logger logger = LogManager.getLogger(this.getClass());

    private AdminSettings settings;

    /**
     * Setup admin manager.
     *
     */
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
            newSettings.setVersion(3L); /* version 3 as of v16.0 */
            newSettings.addUser(new AdminUserSettings(INITIAL_USER_LOGIN, INITIAL_USER_PASSWORD, INITIAL_USER_DESCRIPTION, ""));

            // pass the settings to the OEM override function and use the override settings
            AdminSettings overrideSettings = (AdminSettings)UvmContextFactory.context().oemManager().applyOemOverrides(newSettings);
            this.setSettings(overrideSettings);
        }
        else {
            logger.debug("Loading Settings...");

            this.settings = readSettings;

            if (this.settings.getVersion() == 2) {
                /*
                 * If we just loaded version 2 settings, doing a setSettings will blank the passwordHashBase64
                 * field for any user that already has passwordHashShadow defined (which should just be the
                 * 'admin' user.
                 */
                this.settings.setVersion(3L);
                this.setSettings(this.settings);
            }

            logger.debug("Settings: " + this.settings.toJSONString());
        }

        /**
         * If the settings file date is newer than the system files, re-sync them
         */
        if ( ! UvmContextFactory.context().isDevel() ) {
            File settingsFile = new File( settingsFileName );
            File shadowFile = new File("/etc/shadow");
            if (settingsFile.lastModified() > shadowFile.lastModified() ) {
                setRootPasswordAndShadowHash( this.settings );
            }
        }

        logger.info("Initialized AdminManager");
    }

    /**
     * Return administrator settings.
     * 
     * @return
     *     AdminSettings object.
     */
    public AdminSettings getSettings()
    {
        return this.settings;
    }

    /**
     * Write administrator settings.
     * 
     * @param newSettings
     *     AdminSettings object.
     */
    public void setSettings( final AdminSettings newSettings )
    {
        this.setSettings( newSettings, true );
    }

    /**
     * Write administrator settings.
     * 
     * @param newSettings
     *     AdminSettings object.
     * @param setRootPassword
     *     boolean.  If true, set the root password from these settings.  
     */
    private void setSettings( final AdminSettings newSettings, boolean setRootPassword )
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

        if ( setRootPassword )
            setRootPasswordAndShadowHash( this.settings );
    }
    
    /**
     * Return system version.
     * 
     * @return
     *     String of the ngfw version.  If developer system, this will be "DEVEL_VERSION".
     */
    public String getFullVersionAndRevision()
    {
        if (UvmContextFactory.context().isDevel())
            return "DEVEL-VERSION";

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

    /**
     * Return whether sytem has been modified from command line.
     * 
     * @return
     *     String of the following values:
     *     none         No history file exists.
     *     blessed      History exists but has been approved.
     *     yes (count)  History file exists and the count of imtems in it.
     */
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

        List<?> zshContent; // List<String>
        try {
            String encoding = null;
            zshContent = (List<?>) FileUtils.readLines(zshHistoryFile, encoding);
        } catch (IOException e) {
            return "UNKNOWN";
        }

        return new String("yes (" + zshContent.size() + ")");
    }

    /**
     * Return number of reboots and crashes
     * 
     * @return
     *     String of the format "reboot_count (crash_count)"
     */
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
    
    /**
     * Return Linux kernel version.
     * 
     * @return
     *     String of the Linux kernel version.
     */
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
    
    /**
     * Return email address of administrator (admin) user.
     * 
     * @return
     *     String of email address.
     */
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

    /**
     * Return true if any admin users have weak password hashes
     *
     * @return
     *     true if any users have weak password hashes, false otherwise
     */
    public boolean getWeakPasswordHashes()
    {
        AdminSettings adminSettings = getSettings();

        if(adminSettings == null)
            return false;

        for ( AdminUserSettings user : adminSettings.getUsers() ) {
            String passwordHashBase64 = user.getPasswordHashBase64();
            if ( passwordHashBase64 != null && !"".equals(passwordHashBase64) )
                return true;
        }

        return false;
    }

    /**
     * Send adminstrator login result to event log.
     * 
     * @param login
     *        String username
     * @param local
     *        boolean.  If true, from 127.0.0.0, false otherwise.
     * @param clientAddress 
     *        Inet address.  Client login address.
     * @param succeeded
     *        boolean.  If true, login was succesful.  Otherwise false.
     * @param reason
     *        String.  Reason for login failure.
     */
    public void logAdminLoginEvent( String login, boolean local, InetAddress clientAddress, boolean succeeded, String reason )
    {
        AdminLoginEvent loginEvent = new AdminLoginEvent( login, local, clientAddress, succeeded, reason );
        UvmContextImpl.context().logEvent( loginEvent );
    }
    
    /**
     * This sets the root password in /etc/shadow
     * and also sets the 'passwordHashShadow' value in the settings to the same hash
     *
     * @param settings
     *        AdminSettings object containing administrator account to pull password.
     */
    private void setRootPasswordAndShadowHash( AdminSettings settings )
    {
        AdminUserSettings admin = null;

        for ( AdminUserSettings user : settings.getUsers() ) {
            if ( "admin".equals( user.getUsername() ) ) {
                admin = user;
                break;
            }
        }

        if ( admin == null ) {
            logger.warn("No \"admin\" account - not setting root password");
            return;
        }
 
            
        String pass = admin.trans_getPassword();
        String passwordHashShadow = admin.getPasswordHashShadow();
        if ( pass != null ) {
            logger.info("Setting root password.");
                    
            ExecManagerResult result = UvmContextImpl.context().execManager().exec( "usermod -p '" + passwordHashShadow + "' root" );
                    
            int exitCode = result.getResult();
            if ( exitCode != 0 ) {
                logger.warn( "Setting root password returned non-zero exit code: " + exitCode );
            }

            String shadowHash = UvmContextImpl.context().execManager().execOutput("awk -F: '/root/ {print $2}' /etc/shadow");
            admin.setPasswordHashShadow( shadowHash.trim() );
            setSettings( settings, false ); // save settings but do not set root password again

        } else {
            if ( passwordHashShadow == null ) {
                passwordHashShadow = "!";
            }
            ExecManagerResult result = UvmContextImpl.context().execManager().exec( "usermod -p '" + passwordHashShadow + "' root" );
            int exitCode = result.getResult();
            if ( exitCode != 0 ) {
                logger.warn( "Setting root password returned non-zero exit code: " + exitCode );
            }
        }
    }
}
