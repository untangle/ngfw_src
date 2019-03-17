/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine.store;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.app.smtp.quarantine.InboxIndex;
import com.untangle.app.smtp.quarantine.InboxRecord;
import com.untangle.app.smtp.quarantine.MailSummary;
import com.untangle.app.smtp.quarantine.QuarantineEjectionHandler;
import com.untangle.uvm.util.IOUtil;
import com.untangle.uvm.util.Pair;
import com.untangle.uvm.UvmContextFactory;

/**
 * Always add a file *then* update the index. Always update the index *then* delete the file
 */
public class QuarantineStore
{
    private static final String DATA_FILE_PREFIX = "meta";
    private static final String DATA_FILE_SUFFIX = ".mime";

    /**
     * Results of {@link #quarantineMail adding a file to the quarantine}
     */
    public enum AdditionStatus
    {
        /**
         * The mail was added, and the passed-in copy can be discarded (it was not renamed)
         */
        SUCCESS_FILE_COPIED,
        /**
         * The mail was added, and the passed-in file was renamed (moved) to the quarantine
         */
        SUCCESS_FILE_RENAMED,
        /**
         * The add operation failed.
         */
        FAILURE
    };

    /**
     * Generic enum describing the result of operations. Used to convery the outcome of a few operations.
     */
    public enum GenericStatus
    {
        /**
         * The inbox does not exist
         */
        NO_SUCH_INBOX,
        /**
         * Operation was successful
         */
        SUCCESS,
        /**
         * Operation encountered an error
         */
        ERROR
    };

    private final Logger logger = Logger.getLogger(QuarantineStore.class);
    private File rootDir;
    private AddressLock addressLock;
    private MasterTable masterTable;

    /**
     * Iniitalize instance of QuarantineStore.
     * @param  dir String of directory.
     * @return     Instance of QuarantineStore..
     */
    public QuarantineStore(File dir) {
        rootDir = dir;

        if (!rootDir.exists()) {
            logger.debug("Creating Quarantine root \"" + rootDir + "\"");
            rootDir.mkdirs();
        }

        // Create address lock
        addressLock = AddressLock.getInstance();

        // Load the MasterTable
        logger.debug("About to Open Master Table...");
        masterTable = MasterTable.open(rootDir.getAbsolutePath());
        logger.debug("Opened Master Table...");
    }

    /**
     * Tell the quarantine that it is closing. Stray calls may still be made (thread timing), but will likely be slower.
     */
    public void close()
    {
        masterTable.close();
    }

    /**
     * Total size of the entire store (in bytes)
     * @return long of total size of store.
     */
    public long getTotalSize()
    {
        return masterTable.getTotalQuarantineSize();
    }

    /**
     * return formatted total size as a string.
     * @param  inMB if true, return as MB, otherwise as bytes.
     * @return      String of total size summary.
     */
    public final String getFormattedTotalSize(boolean inMB)
    {
        try {
            double unitDivisor;

            if (false == inMB) {
                unitDivisor = 1024.0; // in kilobytes
            } else {
                unitDivisor = (1024.0 * 1024.0); // in megabytes
            }

            return String.format("%01.3f", new Float(getTotalSize() / unitDivisor));
        } catch (Exception ex) {
            return "<unknown>";
        }
    }

    /**
     * Provides a summary of all Inboxes.
     * @return List of InboxSummary.
     */
    public List<InboxSummary> listInboxes()
    {
        Set<Map.Entry<String, InboxSummary>> allAccounts = masterTable.entries();
        ArrayList<InboxSummary> ret = new ArrayList<InboxSummary>(allAccounts.size());
        for (Map.Entry<String, InboxSummary> entry : allAccounts) {
            ret.add(new InboxSummary(entry.getKey(), entry.getValue().getTotalSz(), entry.getValue().getTotalMails()));
        }
        return ret;
    }

    /**
     * Test if this account exists (has an inbox)
     * 
     * @param address
     *            the address
     * 
     * @return true or false (duh)
     */
    public boolean inboxExists(String address)
    {
        return masterTable.inboxExists(address.toLowerCase());
    }

    /**
     * Quarantine the mail file
     * 
     * @param file
     *            the file
     * @param inboxAddr
     *            the inbox address
     * @param recipients
     *            the recipients of the email
     * @param summary
     *            a summary of the mail for quarantine
     * @param attemptRename
     *            if true, this operation will attempt to move the file into the quarantine (avoiding a file copy). If
     *            this flag is true and the move is successful, then the status is <code>SUCCESS_FILE_RENAMED</code> the
     *            caller can ignore dealing with the file. Otherwise the caller still "owns" the file.
     * 
     * @return the result
     */
    public Pair<AdditionStatus, String> quarantineMail(File file, String inboxAddr, String[] recipients,
            MailSummary summary, boolean attemptRename)
    {

        inboxAddr = inboxAddr.toLowerCase();

        logger.debug("Call to quarantine mail from file \"" + file + "\" into inbox \"" + inboxAddr + "\"");

        File dir = getInboxDir(inboxAddr, true);

        // We need to hold the account lock for the whole
        // operation of addition and index update. This
        // is to prevent concurrent deletion of the directory
        // while purging old accounts.
        addressLock.lock(inboxAddr);

        long size = file.length();
        summary.setQuarantineSize(size);

        boolean renamedFile = true;
        String newFileName;

        if (attemptRename) {
            // Add the file (move)
            newFileName = moveFileToInbox(file, dir);
            if (newFileName == null) {
                // Try copy
                newFileName = copyFileToInbox(file, dir);
                if (newFileName == null) {
                    addressLock.unlock(inboxAddr);
                    return new Pair<AdditionStatus, String>(AdditionStatus.FAILURE);
                }
                renamedFile = false;
            }
        } else {
            newFileName = copyFileToInbox(file, dir);
            if (newFileName == null) {
                addressLock.unlock(inboxAddr);
                return new Pair<AdditionStatus, String>(AdditionStatus.FAILURE);
            }
            renamedFile = false;
        }

        // Update (append) to the index
        if (!appendSummaryToIndex(dir, inboxAddr, recipients, newFileName, summary)) {
            if (renamedFile) {
                // We're likely so hosed at this point anyway,
                // what's the use of worrying about the return
                new File(dir, newFileName).renameTo(file);
            } else {
                new File(dir, newFileName).delete();
            }
            addressLock.unlock(inboxAddr);
            return new Pair<AdditionStatus, String>(AdditionStatus.FAILURE);
        }

        if (!masterTable.mailAdded(inboxAddr, size)) {
            masterTable = MasterTable.rebuild(rootDir.getAbsolutePath());
            masterTable.mailAdded(inboxAddr, size);
        }
        addressLock.unlock(inboxAddr);
        return new Pair<AdditionStatus, String>(renamedFile ? AdditionStatus.SUCCESS_FILE_RENAMED
                : AdditionStatus.SUCCESS_FILE_COPIED, newFileName);
    }

    /**
     * Prune the store, removing old (expired) messages as well as empty, inactive accounts. Although I believe this
     * method could be executed concurrently (i.e. twice by two threads at the same time), that would be really goofy.
     * 
     * @param relativeOldestMail
     *            the relative age (e.g. 5 days) for mails to be considered inactive and candidates for pruning.
     * @param relativeInactiveInboxTime
     *            the relative age (e.g. 30 days) for inboxes to be considered inactive and candidates for pruning.
     */
    public void prune(long relativeOldestMail, long relativeInactiveInboxTime)
    {

        long oldestValidInboxTimestamp = System.currentTimeMillis() - relativeInactiveInboxTime;

        // Create the two visitors - the deleter and the selector.
        // The selector also doubles to collect candidate inboxes for culling
        // for inactivity.
        PruningSelector pruningSelector = new PruningSelector(System.currentTimeMillis() - relativeOldestMail,
                oldestValidInboxTimestamp);

        // Get a copy of the entire address/dir mapping
        for (Map.Entry<String, InboxSummary> mapping : masterTable.entries()) {
            // Visit each one via "eject"
            eject(mapping.getKey(), new QuarantineEjectionHandler()
            {
                /**
                 * Delete message.
                 * @param record       InboxRecord to delete.
                 * @param inboxAddress String of address.
                 * @param recipients   Array of String of address recipients.
                 * @param data         Inbox file.
                 */
                @Override
                public void ejectMail(InboxRecord record, String inboxAddress, String[] recipients, File data)
                {
                    if (data.delete()) {
                        logger.debug("Pruned file \"" + data + "\" for inbox \"" + inboxAddress + "\"");
                    } else {
                        logger.debug("Unable to Pruned file \"" + data + "\" for inbox \"" + inboxAddress + "\"");
                    }
                }
            }, pruningSelector);
        }

        // Go through the list of dead inbox candidates
        for (String account : pruningSelector.getDoomedInboxes()) {
            // Lock
            if (!addressLock.tryLock(account)) {
                continue;
                // Don't hang up on this. We'll get it sometime later. Besides,
                // if it is locked
                // it is likely getting a new mail (or the software is totaly
                // goofed).
            }
            // Read the index (it may not exist)
            InboxIndex inboxIndex = QuarantineStorageManager.readQuarantine(account, getInboxPath(account));

            boolean shouldDelete = false;

            if (inboxIndex != null) {
                shouldDelete = inboxIndex.getLastAccessTimestamp() < oldestValidInboxTimestamp;
            }

            if (shouldDelete) {
                IOUtil.rmDir(new File(getInboxPath(account)));
                // Note that this call implicitly causes any mails accounted for
                // to be removed
                masterTable.removeInbox(account);
            }
            addressLock.unlock(account);
        }
    }

    /**
     * Force the delete of a given account.
     * 
     * @param address
     *            the address
     * 
     * @return the outcome.
     */
    public GenericStatus deleteInbox(String address)
    {
        String account = address.toLowerCase();
        addressLock.lock(account);

        GenericStatus ret = null;

        IOUtil.rmDir(new File(getInboxPath(account)));
        masterTable.removeInbox(account);
        ret = GenericStatus.SUCCESS;
        addressLock.unlock(account);
        return ret;
    }

    /**
     * Gets the inbox for the given address. Return "codes" are part of the generic result.
     * 
     * @param address String of address.
     * 
     * @return the result
     */
    public Pair<GenericStatus, InboxIndex> getIndex(String address)
    {
        address = address.toLowerCase();
        
        // Get the inbox directory
        File dirRF = getInboxDir(address, false);
        if (dirRF == null) {
            logger.warn("Unable to get inbox folderfor " + address);
            return new Pair<GenericStatus, InboxIndex>(GenericStatus.NO_SUCH_INBOX);
        }
        
        // lock the inbox
        addressLock.lock(address);

        // Read the index file.
        // Remove any mails from the in-memory index which are in our list and
        // add them to a new list
        InboxIndex inboxIndex = QuarantineStorageManager.readQuarantine(address, getInboxPath(address));
        addressLock.unlock(address);
        if (inboxIndex == null) {
            logger.warn("Unable to read index for " + address);
            return new Pair<GenericStatus, InboxIndex>(GenericStatus.ERROR);
        }
        return new Pair<GenericStatus, InboxIndex>(GenericStatus.SUCCESS, inboxIndex);
    }

    /**
     * Note that if one or more of the mails no longer exist, this is not considered an error.
     * 
     * @param address String of address.
     * @param mailIDs Variable argument of String of messagge ids.
     * @return the result. If <code>SUCCESS</code>, then the index just after the modification is returned attached to
     *         the result
     */
    public Pair<GenericStatus, InboxIndex> rescue(String address, String... mailIDs)
    {
        logger.debug("Rescue requested for " + mailIDs.length + " mails for account \"" + address + "\"");
        return eject(address, new QuarantineEjectionHandler()
        {
                /**
                 * Delete message.
                 * @param record       InboxRecord to delete.
                 * @param inboxAddress String of address.
                 * @param recipients   Array of String of address recipients.
                 * @param data         Inbox file.
                 */
            @Override
            public void ejectMail(InboxRecord record, String inboxAddress, String[] recipients, File data)
            {
                try (
                    FileInputStream fIn = new FileInputStream(data);
                    BufferedInputStream bufIn = new BufferedInputStream(fIn);
                ) {
                    boolean success = UvmContextFactory.context().mailSender().sendMessage(bufIn, recipients);
                    if (success) {
                        logger.debug("Released mail \"" + record.getMailID() + "\" for " + recipients.length
                                + " recipients from inbox \"" + inboxAddress + "\"");
                    } else {
                        logger.warn("Unable to release mail \"" + record.getMailID() + "\" for " + recipients.length
                                + " recipients from inbox \"" + inboxAddress + "\"");
                    }
                } catch (Exception ex) {
                    logger.warn("Exception reading mail file for rescue", ex);
                }
                IOUtil.delete(data);
            }
        }, new ListEjectionSelector(mailIDs));
    }

    /**
     * Note that if one or more of the mails no longer exist, this is not considered an error.
     * 
     * @param address String of address.
     * @param mailIDs Variable argument of String of messagge ids.
     * @return the result. If <code>SUCCESS</code>, then the index just after the modification is returned attached to
     *         the result
     */
    public Pair<GenericStatus, InboxIndex> purge(String address, String... mailIDs)
    {
        logger.debug("Purge requested for " + mailIDs.length + " mails for account \"" + address + "\"");
        return eject(address, new QuarantineEjectionHandler()
        {
            /**
             * Delete message.
             * @param record       InboxRecord to delete.
             * @param inboxAddress String of address.
             * @param recipients   Array of String of address recipients.
             * @param data         Inbox file.
            */
            @Override
            public void ejectMail(InboxRecord record, String inboxAddress, String[] recipients, File data)
            {
                if (data.delete()) {
                    logger.debug("Deleted file \"" + data + "\" for inbox \"" + inboxAddress + "\"");
                } else {
                    logger.debug("Unable to delete file \"" + data + "\" for inbox \"" + inboxAddress + "\"");
                }

            }
        }, new ListEjectionSelector(mailIDs));
    }

    /**
     * Eject mails, passing them either to a handler for rescue or purge
     * (delete).
     * The EjectionSelector is able to view the entire InboxIndex, and select
     * which mails are to be purged based on the context of the calling
     * operation.
     *
     * @param address String of address.
     * @param handler QuarantineEjectionHandler.
     * @param selector EjectionSelector.
     * @return the result. If <code>SUCCESS</code>, then
     * the index just after the modification is returned attached to the result
     */
    private Pair<GenericStatus, InboxIndex> eject(String address, QuarantineEjectionHandler handler,
            EjectionSelector selector)
    {

        address = address.toLowerCase();

        // Get/create the inbox directory
        File dirRF = getInboxDir(address, false);
        if (dirRF == null) {
            logger.warn("Unable to purge mails for \"" + address + "\"  No such inbox");
            return new Pair<GenericStatus, InboxIndex>(GenericStatus.NO_SUCH_INBOX);
        }

        // lock the inbox
        addressLock.lock(address);

        // Read the index file. Remove any mails from the in-memory
        // index which are in our list and add them to a new list
        InboxIndex inboxIndex = QuarantineStorageManager.readQuarantine(address, getInboxPath(address));

        if (inboxIndex == null) {
            logger.warn("Unable to purge mails for " + address);
            addressLock.unlock(address);
            return new Pair<GenericStatus, InboxIndex>(GenericStatus.ERROR);
        }

        List<InboxRecord> toDelete = selector.selectEjections(inboxIndex, dirRF);

        if (toDelete.size() == 0) {
            // Nothing to do, and we don't want to update the index w/ a NOOP
            addressLock.unlock(address);
            return new Pair<GenericStatus, InboxIndex>(GenericStatus.SUCCESS, inboxIndex);
        }

        // Update the index. We'll defer actual ejection until after we release
        // the lock
        if (!QuarantineStorageManager.writeQuarantineIndex(address, inboxIndex, getInboxPath(address))) {
            logger.warn("Unable to replace index for address \"" + address + "\".  Abort purge");
            addressLock.unlock(address);
            return new Pair<GenericStatus, InboxIndex>(GenericStatus.ERROR);
        }

        // Unlock
        addressLock.unlock(address);

        // Perform ejection
        for (InboxRecord record : toDelete) {
            File file = new File(dirRF, record.getMailID());
            if (!file.exists()) {
                logger.debug("Unable to delete file \"" + file.getPath() + "\" for inbox \"" + address
                        + "\".  File does not exist ?!?");
                continue;
            }
            handler.ejectMail(record, inboxIndex.getOwnerAddress(), record.getRecipients(), file);
            if (file.exists()) {
                logger.debug("Handler for file  \"" + file.getPath() + "\" in inbox \"" + address
                        + "\" did not delete file (?!?).  Force delete.");
                file.delete();
            }

            if (!masterTable.mailRemoved(address, record.getMailSummary().getQuarantineSize())) {
                masterTable = MasterTable.rebuild(rootDir.getAbsolutePath());
                masterTable.mailRemoved(address, record.getMailSummary().getQuarantineSize());
            }
        }

        return new Pair<GenericStatus, InboxIndex>(GenericStatus.SUCCESS, inboxIndex);
    }

    /**
     * This method performs <b>no locking</b> (assumes the caller
     * has done this).
     * @param  inboxDir        File of inbox directory.
     * @param  inboxAddr       String of inbox address.
     * @param  recipients      Array of string of recipient addresses.
     * @param  fileNameInInbox String of filename in inbox.
     * @param  summary         MailSummary
     * @return                 if true, quarantine record was written otherwise false.
     */
    private boolean appendSummaryToIndex(File inboxDir, String inboxAddr, String[] recipients, String fileNameInInbox,
            MailSummary summary)
    {
        // Update (append) to the index
        return QuarantineStorageManager.writeQuarantineRecord(inboxAddr,
                new InboxRecord(fileNameInInbox, System.currentTimeMillis(), summary, recipients),
                getInboxPath(inboxAddr));

    }

    /**
     * This method performs no locking, and does not update the master index
     *
     * @param source File of source mailbox.
     * @param targetDir Fileof target mailbox.
     * @return the File within the Inbox w/ the
     * newly added mail, or null if there was an error.
     */
    private String moveFileToInbox(File source, File targetDir)
    {

        File targetFile = createDataFile(targetDir);
        if (targetFile == null) {
            return null;
        }
        if (source.renameTo(targetFile)) {
            return targetFile.getName();
        }
        targetFile.delete();
        return null;
    }

    /**
     * This method performs no locking, and does not update the master index
     *
     * @param source File of source mailbox.
     * @param targetDir Fileof target mailbox.
     * @return the File within the Inbox w/ the newly added mail, or null if
     * there was an error.
     */
    private String copyFileToInbox(File source, File targetDir)
    {

        File targetFile = createDataFile(targetDir);
        if (targetFile == null) {
            return null;
        }

        try {
            IOUtil.copyFile(source, targetFile);
            return targetFile.getName();
        } catch (IOException ex) {
            targetFile.delete();
            logger.warn("Unable to copy data file", ex);
            return null;
        }
    }

    /**
     * Creates an File object in the target directory.
     * Guaranteed to be unique ('cause I'm using Java's
     * stuff to do it).
     * @param targetDir Fileof target mailbox.
     * @return File of temp file.
     */
    private File createDataFile(File targetDir)
    {
        try {
            // Be lazy, and use Java's temp file
            // stuff which ensures no duplicates
            return File.createTempFile(DATA_FILE_PREFIX, DATA_FILE_SUFFIX, targetDir);
        } catch (IOException ex) {
            logger.error("Unable to create data file", ex);
            return null;
        }
    }

    /**
     * Return inbox path.
     * @param  address String of address.
     * @return         Path for inbox.
     */
    private String getInboxPath(String address)
    {
        return rootDir.getAbsolutePath() + "/inboxes/" + address;
    }

    /**
     * This method may lock the address if
     * the account does not already exist (so
     * do not call with the address locked or
     * the system will be hosed).
     * @param lcAddress String of address.
     * @param autoCreate I?f true, automatically create if doesn't exist.
     * @return File of inbox directory.
     */
    private File getInboxDir(String lcAddress, boolean autoCreate)
    {

        // RelativeFileName subDirName = m_masterTable.getInboxDir(lcAddress);
        File baseDir = new File(getInboxPath(lcAddress));
        if (!baseDir.exists()) {
            if (!autoCreate) {
                logger.debug("No inbox for \"" + lcAddress + "\"");
                return null;
            }
            addressLock.lock(lcAddress);
            baseDir = getOrCreateInboxDirWL(lcAddress);
            addressLock.unlock(lcAddress);
        }
        return baseDir;
    }

    /**
     * "WL" = "With Lock"
    // Assumes lock is already obtained
     * @param  lcAddress Address of inbox.
     * @return           File of inbox.
     */
    private File getOrCreateInboxDirWL(String lcAddress)
    {

        File baseDir = new File(getInboxPath(lcAddress));

        try {
            if (!baseDir.exists()) {
                if (!baseDir.mkdirs()) {
                    logger.warn("Inbox for \"" + lcAddress + "\" could not be created.");
                    return null;
                }
                InboxIndex inboxIndex = new InboxIndex();
                inboxIndex.setOwnerAddress(lcAddress);
                QuarantineStorageManager.writeQuarantineIndex(lcAddress, inboxIndex, getInboxPath(lcAddress));
                masterTable.addInbox(lcAddress);
            } else {
                logger.debug("Inbox for \"" + lcAddress + "\" created by concurrent thread");
            }
            return baseDir;
        } catch (Exception ex) {
            logger.warn("getOrCreateInboxDirWL: ", ex);
            return null;
        }
    }

    /**
     * Ejection Selector.
     */
    private abstract class EjectionSelector
    {
        /**
         * Any in the returned list <b>must</b>
         * be removed from index
         * @param  index InboxIndex.
         * @param  rf    File of returned list.
         * @return       List of InboxRecord.
         */
        abstract List<InboxRecord> selectEjections(InboxIndex index, File rf);
    }

    /**
     * Pruning Selector.
     */
    private class PruningSelector extends EjectionSelector
    {

        private long m_mailCutoff;
        private long m_inboxCutoff;
        private List<String> m_doomedInboxes;

        /**
         * Initialize instance of PruningSelector.
         * @param mailCutoff Max number of messages.
         * @param inboxCutoff Index cutoff.
         * @return instance of PruningSelector.
         */
        PruningSelector(long mailCutoff, long inboxCutoff) {
            m_mailCutoff = mailCutoff;
            m_inboxCutoff = inboxCutoff;
            m_doomedInboxes = new ArrayList<String>();
        }

        /**
         * Any in the returned list <b>must</b>
         * be removed from index
         * @param  index InboxIndex.
         * @param  rf    File of returned list.
         * @return       List of InboxRecord.
         */
        List<InboxRecord> selectEjections(InboxIndex index, File rf)
        {
            ArrayList<InboxRecord> ret = new ArrayList<InboxRecord>();

            if (index.getLastAccessTimestamp() < m_inboxCutoff) {
                m_doomedInboxes.add(index.getOwnerAddress());
            }

            // I'm never sure about concurrent modification,
            // so we won't remove in the initial loop
            for (InboxRecord record : index.getInboxMap().values()) {
                if (record.getInternDate() < m_mailCutoff) {
                    ret.add(record);
                }
            }

            for (InboxRecord record : ret) {
                index.getInboxMap().remove(record.getMailID());
            }

            return ret;
        }

        /**
         * Return list of inboxes to rmeoved.
         * @return List of inbox addresses.
         */
        List<String> getDoomedInboxes()
        {
            return m_doomedInboxes;
        }
    }

    /**
     * List Ejection Selector.
     */
    private class ListEjectionSelector extends EjectionSelector
    {

        private final String[] m_list;

        /**
         * Initialize instance of ListEjectionSelector.
         * @param list Array of string of list.
         * @return instance of ListEjectionSelector.
         */
        ListEjectionSelector(String[] list) {
            m_list = list;
        }

        /**
         * Any in the returned list <b>must</b>
         * be removed from index
         * @param  index InboxIndex.
         * @param  rf    File of returned list.
         * @return       List of InboxRecord.
         */
        List<InboxRecord> selectEjections(InboxIndex index, File rf)
        {
            List<InboxRecord> toEject = new ArrayList<InboxRecord>();

            for (String id : m_list) {
                if (index.getInboxMap().containsKey(id)) {
                    toEject.add(index.getInboxMap().remove(id));
                }
            }
            return toEject;
        }
    }

}
