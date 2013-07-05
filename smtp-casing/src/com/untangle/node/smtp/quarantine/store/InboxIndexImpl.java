/**
 * $Id$
 */
package com.untangle.node.smtp.quarantine.store;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import com.untangle.node.smtp.quarantine.InboxIndex;
import com.untangle.node.smtp.quarantine.InboxRecord;
import com.untangle.node.smtp.quarantine.InboxRecordComparator;

/**
 * Private implementation of InboxIndex
 */
@SuppressWarnings("serial")
public final class InboxIndexImpl implements InboxIndex, Serializable
{

    private String m_address;
    private long m_timestamp;
    
    private HashMap<String, InboxRecord> inboxMap = new HashMap<String, InboxRecord>();

    public void setOwnerAddress(String address) {
        m_address = address;
    }

    public String getOwnerAddress() {
        return m_address;
    }

    public long getLastAccessTimestamp() {
        return m_timestamp;
    }
    public void setLastAccessTimestamp(long timestamp) {
        m_timestamp = timestamp;
    }

    /**
     * Helper method which returns the timestamp of the most recently
     * added mail, or 0 if the inbox is empty.
     */
    public long getNewestMailTimestamp() {
        if(inboxMap.size() == 0) {
            return 0;
        }
        InboxRecord rec = Collections.max(inboxMap.values(), InboxRecordComparator.getComparator(
                                                                                             InboxRecordComparator.SortBy.INTERN_DATE, true));
        return rec==null?0:rec.getInternDate();
    }

    public Iterator<InboxRecord> iterator() {
        return inboxMap.values().iterator();
    }

    public int inboxCount() {
        return inboxMap.size();
    }

    public long inboxSize() {
        long inboxSize = 0;
        for(InboxRecord iRecord : this.inboxMap.values()) {
            inboxSize += iRecord.getSize();
        }
        return inboxSize;
    }

    public InboxRecord getRecord(String mailID) {
        return inboxMap.get(mailID);
    }
    public InboxRecord[] getAllRecords() {
        return inboxMap.values().toArray(new InboxRecord[inboxMap.size()]);
    }
    
    public HashMap<String, InboxRecord> getInboxMap()
    {
        return inboxMap;
    }
    
    public void setInboxMap(HashMap<String, InboxRecord> inboxMap)
    {
        this.inboxMap = inboxMap;
    }

    protected void debugPrint() {
        System.out.println("Access: " + getLastAccessTimestamp());
        System.out.println("Owner: " + getOwnerAddress());
        for(InboxRecord record : this.inboxMap.values()) {
            System.out.println("----- RECORD -----");
            System.out.println("\tID: " + record.getMailID());
            System.out.println("\tDate: " + record.getInternDate());
            System.out.println("\tSize: " + record.getSize());
            System.out.println("\tSender: " + record.getMailSummary().getTruncatedSender());
            System.out.println("\tSubject: " + record.getMailSummary().getTruncatedSubject());
            System.out.println("\tAttchCount: " + record.getMailSummary().getAttachmentCount());
            System.out.println("\tCat: " + record.getMailSummary().getQuarantineCategory());
            System.out.println("\tDetail: " + record.getMailSummary().getQuarantineDetail());
        }
    }

    @Override
    public int size()
    {
        return inboxMap.size();
    }
    
    
}
