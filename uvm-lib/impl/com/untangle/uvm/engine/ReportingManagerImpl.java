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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.reports.Application;
import com.untangle.uvm.reports.ApplicationData;
import com.untangle.uvm.reports.DetailSection;
import com.untangle.uvm.reports.Host;
import com.untangle.uvm.reports.RemoteReportingManager;
import com.untangle.uvm.reports.Section;
import com.untangle.uvm.reports.TableOfContents;
import com.untangle.uvm.reports.User;
import com.untangle.uvm.security.Tid;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

class RemoteReportingManagerImpl implements RemoteReportingManager
{
    private static final String BUNNICULA_REPORTS_DATA
        = System.getProperty("bunnicula.web.dir") + "/reports/data";

    private static final File REPORTS_DIR = new File(BUNNICULA_REPORTS_DATA);
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private final Logger logger = Logger.getLogger(getClass());

    private static RemoteReportingManagerImpl REPORTING_MANAGER = new RemoteReportingManagerImpl();

    private RemoteReportingManagerImpl()
    {
    }

    static RemoteReportingManagerImpl reportingManager()
    {
        return REPORTING_MANAGER;
    }

    // NEW SHIZZLE -------------------------------------------------------------

    // XXX SAMPLE DATA
    public List<Date> getDates()
    {
        DateFormat df = new SimpleDateFormat(DATE_FORMAT);

        Calendar c = Calendar.getInstance();

        List<Date> l = new ArrayList<Date>();

        if (REPORTS_DIR.exists()) {
            for (String s : REPORTS_DIR.list()) {
                try {
                    l.add(df.parse(s));
                } catch (ParseException exn) {
                    logger.warn("skipping non-date directory: " + s, exn);
                }
            }
        }

        return l;
    }

    // XXX SAMPLE DATA
    public TableOfContents getTableOfContents(Date d)
    {
        Application platform = new Application("untangle-vm", "Platform");

        // XXX TODO
        List<User> users = new ArrayList<User>();
        List<Host> hosts = new ArrayList<Host>();

        List<Application> apps = new ArrayList<Application>();
        File dir = new File(getDateDir(d));
        if (dir.exists()) {
            for (String s : dir.list()) {
                ApplicationData ad = readXml(d, s);
                if (null != ad) {
                    apps.add(new Application(ad.getName(), ad.getTitle()));
                }
            }
        }

        return new TableOfContents(platform, apps, users, hosts);
    }

    // XXX SAMPLE DATA
    public ApplicationData getApplicationData(Date d, String appName)
    {
        return readXml(d, appName);
    }

    public ApplicationData getApplicationDataForUser(Date d, String appName,
                                                     String username)
    {
        return null; //XXX
    }

    public ApplicationData getApplicationDataForHost(Date d, String appName,
                                                     String hostname)
    {
        return null; //XXX
    }

    public ApplicationData getApplicationDataForEmail(Date d, String appName,
                                                      String emailAddr)
    {
        return null; //XXX
    }

    public List<List> getDetailData(Date d, String appName, String detailName)
    {
        List<List> rv = null;

        ApplicationData ad = readXml(d, appName);
        for (Section section : ad.getSections()) {
            if (section instanceof DetailSection) {
                DetailSection sds = (DetailSection)section;
                if (sds.getName().equals(detailName)) {
                    rv = new ArrayList<List>();
                    String sql = sds.getSql();

                    Connection conn = null;
                    try {
                        conn = DataSourceFactory.factory().getConnection();
                        Statement stmt = conn.createStatement();
                        stmt.setMaxRows(10);
                        ResultSet rs = stmt.executeQuery(sql);
                        int columnCount = rs.getMetaData().getColumnCount();
                        while (rs.next()) {
                            List l = new ArrayList(columnCount);
                            for (int i = 1; i <= columnCount; i++) {
                                l.add(rs.getObject(i));
                            }
                            rv.add(l);
                        }
                    } catch (SQLException exn) {
                        logger.warn("could not get DetailData for: " + sql,
                                    exn);
                    } finally {
                        if (conn != null) {
                            try {
                                DataSourceFactory.factory().closeConnection(conn);
                            } catch (Exception x) { }
                            conn = null;
                        }
                    }
                }
            }
        }

        return rv;
    }

    // private methods ---------------------------------------------------------

    private ApplicationData readXml(Date d, String appName)
    {
        ReportXmlHandler h = new ReportXmlHandler();

        try {
            FileInputStream fis = new FileInputStream(getAppDir(d, appName)
                                                      + "/report.xml");

            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(h);
            xr.parse(new InputSource(fis));
        } catch (SAXException exn) {
            return null;
        } catch (IOException exn) {
            return null;
        }

        return h.getReport();
    }

    private String getDateDir(Date d)
    {
        StringBuffer sb = new StringBuffer(BUNNICULA_REPORTS_DATA);
        sb.append("/");

        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        sb.append(df.format(d));

        return String.format(sb.toString());
    }

    private String getAppDir(Date d, String appName)
    {
        StringBuffer sb = new StringBuffer(BUNNICULA_REPORTS_DATA);
        sb.append("/");

        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        sb.append(df.format(d));

        sb.append("/");
        sb.append(appName);

        return String.format(sb.toString());
    }

    // OLD SHIT ----------------------------------------------------------------

    public boolean isReportingEnabled() {
        LocalUvmContext uvm = LocalUvmContextFactory.context();
        LocalNodeManager nodeManager = uvm.nodeManager();
        List<Tid> tids = nodeManager.nodeInstances("untangle-node-reporting");
        if(tids == null || tids.size() == 0)
            return false;
        // What if more than one? Shouldn't happen. XX
        NodeContext context = nodeManager.nodeContext(tids.get(0));
        if (context == null)
            return false;

        return true;
    }

    public boolean isReportsAvailable()
    {
        return true;
    }
}
