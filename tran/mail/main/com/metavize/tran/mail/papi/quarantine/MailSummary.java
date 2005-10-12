/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
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


  public MailSummary() {}

  public MailSummary(String sender,
    String subject,
    String quarantineCategory,
    String quarantineDetail) {

    m_sender = sender;
    m_subject = subject;
    m_quarantineCategory = quarantineCategory;
    m_quarantineDetail = quarantineDetail;
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

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Sender: ").append(getSender()).
      append(", Subject: ").append(getSubject()).
      append(", Cat: ").append(getQuarantineCategory()).
      append(", Detail: ").append(getQuarantineDetail());

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