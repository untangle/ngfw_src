/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.smtp.web.euv;

import com.untangle.node.smtp.quarantine.WebConstants;

/**
 * Constants used in this web app
 */
public final class Constants extends WebConstants {
    public static final String REQ_DIGEST_ADDR_RP = "draddr";

    public static final String SORT_BY_RP = "sort";
    public static final String SORT_ASCEND_RP = "ascend";
    public static final String FIRST_RECORD_RP = "first";

    public static final String ROWS_PER_PAGE_RP = "rowsperpage";
    public static final int RECORDS_PER_PAGE = 25;

    /**
     * Page to forward end-users to, if the system is
     * hosed and cannot fufill request.
     */
    public static final String SERVER_UNAVAILABLE_ERRO_VIEW = "/TryLater.jsp";

    /**
     * View for requesting a digest email/login
     */
    public static final String REQ_DIGEST_VIEW = "/request.jspx";

    public static final String INBOX_VIEW = "/inbox.jspx";

    public static final String INBOX_MAINTENENCE_CTL = "/manageuser";
}
