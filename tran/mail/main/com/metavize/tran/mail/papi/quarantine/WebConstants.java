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

public class WebConstants {

  public static final String AUTH_TOKEN_RP = "tkn";
  public static final String ACTION_RP = "action";
  public static final String PURGE_RV = "purge";
  public static final String RESCUE_RV = "rescue";
  public static final String SAFELIST_VIEW_RV = "slview";
  public static final String SAFELIST_ADD_RV = "sladd";
  public static final String SAFELIST_REMOVE_RV = "slremove";
  public static final String VIEW_INBOX_RV = "viewibx";
  public static final String MAIL_ID_RP = "mid";

  //When the "action" is "sladd" or "slremove", this is the
  //key for the value of the (base64 encoded) email address
  public static final String SAFELIST_TARGET_ADDR_RP = "sladdr";

}