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

package com.untangle.uvm.engine;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.log4j.Logger;

/**
 * Sends a friendly error page when a problem occurs.
 *
 * The error message is supplied by either setting the system property
 * {@link #UVM_WEB_MESSAGE_ATTR} or by using the {@link
 * HttpServletResponse.sendError(int, String))} method.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class UvmErrorReportValve extends ErrorReportValve
{
    public static final String UVM_WEB_MESSAGE_ATTR = "com.untangle.uvm.web.message";

    private static final Logger logger = Logger.getLogger(UvmErrorReportValve.class);

    protected void report(Request request, Response response,
                          Throwable throwable)
        throws IOException
    {
        try {
            doReport(request, response, throwable);
        } catch (Throwable t) {
            logger.warn("could not make error page", t);
        }
    }

    protected void doReport(Request request, Response response,
                            Throwable throwable)
        throws IOException
    {
        int statusCode = response.getStatus();

        if ((statusCode < 400) || (response.getContentCount() > 0)) {
            return;
        }

        Map<String, String> i18nMap = getTranslations();

        String errorMessage = null;

        Object o = request.getAttribute(UVM_WEB_MESSAGE_ATTR);
        if (o instanceof String) {
            errorMessage = (String)o;
        }

        if (null == errorMessage) {
            errorMessage = getError(i18nMap, statusCode);
        }

        errorMessage = RequestUtil.filter(errorMessage);

        response.setContentType("text/html");
        response.setCharacterEncoding("utf-8");
        PrintWriter writer = response.getReporter();
        if (null != writer) {
            writeReport(writer, i18nMap, errorMessage);
        }
    }

    private void writeReport(PrintWriter w, Map<String, String> i18nMap,
                             String errorMessage)
        throws IOException
    {
        w.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");
        w.write("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
        w.write("<head>\n");
        w.write("<title>");
        w.write(tr(i18nMap, "Untangle Server"));
        w.write("</title>\n");
        w.write("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=iso-8859-1\" />\n");
        w.write("<style type=\"text/css\">\n");
        w.write("/* <![CDATA[ */\n");
        w.write("@import url(/images/base.css);\n");
        w.write("/* ]]> */\n");
        w.write("</style>\n");
        w.write("</head>\n");
        w.write("<body>\n");
        w.write("<div id=\"main\" style=\"width:500px;margin:50px auto 0 auto;\">\n");
        w.write("<div class=\"main-top-left\"></div><div class=\"main-top-right\"></div><div class=\"main-mid-left\"><div class=\"main-mid-right\"><div class=\"main-mid\">\n");
        w.write("<center>");
        w.write("<img alt=\"\" src=\"/images/BrandingLogo.gif\" /><br /><br />\n");
        w.write("<b>");
        w.write(tr(i18nMap, "Untangle Server"));
        w.write("</b><br /><br />\n");
        w.write("<em>");  w.write(errorMessage);  w.write("</em>\n");
        w.write("</center><br /><br />\n");
        w.write("</div></div></div><div class=\"main-bot-left\"></div><div class=\"main-bot-right\"></div>\n");
        w.write("</div>\n");
        w.write("</body>\n");
        w.write("</html>\n");
    }

    private Map<String, String> getTranslations()
    {
        return Main.getMain().getTranslations("bootstrap");
    }

    private String tr(Map<String, String> i18nMap, String value)
    {
        String tr = i18nMap.get(value);
        return null == tr ? value : tr;
    }

    private String getError(Map<String, String> i18nMap, int errorCode)
    {
        switch (errorCode) {
        case 400:
            return tr(i18nMap, "Bad Request");

        case 401:
            return tr(i18nMap, "Unauthorized");

        case 402:
            return tr(i18nMap, "Payment Required");

        case 403:
            return tr(i18nMap, "Forbidden");

        case 404:
            return tr(i18nMap, "Not Found");

        case 405:
            return tr(i18nMap, "Method Not Allowed");

        case 406:
            return tr(i18nMap, "Not Acceptable");

        case 407:
            return tr(i18nMap, "Proxy Authentication Required");

        case 408:
            return tr(i18nMap, "Request Timeout");

        case 409:
            return tr(i18nMap, "Conflict");

        case 410:
            return tr(i18nMap, "Gone");

        case 411:
            return tr(i18nMap, "Length Required");

        case 412:
            return tr(i18nMap, "Precondition Failed");

        case 413:
            return tr(i18nMap, "Request Entity Too Large");

        case 414:
            return tr(i18nMap, "Request-URI Too Long");

        case 415:
            return tr(i18nMap, "Unsupported Media Type");

        case 416:
            return tr(i18nMap, "Requested Range Not Satisfiable");

        case 417:
            return tr(i18nMap, "Expectation Failed");

        case 500:
            return tr(i18nMap, "Internal Server Error");

        case 501:
            return tr(i18nMap, "Not Implemented");

        case 502:
            return tr(i18nMap, "Bad Gateway");

        case 503:
            return tr(i18nMap, "Service Unavailable");

        case 504:
            return tr(i18nMap, "Gateway Timeout");

        case 505:
            return tr(i18nMap, "HTTP Version Not Supported");

        default:
            return tr(i18nMap, "Error");
        }
    }
}
