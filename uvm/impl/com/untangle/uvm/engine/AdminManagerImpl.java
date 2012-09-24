/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.LanguageSettings;
import com.untangle.uvm.MailSender;
import com.untangle.uvm.MailSettings;
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

    private static final String SET_TIMEZONE_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-timezone";
    private static final String UVM_VERSION_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-uvm-version.sh";
    private static final String KERNEL_VERSION_SCRIPT = "/bin/uname -r";
    private static final String REBOOT_COUNT_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-reboot-count.sh";
    private static final String TIMEZONE_FILE = "/etc/timezone";
    
    private static final String ALPACA_NONCE_FILE = "/etc/untangle-net-alpaca/nonce";

    private final Logger logger = Logger.getLogger(this.getClass());

    private AdminSettings settings;

    protected AdminManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        AdminSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "admin";

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
            newSettings.addUser(new AdminUserSettings(INITIAL_USER_LOGIN, INITIAL_USER_PASSWORD, INITIAL_USER_DESCRIPTION));
            this.setSettings(newSettings);
        }
        else {
            logger.debug("Loading Settings...");

            this.settings = readSettings;
            this.reconfigure();
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        logger.info("Initialized AdminManager");
    }

    public AdminSettings getSettings()
    {
        return this.settings;
    }

    public void setSettings(final AdminSettings settings)
    {
        this._setSettings( settings );
    }

    @Override
    public TimeZone getTimeZone()
    {
        try {
            BufferedReader in = new BufferedReader(new FileReader(TIMEZONE_FILE));
            String str = in.readLine();
            str = str.trim();
            in.close();
            TimeZone current = TimeZone.getTimeZone(str);
            return current;
        } catch (Exception x) {
            logger.warn("Unable to get timezone, using java default:" , x);
            return TimeZone.getDefault();
        }
    }

    public void setTimeZone(TimeZone timezone)
    {
        String id = timezone.getID();

        Integer exitValue = UvmContextImpl.context().execManager().execResult( SET_TIMEZONE_SCRIPT + " " + id );
        if (0 != exitValue) {
            String message = "Unable to set time zone (" + exitValue + ") to: " + id;
            logger.error(message);
            throw new RuntimeException(message);
        } else {
            logger.info("Time zone set to : " + id);
            TimeZone.setDefault(timezone); // Note: Only works for threads who haven't yet cached the zone!  XX
        }
    }

    public String getDate()
    {
        return (new Date(System.currentTimeMillis())).toString();
    }

    public String getAlpacaNonce()
    {
        BufferedReader stream = null;
        try {
            stream = new BufferedReader(new FileReader(ALPACA_NONCE_FILE));

            String nonce = stream.readLine();
            if (nonce.length() < 3) {
                logger.warn("Invalid nonce in the file ["
                            + ALPACA_NONCE_FILE + "]: ', " +
                            nonce + "'");
                return null;
            }

            return nonce;
        } catch (IOException exn) {
            logger.warn("could not get alpaca nonce", exn);
            return null;
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (IOException e) {
                logger.warn("Unable to close the nonce file: "
                            + ALPACA_NONCE_FILE, e);
            }
        }
    }

    public String getFullVersionAndRevision()
    {
        try {
            String version = UvmContextImpl.context().execManager().execOutput(UVM_VERSION_SCRIPT);

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

    private void _setSettings( AdminSettings newSettings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save(AdminSettings.class, System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "admin", newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

        this.reconfigure();
    }

    private void reconfigure() 
    {
        // If timezone on box is different (example: kernel upgrade), reset it:
        TimeZone currentZone = getTimeZone();
        if (!currentZone.equals(TimeZone.getDefault()))
            try {
                setTimeZone(currentZone);
            } catch (Exception x) {
                // Already logged.
            }
    }

}
