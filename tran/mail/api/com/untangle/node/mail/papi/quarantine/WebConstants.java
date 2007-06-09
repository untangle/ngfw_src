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
package com.untangle.node.mail.papi.quarantine;

public class WebConstants {

    // constant values must be in lower case
    public static final String AUTH_TOKEN_RP = "tkn";
    public static final String ACTION_RP = "action";
    public static final String PURGE_RV = "purge";
    public static final String RESCUE_RV = "rescue";
    public static final String REFRESH_RV = "refresh";
    public static final String SAFELIST_VIEW_RV = "slview";
    public static final String SAFELIST_ADD_RV = "sladd";
    public static final String SAFELIST_REMOVE_RV = "slremove";
    public static final String VIEW_INBOX_RV = "viewibx";
    public static final String MAIL_ID_RP = "mailid";
    //When the "action" is "sladd" or "slremove", this is the
    //key for the value of the (base64 encoded) email address
    public static final String SAFELIST_TARGET_ADDR_RP = "sladdr";

    public static final String UNMAPPER_VIEW_RV = "unmapview";
    public static final String UNMAPPER_REMOVE_RV = "unmapremove";
    //When the "action" is "unmapremove", this is the
    //key for the value of the (base64 encoded) email address
    public static final String UNMAPPER_TARGET_ADDR_RP = "unmapaddr";

    //The action is really redundant, as there is only
    //one "action" for this servlet.  However, with all of
    //the other obnoxious scafolding around web<->java coding
    //what's a few more lines just for consistency sake.
    public static final String MAPPER_DO_REMAP_RV = "remap";
    public static final String MAPPER_VIEW_RV = "remap";
    public static final String MAPPER_TARGET_ADDR_RP = "mapaddr";
}
