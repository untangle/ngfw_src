/*
 * $Id$
 */
package com.untangle.node.reporting;

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

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.node.reporting.ReportingNode;
import com.untangle.uvm.toolbox.PackageDesc;
import com.untangle.node.reporting.items.ApplicationData;
import com.untangle.node.reporting.items.Application;
import com.untangle.node.reporting.items.Chart;
import com.untangle.node.reporting.items.ColumnDesc;
import com.untangle.node.reporting.items.DateItem;
import com.untangle.node.reporting.items.DetailSection;
import com.untangle.node.reporting.items.Email;
import com.untangle.node.reporting.items.GraphGenerator;
import com.untangle.node.reporting.items.Highlight;
import com.untangle.node.reporting.items.Host;
import com.untangle.node.reporting.items.KeyStatistic;
import com.untangle.node.reporting.items.LegendItem;
import com.untangle.node.reporting.items.PieChart;
import com.untangle.node.reporting.items.Plot;
import com.untangle.node.reporting.items.ReportXmlHandler;
import com.untangle.node.reporting.items.Section;
import com.untangle.node.reporting.items.StackedBarChart;
import com.untangle.node.reporting.items.SummaryItem;
import com.untangle.node.reporting.items.SummarySection;
import com.untangle.node.reporting.items.TableOfContents;
import com.untangle.node.reporting.items.TimeSeriesChart;
import com.untangle.node.reporting.items.User;

class ReportingManagerImpl implements ReportingManager
{
    private static final Logger logger = Logger.getLogger(ReportingManagerImpl.class);

    private static final String UVM_REPORTS_DATA = System.getProperty("uvm.web.dir") + "/reports/data";
    private static final String SUBREPORT_SCRIPT = System.getProperty("uvm.bin.dir") + "/reporting-generate-subreport.py";

    private static final File REPORTS_DIR = new File(UVM_REPORTS_DATA);
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private ReportingNode node;
    
    protected ReportingManagerImpl( ReportingNode node)
    {
        this.node = node;
    }

    public List<DateItem> getDates()
    {
        DateFormat df = new SimpleDateFormat(DATE_FORMAT);

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
            conn = node.getDbConnection();

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
                    conn.close();
                } catch (Exception x) { }
                conn = null;
            }
        }

        return d;
    }

    public List<Highlight> getHighlights(Date d, int numDays)
    {
        List<Highlight> list = new ArrayList<Highlight>();
        List<Application> l = getApplications( getDateDir(d, numDays), "top-level");

        // add untangle-vm's highlights
        File f = new File(getDateDir(d, numDays) + "/untangle-vm/report.xml");
        if (f.exists()) {
            ApplicationData ad = readXml(f);
            l.add(0, new Application(ad.getName(), ad.getTitle()));
        }

        for (Application app : l) {
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

        List<Application> apps = getApplications(getDateDir(d, numDays),"top-level");
        List<User> users = getUsers(d, numDays);
        List<Host> hosts = getHosts(d, numDays);
        List<Email> emails = getEmails(d, numDays);

        return new TableOfContents(d, null, null, null, platform, apps, users, hosts, emails);
    }

    public TableOfContents getTableOfContentsForHost(Date d, int numDays, String hostname)
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

    public TableOfContents getTableOfContentsForUser(Date d, int numDays, String username)
    {
        Application platform = new Application("untangle-vm", "System");

        List<Application> apps = getApplications(getDateDir(d, numDays), "user-drilldown");

        List<User> users = new ArrayList<User>();
        List<Host> hosts = new ArrayList<Host>();
        List<Email> emails = new ArrayList<Email>();

        return new TableOfContents(d, username, null, null, platform, apps, users, hosts, emails);
    }

    public TableOfContents getTableOfContentsForEmail(Date d, int numDays, String email)
    {
        Application platform = new Application("untangle-vm", "System");

        List<Application> apps = getApplications(getDateDir(d, numDays), "email-drilldown");

        List<User> users = new ArrayList<User>();
        List<Host> hosts = new ArrayList<Host>();
        List<Email> emails = new ArrayList<Email>();

        return new TableOfContents(d, null, null, email, platform, apps, users, hosts, emails);
    }

    public ApplicationData getApplicationData(Date d, int numDays, String appName, String type, String value)
    {
        return readXml(d, numDays, appName, type, value);
    }

    public ApplicationData getApplicationData(Date d, int numDays, String appName)
    {
        return readXml(d, numDays, appName, null, null);
    }

    public ApplicationData getApplicationDataForUser(Date d, int numDays, String appName, String username)
    {
        return readXml(d, numDays, appName, "user", username);
    }

    public ApplicationData getApplicationDataForHost(Date d, int numDays, String appName, String hostname)
    {
        return readXml(d, numDays, appName, "host", hostname);
    }

    public ApplicationData getApplicationDataForEmail(Date d, int numDays, String appName, String emailAddr)
    {
        return readXml(d, numDays, appName, "email", emailAddr);
    }

    public List<List<Object>> getDetailData(Date d, int numDays, String appName, String detailName, String type, String value)
    {
        return doGetDetailData(d, numDays, appName, detailName, type, value, true);
    }

    public List<List<Object>> getAllDetailData(Date d, int numDays, String appName, String detailName, String type, String value)
    {
        return doGetDetailData(d, numDays, appName, detailName, type, value, false);
    }

    // private methods ---------------------------------------------------------

    private List<List<Object>> doGetDetailData(Date d, int numDays, String appName, String detailName, String type, String value, boolean limitResultSet)
    {
        logger.warn("doGetDetailData for '" + appName +
                    "' (detail='" + detailName + "', " +
                    "type='" + type + "', " +
                    "value='" + value + "', " +
                    "limitResultSet='" + limitResultSet + "')");

        if (isDateBefore(getDaysBefore(d, numDays), getReportsCutoff())) {
            logger.warn("Date " + getDaysBefore(d, numDays) + " is before " + getReportsCutoff());
            return null;
        }

        List<List<Object>> rv = new ArrayList<List<Object>>();

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
                        conn = node.getDbConnection();
                        Statement stmt = conn.createStatement();
                        if (limitResultSet) {
                            stmt.setMaxRows(1000);
                        }
                        ResultSet rs = stmt.executeQuery(sql);
                        int columnCount = rs.getMetaData().getColumnCount();
                        logger.info("** got " + columnCount + " columns.");
                        while (rs.next()) {
                            List<Object> l = new ArrayList<Object>(columnCount);
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
                                conn.close();
                            } catch (Exception x) { }
                            conn = null;
                        }
                    }
                }
            }
        }

        return rv;
    }

    private ApplicationData readXml(Date d, int numDays, String appName, String type, String value)
    {
        File f = new File(getAppDir(d, numDays, appName, type, value) + "/report.xml");

        logger.debug("Trying to read XML file '" + f + "'");

        if (!f.exists() && type != null) {
            logger.debug("XML file " + f + " does not exist: generating.");
            generateReport(d, numDays, appName, type, value);
        }

        if (!f.exists()) {
            logger.warn("XML file " + f + " still does not exist: generation failed.");
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

    private List<String> readPlainFile(Date d, int numDays, String category) {
        List<String> values = getAppNames(getDateDir(d, numDays), "/" + category + ".txt");
        return values;
    }

    private List<Host> getHosts(Date d, int numDays) {
        List<Host> l = new ArrayList<Host>();
        for (String e : readPlainFile(d, numDays, "hosts")) {
            l.add(new Host(e));
        }
        return l;
    }

    private List<User> getUsers(Date d, int numDays) {
        List<User> l = new ArrayList<User>();
        for (String e : readPlainFile(d, numDays, "users")) {
            l.add(new User(e));
        }
        return l;
    }

    private List<Email> getEmails(Date d, int numDays) {
        List<Email> l = new ArrayList<Email>();
        for (String e : readPlainFile(d, numDays, "emails")) {
            l.add(new Email(e));
        }
        return l;
    }

    private String getDateDir(Date d, int numDays)
    {
        StringBuffer sb = new StringBuffer(UVM_REPORTS_DATA);
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

    private String getAppDir(Date d, int numDays, String appName, String type, String val)
    {
        StringBuffer sb = new StringBuffer(UVM_REPORTS_DATA);
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
        List<String> appNames = getAppNames(dirName + "/../", type);

        Map<Integer, Application> m = new TreeMap<Integer, Application>();

        for (String s : appNames) {
            PackageDesc md = UvmContextFactory.context().toolboxManager().packageDesc(s);

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
                                     (m.get(pos)).getName() + "', but '" + 
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

        String f = dirName + type;

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

    private boolean generateReport(Date d, int numDays, String appName, String type, String value)
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

        String cmdStr = SUBREPORT_SCRIPT +
            " --node=\"" + appName + "\" " +
            " --end-date=\"" + df.format(d) + "\" " +
            " --report-days=\"" + numDays + "\" " +
            " --host=\"" + host + "\" " +
            " --user=\"" + user + "\" " +
            " --email=\"" + email + "\"";
        
        ExecManagerResult result = UvmContextFactory.context().execManager().exec(cmdStr);

        if (result.getResult() != 0) {
            logger.warn("Failed to generate subreport: \"" + cmdStr + "\" -> "  + result.getResult());
            try {
                String lines[] = result.getOutput().split("\\r?\\n");
                logger.warn("Creating Schema: ");
                for ( String line : lines )
                    logger.warn("Failed to generate subreport: " + line);
            } catch (Exception e) {}

            throw new RuntimeException("Failed to generate subreport: " + result.getOutput());
        }

        return true;
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

    public boolean isReportingEnabled()
    {
        UvmContext uvm = UvmContextFactory.context();
        NodeManager nodeManager = uvm.nodeManager();
        Node node = nodeManager.nodeInstances("untangle-node-reporting").get(0);
        if (node == null) {
            return false;
        }

        return true;
    }

    public boolean isReportsAvailable()
    {
        return 0 < getDates().size();
    }

}
