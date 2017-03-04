/**
 * $Id: FacebookAuthenticator.java,v 1.00 2017/03/03 19:30:10 dmorris Exp $
 *
 * Copyright (c) 2003-2017 Untangle, Inc.
 *
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Untangle.
 */
package com.untangle.node.configuration_backup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.DirectoryConnector;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.util.I18nUtil;

public class ConfigurationBackupApp extends NodeBase
{
    private final Logger logger = Logger.getLogger(ConfigurationBackupApp.class);

    private static final String CRON_STRING = "* * * root /usr/share/untangle/bin/configuration-backup-send-backup.py >/dev/null 2>&1";
    private static final File CRON_FILE = new File("/etc/cron.d/untangle-configuration-backup-nightly");
    
    private static final String BACKUP_URL = "https://boxbackup.untangle.com/boxbackup/backup.php";

    private final PipelineConnector[] connectors = new PipelineConnector[] { };

    private ConfigurationBackupSettings settings = null;

    public ConfigurationBackupApp( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );
    }

    public ConfigurationBackupSettings getSettings()
    {
        return this.settings;
    }

    public void setSettings(final ConfigurationBackupSettings settings)
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-node-configuration-backup/settings_" + nodeID + ".js";

        this.settings = settings;

        try {
            UvmContextFactory.context().settingsManager().save( settingsFile, settings );
        } catch (Exception exn) {
            logger.error("Could not save ConfigurationBackup settings", exn);
            return;
        }

        writeCronFile();
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    @Override
    public void initializeSettings()
    {
        ConfigurationBackupSettings settings = new ConfigurationBackupSettings();
        logger.info("Initializing Settings...");

        //Doesn't really matter when the backup takes place, but we
        //want it to be random so all customers do not post-back
        //their data files concurrently.
        java.util.Random r = new java.util.Random();
        settings.setHourInDay(r.nextInt(24));
        settings.setMinuteInHour(r.nextInt(60));

        setSettings( settings );
    }

    public void sendBackup()
    {
        if ( !isLicenseValid()) {
            this.logEvent(new ConfigurationBackupEvent(false, "No valid license.", "" ));
            return;
        }

        File backupFile = UvmContextFactory.context().backupManager().createBackup();

        if ( backupFile == null ) {
            logger.warn("Failed to create backup file!");
            return;
        }

        /**
         * Upload to untangle.com
         */
        uploadBackup( backupFile );

        /**
         * If google drive is enabled and licensed and configured
         * upload to google drive
         */
        DirectoryConnector directoryConnector = (DirectoryConnector)UvmContextFactory.context().nodeManager().node("untangle-node-directory-connector");
        if ( settings.getGoogleDriveEnabled() &&
             UvmContextFactory.context().licenseManager().isLicenseValid(License.DIRECTORY_CONNECTOR) &&
             directoryConnector != null &&
             directoryConnector.isGoogleDriveConnected()) {
            uploadBackupToGoogleDrive( backupFile );
        }
        
        try {
            backupFile.delete();
        } catch (Exception e) {
            logger.warn("Failed to delete backup file",e);
        }
    }
    
    @Override
    protected void postInit()
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-configuration-backup/settings_" + nodeID + ".js";

        ConfigurationBackupSettings readSettings = null;
        logger.info("Loading settings from " + settingsFileName );

        try {
            // first we try to read our json settings
            readSettings = UvmContextFactory.context().settingsManager().load( ConfigurationBackupSettings.class, settingsFileName );
        } catch (Exception exn) {
            logger.error("postInit()",exn);
        }

        try {
            // no settings found so init with defaults
            if (readSettings == null) {
                logger.warn("Initializing with default settings");
                initializeSettings();
            }

            // otherwise apply the loaded or imported settings from the file
            else {
                logger.info("Loaded settings from " + settingsFileName);
                this.settings = readSettings;
            }
        } catch (Exception exn) {
            logger.error("Could not apply node settings",exn);
        }

        /* if settings are newer than cron file, rewrite cron file */
        File settingsFile = new File( settingsFileName );
        if (settingsFile.lastModified() > CRON_FILE.lastModified())
            writeCronFile();
    }

    @Override
    protected void preStart( boolean isPermanentTransition )
    {
        if ( ! isLicenseValid() ) {
            throw new RuntimeException( "invalid license" );
        }

        writeCronFile();
    }

    @Override
    protected void postStop( boolean isPermanentTransition )
    {
        // if this is being permanently disable - stop cron job
        if ( isPermanentTransition )
            UvmContextFactory.context().execManager().execResult("/bin/rm -f " + CRON_FILE);
    }

    @Override
    protected void postDestroy()
    {
        UvmContextFactory.context().execManager().execResult("/bin/rm -f " + CRON_FILE);
    }

    /**
     * upload the backup to untangle.com
     */
    private void uploadBackup( File backupFile )
    {
            
        String cmd = System.getProperty("uvm.bin.dir") + "/configuration-backup-upload-backup.sh" + " -u " + BACKUP_URL + " -v -k " + UvmContextFactory.context().getServerUID() + " -t 180 " + " -f " + backupFile.getAbsoluteFile();

        logger.info("Backing up " + backupFile.getAbsoluteFile() + " to " + BACKUP_URL);
        logger.info("Backup command: " + cmd);
        
        Integer exitCode = UvmContextFactory.context().execManager().execResult(cmd);

        if(exitCode != 0) {
            logger.error("Backup returned non-zero error code (" + exitCode + ")");

            String reason = null;
            switch(exitCode) {
            case 1:
                reason = "Error in arguments";
                break;
            case 2:
                reason = "Error from remote server " + BACKUP_URL;
                break;
            case 3:
                reason = "Permission problem with remote server " + BACKUP_URL;
                break;
            case 4:
                reason = "Unable to contact " + BACKUP_URL;
                break;
            case 5:
                reason = "Timeout contacting " + BACKUP_URL;
                break;
            default:
                reason = "Unknown error";
            }
            logger.info("Backup failed: " + reason);
            this.logEvent( new ConfigurationBackupEvent(false, reason, I18nUtil.marktr("My Account")) );
        }
        else {
            logger.info("Backup successful.");
            this.logEvent( new ConfigurationBackupEvent(true, I18nUtil.marktr("Successfully uploaded."), I18nUtil.marktr("My Account")) );
        }
    }

    /**
     * upload the backup to untangle.com
     */
    private void uploadBackupToGoogleDrive( File backupFile )
    {
        String directory = null;
        if ( settings.getGoogleDriveDirectory() == null || "".equals(settings.getGoogleDriveDirectory()) )
            directory = "";
        else
            directory = "-d \"" + settings.getGoogleDriveDirectory() + "\"";
        String cmd = "/usr/share/untangle-google-connector/bin/google-drive-upload.py " + directory + " " + backupFile.getAbsoluteFile();

        logger.info("Backing up " + backupFile.getAbsoluteFile() + " to Google Drive");
        logger.info("Backup command: " + cmd);
        
        Integer exitCode = UvmContextFactory.context().execManager().execResult(cmd);

        if(exitCode != 0) {
            logger.error("Backup returned non-zero error code (" + exitCode + ")");

            String reason = null;
            switch(exitCode) {
            default:
                reason = "Unknown error";
            }
            logger.info("Backup failed: " + reason);
            this.logEvent( new ConfigurationBackupEvent(false, reason, I18nUtil.marktr("Google Drive")) );
        }
        else {
            logger.info("Backup successful.");
            this.logEvent( new ConfigurationBackupEvent(true, I18nUtil.marktr("Successfully uploaded."), I18nUtil.marktr("Google Drive")) );
        }
    }
    
    private void writeCronFile()
    {
        // write the cron file for nightly runs
        String conf = settings.getMinuteInHour() + " " + settings.getHourInDay() + " " + CRON_STRING;
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(CRON_FILE));
            out.write(conf, 0, conf.length());
            out.write("\n");
        } catch (IOException ex) {
            logger.error("Unable to write file", ex);
            return;
        }
        try {
            out.close();
        } catch (IOException ex) {
            logger.error("Unable to close file", ex);
            return;
        }
    }

    private boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.CONFIGURATION_BACKUP))
            return true;
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.CONFIGURATION_BACKUP_OLDNAME))
            return true;
        return false;
    }
}
