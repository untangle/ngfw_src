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

package com.untangle.tran.mail.papi.quarantine;

import java.util.Comparator;
import java.util.EnumMap;


/**
 * Class acts as a helper for sorting InboxRecords
 */
public final class InboxRecordComparator {

  /**
   * Ways to sort an InboxRecord
   */
  public static enum SortBy {
    /**
     * 0 = Sort by the date the message was interned
     */
    INTERN_DATE,
    /**
     * 1 = Sort by the message size
     */
    SIZE,
    /**
     * 2 = Sort by the sender of the message
     */
    SENDER,
    /**
     * 3 = Sort by the subject
     */
    SUBJECT,
    /**
     * 4 = Sort by the quarantine detail (score)
     */
    DETAIL,
    /**
     * 5 = Sort by the attachment count
     */
    ATTACHMENT_COUNT    
  };

  private static final EnumMap<SortBy, IRComp> m_fwdComparitors;
  private static final EnumMap<SortBy, IRComp> m_bwdComparitors;

  static {
    m_fwdComparitors = new EnumMap<SortBy, IRComp>(SortBy.class);
    m_bwdComparitors = new EnumMap<SortBy, IRComp>(SortBy.class);

    m_fwdComparitors.put(SortBy.INTERN_DATE, new DateComp().setReverse(false));
    m_bwdComparitors.put(SortBy.INTERN_DATE, new DateComp().setReverse(true));

    m_fwdComparitors.put(SortBy.SIZE, new SizeComp().setReverse(false));
    m_bwdComparitors.put(SortBy.SIZE, new SizeComp().setReverse(true));

    m_fwdComparitors.put(SortBy.SENDER, new SenderComp().setReverse(false));
    m_bwdComparitors.put(SortBy.SENDER, new SenderComp().setReverse(true));

    m_fwdComparitors.put(SortBy.SUBJECT, new SubjectComp().setReverse(false));
    m_bwdComparitors.put(SortBy.SUBJECT, new SubjectComp().setReverse(true));

    m_fwdComparitors.put(SortBy.DETAIL, new DetailComp().setReverse(false));
    m_bwdComparitors.put(SortBy.DETAIL, new DetailComp().setReverse(true));
    
    m_fwdComparitors.put(SortBy.ATTACHMENT_COUNT, new AttachmentComp().setReverse(false));
    m_bwdComparitors.put(SortBy.ATTACHMENT_COUNT, new AttachmentComp().setReverse(true));
  }

  /**
   * Get a Comparitor (for InboxRecords) based on the given
   * criteria.
   *
   * @param criteria how things should be sorted
   * @param forward if true, the forward (normal) ordering.  If false,
   *        backward (reverse) ordering.
   *
   * @return the Comparitor
   */
  public static Comparator<InboxRecord> getComparator(SortBy criteria,
    boolean forward) {
    return (forward?
        m_fwdComparitors:
        m_bwdComparitors).get(criteria);
  }


  //============================ Inner Class ============================
  
  private abstract static class IRComp
    implements Comparator<InboxRecord> {

    private int m_reverse;

    /**
     * Cause the result to be reversed
     */
    IRComp setReverse(boolean reverse) {
      m_reverse = reverse?-1:1;
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

    protected int compareImpl(InboxRecord o1, InboxRecord o2) {
      return o1.getInternDate()<o2.getInternDate()?
        -1:
        o1.getInternDate()>o2.getInternDate()?
          1:0;
    }
  }


  //============================ Inner Class ============================
  
  private static class SizeComp extends IRComp {

    protected int compareImpl(InboxRecord o1, InboxRecord o2) {
      return o1.getSize()<o2.getSize()?
        -1:
        o1.getSize()>o2.getSize()?
          1:0;
    }
  }


  //============================ Inner Class ============================
  
  private static class SenderComp extends IRComp {

    //Null is less than not null
  
    protected int compareImpl(InboxRecord o1, InboxRecord o2) {

      // compare on original sender (not truncated sender)
      // because truncation may drop uniqueness
      String sender1 = o1.getMailSummary()==null?
        null:o1.getMailSummary().getSender();
        
      String sender2 = o2.getMailSummary()==null?
        null:o2.getMailSummary().getSender();
        
      return sender1==null?
        (sender2==null?0:-1)://Sender1 null
        (sender2==null?1:sender1.compareToIgnoreCase(sender2));
    }
  }


  //============================ Inner Class ============================
  
  private static class SubjectComp extends IRComp {

    //Null is less than not null
  
    protected int compareImpl(InboxRecord o1, InboxRecord o2) {

      // compare on original subject (not truncated subject)
      // because truncation may drop uniqueness
      String subject1 = o1.getMailSummary()==null?
        null:o1.getMailSummary().getSubject();
        
      String subject2 = o2.getMailSummary()==null?
        null:o2.getMailSummary().getSubject();
        
      return subject1==null?
        (subject2==null?0:-1)://Subject1 null
        (subject2==null?1:subject1.compareToIgnoreCase(subject2));
    }
  }


  //============================ Inner Class ============================
  
  private static class DetailComp extends IRComp {

    //Null is less than not null
  
    protected int compareImpl(InboxRecord o1, InboxRecord o2) {
    
      String detail1 = o1.getMailSummary()==null?
        null:o1.getMailSummary().getQuarantineDetail();
        
      String detail2 = o2.getMailSummary()==null?
        null:o2.getMailSummary().getQuarantineDetail();
        
      return detail1==null?
        (detail2==null?0:-1)://Sender1 null
        (detail2==null?1:compareDetailsNotNull(detail1, detail2));
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

      return d1IsNum?
        (//D1 is a number
          d2IsNum?
            (//D2 is number
              d1AsNum>d2AsNum?
                1:
                (
                  d1AsNum<d2AsNum?-1:0
                )
            ):
            (1)//D2 not a number
        ):
        (//D1 is not a number
          d2IsNum?
            -1://D2 a number
            d1.compareTo(d2)//Neither is a number
        );
    }
  }

  
  //============================ Inner Class ============================
  
  private static class AttachmentComp extends IRComp {

    protected int compareImpl(InboxRecord o1, InboxRecord o2) {
      return o1.getMailSummary().getAttachmentCount()<o2.getMailSummary().getAttachmentCount()?
        -1:
        o1.getMailSummary().getAttachmentCount()>o2.getMailSummary().getAttachmentCount()?
          1:0;
    }
  }   
}
