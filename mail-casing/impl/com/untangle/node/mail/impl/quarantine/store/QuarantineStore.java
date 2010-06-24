/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.mail.impl.quarantine.store;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.untangle.node.mail.papi.quarantine.Inbox;
import com.untangle.node.mail.papi.quarantine.InboxRecord;
import com.untangle.node.mail.papi.quarantine.MailSummary;
import com.untangle.node.mail.papi.quarantine.QuarantineEjectionHandler;
import com.untangle.node.util.IOUtil;
import com.untangle.node.util.Pair;
import com.untangle.node.util.UtLogger;


/**
 * Always add a file *then* update the index
 * Always update the index *then* delete the file
 */
public class QuarantineStore {

    private static final String DATA_FILE_PREFIX = "meta";
    private static final String DATA_FILE_SUFFIX = ".mime";

    /**
     * Results of {@link #quarantineMail adding a file to the quarantine}
     */
    public enum AdditionStatus {
        /**
         * The mail was added, and the passed-in copy
         * can be discarded (it was not renamed)
         */
        SUCCESS_FILE_COPIED,
        /**
         * The mail was added, and the passed-in
         * file was renamed (moved) to the quarantine
         */
        SUCCESS_FILE_RENAMED,
        /**
         * The add operation failed.
         */
        FAILURE
    };


    /**
     * Generic enum describing the
     * result of operations.  Used
     * to convery the outcome of
     * a few operations.
     */
    public enum GenericStatus {
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

    private final UtLogger m_logger = new UtLogger(QuarantineStore.class);
    private File m_rootDir;
    private AddressLock m_addressLock;
    private MasterTable m_masterTable;
    private InboxDirectoryTree m_dirTracker;

    private QuarantineEjectionHandler m_deleter = new DeletingEjectionHandler();

    public QuarantineStore(File rootDir) 
    {
        m_rootDir = rootDir;

        if(!m_rootDir.exists()) {
            m_logger.debug("Creating Quarantine root \"",
                           m_rootDir,
                           "\"");
            m_rootDir.mkdirs();
        }

        //Create address lock
        m_addressLock = AddressLock.getInstance();

        //Initiailze the InboxDirectoryTree
        m_logger.debug("About to initialize InboxDirectoryTree...");
        m_dirTracker = new InboxDirectoryTree(m_rootDir);
        m_logger.debug("Initialized InboxDirectoryTree");

        //Load the MasterTable
        m_logger.debug("About to Open Master Table...");
        m_masterTable = MasterTable.open(m_rootDir, m_dirTracker);
        m_logger.debug("Opened Master Table...");
    }

    /**
     * Tell the quarantine that it is closing.  Stray calls
     * may still be made (thread timing), but will likely be
     * slower.
     */
    public void close() {
        m_masterTable.close();
    }

    /**
     * Total size of the entire store (in bytes)
     */
    public long getTotalSize() {
        return m_masterTable.getTotalQuarantineSize();
    }

    public final String getFormattedTotalSize(boolean inMB) {
        try {
            double unitDivisor;

            if (false == inMB) {
                unitDivisor = 1024.0; // in kilobytes
            } else {
                unitDivisor = (1024.0 * 1024.0); // in megabytes
            }

            return String.format("%01.3f", new Float(getTotalSize() / unitDivisor));
        } catch(Exception ex) { return "<unknown>"; }
    }

    /**
     * Provides a summary of all Inboxes.
     */
    public List<Inbox> listInboxes() {
        Set<Map.Entry<String,InboxSummary>> allAccounts =
            m_masterTable.entries();
        ArrayList<Inbox> ret = new ArrayList<Inbox>(allAccounts.size());
        for(Map.Entry<String,InboxSummary> entry : allAccounts) {
            ret.add(new Inbox(entry.getKey(),
                              entry.getValue().getTotalSz(),
                              entry.getValue().getTotalMails()));
        }
        return ret;
    }

    /**
     * Test if this account exists (has an inbox)
     *
     * @param address the address
     *
     * @return true or false (duh)
     */
    public boolean inboxExists(String address) {
        return m_masterTable.inboxExists(address.toLowerCase());
    }

    /**
     * Quarantine the mail file
     *
     * @param file the file
     * @param inboxAddr the inbox address
     * @param recipients the recipients of the email
     * @param summary a summary of the mail for quarantine
     * @param attemptRename if true, this operation will
     *        attempt to move the file into the quarantine
     *        (avoiding a file copy).  If this flag is true
     *        and the move is successful, then
     *        the status is <code>SUCCESS_FILE_RENAMED</code>
     *        the caller can ignore dealing with the file.  Otherwise
     *        the caller still "owns" the file.
     *
     * @return the result
     */
    public Pair<AdditionStatus, String> quarantineMail(File file,
                                                       String inboxAddr,
                                                       String[] recipients,
                                                       MailSummary summary,
                                                       boolean attemptRename) {

        inboxAddr = inboxAddr.toLowerCase();

        m_logger.debug("Call to quarantine mail from file \"",
                       file,
                       "\" into inbox \"",
                       inboxAddr,
                       "\"");

        //Get/create the inbox directory
        RelativeFile dirRF = getInboxDir(inboxAddr, true);
        if(dirRF == null) {
            return new Pair<AdditionStatus, String>(AdditionStatus.FAILURE);
        }
        File dir = dirRF.file;

        //We need to hold the account lock for the whole
        //operation of addition and index update.  This
        //is to prevent concurrent deletion of the directory
        //while purging old accounts.
        m_addressLock.lock(inboxAddr);

        long size = file.length();
        summary.setQuarantineSize(size);

        boolean renamedFile = true;
        String newFileName;

        if(attemptRename) {
            //Add the file (move)
            newFileName = moveFileToInbox(file, dir);
            if(newFileName == null) {
                //Try copy
                newFileName = copyFileToInbox(file, dir);
                if(newFileName == null) {
                    m_addressLock.unlock(inboxAddr);
                    return new Pair<AdditionStatus, String>(AdditionStatus.FAILURE);
                }
                renamedFile = false;
            }
        } else {
            newFileName = copyFileToInbox(file, dir);
            if(newFileName == null) {
                m_addressLock.unlock(inboxAddr);
                return new Pair<AdditionStatus, String>(AdditionStatus.FAILURE);
            }
            renamedFile = false;
        }

        //Update (append) to the index
        if(!appendSummaryToIndex(dir, inboxAddr, recipients, newFileName, summary)) {
            if(renamedFile) {
                //We're likely so hosed at this point
                //anyway, what's the use of worrying about
                //the return
                new File(dir, newFileName).renameTo(file);
            }
            else {
                new File(dir, newFileName).delete();
            }
            m_addressLock.unlock(inboxAddr);
            return new Pair<AdditionStatus, String>(AdditionStatus.FAILURE);
        }

        m_masterTable.mailAdded(inboxAddr, size);
        m_addressLock.unlock(inboxAddr);
        return new Pair<AdditionStatus, String>(
                                                renamedFile?AdditionStatus.SUCCESS_FILE_RENAMED:
                                                AdditionStatus.SUCCESS_FILE_COPIED,
                                                newFileName);
    }


    /**
     * Prune the store, removing old (expired) messages
     * as well as empty, inactive accounts.  Although I
     * believe this method could be executed concurrently
     * (i.e. twice by two threads at the same time), that
     * would be really goofy.
     *
     * @param relativeOldestMail the relative age (e.g. 5 days)
     *        for mails to be considered inactive and candidates
     *        for pruning.
     * @param relativeInactiveInboxTime the relative age (e.g. 30 days)
     *        for inboxes to be considered inactive and candidates
     *        for pruning.
     * @param observer the observer, to receive informative
     *        progress callbacks.
     */
    public void prune(long relativeOldestMail,
                      long relativeInactiveInboxTime,
                      QuarantinePruningObserver observer) {

        long oldestValidInboxTimestamp =
            System.currentTimeMillis() - relativeInactiveInboxTime;

        //Create the two visitors - the deleter
        //and the selector.  The selector also doubles
        //to collect candidate inboxes for culling
        //for inactivity.
        PruningDeleter pruningDeleter = new PruningDeleter(observer);
        PruningSelector pruningSelector = new PruningSelector(
                                                              System.currentTimeMillis() - relativeOldestMail,
                                                              oldestValidInboxTimestamp);

        //Get a copy of the entire address/dir mapping
        for(Map.Entry<String,InboxSummary> mapping : m_masterTable.entries()) {
            //Visit each one via "eject"
            observer.preVisitInboxForOldMessages(mapping.getKey(), mapping.getValue().getDir());
            eject(mapping.getKey(), pruningDeleter, pruningSelector);
            observer.postVisitInboxForOldMessages(mapping.getKey(), mapping.getValue().getDir());
        }

        //Go through the list of dead inbox candidates
        for(String account : pruningSelector.getDoomedInboxes()) {
            //Lock
            if(!m_addressLock.tryLock(account)) {
                continue;//Don't hang up on this.  We'll get
                //it sometime later.  Besides, if it is locked
                //it is likely getting a new mail (or the software
                //is totaly goofed).
            }

            //Get the RelativeFileName from the AddrDir map
            RelativeFileName dirName = m_masterTable.getInboxDir(account);
            if(dirName == null) {
                //Someone beat us too it.
                m_addressLock.unlock(account);
                continue;
            }

            //Read the index (it may not exist)
            Pair<InboxIndexDriver.FileReadOutcome, InboxIndexImpl> read = InboxIndexDriver.readIndex(
                                                                                                     new File(m_rootDir, dirName.relativePath));

            boolean shouldDelete = false;

            switch(read.a) {
            case OK:
                shouldDelete =
                    read.b.getLastAccessTimestamp() < oldestValidInboxTimestamp;
                break;
            case NO_SUCH_FILE:
                //Someone beat us too it
                shouldDelete = false;
                break;
            case FILE_CORRUPT:
            case EXCEPTION:
                m_logger.debug("Index corrupt for too-old inbox.  Nuke dir");
                shouldDelete = true;
                break;
            }
            if(shouldDelete) {
                observer.pruningOldMailbox(account,
                                           dirName,
                                           read.b == null?
                                           0:
                                           read.b.getLastAccessTimestamp());
                m_dirTracker.deleteInboxDir(dirName);
                //Note that this call implicitlly causes
                //any mails accounted for to be removed
                m_masterTable.removeInbox(account);
            }
            m_addressLock.unlock(account);
        }
    }

    /**
     * Force the delete of a given account.
     *
     * @param address the address
     *
     * @return the outcome.
     */
    public GenericStatus deleteInbox(String address) {
        String account = address.toLowerCase();
        m_addressLock.lock(account);

        GenericStatus ret = null;

        //Get the RelativeFileName from the AddrDir map
        RelativeFileName dirName = m_masterTable.getInboxDir(account);
        if(dirName == null) {
            ret =  GenericStatus.NO_SUCH_INBOX;
        }
        else {
            m_dirTracker.deleteInboxDir(dirName);
            m_masterTable.removeInbox(account);
            ret = GenericStatus.SUCCESS;
        }
        m_addressLock.unlock(account);
        return ret;
    }


    /**
     * Gets the inbox for the given address.  Return "codes"
     * are part of the generic result.
     *
     * @param address the address
     *
     * @return the result
     */
    public Pair<GenericStatus, InboxIndexImpl> getIndex(String address) {
        address = address.toLowerCase();

        //Get/create the inbox directory
        RelativeFile dirRF = getInboxDir(address, false);
        if(dirRF == null) {
            m_logger.debug("Unable to get index for \"" +
                           address + "\"  No such inbox");
            return new Pair<GenericStatus, InboxIndexImpl>(GenericStatus.NO_SUCH_INBOX);
        }
        File dir = dirRF.file;

        //lock the inbox
        m_addressLock.lock(address);

        //Read the index file.  Remove
        //any mails from the in-memory
        //index which are in our
        //list and add them to a new list
        Pair<InboxIndexDriver.FileReadOutcome, InboxIndexImpl> read = InboxIndexDriver.readIndex(dir);
        m_addressLock.unlock(address);
        if(read.a != InboxIndexDriver.FileReadOutcome.OK) {
            m_logger.warn("Unable to read index for \"" +
                          address + "\"  Index read returned \"" +
                          read.a + "\"");
            return new Pair<GenericStatus, InboxIndexImpl>(GenericStatus.ERROR);
        }
        return new Pair<GenericStatus, InboxIndexImpl>(GenericStatus.SUCCESS, read.b);
    }


    /**
     * Note that if one or more of the mails no longer exist,
     * this is not considered an error.
     *
     * @return the result.  If <code>SUCCESS</code>, then
     *         the index just after the modification is
     *         returned attached to the result
     */
    public Pair<GenericStatus, InboxIndexImpl> rescue(String address,
                                                      QuarantineEjectionHandler handler,
                                                      String...mailIDs) {
        m_logger.debug("Rescue requested for ",
                       mailIDs.length,
                       " mails for account \"",
                       address,
                       "\"");
        return eject(address, handler, new ListEjectionSelector(mailIDs));
    }

    /**
     * Note that if one or more of the mails no longer exist,
     * this is not considered an error.
     *
     * @return the result.  If <code>SUCCESS</code>, then
     *         the index just after the modification is
     *         returned attached to the result
     */
    public Pair<GenericStatus, InboxIndexImpl> purge(String address,
                                                     String...mailIDs) {
        m_logger.debug("Purge requested for ",
                       mailIDs.length,
                       " mails for account \"",
                       address,
                       "\"");
        return eject(address, m_deleter, new ListEjectionSelector(mailIDs));
    }

    //
    // Eject mails, passing them either to a handler
    // for rescue or purge (delete).  The EjectionSelector
    // is able to view the entire InboxIndex, and select
    // which mails are to be purged based on the context
    // of the calling operation.
    //
    // @return the result.  If <code>SUCCESS</code>, then
    //         the index just after the modification is
    //         returned attached to the result
    //
    private Pair<GenericStatus, InboxIndexImpl> eject(String address,
                                                      QuarantineEjectionHandler handler,
                                                      EjectionSelector selector) {

        address = address.toLowerCase();

        //Get/create the inbox directory
        RelativeFile dirRF = getInboxDir(address, false);
        if(dirRF == null) {
            m_logger.warn("Unable to purge mails for \"" +
                          address + "\"  No such inbox");
            return new Pair<GenericStatus, InboxIndexImpl>(GenericStatus.NO_SUCH_INBOX);
        }
        File dir = dirRF.file;

        //lock the inbox
        m_addressLock.lock(address);

        //Read the index file.  Remove
        //any mails from the in-memory
        //index which are in our
        //list and add them to a new list
        Pair<InboxIndexDriver.FileReadOutcome, InboxIndexImpl> read = InboxIndexDriver.readIndex(dir);
        if(read.a != InboxIndexDriver.FileReadOutcome.OK) {
            m_logger.warn("Unable to purge mails for \"" +
                          address + "\"  Index read returned \"" +
                          read.a + "\"");
            m_addressLock.unlock(address);
            return new Pair<GenericStatus, InboxIndexImpl>(GenericStatus.ERROR);
        }

        List<InboxRecord> toDelete = selector.selectEjections(read.b, dirRF);

        if(toDelete.size() == 0) {
            //Nothing to do, and we don't want to update the
            //index w/ a NOOP
            m_addressLock.unlock(address);
            return new Pair<GenericStatus, InboxIndexImpl>(GenericStatus.SUCCESS, read.b);
        }

        //Update the index.  We'll defer actual ejection
        //until after we release the lock
        if(!InboxIndexDriver.replaceIndex(read.b, dir)) {
            m_logger.warn("Unable to replace index for address \"" +
                          address + "\".  Abort purge");
            m_addressLock.unlock(address);
            return new Pair<GenericStatus, InboxIndexImpl>(GenericStatus.ERROR);
        }

        //Unlock
        m_addressLock.unlock(address);

        //Perform ejection
        for(InboxRecord record : toDelete) {
            File file = new File(dir, record.getMailID());
            if(!file.exists()) {
                m_logger.debug("Unable to delete file \"" +
                               file.getPath() + "\" for inbox \"" +
                               address + "\".  File does not exist ?!?");
                continue;
            }
            handler.ejectMail(record,
                              read.b.getOwnerAddress(),
                              record.getRecipients(),
                              file);
            if(file.exists()) {
                m_logger.debug("Handler for file  \"" +
                               file.getPath() + "\" in inbox \"" +
                               address + "\" did not delete file (?!?).  Force delete.");
                file.delete();
            }
            m_masterTable.mailRemoved(address, record.getSize());
        }

        return new Pair<GenericStatus, InboxIndexImpl>(GenericStatus.SUCCESS, read.b);
    }


    //
    // This method performs <b>no locking</b> (assumes the caller
    // has done this).
    //
    private boolean appendSummaryToIndex(File inboxDir,
                                         String inboxAddr,
                                         String[] recipients,
                                         String fileNameInInbox,
                                         MailSummary summary) {

        //Update (append) to the index
        return InboxIndexDriver.appendIndex(inboxAddr,
                                            inboxDir,
                                            new InboxRecordImpl(fileNameInInbox,
                                                                System.currentTimeMillis(),
                                                                summary,
                                                                recipients));
    }

    //
    // This method performs no locking,
    // and does not update the master index
    //
    // @return the File within the Inbox w/ the
    //         newly added mail, or null if there
    //         was an error.
    //
    private String moveFileToInbox(File source,
                                   File targetDir) {

        File targetFile = createDataFile(targetDir);
        if(targetFile == null) {
            return null;
        }
        if(source.renameTo(targetFile)) {
            return targetFile.getName();
        }
        targetFile.delete();
        return null;
    }

    //
    // This method performs no locking,
    // and does not update the master index
    //
    // @return the File within the Inbox w/ the
    //         newly added mail, or null if there
    //         was an error.
    //
    private String copyFileToInbox(File source,
                                   File targetDir) {

        File targetFile = createDataFile(targetDir);
        if(targetFile == null) {
            return null;
        }

        try {
            IOUtil.copyFile(source, targetFile);
            return targetFile.getName();
        }
        catch(IOException ex) {
            targetFile.delete();
            m_logger.warn("Unable to copy data file", ex);
            return null;
        }
    }

    //
    // Creates an File object in the target directory.
    // Guaranteed to be unique ('cause I'm using Java's
    // stuff to do it).
    //
    private File createDataFile(File targetDir) {
        try {
            //Be lazy, and use Java's temp file
            //stuff which ensures no duplicates
            return File.createTempFile(DATA_FILE_PREFIX,
                                       DATA_FILE_SUFFIX,
                                       targetDir);
        }
        catch(IOException ex) {
            m_logger.error("Unable to create data file", ex);
            return null;
        }
    }


    //
    // This method may lock the address if
    // the account does not already exist (so
    // do not call with the address locked or
    // the system will be hosed).
    //
    private RelativeFile getInboxDir(String lcAddress,
                                     boolean autoCreate) {

        RelativeFileName subDirName = m_masterTable.getInboxDir(lcAddress);

        if(subDirName == null) {
            if(!autoCreate) {
                m_logger.debug("No inbox for \"",
                               lcAddress,
                               "\"");
                return null;
            }
            m_addressLock.lock(lcAddress);
            RelativeFile rf = getOrCreateInboxDirWL(lcAddress);
            m_addressLock.unlock(lcAddress);
            return rf;
        }
        else {
            return new RelativeFile(subDirName.relativePath,
                                    new File(m_rootDir, subDirName.relativePath));
        }
    }

    //"WL" = "With Lock"
    //Assumes lock is already obtained
    private RelativeFile getOrCreateInboxDirWL(String lcAddress) {

        RelativeFileName subDirName = null;
        File subDir = null;
        try {
            subDirName = m_masterTable.getInboxDir(lcAddress);
            if(subDirName == null) {
                subDirName = m_dirTracker.createInboxDir();
                m_logger.debug("Created Inbox for \"",
                               lcAddress,
                               "\" in directory \"",
                               subDirName.relativePath,
                               "\"");
                subDir = new File(m_rootDir, subDirName.relativePath);
                InboxIndexDriver.createBlankIndex(lcAddress, subDir);
                m_masterTable.addInbox(lcAddress, subDirName);
            }
            else {
                m_logger.debug("Inbox for \"",
                               lcAddress,
                               "\" created by concurrent thread");
                subDir = new File(m_rootDir, subDirName.relativePath);
            }
            return new RelativeFile(subDirName.relativePath, subDir);
        }
        catch(Exception ex) {
            //m_logger.warn("getOrCreateInboxDirWL: ", ex);
            ex.printStackTrace(System.out);
            return null;
        }
    }


    //-------------------- Inner Class ----------------------

    private abstract class EjectionSelector {
        //
        // Any in the returned list <b>must</b>
        // be removed from index
        //
        abstract List<InboxRecord> selectEjections(InboxIndexImpl index,
                                                   RelativeFile rf);
    }

    //-------------------- Inner Class ----------------------

    //
    // Simply selects the entire list
    //
    private class SelectsAll
        extends EjectionSelector {

        List<InboxRecord> selectEjections(InboxIndexImpl index,
                                          RelativeFile rf) {
            ArrayList<InboxRecord> ret = new ArrayList<InboxRecord>(
                                                                    index.values());
            index.clear();
            return ret;
        }
    }

    //-------------------- Inner Class ----------------------

    //
    // Selects any records interned before the
    // given date.  Also produces a list of candidate
    // "inactive" Inboxes.  For those candidates,
    // it still prunes mails in case we have the
    // (very) unlikely boundary case where a mail
    // is being entered as we're pruning.  The index
    // timestamp, however, will not be updated if
    // the account is empty.
    //
    private class PruningSelector
        extends EjectionSelector {

        private long m_mailCutoff;
        private long m_inboxCutoff;
        private List<String> m_doomedInboxes;

        PruningSelector(long mailCutoff,
                        long inboxCutoff) {
            m_mailCutoff = mailCutoff;
            m_inboxCutoff = inboxCutoff;
            m_doomedInboxes = new ArrayList<String>();
        }

        List<InboxRecord> selectEjections(InboxIndexImpl index,
                                          RelativeFile rf) {
            ArrayList<InboxRecord> ret = new ArrayList<InboxRecord>();

            if(index.getLastAccessTimestamp() < m_inboxCutoff) {
                m_doomedInboxes.add(index.getOwnerAddress());
            }

            //I'm never sure about concurrent modification,
            //so we won't remove in the initial loop
            for(InboxRecord record : index.values()) {
                if(record.getInternDate() < m_mailCutoff) {
                    ret.add(record);
                }
            }

            for(InboxRecord record : ret) {
                index.remove(record.getMailID());
            }

            return ret;
        }

        List<String> getDoomedInboxes() {
            return m_doomedInboxes;
        }
    }


    //-------------------- Inner Class ----------------------

    //
    // Selects mails to eject based on
    // a list
    //
    private class ListEjectionSelector
        extends EjectionSelector {

        private final String[] m_list;

        ListEjectionSelector(String[] list) {
            m_list = list;
        }

        List<InboxRecord> selectEjections(InboxIndexImpl index,
                                          RelativeFile rf) {
            List<InboxRecord> toEject = new ArrayList<InboxRecord>();

            for(String id : m_list) {
                if(index.containsKey(id)) {
                    toEject.add(index.remove(id));
                }
            }
            return toEject;
        }
    }


    //-------------------- Inner Class ----------------------

    private class DeletingEjectionHandler
        implements QuarantineEjectionHandler {

        public void ejectMail(InboxRecord record,
                              String inboxAddress,
                              String[] recipients,
                              File data) {
            if(data.delete()) {
                m_logger.debug("Deleted file \"" + data + "\" for inbox \"" +
                               inboxAddress + "\"");
            }
            else {
                m_logger.debug("Unable to delete file \"" + data + "\" for inbox \"" +
                               inboxAddress + "\"");
            }
        }
    }


    //-------------------- Inner Class ----------------------

    private class PruningDeleter
        implements QuarantineEjectionHandler {

        private final QuarantinePruningObserver m_observer;

        PruningDeleter(QuarantinePruningObserver observer) {
            m_observer = observer;
        }

        public void ejectMail(InboxRecord record,
                              String inboxAddress,
                              String[] recipients,
                              File data) {
            if(data.delete()) {
                m_logger.debug("Pruned file \"" + data + "\" for inbox \"" +
                               inboxAddress + "\"");
                m_observer.pruningOldMessage(inboxAddress, data, record);
            }
            else {
                m_logger.debug("Unable to Pruned file \"" + data + "\" for inbox \"" +
                               inboxAddress + "\"");
            }
        }
    }
}
