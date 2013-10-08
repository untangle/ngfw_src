/**
 * $Id: Constants.java 34293 2013-03-17 05:22:02Z dmorris $
 */
package com.untangle.node.smtp.web.euv;

import com.untangle.node.smtp.quarantine.WebConstants;

/**
 * Constants used in this web app
 */
public final class Constants extends WebConstants
{
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
