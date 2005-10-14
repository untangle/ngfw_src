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
package com.metavize.tran.mail.web.euv;

import com.metavize.tran.mail.papi.quarantine.WebConstants;

/**
 * Constants used in this web app
 */
public final class Constants
  extends WebConstants {


  public static final String REQ_DIGEST_ADDR_RP = "draddr";


  /**
   * Page to forward end-users to, if the system is
   * hosed and cannot fufill request.
   */
  public static final String SERVER_UNAVAILABLE_ERRO_VIEW = "/TryLater.jsp";

  /**
   * FWD for requesting a digest email/login
   */
  public static final String REQ_DIGEST_CTL = "/rdc";

  /**
   * View for requesting a digest email/login
   */
  public static final String REQ_DIGEST_VIEW = "/ReqLogin.jsp";

  public static final String INBOX_VIEW = "/Inbox.jsp";

  public static final String INBOX_MAINTENENCE_CTL = "/imc";
  
  
}