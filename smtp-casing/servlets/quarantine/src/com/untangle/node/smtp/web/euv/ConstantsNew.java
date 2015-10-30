/**
 * $Id: Constants.java 36445 2013-11-20 00:04:22Z dmorris $
 */
package com.untangle.node.smtp.web.euv;

import com.untangle.node.smtp.quarantine.WebConstants;

/**
 * Constants used in this web app
 */
public final class ConstantsNew extends WebConstants
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
    public static final String REQ_DIGEST_VIEW = "/WEB-INF/jsp/request.jsp";

    public static final String INBOX_VIEW = "/WEB-INF/jsp/inbox.jsp";
}
