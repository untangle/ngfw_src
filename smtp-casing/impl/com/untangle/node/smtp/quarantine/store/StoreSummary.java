/**
 * $Id$
 */
package com.untangle.node.smtp.quarantine.store;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * POJO representing the state of the Store.  Used
 * by the {@link com.untangle.node.smtp.quarantine.store.MasterTable MasterTable}
 * to keep track of the contents of the system.
 * <br><br>
 * No synchronization, although since the InboxSummary
 * objects are assumed to be shared as this class
 * is copied, it is safe for size/count updates.
 * <br><br>
 * Assumes all addresses have been lower-cased
 */
class StoreSummary
{
    private final HashMap<String, InboxSummary> m_map;
    private final AtomicLong m_totalSz;
    private final AtomicInteger m_totalMails;

    StoreSummary()
    {
        m_map = new HashMap<String, InboxSummary>();
        m_totalSz = new AtomicLong(0);
        m_totalMails = new AtomicInteger(0);
    }

    /**
     * Create a StoreSummary which shares all
     * contents of <code>copyFrom</code>.  Subsequent
     * additions and removals of inboxes from either
     * this Object of <code>copyFrom</code> will not
     * be seen by each other.
     *
     */
    StoreSummary(StoreSummary copyFrom)
    {
        m_map = new HashMap<String, InboxSummary>(copyFrom.m_map);
        m_totalSz = copyFrom.m_totalSz;
        m_totalMails = copyFrom.m_totalMails;
    }

    /**
     * Access the total size of all mails
     * in the system
     */
    long getTotalSz()
    {
        return m_totalSz.get();
    }

    /**
     * Get the total number of mails in the quarantine system
     */
    int getTotalMails()
    {
        return m_totalMails.get();
    }

    /**
     * Get the total number of inboxes in the system.
     */
    int size()
    {
        return m_map.size();
    }

    boolean containsInbox(String lcAddress)
    {
        return m_map.containsKey(lcAddress);
    }

    void addInbox(String lcAddress, InboxSummary meta)
    {
        m_map.put(lcAddress, meta);
        m_totalSz.getAndAdd(meta.getTotalSz());
        m_totalMails.getAndAdd(meta.getTotalMails());
    }

    /**
     *
     */
    void removeInbox(String address)
    {
        InboxSummary doomed = m_map.remove(address);
        if(doomed != null) {
            m_totalSz.getAndAdd(-1*doomed.getTotalSz());
            m_totalMails.getAndAdd(-1*doomed.getTotalMails());
        }
    }

    void mailAdded(InboxSummary inbox, long sz)
    {
        m_totalSz.getAndAdd(sz);
        m_totalMails.getAndAdd(1);
        inbox.incrementTotalSz(sz);
        inbox.incrementTotalMails(1);
    }

    void mailRemoved(InboxSummary inbox, long sz)
    {
        m_totalSz.getAndAdd(-1*sz);
        m_totalMails.getAndAdd(-1);
        inbox.decrementTotalSz(sz);
        inbox.decrementTotalMails(1);
    }

    /**
     * When scanning (possibly for other reasons) an
     * inbox, we also perform a re-check of the
     * size/count of the inbox.  This is the
     * call to perform the update.
     */
    void updateMailbox(InboxSummary inbox, long totalSz, int totalMails)
    {
        m_totalSz.getAndAdd(-1*inbox.updateTotalSz(totalSz));
        m_totalSz.getAndAdd(inbox.getTotalSz());

        m_totalMails.getAndAdd(-1*inbox.updateTotalMails(totalMails));
        m_totalMails.getAndAdd(inbox.getTotalMails());
    }

    /**
     * Returns null if not found.
     */
    InboxSummary getInbox(String address)
    {
        return m_map.get(address);
    }

    /**
     * Do not modify any of the returned entries, as it is a shared
     * reference.  The returned set itself is guaranteed never
     * to be modified.
     */
    Set<Map.Entry<String,InboxSummary>> entries()
    {
        return m_map.entrySet();
    }
}
