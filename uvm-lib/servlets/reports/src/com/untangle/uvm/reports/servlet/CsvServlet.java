/*
 * $HeadURL: svn://chef/work/src/uvm-lib/impl/com/untangle/uvm/engine/ReportingManagerImpl.java $
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

package com.untangle.uvm.reports.servlet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.client.RemoteUvmContext;
import com.untangle.uvm.reports.ApplicationData;
import com.untangle.uvm.reports.ColumnDesc;
import com.untangle.uvm.reports.DetailSection;
import com.untangle.uvm.reports.RemoteReportingManager;
import com.untangle.uvm.reports.Section;
import org.apache.log4j.Logger;

/**
 * Gets CSV for a detail report.
 *
 * @author Aaron Read <amread@untangle.com>
 */
public class CsvServlet extends HttpServlet
{
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private final Logger logger = Logger.getLogger(getClass());

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        String dateStr = req.getParameter("date");
        String app = req.getParameter("app");
        String detail = req.getParameter("detail");

        String type = req.getParameter("type");
        String value = req.getParameter("value");

        if (null == dateStr || null == app || null == detail) {
            return;
        }

        RemoteUvmContext uvm = LocalUvmContextFactory.context().remoteContext();
        RemoteReportingManager rm = uvm.reportingManager();

        BufferedWriter bw = null;
        try {
            resp.setHeader("Content-Type", "text/csv");
            resp.setHeader("Content-Disposition", "attachment; filename=\""
                           + dateStr + "-" + app + "-" + detail + ".csv\"");

            ServletOutputStream out = resp.getOutputStream();
            bw = new BufferedWriter(new OutputStreamWriter(out));

            DateFormat df = new SimpleDateFormat(DATE_FORMAT);
            Date date = df.parse(dateStr);

            ApplicationData ad = null;
            ad = rm.getApplicationData(date, app, type, value);
            if (null != ad) {
                for (Section s : ad.getSections()) {
                    if (s.getName().equals(detail)) {
                        try {
                            DetailSection ds = (DetailSection)s;
                            List header = new ArrayList();
                            for (ColumnDesc cd : ds.getColumns()) {
                                header.add(cd.getTitle());
                            }
                            writeCsvRow(bw, header);
                            break;
                        } catch (ClassCastException exn) {
                            logger.warn("could not get header", exn);
                        }
                    }
                }
            }

            List<List> ll = rm.getAllDetailData(date, app, detail, type, value);

            for (List l : ll) {
                writeCsvRow(bw, l);
            }
            bw.flush();
        } catch (ParseException exn) {
            logger.warn("could not parse date: " + dateStr, exn);
            return;
        } catch (IOException exn) {
            logger.warn("could not write csv: ", exn);
            return;
        } catch (Exception exn) {
            logger.warn(exn, exn);
        } finally {
            try {
                if (null != bw) {
                    bw.close();
                }
            } catch (IOException exn) {
                logger.warn("could not close data stream", exn);
            }
        }
    }

    private void writeCsvRow(BufferedWriter bw, List l)
        throws IOException
    {
        for (int i = 0; i < l.size(); i++) {
            Object o = l.get(i);
            if (null == o) {
                o = "";
            }
            String s = o.toString();
            boolean enclose = false;
            if (0 <= s.indexOf(',')) {
                enclose = true;
            }
            if (0 <= s.indexOf('"')) {
                enclose = true;
                s = s.replaceAll("\"", "\"\"");
            }
            if (0 <= s.indexOf("\n") || 0 <= s.indexOf("\r")) {
                enclose = true;
            }
            if (s.length() > 0) {
                switch(s.charAt(0)) {
                case ' ': case '\t': case '\n': case '\r':
                    enclose = true;
                }
                switch(s.charAt(s.length() - 1)) {
                case ' ': case '\t': case '\n': case '\r':
                    enclose = true;
                }
            } else {
                enclose=true;
            }

            if (enclose) {
                s = "\"" + s + "\"";
            }

            bw.write(s);
            if (i < l.size() - 1) {
                bw.write(",");
            }
        }
        bw.write("\n");
    }
}
