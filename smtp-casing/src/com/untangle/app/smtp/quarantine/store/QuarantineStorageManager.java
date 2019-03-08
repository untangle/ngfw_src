/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine.store;

import java.io.File;

import org.apache.log4j.Logger;

import com.untangle.app.smtp.quarantine.InboxIndex;
import com.untangle.app.smtp.quarantine.InboxRecord;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;

/**
 * Provides static methods for reading/writing quarantine info in json format using SettingsManager
 * 
 */
public class QuarantineStorageManager
{
    private static final Logger logger = Logger.getLogger(QuarantineStorageManager.class);
    private static final String SUMMARY_FILE_NAME = "summary.js";

    /**
     * reads InboxIndex from a given file
     * 
     * @param quarantineFile Quarantinefile.
     * @return InboxIndex for quarantine.
     */
    public static InboxIndex readQuarantineFromFile(String quarantineFile)
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        InboxIndex inboxIndex = null;

        logger.info("Loading quarantine index from " + quarantineFile);

        try {
            inboxIndex = settingsManager.load(InboxIndex.class, quarantineFile);
            File f = new File(quarantineFile);
            inboxIndex.setLastAccessTimestamp(f.lastModified());
        } catch (Exception exn) {
            logger.error("Could not read quarantine index from file " + quarantineFile, exn);
        }

        if (inboxIndex == null) {
            logger.warn("No quarantine index from file " + quarantineFile);
        }

        return inboxIndex;
    }

    /**
     * reads the InboxIndex for a given email address
     * 
     * @param emailAddress of inbox.
     * @param baseDir String of base directory.
     * @return InboxIndex for quarantine.
     */
    public static InboxIndex readQuarantine(String emailAddress, String baseDir)
    {
        return readQuarantineFromFile(baseDir + "/" + emailAddress + ".js");
    }

    /**
     * write a new record in the InboxIndex file of the give email address
     * 
     * @param emailAddress String of inbox.
     * @param newRecord InboxRecord to add.
     * @param baseDir String of base directory to add.
     * @return true if write was successful, otherwise false.
     */
    public static boolean writeQuarantineRecord(String emailAddress, InboxRecord newRecord, String baseDir)
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String quarantineFile = baseDir + "/" + emailAddress + ".js";
        InboxIndex inboxIndex = readQuarantine(emailAddress, baseDir);
        if (inboxIndex == null) {
            inboxIndex = new InboxIndex();
            inboxIndex.setOwnerAddress(emailAddress);
        }
        inboxIndex.getInboxMap().put(newRecord.getMailID(), newRecord);
        try {
            settingsManager.save( quarantineFile, inboxIndex, false, false );
        } catch (Exception exn) {
            logger.error("Could not save quarantine record", exn);
            return false;
        }
        return true;
    }

    /**
     * write the entire inboxIndex for the given email address
     * 
     * @param emailAddress String of quarantine inbox.
     * @param inboxIndex Quarantine index.
     * @param baseDir String of quarantine.
     * @return true if index was written, false othewise.
     */
    public static boolean writeQuarantineIndex(String emailAddress, InboxIndex inboxIndex, String baseDir)
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String quarantineFile = baseDir + "/" + emailAddress + ".js";

        try {
            settingsManager.save( quarantineFile, inboxIndex, false, false );
        } catch (Exception exn) {
            logger.error("Could not save quarantine record", exn);
            return false;
        }
        return true;
    }

    /**
     * Return quarantine summary.
     * @param  baseDir String of base directory.
     * @return         StoreSummary of quarantine.
     */
    public static StoreSummary readSummary(String baseDir)
    {

        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        StoreSummary storeSummary = null;
        String quarantineFile = baseDir + "/" + SUMMARY_FILE_NAME;

        logger.info("Loading quarantine summary from " + quarantineFile);

        try {
            storeSummary = settingsManager.load(StoreSummary.class, quarantineFile);
        } catch (Exception exn) {
            logger.error("Could not read quarantine index from file " + quarantineFile, exn);
        }

        if (storeSummary == null) {
            logger.warn("No quarantine summary from file " + quarantineFile);
        }

        return storeSummary;
    }

    /**
     * Set summary.
     * @param  summary StoreSummary to write.
     * @param  baseDir String of base directory.
     * @return         true if set was successful, false otherwise.
     */
    public static boolean writeSummary(StoreSummary summary, String baseDir)
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String quarantineFile = baseDir + "/" + SUMMARY_FILE_NAME;

        try {
            settingsManager.save( quarantineFile, summary, false, false );
        } catch (Exception exn) {
            logger.error("Could not save quarantine summary", exn);
            return false;
        }
        return true;
    }
}
