/*
 * $Id: CsvServlet.java,v 1.00 2011/12/17 10:13:49 dmorris Exp $
 */
package com.untangle.uvm.reports.servlet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.reports.ApplicationData;
import com.untangle.uvm.reports.ColumnDesc;
import com.untangle.uvm.reports.DetailSection;
import com.untangle.uvm.reports.ReportingManager;
import com.untangle.uvm.reports.Section;
import org.apache.log4j.Logger;

/**
 * Gets CSV for a detail report.
 */
@SuppressWarnings("serial")
public class CsvServlet extends HttpServlet
{
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private final Logger logger = Logger.getLogger(getClass());

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        String dateStr = req.getParameter("date");
        String numDaysStr = req.getParameter("numDays");
        String app = req.getParameter("app");
        String detail = req.getParameter("detail");

        String type = req.getParameter("type");
        String value = req.getParameter("value");

        logger.info("Got a CSV request: " +
                    "date='" + dateStr + "', " +
                    "numDays='" + numDaysStr + "', " + 
                    "app='" + app + "', " +
                    "detail='" + detail + "', " +
                    "type='" + type + "', " +
                    "value='" + value + "'");

        if (null == dateStr || null == numDaysStr || null == app || null == detail) {
            return;
        }

        UvmContext uvm = UvmContextFactory.context();
        ReportingManager rm = uvm.reportingManager();

        BufferedWriter bw = null;
        try {
            resp.setHeader("Content-Type", "text/csv");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + dateStr + "-" + app + "-" + detail + ".csv\"");

            ServletOutputStream out = resp.getOutputStream();
            bw = new BufferedWriter(new OutputStreamWriter(out));

            DateFormat df = new SimpleDateFormat(DATE_FORMAT);
            Date date = df.parse(dateStr);

            int numDays = Integer.parseInt(numDaysStr);

            ApplicationData ad = null;
            ad = rm.getApplicationData(date, numDays, app, type, value);
            if (null != ad) {
                for (Section section : ad.getSections()) {
                    if (section.getName().equals(detail)) {
                        try {
                            DetailSection ds = (DetailSection) section;
                            List<Object> header = new ArrayList<Object>();
                            for (ColumnDesc cd : ds.getColumns()) {
                                header.add(cd.getTitle());
                            }
                            writeCsvRow(bw, header);

                            doQuery(bw, ds.getSql());

                            break;
                        } catch (ClassCastException exn) {
                            logger.warn("could not get header", exn);
                        }
                    }
                }
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

    private void doQuery(BufferedWriter bw, String sql)
        throws IOException
    {
        logger.info("About to do query: '" + sql + "'");

        Connection conn = null;
        try {
            conn = UvmContextFactory.context().getDBConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            int columnCount = rs.getMetaData().getColumnCount();
            logger.info("** got " + columnCount + " columns");
            while (rs.next()) {
                List<Object> l = new ArrayList<Object>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    l.add(rs.getObject(i));
                }
                writeCsvRow(bw, l);
            }
        } catch (SQLException exn) {
            logger.warn("** could not get DetailData...",
                        exn);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception x) { }
                conn = null;
            }
        }
    }

    private void writeCsvRow(BufferedWriter bw, List<Object> l)
        throws IOException
    {
        for (int i = 0; i < l.size(); i++) {
            Object o = l.get(i);
            if (null == o) {
                o = "";
            }

            String s = o.toString();
            if (s.matches("^\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d.*")) {
                s = s.replaceAll("\\.\\d+$", "");
            }
                
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
