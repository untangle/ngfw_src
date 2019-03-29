/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine.store;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * POJO representing the state of the Store. Used by the {@link com.untangle.app.smtp.quarantine.store.MasterTable
 * MasterTable} to keep track of the contents of the system. <br>
 * <br>
 * No synchronization, although since the InboxSummary objects are assumed to be shared as this class is copied, it is
 * safe for size/count updates. <br>
 * <br>
 * Assumes all addresses have been lower-cased
 */
@SuppressWarnings("serial")
public class StoreSummary implements Serializable
{
    private final HashMap<String, InboxSummary> map;
    private transient final AtomicLong totalSz;
    private transient final AtomicInteger totalMails;

    /**
     * Iniitalize instance of StoreSummary.
     * @return instance of StoreSummary.
     */
    public StoreSummary() {
        map = new HashMap<>();
        totalSz = new AtomicLong(0);
        totalMails = new AtomicInteger(0);
    }

    /**
     * Create a StoreSummary which shares all contents of <code>copyFrom</code>. Subsequent additions and removals of
     * inboxes from either this Object of <code>copyFrom</code> will not be seen by each other.
     * @param copyFrom StoreSummary to copy from.
     * @return instance of StoreSummary.
     */
    public StoreSummary(StoreSummary copyFrom) {
        map = new HashMap<>(copyFrom.map);
        totalSz = copyFrom.totalSz;
        totalMails = copyFrom.totalMails;
    }

    /**
     * Access the total size of all mails in the system
     * @return total size.
     */
    long getTotalSz()
    {
        return totalSz.get();
    }

    /**
     * Get the total number of mails in the quarantine system
     * @return total email count.
     */
    int getTotalMails()
    {
        return totalMails.get();
    }

    /**
     * Get the total number of inboxes in the system.
     * @return size of inboxes.
     */
    public int size()
    {
        return map.size();
    }

    /**
     * Determine if any mailboxes contain address key.
     * @param  lcAddress Key to search.
     * @return           true if found, false if not.
     */
    public boolean containsInbox(String lcAddress)
    {
        return map.containsKey(lcAddress);
    }

    /**
     * Add inbox.
     * @param lcAddress Address of inbox.
     * @param meta      InboxSummary to initialze with.
     */
    public void addInbox(String lcAddress, InboxSummary meta)
    {
        map.put(lcAddress, meta);
        totalSz.getAndAdd(meta.getTotalSz());
        totalMails.getAndAdd(meta.getTotalMails());
    }

    /**
     * Remove inbox.
     * @param address Address of inbox.
     */
    public void removeInbox(String address)
    {
        InboxSummary doomed = map.remove(address);
        if (doomed != null) {
            totalSz.getAndAdd(-1 * doomed.getTotalSz());
            totalMails.getAndAdd(-1 * doomed.getTotalMails());
        }
    }

    /**
     * Add mail counter.
     * @param inbox Inbox to update.
     * @param sz    Size updated.
     */
    public void mailAdded(InboxSummary inbox, long sz)
    {
        totalSz.getAndAdd(sz);
        totalMails.getAndAdd(1);
        inbox.incrementTotalSz(sz);
        inbox.incrementTotalMails(1);
    }

    /**
     * Decrement mail counter.
     * @param inbox Inbox to update.
     * @param sz    Size updated.
     */
    void mailRemoved(InboxSummary inbox, long sz)
    {
        totalSz.getAndAdd(-1 * sz);
        totalMails.getAndAdd(-1);
        inbox.decrementTotalSz(sz);
        inbox.decrementTotalMails(1);
    }

    /**
     * When scanning (possibly for other reasons) an inbox, we also perform a re-check of the size/count of the inbox.
     * This is the call to perform the update.
     * @param inbox InboxSummary.
     * @param totalSz Total size to update.
     * @param totalMails Total emails to update.
     */
    public void updateMailbox(InboxSummary inbox, long totalSz, int totalMails)
    {
        this.totalSz.getAndAdd(-1 * inbox.updateTotalSz(totalSz));
        this.totalSz.getAndAdd(inbox.getTotalSz());

        this.totalMails.getAndAdd(-1 * inbox.updateTotalMails(totalMails));
        this.totalMails.getAndAdd(inbox.getTotalMails());
    }

    /**
     * Returns null if not found.
     * @param address Email address of inbox.
     * @return InboxSuummary.
     */
    public InboxSummary getInbox(String address)
    {
        return map.get(address);
    }

    /**
     * Do not modify any of the returned entries, as it is a shared reference. The returned set itself is guaranteed
     * never to be modified.
     * @return Set of String, InboxSummary.
     */
    public Set<Map.Entry<String, InboxSummary>> entries()
    {
        return map.entrySet();
    }

    /**
     * Get maip.
     * @return Map of String, InboxSummary.
     */
    public HashMap<String, InboxSummary> getMap()
    {
        return map;
    }

    /**
     * Write map.
     * @param map Map of String, InboxSummary.
     */
    public void setMap(HashMap<String, InboxSummary> map)
    {
        this.map.putAll(map);
    }

}
