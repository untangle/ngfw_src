/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.mail.impl.quarantine.store;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import com.untangle.node.mail.papi.quarantine.InboxIndex;
import com.untangle.node.mail.papi.quarantine.InboxRecord;
import com.untangle.node.mail.papi.quarantine.InboxRecordComparator;

/**
 * Private implementation of InboxIndex
 */
@SuppressWarnings("serial")
public final class InboxIndexImpl extends HashMap<String, InboxRecord> implements InboxIndex, Serializable
{

    private String m_address;
    private long m_timestamp;

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
        if(size() == 0) {
            return 0;
        }
        InboxRecord rec = Collections.max(this.values(), InboxRecordComparator.getComparator(
                                                                                             InboxRecordComparator.SortBy.INTERN_DATE, true));
        return rec==null?0:rec.getInternDate();
    }

    public Iterator<InboxRecord> iterator() {
        return values().iterator();
    }

    public int inboxCount() {
        return size();
    }

    public long inboxSize() {
        long inboxSize = 0;
        for(InboxRecord iRecord : this) {
            inboxSize += iRecord.getSize();
        }
        return inboxSize;
    }

    public InboxRecord getRecord(String mailID) {
        return get(mailID);
    }
    public InboxRecord[] getAllRecords() {
        return values().toArray(new InboxRecord[size()]);
    }

    protected void debugPrint() {
        System.out.println("Access: " + getLastAccessTimestamp());
        System.out.println("Owner: " + getOwnerAddress());
        for(InboxRecord record : this) {
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
}
