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

    public InboxSummary() {
        this(null, 0, 0);
    }

    public InboxSummary(String inbox) {
        this(inbox, 0, 0);
    }

    public InboxSummary(String inbox, long totalSz, int totalMails) {

        this.totalSz = new AtomicLong(totalSz);
        this.totalMails = new AtomicInteger(totalMails);
        setAddress(inbox);
    }

    public String getAddress()
    {
        return address;
    }

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

    public void incrementTotalSz(long toAdd)
    {
        totalSz.addAndGet(toAdd);
    }

    public void decrementTotalSz(long toSubtract)
    {
        totalSz.addAndGet(-1 * toSubtract);
    }

    /**
     * Get the total size (sum of lengths of all files) for this inbox
     */
    public long getTotalSz()
    {
        return totalSz.get();
    }

    /**
     * Get the total number of mails in this inbox.
     */
    public int getTotalMails()
    {
        return totalMails.get();
    }

    public void setTotalSz(long newVal)
    {
        totalSz.set(newVal);
    }

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

    public void incrementTotalMails(int toAdd)
    {
        totalMails.addAndGet(toAdd);
    }

    public void decrementTotalMails(int toSubtract)
    {
        totalMails.addAndGet(-1 * toSubtract);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Address: ").append(getAddress());
        return sb.toString();
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof InboxSummary)) {
            return false;
        }
        return ((InboxSummary) other).getAddress().equalsIgnoreCase(getAddress());
    }

    @Override
    public int hashCode()
    {
        return getAddress().hashCode();
    }

}
