/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Inbox index.
 */
@SuppressWarnings("serial")
public final class InboxIndex implements Serializable, Iterable<InboxRecord>
{
    private String m_address;
    private long m_timestamp;

    private HashMap<String, InboxRecord> inboxMap = new HashMap<String, InboxRecord>();

    /**
     * Write owner address.
     * @param address owner email address.
     */
    public void setOwnerAddress(String address)
    {
        m_address = address;
    }

    /**
     * Retrieve owner address.
     * @return String of owner address.
     */
    public String getOwnerAddress()
    {
        return m_address;
    }

    /**
     * Retrieve last access time stamp.
     * @return Long of last acesss timestamp.
     */
    public long getLastAccessTimestamp()
    {
        return m_timestamp;
    }

    /**
     * Write last access time stamp.
     * @param timestamp Long of last acesss timestamp.
     */
    public void setLastAccessTimestamp(long timestamp)
    {
        m_timestamp = timestamp;
    }

    /**
     * Helper method which returns the timestamp of the most recently added mail, or 0 if the inbox is empty.
     * @return newest email timestamp.
     */
    public long getNewestMailTimestamp()
    {
        if (inboxMap.size() == 0) {
            return 0;
        }
        InboxRecord rec = Collections.max(inboxMap.values(), new Comparator<InboxRecord>()
        {
            /**
             * Compare two inbox records.
             * @param  o1 InboxRecord
             * @param  o2 InboxRecord
             * @return    Compare value of the two records.
             */
            @Override
            public int compare(InboxRecord o1, InboxRecord o2)
            {
                return o1.getInternDate() < o2.getInternDate() ? -1 : o1.getInternDate() > o2.getInternDate() ? 1 : 0;
            }

        });
        return rec == null ? 0 : rec.getInternDate();
    }

    /**
     * Inbox iterator
     * @return Iterator for inbox.
     */
    public Iterator<InboxRecord> iterator()
    {
        return inboxMap.values().iterator();
    }

    /**
     * Inbox count.
     * @return int number of messages in inbox.
     */
    public int inboxCount()
    {
        return inboxMap.size();
    }

    /**
     * Return size of inbox.
     * @return Long of size of mailbox.
     */
    public long inboxSize()
    {
        long inboxSize = 0;
        for (InboxRecord iRecord : this.inboxMap.values()) {
            inboxSize += iRecord.getMailSummary().getQuarantineSize();
        }
        return inboxSize;
    }

    /**
     * Retreive record from mail id.
     * @param  mailID Message ID to search.
     * @return        InboxRecord.
     */
    public InboxRecord getRecord(String mailID)
    {
        return inboxMap.get(mailID);
    }

    /**
     * Retrieve all inbox records.
     * @return Array of InboxRecord.
     */
    public InboxRecord[] allRecords()
    {
        return inboxMap.values().toArray(new InboxRecord[inboxMap.size()]);
    }

    /**
     * Retrieve inbox map.
     * @return Map of email address to inbox record.
     */
    public HashMap<String, InboxRecord> getInboxMap()
    {
        return inboxMap;
    }

    /**
     * Write inbox map.
     * @param inboxMap Map of email address to inbox recods.
     */
    public void setInboxMap(HashMap<String, InboxRecord> inboxMap)
    {
        this.inboxMap = inboxMap;
    }

    /**
     * Print debug inbox information.
     */
    protected void debugPrint()
    {
        System.out.println("Access: " + getLastAccessTimestamp());
        System.out.println("Owner: " + getOwnerAddress());
        for (InboxRecord record : this.inboxMap.values()) {
            System.out.println("----- RECORD -----");
            System.out.println("\tID: " + record.getMailID());
            System.out.println("\tDate: " + record.getInternDate());
            System.out.println("\tSize: " + record.getMailSummary().getQuarantineSize());
            System.out.println("\tSender: " + record.getMailSummary().getTruncatedSender());
            System.out.println("\tSubject: " + record.getMailSummary().getTruncatedSubject());
            System.out.println("\tAttchCount: " + record.getMailSummary().getAttachmentCount());
            System.out.println("\tCat: " + record.getMailSummary().getQuarantineCategory());
            System.out.println("\tDetail: " + record.getMailSummary().getQuarantineDetail());
        }
    }

    /**
     * Return size of inbox.
     * @return Size of inbox.
     */
    public int size()
    {
        return inboxMap.size();
    }

}
