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

    private MasterTable(String dir, StoreSummary initialSummary) {
        rootDir = dir;
        summary = initialSummary;
    }

    /**
     * Open the MasterTable. The InboxDirectoryTree is needed in case the system closed abnormally and the StoreSummary
     * needs to be rebuilt.
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

    static MasterTable rebuild(String rootDir)
    {
        Logger logger = Logger.getLogger(MasterTable.class);
        StoreSummary storeMeta = new StoreSummary();
        logger.debug("About to scan Inbox directories to rebuild summary");
        visitInboxes(new File(rootDir, DATA_DIR_NAME), storeMeta);
        logger.debug("Done scanning Inbox directories to rebuild summary");
        return new MasterTable(rootDir, storeMeta);
    }

    boolean inboxExists(String lcAddress)
    {
        return summary.containsInbox(lcAddress);
    }

    /**
     * Assumes caller has already found that there is no such inbox, while holding the master lock for this account
     */
    synchronized void addInbox(String address)
    {

        StoreSummary newSummary = new StoreSummary(summary);
        newSummary.addInbox(address, new InboxSummary(address));
        summary = newSummary;
        save();
    }

    /**
     *
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
     */
    long getTotalQuarantineSize()
    {
        return summary.getTotalSz();
    }

    /**
     * Get the total number of mails in all inboxes
     */
    int getTotalNumMails()
    {
        return summary.getTotalMails();
    }

    /**
     * Get the total number of inboxes
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
     */
    Set<Map.Entry<String, InboxSummary>> entries()
    {
        return summary.entries();
    }

    /**
     * Visit all the inboxes
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
     * 
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
