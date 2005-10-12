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

package com.metavize.tran.mail.impl.quarantine;

/**
 * Little class used to generate links in digest emails.
 * It exists to be "called" from a Velocity template.
 * <br><br>
 * Instance is stateful (it "knows" the current address
 * being templated).
 */
public class LinkGenerator {


  public String generateInboxLink() {
    return "inbox_link";
  }
  public String generateHelpLink() {
    return "help_link";
  }
  public String generateRescueLink(String mid) {
    return "rescue_" + mid + "_link";
  }
  public String generatePurgeLink(String mid) {
    return "purge_" + mid + "_link";
  }  

}