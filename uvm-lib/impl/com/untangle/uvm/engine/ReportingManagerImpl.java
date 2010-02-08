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
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.reports.Application;
import com.untangle.uvm.reports.ApplicationData;
import com.untangle.uvm.reports.DateItem;
import com.untangle.uvm.reports.DetailSection;
import com.untangle.uvm.reports.Email;
import com.untangle.uvm.reports.Highlight;
import com.untangle.uvm.reports.Host;
import com.untangle.uvm.reports.RemoteReportingManager;
import com.untangle.uvm.reports.ReportXmlHandler;
import com.untangle.uvm.reports.Section;
import com.untangle.uvm.reports.SummaryItem;
import com.untangle.uvm.reports.SummarySection;
import com.untangle.uvm.reports.TableOfContents;
import com.untangle.uvm.reports.User;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.toolbox.MackageDesc;
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

    private static RemoteReportingManagerImpl REPORTING_MANAGER
        = new RemoteReportingManagerImpl();

    private RemoteReportingManagerImpl()
    {
    }

    static RemoteReportingManagerImpl reportingManager()
    {
        return REPORTING_MANAGER;
    }

    public List<DateItem> getDates()
    {
        DateFormat df = new SimpleDateFormat(DATE_FORMAT);

        Calendar c = Calendar.getInstance();

        List<DateItem> l = new ArrayList<DateItem>();

        if (REPORTS_DIR.exists()) {
            for (String s : REPORTS_DIR.list()) {
                try {
                    Date d = df.parse(s);

                    for (String ds : new File(REPORTS_DIR, s).list()) {
                        String[] split = ds.split("-");
                        if (split.length == 2) {
                            String num = split[0];
                            try {
                                l.add(new DateItem(d, new Integer(num)));
                            } catch (NumberFormatException exn) {
                                logger.debug("skipping non-day directory: '" + ds + "'");
                            }
                        }
                    }
                } catch (ParseException exn) {
                    logger.warn("skipping non-date directory: " + s, exn);
                }
            }
        }

        Collections.sort(l);
        Collections.reverse(l);

        return l;
    }

    public Date getReportsCutoff()
    {
        Date d = null;

        Connection conn = null;
        try {
            conn = DataSourceFactory.factory().getConnection();

            PreparedStatement ps = conn.prepareStatement("SELECT last_cutoff FROM reports.reports_state");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp(1);
                d = new Date(ts.getTime());
            }
        } catch (SQLException exn) {
            logger.warn("could not get reports cutoff", exn);
	    
        } finally {
            if (conn != null) {
                try {
                    DataSourceFactory.factory().closeConnection(conn);
                } catch (Exception x) { }
                conn = null;
            }
        }

        return d;
    }

    // FIXME: this is ugly; SummarySection should 
    public List<Highlight> getHighlights(Date d, int numDays)
    {
	List<Highlight> list = new ArrayList<Highlight>();

        for (Application app : getApplications(getDateDir(d, numDays),
					      "top-level")) {
	    for (Section s : getApplicationData(d, numDays, app.getName()).
		     getSections()) {
		if (s instanceof SummarySection) {
		    list.addAll(((SummarySection)s).getHighlights());
		}
	    }
	}

	return list;
    }

    public TableOfContents getTableOfContents(Date d, int numDays)
    {
        Application platform = new Application("untangle-vm", "System");

        List<Application> apps = getApplications(getDateDir(d, numDays),
                                                 "top-level");
        List<User> users = getUsers(d, numDays);
        List<Host> hosts = getHosts(d, numDays);
        List<Email> emails = getEmails(d, numDays);

        return new TableOfContents(d, null, null, null, platform, apps, users,
                                   hosts, emails);
    }

    public TableOfContents getTableOfContentsForHost(Date d, int numDays,
                                                     String hostname)
    {
        Application platform = new Application("untangle-vm", "System");

        List<Application> apps = getApplications(getDateDir(d, numDays),
                                                 "user-drilldown");

        List<User> users = new ArrayList<User>();
        List<Host> hosts = new ArrayList<Host>();
        List<Email> emails = new ArrayList<Email>();

        return new TableOfContents(d, null, hostname, null,
                                   platform, apps, users, hosts, emails);
    }

    public TableOfContents getTableOfContentsForUser(Date d, int numDays,
                                                     String username)
    {
        Application platform = new Application("untangle-vm", "System");

        List<Application> apps = getApplications(getDateDir(d, numDays),
                                                 "user-drilldown");

        List<User> users = new ArrayList<User>();
        List<Host> hosts = new ArrayList<Host>();
        List<Email> emails = new ArrayList<Email>();

        return new TableOfContents(d, username, null, null, platform, apps,
                                   users, hosts, emails);
    }

    public TableOfContents getTableOfContentsForEmail(Date d, int numDays,
                                                      String email)
    {
        Application platform = new Application("untangle-vm", "System");

        List<Application> apps = getApplications(getDateDir(d, numDays),
                                                 "email-drilldown");

        List<User> users = new ArrayList<User>();
        List<Host> hosts = new ArrayList<Host>();
        List<Email> emails = new ArrayList<Email>();

        return new TableOfContents(d, null, null, email, platform, apps, users,
                                   hosts, emails);
    }

    public ApplicationData getApplicationData(Date d, int numDays,
                                              String appName, String type,
                                              String value)
    {
        return readXml(d, numDays, appName, type, value);
    }

    public ApplicationData getApplicationData(Date d, int numDays,
                                              String appName)
    {
        return readXml(d, numDays, appName, null, null);
    }

    public ApplicationData getApplicationDataForUser(Date d, int numDays,
                                                     String appName,
                                                     String username)
    {
        return readXml(d, numDays, appName, "user", username);
    }

    public ApplicationData getApplicationDataForHost(Date d, int numDays,
                                                     String appName,
                                                     String hostname)
    {
        return readXml(d, numDays, appName, "host", hostname);
    }

    public ApplicationData getApplicationDataForEmail(Date d, int numDays,
                                                      String appName,
                                                      String emailAddr)
    {
        return readXml(d, numDays, appName, "email", emailAddr);
    }

    public List<List> getDetailData(Date d, int numDays, String appName,
                                    String detailName, String type,
                                    String value)
    {
        return doGetDetailData(d, numDays, appName, detailName, type, value,
                               true);
    }

    public List<List> getAllDetailData(Date d, int numDays, String appName,
                                       String detailName, String type,
                                       String value)
    {
        return doGetDetailData(d, numDays, appName, detailName, type, value,
                               false);
    }

    // private methods ---------------------------------------------------------

    private List<List> doGetDetailData(Date d, int numDays, String appName,
                                       String detailName, String type,
                                       String value, boolean limitResultSet)
    {
	logger.info("doGetDetailData for '" + appName + "' (detail='" +
		    detailName + "', type='" + type + "', value='" + 
                    value + "', limitResultSet='" + limitResultSet + "')");

	if (isDateBefore(getDaysBefore(d, numDays), getReportsCutoff()))
	    return null;

        List<List> rv = new ArrayList<List>();

        ApplicationData ad = readXml(d, numDays, appName, type, value);
        if (null == ad) {
            return rv;
        }

        for (Section section : ad.getSections()) {
            if (section instanceof DetailSection) {
                DetailSection sds = (DetailSection)section;
                if (sds.getName().equals(detailName)) {
                    String sql = sds.getSql();
		    logger.info("** sql='" + sql + "'");
                    Connection conn = null;
                    try {
                        conn = DataSourceFactory.factory().getConnection();
                        Statement stmt = conn.createStatement();
                        if (limitResultSet) {
                            stmt.setMaxRows(1000);
                        }
                        ResultSet rs = stmt.executeQuery(sql);
                        int columnCount = rs.getMetaData().getColumnCount();
			logger.info("** got " + columnCount + " columns.");
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

    private ApplicationData readXml(Date d, int numDays, String appName,
                                    String type, String value)
    {
        ReportXmlHandler h = new ReportXmlHandler();

        File f = new File(getAppDir(d, numDays, appName, type, value)
                          + "/report.xml");

        logger.debug("Trying to XML file '" + f + "'");

        if (!f.exists() && type != null) {
            logger.debug("** ... does not exist, trying to generate");
            generateReport(d, numDays, appName, type, value);
        }

        if (!f.exists()) {
            logger.debug("** ... still does not exist, giving up (generation must have failed)");
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

    private List<Host> getHosts(Date d, int numDays)
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

    private List<User> getUsers(Date d, int numDays)
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

    private List<Email> getEmails(Date d, int numDays)
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

    private String getDateDir(Date d, int numDays)
    {
        StringBuffer sb = new StringBuffer(BUNNICULA_REPORTS_DATA);
        sb.append("/");

        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        sb.append(df.format(d));

        sb.append("/");
        sb.append(numDays);
        if (numDays <= 1) {
            sb.append("-day");
        } else {
            sb.append("-days");
        }

        return sb.toString();
    }

    private String getAppDir(Date d, int numDays, String appName, String type,
                             String val)
    {
        StringBuffer sb = new StringBuffer(BUNNICULA_REPORTS_DATA);
        sb.append("/");

        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        sb.append(df.format(d));

        sb.append("/");
        sb.append(numDays);
        if (numDays <= 1) {
            sb.append("-day");
        } else {
            sb.append("-days");
        }

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

        RemoteToolboxManagerImpl tm = RemoteToolboxManagerImpl.toolboxManager();

        Map<Integer, Application> m = new TreeMap<Integer, Application>();

        for (String s : appNames) {
            MackageDesc md = tm.mackageDesc(s);

            int pos;

            if (null == md) {
                logger.warn("cannot get viewposition for: " + s);
                pos = 10000;
            } else {
                pos = md.getViewPosition();
            }

            File f = new File(dirName + "/" + s + "/report.xml");
            if (f.exists()) {
                ApplicationData ad = readXml(f);
                if (null != ad) {
                    if (m.containsKey(pos)) {
                        logger.error("View-Position '" + pos + "' was already used by '" +
                                     ((Application)m.get(pos)).getName() + "', but '" + 
                                     ad.getName() + "' is also using it, so it will show up in your reports instead.");
                    }
                    m.put(pos, new Application(ad.getName(), ad.getTitle()));
                }
            }
        }

        return new ArrayList<Application>(m.values());
    }

    private List<String> getAppNames(String dirName, String type)
    {
        List<String> l = new ArrayList<String>();

        String f = dirName + "/../" + type;

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

    private boolean generateReport(Date d, int numDays, String appName,
                                   String type, String value)
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
            + "," + numDays + "," + host + "," + user + "," + email + "\n";

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

    private Date getDaysBefore(Date d, int numDays)
    {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.add(Calendar.DATE, -numDays);
        return c.getTime();
    }

    private boolean isDateBefore(Date d1, Date d2) 
    {
	Calendar c1 = Calendar.getInstance();
	c1.setTime(d1);
	Calendar c2 = Calendar.getInstance();
	c2.setTime(d2);

	return c1.before(c2);
    }

    public boolean isReportingEnabled() {
        LocalUvmContext uvm = LocalUvmContextFactory.context();
        LocalNodeManager nodeManager = uvm.nodeManager();
        List<Tid> tids = nodeManager.nodeInstances("untangle-node-reporting");
        if(tids == null || tids.size() == 0)
            return false;
        // What if more than one? Shouldn't happen. XX
        NodeContext context = nodeManager.nodeContext(tids.get(0));
        if (context == null) {
            return false;
        }

        return true;
    }

    public boolean isReportsAvailable()
    {
        return 0 < getDates().size();
    }
}
