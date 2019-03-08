/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine.store;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Information about a given Inbox, as maintained outside of the Inbox itself (i.e. for lookup and reporting purposes). <br>
 * <br>
 * Be careful. Changes to the size/count properties should <b>only</b> be made by the StoreSummary which holds it.
 * 
 * 
 * Assumes all addresses have been lower-cased
 */
@SuppressWarnings("serial")
public final class InboxSummary implements Serializable
{
    private String address;
    private final AtomicLong totalSz;
    private final AtomicInteger totalMails;

    /**
     * Initialize instance of InboxSummary.
     * @return Isntance of InboxSummary.
     */
    public InboxSummary() {
        this(null, 0, 0);
    }

    /**
     * Initialize instance of InboxSummary.
     * @param inbox account name.
     * @return Initialize instance of InboxSummary.
     */
    public InboxSummary(String inbox) {
        this(inbox, 0, 0);
    }

    /**
     * Initialize instance of InboxSummary.
     * @param inbox account name.
     * @param  totalSz    Total size.
     * @param  totalMails Total emails.
     * @return Initialize instance of InboxSummary.
     */
    public InboxSummary(String inbox, long totalSz, int totalMails) {

        this.totalSz = new AtomicLong(totalSz);
        this.totalMails = new AtomicInteger(totalMails);
        setAddress(inbox);
    }

    /**
     * Return inbox address.
     * @return String of address.
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * Write inbox address.
     * @param address String of address.
     */
    public void setAddress(String address)
    {
        this.address = address;
    }

    /**
     * Updates the total size, based on a recalculated value
     * 
     * @param newValue
     *            the new value
     * @return the <b>old</b> value.
     */
    public long updateTotalSz(long newValue)
    {
        return totalSz.getAndSet(newValue);
    }

    /**
     * Add to total size.
     * @param toAdd Amount in bytes to add.
     */
    public void incrementTotalSz(long toAdd)
    {
        totalSz.addAndGet(toAdd);
    }

    /**
     * Remove from total size.
     * @param toSubtract Amount to remove in bytes.
     */
    public void decrementTotalSz(long toSubtract)
    {
        totalSz.addAndGet(-1 * toSubtract);
    }

    /**
     * Return the total size (sum of lengths of all files) for this inbox
     * @return long of total size.
     */
    public long getTotalSz()
    {
        return totalSz.get();
    }

    /**
     * Get the total number of mails in this inbox.
     * @return integer of email count.
     */
    public int getTotalMails()
    {
        return totalMails.get();
    }

    /**
     * Wrtie the total size (sum of lengths of all files) for this inbox
     * @param newVal long of total size.
     */
    public void setTotalSz(long newVal)
    {
        totalSz.set(newVal);
    }

    /**
     * Write the total number of mails in this inbox.
     * @param newVal integer of email count.
     */
    public void setTotalMails(int newVal)
    {
        totalMails.set(newVal);
    }

    /**
     * Updates the total mails, based on a recalculated value
     * 
     * @param newValue
     *            the new value
     * @return the <b>old</b> value.
     */
    public int updateTotalMails(int newValue)
    {
        return totalMails.getAndSet(newValue);
    }

    /**
     * Add to number of total emails.
     * @param toAdd number of emails to add
     */
    public void incrementTotalMails(int toAdd)
    {
        totalMails.addAndGet(toAdd);
    }

    /**
     * Subtract from number of total emails.
     * @param toSubtract number of emails to remove
     */
    public void decrementTotalMails(int toSubtract)
    {
        totalMails.addAndGet(-1 * toSubtract);
    }

    /**
     * Get inbox summary.
     * @return String of summary.
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Address: ").append(getAddress());
        return sb.toString();
    }

    /**
     * Compare this inbox to another.
     * @param  other Other inbix to compare.
     * @return       If true, matches, false otherwise.
     */
    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof InboxSummary)) {
            return false;
        }
        return ((InboxSummary) other).getAddress().equalsIgnoreCase(getAddress());
    }

    /**
     * Return hashcode of mailbox.
     * @return Mailbox hashcode based on address.
     */
    @Override
    public int hashCode()
    {
        return getAddress().hashCode();
    }

}
