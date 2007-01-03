/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.openvpn;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Date;


/**
 * Class acts as a helper for sorting ClientConnectEvents
 */
public final class ClientConnectEventComparator {


  /**
   * Ways to sort a ClientConnectEvent
   */
  public static enum SortBy {
    /**
     * Sort by the client address
     */
    IP_ADDRESS,
    /**
     * Sort by the client name
     */
    CLIENT_NAME,
    /**
     * Sort by the session start date
     */
    START_DATE,
    /**
     * Sort by the session end date.  Note that events without an
     * end date are "greater" than those with an end date (i.e. the
     * start date is implicitly 0).
     */
    END_DATE,
    /**
     * Sort by the bytes received by the client
     */
    BYTES_RECEIVED,
    /**
     * Sort by the bytes sent by the client
     */
    BYTES_TRANSMITTED
  };

  private static final EnumMap<SortBy, CCEComp> m_fwdComparitors;
  private static final EnumMap<SortBy, CCEComp> m_bwdComparitors;

  static {
    m_fwdComparitors = new EnumMap<SortBy, CCEComp>(SortBy.class);
    m_bwdComparitors = new EnumMap<SortBy, CCEComp>(SortBy.class);

    m_fwdComparitors.put(SortBy.IP_ADDRESS, new IPAddrComp().setReverse(false));
    m_bwdComparitors.put(SortBy.IP_ADDRESS, new IPAddrComp().setReverse(true));

    m_fwdComparitors.put(SortBy.CLIENT_NAME, new ClientNameComp().setReverse(false));
    m_bwdComparitors.put(SortBy.CLIENT_NAME, new ClientNameComp().setReverse(true));

    m_fwdComparitors.put(SortBy.START_DATE, new StartDateComp().setReverse(false));
    m_bwdComparitors.put(SortBy.START_DATE, new StartDateComp().setReverse(true));

    m_fwdComparitors.put(SortBy.END_DATE, new EndDateComp().setReverse(false));
    m_bwdComparitors.put(SortBy.END_DATE, new EndDateComp().setReverse(true));

    m_fwdComparitors.put(SortBy.BYTES_RECEIVED, new BytesRcvComp().setReverse(false));
    m_bwdComparitors.put(SortBy.BYTES_RECEIVED, new BytesRcvComp().setReverse(true));

    m_fwdComparitors.put(SortBy.BYTES_TRANSMITTED, new BytesSentComp().setReverse(false));
    m_bwdComparitors.put(SortBy.BYTES_TRANSMITTED, new BytesSentComp().setReverse(true));

  }

  /**
   * Get a Comparitor (for ClientConnectEvents) based on the given
   * criteria.
   *
   * @param criteria how things should be sorted
   * @param forward if true, the forward (normal) ordering.  If false,
   *        backward (reverse) ordering.
   *
   * @return the Comparitor
   */
  public static Comparator<ClientConnectEvent> getComparator(SortBy criteria,
    boolean forward) {
    return (forward?
        m_fwdComparitors:
        m_bwdComparitors).get(criteria);
  }


  //============================ Inner Class ============================
  
  private abstract static class CCEComp
    implements Comparator<ClientConnectEvent> {

    private int m_reverse;

    /**
     * Cause the result to be reversed
     */
    CCEComp setReverse(boolean reverse) {
      m_reverse = reverse?-1:1;
      return this;
    }
    
    public final int compare(ClientConnectEvent o1, ClientConnectEvent o2) {
      return compareImpl(o1, o2) * m_reverse;
    }

    protected abstract int compareImpl(ClientConnectEvent o1, ClientConnectEvent o2);

    public boolean equals(Object other) {
      return (other.getClass().equals(this.getClass())) &&
        (((CCEComp) other).m_reverse == m_reverse);
    }    
        
  }

  
  //============================ Inner Class ============================
  
  private static class IPAddrComp extends CCEComp {
    protected int compareImpl(ClientConnectEvent o1, ClientConnectEvent o2) {
      return o1.getAddress().compareTo(o2.getAddress());
    }
  }

  
  //============================ Inner Class ============================
  
  private static class ClientNameComp extends CCEComp {
    protected int compareImpl(ClientConnectEvent o1, ClientConnectEvent o2) {
      String name1 = o1.getClientName();
      String name2 = o2.getClientName();

      if(name1==null) {
        if(name2 == null) {
          return 0;
        }
        return -1;
      }
      else if(name2==null) {
        if(name1==null) {
          return 0;
        }
        return 1;
      }
      else {
        return name1.compareTo(name2);
      }
    }
  }

  
  //============================ Inner Class ============================
  
  private abstract static class DateComp extends CCEComp {
    protected final int compareDates(Date d1, Date d2) {
      return d1==null?
        (d2==null?0:1):
        (d2==null?-1:d1.compareTo(d2));
    }
  }

  
  //============================ Inner Class ============================
  
  private static class StartDateComp extends DateComp {
    protected int compareImpl(ClientConnectEvent o1, ClientConnectEvent o2) {
      return compareDates(o1.getStart(), o2.getStart());
    }
  }

  
  //============================ Inner Class ============================
  
  private static class EndDateComp extends DateComp {
    protected int compareImpl(ClientConnectEvent o1, ClientConnectEvent o2) {
      return compareDates(o1.getEnd(), o2.getEnd());
    }
  }  

  
  //============================ Inner Class ============================
  
  private static class BytesSentComp extends CCEComp {
    protected int compareImpl(ClientConnectEvent o1, ClientConnectEvent o2) {
      long b1 = o1.getBytesTx();
      long b2 = o2.getBytesTx();

      return b1>b2?1:b1==b2?0:-1;
    }
  }


  
  //============================ Inner Class ============================
  
  private static class BytesRcvComp extends CCEComp {
    protected int compareImpl(ClientConnectEvent o1, ClientConnectEvent o2) {
      long b1 = o1.getBytesRx();
      long b2 = o2.getBytesRx();

      return b1>b2?1:b1==b2?0:-1;
    }
  }
  

}