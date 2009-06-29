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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
import com.untangle.uvm.reports.Email;
import com.untangle.uvm.reports.Host;
import com.untangle.uvm.reports.RemoteReportingManager;
import com.untangle.uvm.reports.ReportXmlHandler;
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

    public TableOfContents getTableOfContents(Date d)
    {
        Application platform = new Application("untangle-vm", "Platform");

        List<Application> apps = getApplications(getDateDir(d),
                                                 "top-level");

        List<User> users = getUsers(d);
        List<Host> hosts = getHosts(d);
        List<Email> emails = getEmails(d);

        return new TableOfContents(d, null, null, null, platform, apps, users,
                                   hosts, emails);
    }

    public TableOfContents getTableOfContentsForHost(Date d, String hostname)
    {
        Application platform = new Application("untangle-vm", "Platform");

        List<Application> apps = getApplications(getDateDir(d),
                                                 "user-drilldown");

        List<User> users = new ArrayList<User>();
        List<Host> hosts = new ArrayList<Host>();
        List<Email> emails = new ArrayList<Email>();

        return new TableOfContents(d, null, hostname, null,
                                   platform, apps, users, hosts, emails);
    }

    public TableOfContents getTableOfContentsForUser(Date d, String username)
    {
        Application platform = new Application("untangle-vm", "Platform");

        List<Application> apps = getApplications(getDateDir(d),
                                                 "user-drilldown");

        List<User> users = new ArrayList<User>();
        List<Host> hosts = new ArrayList<Host>();
        List<Email> emails = new ArrayList<Email>();

        return new TableOfContents(d, username, null, null, platform, apps,
                                   users, hosts, emails);
    }

    public TableOfContents getTableOfContentsForEmail(Date d, String email)
    {
        Application platform = new Application("untangle-vm", "Platform");

        List<Application> apps = getApplications(getDateDir(d),
                                                 "email-drilldown");

        List<User> users = new ArrayList<User>();
        List<Host> hosts = new ArrayList<Host>();
        List<Email> emails = new ArrayList<Email>();

        return new TableOfContents(d, null, null, email, platform, apps, users,
                                   hosts, emails);
    }

    public ApplicationData getApplicationData(Date d, String appName)
    {
        return readXml(d, appName, null, null);
    }

    public ApplicationData getApplicationDataForUser(Date d, String appName,
                                                     String username)
    {
        return readXml(d, appName, "user", username);
    }

    public ApplicationData getApplicationDataForHost(Date d, String appName,
                                                     String hostname)
    {
        return readXml(d, appName, "host", hostname);
    }

    public ApplicationData getApplicationDataForEmail(Date d, String appName,
                                                      String emailAddr)
    {
        return readXml(d, appName, "email", emailAddr);
    }

    public List<List> getDetailData(Date d, String appName, String detailName)
    {
        List<List> rv = new ArrayList<List>();

        ApplicationData ad = readXml(d, appName, null, null);
        if (null == ad) {
            return rv;
        }

        for (Section section : ad.getSections()) {
            if (section instanceof DetailSection) {
                DetailSection sds = (DetailSection)section;
                if (sds.getName().equals(detailName)) {
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

    private ApplicationData readXml(Date d, String appName, String type,
                                    String value)
    {
        ReportXmlHandler h = new ReportXmlHandler();

        File f = new File(getAppDir(d, appName, type, value) + "/report.xml");

        if (!f.exists() && type != null) {
            generateReport(d, appName, type, value);
        }

        if (!f.exists()) {
            return null;
        }

        return readXml(f);
    }

    private ApplicationData readXml(File f)
    {
        ReportXmlHandler h = new ReportXmlHandler();

        try {
            FileInputStream fis = new FileInputStream(f);

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

    private List<Host> getHosts(Date d)
    {
        List<Host> l = new ArrayList<Host>();

        Connection conn = null;
        try {
            conn = DataSourceFactory.factory().getConnection();

            PreparedStatement ps = conn.prepareStatement("SELECT hname FROM reports.hnames WHERE date = ?");
            ps.setDate(1, new java.sql.Date(getDayBefore(d)));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String s = rs.getString(1);
                l.add(new Host(s));
            }
        } catch (SQLException exn) {
            logger.warn("could not get hosts", exn);
        } finally {
            if (conn != null) {
                try {
                    DataSourceFactory.factory().closeConnection(conn);
                } catch (Exception x) { }
                conn = null;
            }
        }

        return l;
    }

    private List<User> getUsers(Date d)
    {
        List<User> l = new ArrayList<User>();

        Connection conn = null;
        try {
            conn = DataSourceFactory.factory().getConnection();

            PreparedStatement ps = conn.prepareStatement("SELECT username FROM reports.users WHERE date = ?");
            ps.setDate(1, new java.sql.Date(getDayBefore(d)));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String s = rs.getString(1);
                l.add(new User(s));
            }
        } catch (SQLException exn) {
            logger.warn("could not get users", exn);
        } finally {
            if (conn != null) {
                try {
                    DataSourceFactory.factory().closeConnection(conn);
                } catch (Exception x) { }
                conn = null;
            }
        }

        return l;
    }

    private List<Email> getEmails(Date d)
    {
        List<Email> l = new ArrayList<Email>();

        Connection conn = null;
        try {
            conn = DataSourceFactory.factory().getConnection();

            PreparedStatement ps = conn.prepareStatement("SELECT email FROM reports.email WHERE date = ?");
            ps.setDate(1, new java.sql.Date(getDayBefore(d)));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String s = rs.getString(1);
                l.add(new Email(s));
            }
        } catch (SQLException exn) {
            logger.warn("could not get email", exn);
        } finally {
            if (conn != null) {
                try {
                    DataSourceFactory.factory().closeConnection(conn);
                } catch (Exception x) { }
                conn = null;
            }
        }

        return l;
    }

    private String getDateDir(Date d)
    {
        StringBuffer sb = new StringBuffer(BUNNICULA_REPORTS_DATA);
        sb.append("/");

        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        sb.append(df.format(d));

        return String.format(sb.toString());
    }

    private String getAppDir(Date d, String appName, String type, String val)
    {
        StringBuffer sb = new StringBuffer(BUNNICULA_REPORTS_DATA);
        sb.append("/");

        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        sb.append(df.format(d));

        if (null != type) {
            sb.append("/");
            sb.append(type);
            sb.append("/");
            sb.append(val);
        }

        sb.append("/");
        sb.append(appName);

        return String.format(sb.toString());
    }

    private List<Application> getApplications(String dirName, String type)
    {
        List<String> appNames = getAppNames(dirName, type);

        List<Application> apps = new ArrayList<Application>();
        for (String s : appNames) {
            File f = new File(dirName + "/" + s + "/report.xml");
            if (f.exists()) {
                ApplicationData ad = readXml(f);
                if (null != ad) {
                    apps.add(new Application(ad.getName(), ad.getTitle()));
                }
            }
        }

        return apps;
    }

    private List<String> getAppNames(String dirName, String type)
    {
        List<String> l = new ArrayList<String>();

        String f = dirName + "/" + type;

        BufferedReader br = null;

        try {
            InputStream is = new FileInputStream(f);
            br = new BufferedReader(new InputStreamReader(is));
            String line;
            while (null != (line = br.readLine())) {
                l.add(line);
            }
        } catch (IOException exn) {
            logger.warn("could not read: " + f, exn);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException exn) {
                    logger.warn("could not close: " + f, exn);
                }
            }
        }

        return l;
    }

    private boolean generateReport(Date d, String appName, String type,
                                String value)
    {
        String user = "";
        String host = "";
        String email = "";

        if (type == null) {
            logger.warn("request to generate main report ignored");
            return false;
        } else if (type.equals("user")) {
            user = value; host = ""; email = "";
        } else if (type.equals("host")) {
            user = ""; host = value; email = "";
        } else if (type.equals("email")) {
            user = ""; host = ""; email = value;
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        String cmdStr = "generate_sub_report," + appName + "," + df.format(d)
            + "," + host + "," + user + "," + email + "\n";

        Socket s = null;
        Writer w = null;
        BufferedReader r = null;

        boolean rv = false;

        try {
            s = new Socket("localhost", 55204);
            w = new OutputStreamWriter(s.getOutputStream());
            InputStream is = s.getInputStream();
            r = new BufferedReader(new InputStreamReader(is));

            w.write(cmdStr);
            w.flush();
            String l = r.readLine();

            if (l.equals("DONE")) {
                rv = true;
            } else {
                logger.warn("could not generate graph: '" + cmdStr
                            + "' result: '" + l + "'");
                rv = false;
            }
        } catch (IOException exn) {
            logger.warn("could not generate report: '" + cmdStr + "'", exn);
        } finally {
            if (null != r) {
                try {
                    r.close();
                } catch (IOException exn) {
                    logger.warn("could not close reader", exn);
                }
            }

            if (null != w) {
                try {
                    w.close();
                } catch (IOException exn) {
                    logger.warn("could not close writer", exn);
                }
            }

            if (null != s) {
                try {
                    s.close();
                } catch (IOException exn) {
                    logger.warn("could not close writer", exn);
                }
            }
        }

        return rv;
    }

    private long getDayBefore(Date d)
    {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.add(Calendar.DATE, -1);
        return c.getTimeInMillis();
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
