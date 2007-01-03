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

package com.untangle.tran.mail.impl.quarantine;
import com.untangle.tran.mail.papi.quarantine.WebConstants;

import java.net.URLEncoder;

/**
 * Little class used to generate links in digest emails.
 * It exists to be "called" from a Velocity template.
 * <br><br>
 * Instance is stateful (it "knows" the current address
 * being templated).
 */
public class LinkGenerator {

  private String m_urlBase;

  LinkGenerator(String base,
    String authTkn) {
    StringBuilder sb = new StringBuilder();
    sb.append("https://");
    sb.append(base);
    sb.append("/quarantine/manageuser?");
    sb.append(WebConstants.AUTH_TOKEN_RP);
    sb.append('=');
    sb.append(URLEncoder.encode(authTkn));
    m_urlBase = sb.toString();
  }


  public String generateInboxLink() {
    return appendNVP(m_urlBase,
      WebConstants.ACTION_RP,
      WebConstants.VIEW_INBOX_RV);
  }
  public String generateHelpLink() {
    return "help_link";
  }

  /**
   * Generate a link to rescue the given mail
   *
   * @param mid the mail ID
   *
   * @return the complete URL ("http://... etc");
   */  
  public String generateRescueLink(String mid) {
    return appendNVP(appendNVP(m_urlBase,
        WebConstants.ACTION_RP,
        WebConstants.RESCUE_RV),
      WebConstants.MAIL_ID_RP, mid);
  }

  /**
   * Generate a link to purge the given mail
   *
   * @param mid the mail ID
   *
   * @return the complete URL ("http://... etc");
   */
  public String generatePurgeLink(String mid) {
    return appendNVP(appendNVP(m_urlBase,
        WebConstants.ACTION_RP,
        WebConstants.PURGE_RV),
      WebConstants.MAIL_ID_RP, mid);
  }

  private String appendNVP(String base, String name, String value) {
    StringBuilder sb = new StringBuilder();
    sb.append(base);
    sb.append('&').append(name).append('=');
    sb.append(URLEncoder.encode(value));
    return sb.toString();
  }

}
