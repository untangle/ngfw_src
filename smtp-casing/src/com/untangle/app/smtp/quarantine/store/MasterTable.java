/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine.store;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.app.smtp.quarantine.InboxIndex;
import com.untangle.app.smtp.quarantine.InboxRecord;

//========================================================
// For add/remove operations, we make a copy
// of our "m_summary" reference, so reads
// do not have to be synchronized.  This assumes
// that lookups of account <-> dir are much more
// frequent (at steady state) than additions/removals
// of accounts.

/**
 * Manager of the master records for mapping users to folders, as well as tracking overall size of the store. <br>
 * <br>
 * Assumes that caller has locked any addresses being referenced, unless otherwise noted.
 */
final class MasterTable
{
    private static final String DATA_DIR_NAME = "inboxes";

    private final Logger logger = Logger.getLogger(getClass());

    private String rootDir;
    private StoreSummary summary;

    /**
     * Initialzie instance of MasterTable.
     * @param  dir            String of directory.
     * @param  initialSummary StoreSummary of initial summary.
     * @return                Instance of MasterTable.
     */
    private MasterTable(String dir, StoreSummary initialSummary) {
        rootDir = dir;
        summary = initialSummary;
    }

    /**
     * Open the MasterTable. The InboxDirectoryTree is needed in case the system closed abnormally and the StoreSummary
     * needs to be rebuilt.
     * @param rootDir String of root directory to open.
     * @return instance of MasterTable.
     */
    static MasterTable open(String rootDir)
    {
        Logger logger = Logger.getLogger(MasterTable.class);

        StoreSummary summary = QuarantineStorageManager.readSummary(rootDir);

        boolean needRebuild = (summary == null || summary.getTotalSz() == 0);

        if (needRebuild) {
            StoreSummary storeMeta = new StoreSummary();
            logger.debug("About to scan Inbox directories to rebuild summary");
            visitInboxes(new File(rootDir, DATA_DIR_NAME), storeMeta);
            logger.debug("Done scanning Inbox directories to rebuild summary");
            return new MasterTable(rootDir, storeMeta);
        } else {
            // QuarantineStorageManager.openSummary(rootDir);
            return new MasterTable(rootDir, summary);
        }
    }

    /**
     * Rebuild the MasterTable. The InboxDirectoryTree is needed in case the system closed abnormally and the StoreSummary
     * needs to be rebuilt.
     * @param rootDir String of root directory to open.
     * @return instance of MasterTable.
     */
    static MasterTable rebuild(String rootDir)
    {
        Logger logger = Logger.getLogger(MasterTable.class);
        StoreSummary storeMeta = new StoreSummary();
        logger.debug("About to scan Inbox directories to rebuild summary");
        visitInboxes(new File(rootDir, DATA_DIR_NAME), storeMeta);
        logger.debug("Done scanning Inbox directories to rebuild summary");
        return new MasterTable(rootDir, storeMeta);
    }

    /**
     * Determine if inbox exists.
     * @param  lcAddress String of inbox.
     * @return           true if eixsts, false if not.
     */
    boolean inboxExists(String lcAddress)
    {
        return summary.containsInbox(lcAddress);
    }

    /**
     * Assumes caller has already found that there is no such inbox, while holding the master lock for this account
     * @param address Address for inbox.
     */
    synchronized void addInbox(String address)
    {

        StoreSummary newSummary = new StoreSummary(summary);
        newSummary.addInbox(address, new InboxSummary(address));
        summary = newSummary;
        save();
    }

    /**
     * Delete inbox.
     * @param address Atring of inbox address.
     */
    synchronized void removeInbox(String address)
    {
        StoreSummary newSummary = new StoreSummary(summary);
        newSummary.removeInbox(address);
        summary = newSummary;
        save();
    }

    /**
     * Assumes caller has already determined that this inbox exists.
     * 
     * PRE: address lower case
     * @param address Address of inbox.
     * @param sz Size to check.
     * @return true if inbox exists and added, false otherwise.
     */
    synchronized boolean mailAdded(String address, long sz)
    {
        InboxSummary meta = summary.getInbox(address);
        if (meta == null) {
            return false;
        }
        summary.mailAdded(meta, sz);
        save();
        return true;
    }

    /**
     * Assumes caller has already determined that this inbox exists.
     * 
     * PRE: address lower case
     * @param address Address of inbox.
     * @param sz Size to check.
     * @return true if inbox exists and added, false otherwise.
     */
    synchronized boolean mailRemoved(String address, long sz)
    {
        InboxSummary meta = summary.getInbox(address);
        if (meta == null) {
            return false;
        }
        summary.mailRemoved(meta, sz);
        save();
        return true;
    }

    /**
     * Assumes caller has already determined that this inbox exists.
     * 
     * PRE: address lower case
     * @param address Address of inbox.
     * @param totalSz Size to check.
     * @param totalMails Total mails.
     * @return true if inbox exists and added, false otherwise.
     */
    synchronized boolean updateMailbox(String address, long totalSz, int totalMails)
    {
        InboxSummary meta = summary.getInbox(address);
        if (meta == null) {
            return false;
        }
        summary.updateMailbox(meta, totalSz, totalMails);
        save();
        return true;
    }

    /**
     * Get the sum total of the lengths of all mails across all inboxes.
     * @return Long of size in bytes.
     */
    long getTotalQuarantineSize()
    {
        return summary.getTotalSz();
    }

    /**
     * Get the total number of mails in all inboxes
     * @return Integer of number of messages.
     */
    int getTotalNumMails()
    {
        return summary.getTotalMails();
    }

    /**
     * Get the total number of inboxes
     * @return integer of number of inboxes.
     */
    int getTotalInboxes()
    {
        return summary.size();
    }

    /**
     * Close this table, causing data to be written out to disk. Any subsequent (stray) calls to this object will also
     * cause an update of the on-disk representation of state.
     */
    synchronized void close()
    {
        save();
    }

    /**
     * Save mailbox.
     */
    private void save()
    {
        if (!QuarantineStorageManager.writeSummary(summary, rootDir)) {
            logger.warn("Unable to save StoreSummary.  Next startup " + "will have to rebuild index");
        }
    }

    /**
     * // * Returns null if not found. Note that since this is not // * synchronized, one should call this while holding
     * the // * master account lock to ensure that concurrent // * creation doesn't take place. //
     */
    // RelativeFileName getInboxDir(String address) {
    // InboxSummary meta = m_summary.getInbox(address);
    // return meta==null?
    // null:
    // meta.getDir();
    // }

    /**
     * Do not modify any of the returned entries, as it is a shared reference. The returned set itself is guaranteed
     * never to be modified.
     * @return Set of accounts and their inbox summaries.
     */
    Set<Map.Entry<String, InboxSummary>> entries()
    {
        return summary.entries();
    }

    /**
     * Visit all the inboxes
     * @param dir File of directory.
     * @param storeMeta storeMeta to visit.
     */
    private static void visitInboxes(File dir, StoreSummary storeMeta)
    {
        File[] kids = dir.listFiles();
        if (kids == null)
            return;
        for (File kid : kids) {
            if (kid.isDirectory()) {
                visit(kid, storeMeta);
            }
        }
    }

    /**
     * Visit the given directory and read the summary
     * @param f File of directory.
     * @param storeMeta storeMeta to visit.
     */
    private static void visit(File f, StoreSummary storeMeta)
    {
        String emailAddress = f.getName();
        InboxIndex inboxIndex = QuarantineStorageManager.readQuarantine(emailAddress, f.getAbsolutePath());
        if (inboxIndex != null) {
            long totalSz = 0;
            int totalMails = 0;
            for (InboxRecord record : inboxIndex) {
                totalSz += record.getMailSummary().getQuarantineSize();
                totalMails++;
            }
            storeMeta.addInbox(inboxIndex.getOwnerAddress(), new InboxSummary(f.getName(), totalSz, totalMails));
        }
    }

}
