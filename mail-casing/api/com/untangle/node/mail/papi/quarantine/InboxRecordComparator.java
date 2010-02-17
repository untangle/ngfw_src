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

package com.untangle.node.mail.papi.quarantine;

import java.util.Comparator;
import java.util.EnumMap;

/* Class acts as a helper for sorting InboxRecords */
public final class InboxRecordComparator {
    // Ways to sort an InboxRecord
    // - indices are used by Inbox.jsp to execute a sort request
    public static enum SortBy {
        INTERN_DATE, /* 0 => Sort by the date the message was interned */
        SIZE, /* 1 => Sort by the message size */
        SENDER, /* 2 => Sort by the sender of the message */
        SUBJECT, /* 3 => Sort by the subject */
        DETAIL, /* 4 => Sort by the quarantine detail (score) */
        ATTACHMENT_COUNT; /* 5 => Sort by the attachment count */
    };

    private static final EnumMap<SortBy, IRComp> m_fwdComparators;
    private static final EnumMap<SortBy, IRComp> m_bwdComparators;

    static {
        m_fwdComparators = new EnumMap<SortBy, IRComp>(SortBy.class);
        m_bwdComparators = new EnumMap<SortBy, IRComp>(SortBy.class);

        m_fwdComparators.put(SortBy.INTERN_DATE, new DateComp().setReverse(false));
        m_bwdComparators.put(SortBy.INTERN_DATE, new DateComp().setReverse(true));

        m_fwdComparators.put(SortBy.SIZE, new SizeComp().setReverse(false));
        m_bwdComparators.put(SortBy.SIZE, new SizeComp().setReverse(true));
        
        m_fwdComparators.put(SortBy.SENDER, new SenderComp().setReverse(false));
        m_bwdComparators.put(SortBy.SENDER, new SenderComp().setReverse(true));

        m_fwdComparators.put(SortBy.SUBJECT, new SubjectComp().setReverse(false));
        m_bwdComparators.put(SortBy.SUBJECT, new SubjectComp().setReverse(true));

        m_fwdComparators.put(SortBy.DETAIL, new DetailComp().setReverse(false));
        m_bwdComparators.put(SortBy.DETAIL, new DetailComp().setReverse(true));

        m_fwdComparators.put(SortBy.ATTACHMENT_COUNT, new AttachmentComp().setReverse(false));
        m_bwdComparators.put(SortBy.ATTACHMENT_COUNT, new AttachmentComp().setReverse(true));
    }

    /**
     * Get a Comparator (for InboxRecords) based on the given criteria.
     *
     * @param criteria how things should be sorted
     * @param forward if true, the forward (normal) ordering.  If false,
     *        backward (reverse) ordering.
     *
     * @return the Comparator
     */
    public static Comparator<InboxRecord> getComparator(SortBy criteria, boolean forward) {
        return (forward ? m_fwdComparators : m_bwdComparators).get(criteria);
    }

    //============================ Inner Class ============================

    private abstract static class IRComp implements Comparator<InboxRecord> {

        private int m_reverse;

        /* Cause the result to be reversed */
        IRComp setReverse(boolean reverse) {
            m_reverse = reverse ? -1 : 1;
            return this;
        }

        public final int compare(InboxRecord o1, InboxRecord o2) {
            return compareImpl(o1, o2) * m_reverse;
        }

        protected abstract int compareImpl(InboxRecord o1, InboxRecord o2);

        public boolean equals(Object other) {
            return (other.getClass().equals(this.getClass())) &&
                (((IRComp) other).m_reverse == m_reverse);
        }
    }

    //============================ Inner Class ============================

    private static class DateComp extends IRComp {

        // o2 > o1 => -1, o2 < o1 => 1, o2 = o1 => 0
        protected int compareImpl(InboxRecord o1, InboxRecord o2) {
            return o1.getInternDate() < o2.getInternDate() ? -1 :
                o1.getInternDate() > o2.getInternDate() ? 1 : 0;
        }
    }

    //============================ Inner Class ============================

    private static class SizeComp extends IRComp {

        // o2 > o1 => -1, o2 < o1 => 1, o2 = o1 => 0
        protected int compareImpl(InboxRecord o1, InboxRecord o2) {
            return o1.getSize() < o2.getSize() ? -1 :
                o1.getSize() > o2.getSize() ? 1 : 0;
        }
    }

    //============================ Inner Class ============================

    private static class SenderComp extends IRComp {
        //Null is less than not null

        // o2 > o1 => -1, o2 < o1 => 1, o2 = o1 => 0
        protected int compareImpl(InboxRecord o1, InboxRecord o2) {
            // compare on original sender (not truncated sender)
            // because truncation may drop uniqueness
            String sender1 = o1.getMailSummary() == null ? null :
                o1.getMailSummary().getSender();

            String sender2 = o2.getMailSummary() == null ? null :
                o2.getMailSummary().getSender();

            return sender1 == null ? (sender2 == null ? 0 : -1) :
                (sender2 == null ? 1 : sender1.compareToIgnoreCase(sender2));
        }
    }

    //============================ Inner Class ============================

    private static class SubjectComp extends IRComp {
        //Null is less than not null

        // o2 > o1 => -1, o2 < o1 => 1, o2 = o1 => 0
        protected int compareImpl(InboxRecord o1, InboxRecord o2) {
            // compare on original subject (not truncated subject)
            // because truncation may drop uniqueness
            String subject1 = o1.getMailSummary() == null ? null :
                o1.getMailSummary().getSubject();

            String subject2 = o2.getMailSummary() == null ? null :
                o2.getMailSummary().getSubject();

            return subject1 == null ? (subject2 == null ? 0 : -1):
                (subject2 == null ? 1 : subject1.compareToIgnoreCase(subject2));
        }
    }

    //============================ Inner Class ============================

    private static class DetailComp extends IRComp {
        //Null is less than not null

        // o2 > o1 => -1, o2 < o1 => 1, o2 = o1 => 0
        protected int compareImpl(InboxRecord o1, InboxRecord o2) {

            String detail1 = o1.getMailSummary() == null ? null :
                o1.getMailSummary().getQuarantineDetail();

            String detail2 = o2.getMailSummary() == null ? null :
                o2.getMailSummary().getQuarantineDetail();

            return detail1 == null ? (detail2 == null ? 0 : -1) :
                (detail2 == null ? 1 : compareDetailsNotNull(detail1, detail2));
        }

        private int compareDetailsNotNull(String d1, String d2) {
            //Likely that the Strings are numbers.  In such a case,
            //numbers are "greater-than" Strings.
            boolean d1IsNum = false;
            boolean d2IsNum = false;
            float d1AsNum = 0;
            float d2AsNum = 0;

            try {
                d1AsNum = Float.parseFloat(d1);
                d1IsNum = true;
            }
            catch(Exception ignore) {d1IsNum = false;}

            try {
                d2AsNum = Float.parseFloat(d2);
                d2IsNum = true;
            }
            catch(Exception ignore) {d2IsNum = false;}

            return d1IsNum ?
                (//D1 is a number
                    d2IsNum ?
                    (//D2 is number
                        d1AsNum > d2AsNum ? 1 : (d1AsNum < d2AsNum ? -1 : 0)
                    ) : (1) //D2 not a number
                ) :
                (//D1 is not a number
                    d2IsNum ? -1 : //D2 is a number
                    d1.compareTo(d2) //Neither is a number (compare as strings)
                );
        }
    }

    //============================ Inner Class ============================

    private static class AttachmentComp extends IRComp {

        // o2 > o1 => -1, o2 < o1 => 1, o2 = o1 => 0
        protected int compareImpl(InboxRecord o1, InboxRecord o2) {
            return o1.getMailSummary().getAttachmentCount() < o2.getMailSummary().getAttachmentCount() ? -1 :
                o1.getMailSummary().getAttachmentCount() > o2.getMailSummary().getAttachmentCount() ? 1 : 0;
        }
    }
}
