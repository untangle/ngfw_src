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

/* Class acts as a helper for sorting Inbox */
public final class InboxComparator
{
    // Ways to sort an InboxRecord
    // - indices are used by Inbox.jsp to execute a sort request
    public static enum SortBy
    {
        ADDRESS, /* 0 => Sort by the address of the inbox. */
        SIZE, /* 1 => Sort by the size of the inbox */
        NUMBER_MESSAGES, /* 2 => Sort by the number of messages in the inbox */
    };
    
    private static final EnumMap<SortBy, IComp> m_fwdComparators;
    private static final EnumMap<SortBy, IComp> m_bwdComparators;
    
    static
    {
        m_fwdComparators = new EnumMap<SortBy, IComp>(SortBy.class);
        m_bwdComparators = new EnumMap<SortBy, IComp>(SortBy.class);

        m_fwdComparators.put( SortBy.ADDRESS, new AddressComp().setReverse( false ));
        m_bwdComparators.put( SortBy.ADDRESS, new AddressComp().setReverse( true ));

        m_fwdComparators.put( SortBy.SIZE, new SizeComp().setReverse( false ));
        m_bwdComparators.put( SortBy.SIZE, new SizeComp().setReverse( true ));

        m_fwdComparators.put(SortBy.NUMBER_MESSAGES, new NumberMessagesComp().setReverse(false));
        m_bwdComparators.put(SortBy.NUMBER_MESSAGES, new NumberMessagesComp().setReverse(true));
    }
    
    /**
     * Get a Comparator (for Inbox) based on the given criteria.
     *
     * @param criteria how things should be sorted
     * @param forward if true, the forward (normal) ordering.  If false,
     *        backward (reverse) ordering.
     *
     * @return the Comparator
     */
    public static Comparator<Inbox> getComparator( SortBy criteria, boolean forward )
    {
        return (forward ? m_fwdComparators : m_bwdComparators).get( criteria );
    }

    //============================ Inner Class ============================

    private abstract static class IComp implements Comparator<Inbox>
    {
        private int m_reverse;

        /* Cause the result to be reversed */
        IComp setReverse( boolean reverse )
        {
            m_reverse = reverse ? -1 : 1;
            return this;
        }

        public final int compare( Inbox i1, Inbox i2 )
        {
            return compareImpl( i1, i2 ) * m_reverse;
        }

        protected abstract int compareImpl( Inbox i1, Inbox i2 );
        
        public boolean equals( Object other )
        {
            return ( other.getClass().equals( this.getClass())) &&
                (((IComp) other).m_reverse == m_reverse );
        }
    }

    //============================ Inner Class ============================

    private static class AddressComp extends IComp
    {
        //Null is less than not null

        // o2 > o1 => -1, o2 < o1 => 1, o2 = o1 => 0
        protected int compareImpl( Inbox i1, Inbox i2 )
        {
            // compare on original sender (not truncated sender)
            // because truncation may drop uniqueness
            String address1 = i1.getAddress();
            String address2 = i2.getAddress();
            
            return address1 == null ? (address2 == null ? 0 : -1) :
                (address2 == null ? 1 : address1.compareToIgnoreCase( address2 ));
        }
    }

    //============================ Inner Class ============================

    private static class SizeComp extends IComp
    {
        //Null is less than not null

        // o2 > o1 => -1, o2 < o1 => 1, o2 = o1 => 0
        protected int compareImpl( Inbox i1, Inbox i2 )
        {
            return i1.getTotalSz() < i2.getTotalSz() ? -1 :
                i1.getTotalSz() > i2.getTotalSz() ? 1 : 0;
        }
    }

    //============================ Inner Class ============================

    private static class NumberMessagesComp extends IComp
    {
        //Null is less than not null

        // o2 > o1 => -1, o2 < o1 => 1, o2 = o1 => 0
        protected int compareImpl( Inbox i1, Inbox i2 )
        {
            return i1.getNumMails() < i2.getNumMails() ? -1 :
                i1.getNumMails() > i2.getNumMails() ? 1 : 0;
        }
    }
}
