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
package com.untangle.tran.mail.web.euv;

import com.untangle.tran.mail.papi.quarantine.WebConstants;

/**
 * Constants used in this web app
 */
public final class Constants
  extends WebConstants {

  public static final String REQ_DIGEST_ADDR_RP = "draddr";

  public static final String SORT_BY_RP = "sort";
  public static final String SORT_ASCEND_RP = "ascend";
  public static final String FIRST_RECORD_RP = "first";

  public static final int RECORDS_PER_PAGE = 25;

  /**
   * Page to forward end-users to, if the system is
   * hosed and cannot fufill request.
   */
  public static final String SERVER_UNAVAILABLE_ERRO_VIEW = "/TryLater.jsp";

  /**
   * FWD for requesting a digest email/login
   */
  public static final String REQ_DIGEST_CTL = "/requestdigest";

  /**
   * View for requesting a digest email/login
   */
  public static final String REQ_DIGEST_VIEW = "/ReqLogin.jsp";
  public static final String INVALID_PORTAL_EMAIL = "/InvalidPortalEmail.jsp";

  public static final String INBOX_VIEW = "/Inbox.jsp";

  public static final String SAFELIST_VIEW = "/Safelist.jsp";

  public static final String UNMAP_ADDRESS_VIEW = "/UnmapAddress.jsp";

  public static final String INBOX_MAINTENENCE_CTL = "/manageuser";

  public static final String UNMAP_CTL = "/unmp";

  public static final String MAP_CTL = "/mp";
  public static final String MAP_ADDRESS_VIEW = "/Rmap.jsp";
}
