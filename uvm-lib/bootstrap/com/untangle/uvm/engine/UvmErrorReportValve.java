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

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.valves.ErrorReportValve;

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

    protected void report(Request request, Response response,
                          Throwable throwable)
        throws IOException
    {
        int statusCode = response.getStatus();

        if ((statusCode < 400) || (response.getContentCount() > 0)) {
            return;
        }

        String errorMessage = null;

        Object o = request.getAttribute(UVM_WEB_MESSAGE_ATTR);
        if (o instanceof String) {
            errorMessage = (String)o;
        }

        if (null == errorMessage) {
            errorMessage = response.getMessage();
        }

        if (null == errorMessage) {
            errorMessage = "";
        }
        errorMessage = RequestUtil.filter(errorMessage);
        errorMessage = sm.getString("http." + statusCode, errorMessage);

        response.setContentType("text/html");
        response.setCharacterEncoding("utf-8");
        PrintWriter writer = response.getReporter();
        if (null != writer) {
            writeReport(writer, errorMessage);
        }
    }

    private void writeReport(PrintWriter w, String errorMessage)
        throws IOException
    {
      w.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");
      w.write("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
      w.write("<head>\n");
      w.write("<title>Untangle Server</title>\n");
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
      w.write("<b>Untangle Server</b><br /><br />\n");
      w.write("<em>");  w.write(errorMessage);  w.write("</em>\n");
      w.write("</center><br /><br />\n");
      w.write("</div></div></div><div class=\"main-bot-left\"></div><div class=\"main-bot-right\"></div>\n");
      w.write("</div>\n");
      w.write("</body>\n");
      w.write("</html>\n");
    }
}
