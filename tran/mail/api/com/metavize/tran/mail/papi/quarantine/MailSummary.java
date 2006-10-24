/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.papi.quarantine;

import java.io.Serializable;

/**
 * Summary of a mail within the Quarantine.
 */
public final class MailSummary
  implements Serializable {

  private String m_sender;
  private String m_subject;
  private String m_quarantineCategory;
  private String m_quarantineDetail;
  private int m_attachmentCount = 0;
  private long m_quarantineSize;

  public MailSummary() {}

  public MailSummary(String sender,
    String subject,
    String quarantineCategory,
    String quarantineDetail,
    int attachmentCount,
    long quarantineSize) {

    m_sender = sender;
    m_subject = subject;
    m_quarantineCategory = quarantineCategory;
    m_quarantineDetail = quarantineDetail;
    m_attachmentCount = attachmentCount;
    m_quarantineSize = quarantineSize;
  }

  public int getAttachmentCount() {
    return m_attachmentCount;
  }
  public void setAttachmentCount(int attachmentCount) {
    m_attachmentCount = attachmentCount;
  }

  /**
   * Convienence method (needed for Velocity since "getAttachmentCount()>0"
   * seems to have bugs)
   */
  public boolean hasAttachments() {
    return getAttachmentCount()>0;
  }
  
  public String getSender() {
    return m_sender;
  }
  public void setSender(String sender) {
    m_sender = sender;
  }
  public String getSubject() {
    return m_subject;
  }
  public void setSubject(String subject) {
    m_subject = subject;
  }
  public String getQuarantineCategory() {
    return m_quarantineCategory;
  }
  public void setQuarantineCategory(String cat) {
    m_quarantineCategory = cat;
  }
  public String getQuarantineDetail() {
    return m_quarantineDetail;
  }
  public void setQuarantineDetail(String det) {
    m_quarantineDetail = det;
  }
  public long getQuarantineSize() {
    return m_quarantineSize;
  }
  public void setQuarantineSize(long size) {
    m_quarantineSize = size;
  }

  public String getFormattedQuarantineDetail() {
    //Attempts to convert to a formatted float.  If this fails (i.e.
    //it isn't a number) then just return the detail.
    try {
      float f = Float.parseFloat(m_quarantineDetail);
      return String.format("%03.1f", f);
    }
    catch(Exception ex) {
      return m_quarantineDetail;
    }
  }

  public void setFormattedQuarantineDetail(String detail) {
    m_quarantineDetail = detail;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Sender: ").append(getSender()).
      append(", Subject: ").append(getSubject()).
      append(", Cat: ").append(getQuarantineCategory()).
      append(", AttachCount: ").append(getAttachmentCount()).
      append(", Detail: ").append(getQuarantineDetail()).
      append(", Size: ").append(getQuarantineSize());

    return sb.toString();
  }

/*  

  public static void main(String[] args) throws Exception {

    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

    InputStream in = new FileInputStream(args[0]);
  
//    InputSource input = new org.xml.sax.InputSource(in);

    try {
      while(true) {
        Document document = builder.parse(in);
        System.out.println("Got a document");
      }
    }
    catch(Exception ex) {
      System.out.println("Got exception");
      ex.printStackTrace();
    }
  
    System.out.println("MailSummary");
  }
*/  
}
