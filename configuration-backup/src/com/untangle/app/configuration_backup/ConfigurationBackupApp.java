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
package com.untangle.app.configuration_backup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ExecManagerResultReader;
import com.untangle.uvm.app.License;
import com.untangle.uvm.app.DirectoryConnector;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.util.I18nUtil;

/**
 * Configuration backup application
 */
public class ConfigurationBackupApp extends AppBase
{
    private final Logger logger = Logger.getLogger(ConfigurationBackupApp.class);

    private static final String CRON_STRING = "* * * root /usr/share/untangle/bin/configuration-backup-send-backup.py >/dev/null 2>&1";
    private static final File CRON_FILE = new File("/etc/cron.d/untangle-configuration-backup-nightly");
    
    private static final String BACKUP_URL = "https://boxbackup.untangle.com/boxbackup/backup.php";
    private static final String TIMEOUT_SEC = "1200";

    private final PipelineConnector[] connectors = new PipelineConnector[] { };

    private ConfigurationBackupSettings settings = null;

    /**
     * Initialize Configuration backup application
     * 
     * @param appSettings
     *  Application settings
     * @param appProperties
     *  Application properties
     */
    public ConfigurationBackupApp( com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties )
    {
        super( appSettings, appProperties );
    }

    /**
     * Return current state of settings
     *
     * @return
     *  Configuration backup settings.
     */
    public ConfigurationBackupSettings getSettings()
    {
        return this.settings;
    }

    /**
     * Write new settings.
     *
     * @param newSettings
     *  New configuration backup settings.
     */
    public void setSettings(final ConfigurationBackupSettings newSettings)
    {
        String appID = this.getAppSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/configuration-backup/settings_" + appID + ".js";

        try {
            UvmContextFactory.context().settingsManager().save( settingsFile, newSettings );
        } catch (Exception exn) {
            logger.error("Could not save ConfigurationBackup settings", exn);
            return;
        }

        this.settings = newSettings;

        writeCronFile();
    }

    /**
     * Get the pineliene connector(???)
     *
     * @return PipelineConector
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Initialize settings to defaults.
     */
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

    /**
     * Perform backup
     */
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
        DirectoryConnector directoryConnector = (DirectoryConnector)UvmContextFactory.context().appManager().app("directory-connector");
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
    
    /**
     * Peform post initializaion
     */
    @Override
    protected void postInit()
    {
        String appID = this.getAppSettings().getId().toString();
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/configuration-backup/settings_" + appID + ".js";

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
            logger.error("Could not apply app settings",exn);
        }

        /* if settings are newer than cron file, rewrite cron file */
        File settingsFile = new File( settingsFileName );
        if (settingsFile.lastModified() > CRON_FILE.lastModified())
            writeCronFile();
    }

    /**
     * Pre configuration backup start.
     * Check license.
     *
     * @param isPermanentTransition
     *  If true, the app is permenant
     */
    @Override
    protected void preStart( boolean isPermanentTransition )
    {
        if ( ! isLicenseValid() ) {
            throw new RuntimeException( "invalid license" );
        }

        writeCronFile();
    }

    /**
     * Post configuration backup stop.
     * If being removed, remove cron job.
     *
     * @param isPermanentTransition
     *  If true, the app is permenant
     */
    @Override
    protected void postStop( boolean isPermanentTransition )
    {
        // if this is being permanently disable - stop cron job
        if ( isPermanentTransition )
            UvmContextFactory.context().execManager().execResult("/bin/rm -f " + CRON_FILE);
    }

    /**
     * After stopping, remove the cron job.
     */
    @Override
    protected void postDestroy()
    {
        UvmContextFactory.context().execManager().execResult("/bin/rm -f " + CRON_FILE);
    }

    /**
     * Upload the backup to untangle.com
     *
     * @param backupFile
     *  Handle of file to backup.
     */
    private void uploadBackup( File backupFile )
    {
            
        String[] cmd = new String[]{System.getProperty("uvm.bin.dir") + "/configuration-backup-upload-backup.sh",
                                    "-u",BACKUP_URL,
                                    "-v",
                                    "-k",UvmContextFactory.context().getServerUID(),
                                    "-t",TIMEOUT_SEC,
                                    "-f",backupFile.getAbsoluteFile().toString()};

        logger.info("Backing up " + backupFile.getAbsoluteFile() + " to " + BACKUP_URL);
        logger.info("Backup command: " + cmd);

        Integer exitCode = 0;
        try {
            ExecManagerResultReader reader = UvmContextFactory.context().execManager().execEvil(cmd);
            exitCode = reader.waitFor();
        } catch (Exception e) {
            exitCode = 99;
            logger.warn("Exception running backup",e);
        }

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
            case 99:
                reason = "Exception during backup " + BACKUP_URL;
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
     * Upload the backup to Google drive.
     *
     * @param backupFile
     *  Handle of file to backup.
     */
    private void uploadBackupToGoogleDrive( File backupFile )
    {
        String directory = null;
        String[] cmd;
        if ( settings.getGoogleDriveDirectory() == null || "".equals(settings.getGoogleDriveDirectory()) )
            cmd = new String[]{"/usr/share/untangle-google-connector/bin/google-drive-upload.py",
                               backupFile.getAbsoluteFile().toString()};
        else
            cmd = new String[]{"/usr/share/untangle-google-connector/bin/google-drive-upload.py",
                               "-d",settings.getGoogleDriveDirectory(),
                               backupFile.getAbsoluteFile().toString()};
        Integer exitCode = 0;
        try {
            ExecManagerResultReader reader = UvmContextFactory.context().execManager().execEvil(cmd);
            exitCode = reader.waitFor();
        } catch (Exception e) {
            exitCode = 99;
            logger.warn("Exception running backup",e);
        }
        
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

    /**
     * Write the cronjob file
     */    
    private void writeCronFile()
    {
        // write the cron file for nightly runs
        String conf = settings.getMinuteInHour() + " " + settings.getHourInDay() + " " + CRON_STRING;
        BufferedWriter cronBufferedWriter = null;
        FileWriter cronFileWriter = null;
        try{
            cronFileWriter = new FileWriter(CRON_FILE);
            cronBufferedWriter = new BufferedWriter(cronFileWriter);
            cronBufferedWriter.write(conf, 0, conf.length());
            cronBufferedWriter.write("\n");
        } catch (IOException ex) {
            logger.error("Unable to write file", ex);
        }finally{
            try{
                if(cronBufferedWriter != null){
                    cronBufferedWriter.close();
                }
                if(cronFileWriter != null){
                    cronFileWriter.close();
                }
            }catch(IOException ex){
                logger.error("Unable to close file", ex);
            }
        }
    }
}
