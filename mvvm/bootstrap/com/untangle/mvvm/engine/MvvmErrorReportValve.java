/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: TomcatManager.java 8547 2007-01-08 22:57:36Z amread $
 */

package com.untangle.mvvm.engine;

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.valves.ErrorReportValve;

public class MvvmErrorReportValve extends ErrorReportValve
{
    public static final String MVVM_WEB_MESSAGE_ATTR = "com.untangle.mvvm.web.message";

    protected void report(Request request, Response response,
                          Throwable throwable)
        throws IOException
    {
        int statusCode = response.getStatus();

        if ((statusCode < 400) || (response.getContentCount() > 0)) {
            return;
        }

        String errorMessage = null;

        Object o = request.getAttribute(MVVM_WEB_MESSAGE_ATTR);
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
        if (writer != null) {
            // If writer is null, it's an indication that the response has
            // been hard committed already, which should never happen
            writeReport(writer, errorMessage);
        }
    }

    private void writeReport(PrintWriter w, String errorMessage)
        throws IOException
    {
        w.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n");
        w.write("\n");
        w.write("<html>\n");
        w.write("  <head>\n");
        w.write("    <meta content=\"text/html; charset=ISO-8859-1\" http-equiv=\"content-type\"/>\n");
        w.write("    <title>\n");
        w.write("        Untangle Server\n");
        w.write("    </title>\n");
        w.write("  </head>\n");
        w.write("  <body LINK=\"#0000EE\" VLINK=\"#0000EE\" style=\"background-image: url(/images/DarkBackground1600x100.jpg);\">\n");
        w.write("    <br/>\n");
        w.write("    <br/>\n");
        w.write("    <br/>\n");
        w.write("    <br/>\n");
        w.write("    <center>\n");
        w.write("      <table\n");
        w.write("          style=\"margin-left: auto; margin-right: auto; text-align: left; background-image: url(/images/Background1600x100.jpg);\"\n");
        w.write("          cellpadding=\"40\" cellspacing=\"0\" border=\"2\">\n");
        w.write("        <tbody>\n");
        w.write("          <tr>\n");
        w.write("            <td style=\"vertical-align: top; font-family: helvetica,arial,sans-serif; width: 400px;\">\n");
        w.write("              <div style=\"text-align: center;\">\n");
        w.write("                <img alt=\"\" src=\"/images/Logo150x96.gif\"\n");
        w.write("                     style=\"border: 0px solid ; width: 150px; height: 96px;\" align=\"top\"\n");
        w.write("                     hspace=\"0\" vspace=\"0\"/>\n");
        w.write("                <br/>\n");
        w.write("                <br/>\n");
        w.write("                <span style=\"font-weight: bold;\">\n");
        w.write("                  Untangle Server\n");
        w.write("                </span>\n");
        w.write("                <br/>\n");
        w.write("                <br/>\n");
        w.write("                <br/>\n");
        w.write("                <span style=\"font-weight: italic;\">\n");
        w.write("                  "); w.write(errorMessage); w.write("\n");
        w.write("                </span>\n");
        w.write("              </div>\n");
        w.write("              <br/>\n");
        w.write("              <br/>\n");
        w.write("              <br/>\n");
        w.write("            </td>\n");
        w.write("          </tr>\n");
        w.write("        </tbody>\n");
        w.write("      </table>\n");
        w.write("    </center>\n");
        w.write("    <br/>\n");
        w.write("    <br/>\n");
        w.write("    <br/>\n");
        w.write("  </body>\n");
        w.write("</html>\n");
        w.write("\n");
    }
}
