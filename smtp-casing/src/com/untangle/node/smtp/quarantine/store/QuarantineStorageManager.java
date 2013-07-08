package com.untangle.node.smtp.quarantine.store;

import java.io.File;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.quarantine.InboxRecord;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;

/**
 * Provides static methods for reading/writing quarantine info in json format
 * using SettingsManager
 * 
 */
public class QuarantineStorageManager
{
    private static final Logger logger = Logger.getLogger(QuarantineStorageManager.class);
    private static final String SUMMARY_OPENED_FILE_NAME = "summary_opened.js";
    private static final String SUMMARY_CLOSED_FILE_NAME = "summary_closed.js";

    /**
     * reads InboxIndex from a given file
     * 
     * @param quarantineFile
     * @return
     */
    public static InboxIndexImpl readQuarantineFromFile(String quarantineFile)
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        InboxIndexImpl inboxIndex = null;

        logger.info("Loading quarantine index from " + quarantineFile);

        try {
            inboxIndex = settingsManager.load(InboxIndexImpl.class, quarantineFile);
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
     * @param emailAddress
     * @return
     */
    public static InboxIndexImpl readQuarantine(String emailAddress, String baseDir)
    {
        return readQuarantineFromFile(baseDir + "/" + emailAddress + ".js");
    }

    /**
     * write a new record in the InboxIndex file of the give email address
     * 
     * @param emailAddress
     * @param newRecord
     * @return
     */
    public static boolean writeQuarantineRecord(String emailAddress, InboxRecord newRecord, String baseDir)
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String quarantineFile = baseDir + "/" + emailAddress + ".js";
        InboxIndexImpl inboxIndex = readQuarantine(emailAddress, baseDir);
        if (inboxIndex == null) {
            inboxIndex = new InboxIndexImpl();
            inboxIndex.setOwnerAddress(emailAddress);
        }
        inboxIndex.getInboxMap().put(newRecord.getMailID(), newRecord);
        try {
            settingsManager.save(InboxIndexImpl.class, quarantineFile, inboxIndex, false);
        } catch (Exception exn) {
            logger.error("Could not save quarantine record", exn);
            return false;
        }
        return true;
    }

    /**
     * write the entire inboxIndex for the given email address
     * 
     * @param emailAddress
     * @param inboxIndex
     * @return
     */
    public static boolean writeQuarantineIndex(String emailAddress, InboxIndexImpl inboxIndex, String baseDir)
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String quarantineFile = baseDir + "/" + emailAddress + ".js";

        try {
            settingsManager.save(InboxIndexImpl.class, quarantineFile, inboxIndex, false);
        } catch (Exception exn) {
            logger.error("Could not save quarantine record", exn);
            return false;
        }
        return true;
    }

    /**
     * Marks the file as "open", meaning if there is a crash the file will be
     * assumed to be out-of-date upon the next startup.
     */
    static boolean openSummary(String baseDir)
    {
        File f = new File(baseDir + "/", SUMMARY_CLOSED_FILE_NAME);
        if (f.exists()) {
            return f.renameTo(new File(baseDir, SUMMARY_OPENED_FILE_NAME));
        }
        return false;
    }

    public static StoreSummary readSummary(String baseDir)
    {

        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        StoreSummary storeSummary = null;
        String quarantineFile = baseDir + "/" + SUMMARY_CLOSED_FILE_NAME;

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

    public static boolean writeSummary(StoreSummary summary, String baseDir)
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String quarantineFile = baseDir + "/" + SUMMARY_CLOSED_FILE_NAME;

        try {
            settingsManager.save(StoreSummary.class, quarantineFile, summary, false);
        } catch (Exception exn) {
            logger.error("Could not save quarantine summary", exn);
            return false;
        }
        return true;
    }
}
