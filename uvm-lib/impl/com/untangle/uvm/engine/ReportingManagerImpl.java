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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeDesc;
import com.untangle.uvm.reporting.Reporter;
import com.untangle.uvm.reports.Application;
import com.untangle.uvm.reports.ApplicationData;
import com.untangle.uvm.reports.Chart;
import com.untangle.uvm.reports.ColumnDesc;
import com.untangle.uvm.reports.DetailSection;
import com.untangle.uvm.reports.Host;
import com.untangle.uvm.reports.KeyStatistic;
import com.untangle.uvm.reports.LegendItem;
import com.untangle.uvm.reports.RemoteReportingManager;
import com.untangle.uvm.reports.ReportDesc;
import com.untangle.uvm.reports.Section;
import com.untangle.uvm.reports.SummaryItem;
import com.untangle.uvm.reports.SummarySection;
import com.untangle.uvm.reports.TableOfContents;
import com.untangle.uvm.reports.User;
import com.untangle.uvm.security.Tid;
import org.apache.log4j.Logger;

class RemoteReportingManagerImpl implements RemoteReportingManager
{
    private static final String BUNNICULA_WEB = System.getProperty( "bunnicula.web.dir" );

    private static final String WEB_REPORTS_DIR = BUNNICULA_WEB + "/reports";
    private static final String CURRENT_REPORT_DIR = WEB_REPORTS_DIR + "/current";

    private final Logger logger = Logger.getLogger(getClass());

    private static RemoteReportingManagerImpl REPORTING_MANAGER = new RemoteReportingManagerImpl();

    // Prepare info
    private volatile String outputBaseDir;
    private volatile int daysToKeep;
    private volatile Date midnight ;

    // Run info
    private volatile Thread runThread;
    private Reporter reporter;

    private RemoteReportingManagerImpl() {
        runThread = null;
        reporter = null;
    }

    static RemoteReportingManagerImpl reportingManager()
    {
        return REPORTING_MANAGER;
    }

    // NEW SHIZZLE -------------------------------------------------------------

    // XXX SAMPLE DATA
    public List<Date> getDates()
    {
        List<Date> l = new ArrayList<Date>();

        Calendar c = Calendar.getInstance();
        for (int i = 0; i < 10; i++) {
            c.add(Calendar.DAY_OF_WEEK, -1);
            l.add(c.getTime());
        }

        return l;
    }

    // XXX SAMPLE DATA
    public TableOfContents getTableOfContents(Date d)
    {
        Application platform = new Application("untangle-vm", "Platform");

        LocalUvmContext uvm = LocalUvmContextFactory.context();
        LocalNodeManager nodeManager = uvm.nodeManager();

        List<ReportDesc> reportDescs = new ArrayList<ReportDesc>();

        for (Tid t : nodeManager.nodeInstances()) {
            NodeContext nc = nodeManager.nodeContext(t);
            NodeDesc nd = nc.getNodeDesc();
            ReportDesc rd = ReportDesc.getReportDesc(nd);
            if (null != rd) {
                reportDescs.add(rd);
            }
        }

        Collections.sort(reportDescs);

        List<Application> apps = new ArrayList<Application>(reportDescs.size());

        for (ReportDesc rd : reportDescs) {
            apps.add(rd.getApplication());
        }

        // XXX TODO
        List<User> users = new ArrayList<User>();
        List<Host> hosts = new ArrayList<Host>();

        return new TableOfContents(platform, apps, users, hosts);
    }

    // XXX SAMPLE DATA
    public ApplicationData getApplicationData(Date d, String appName)
    {
        List<Section> s = new ArrayList<Section>();
        s.add(getBogusSummary());
        s.add(getBogusDetails());

        return new ApplicationData(s);
    }

    public ApplicationData getApplicationDataForUser(Date d, String appName,
                                                     String username)
    {
        List<Section> s = new ArrayList<Section>();
        s.add(getBogusDetails());
        return new ApplicationData(s);
    }

    public ApplicationData getApplicationDataForHost(Date d, String appName,
                                                     String hostname)
    {
        List<Section> s = new ArrayList<Section>();
        s.add(getBogusDetails());
        return new ApplicationData(s);
    }

    public ApplicationData getApplicationDataForEmail(Date d, String appName,
                                                      String emailAddr)
    {
        List<Section> s = new ArrayList<Section>();
        s.add(getBogusDetails());
        return new ApplicationData(s);
    }



    private Section getBogusSummary()
    {
        List<SummaryItem> sis = new ArrayList<SummaryItem>();

        String imageUrl = "script/samples/graph0.png";
        String csvUrl = "/reports/date/data.csv";
        String printerUrl = "/reports/date/shizzle.html";
        List<KeyStatistic> ks = new ArrayList<KeyStatistic>();
        ks.add(new KeyStatistic("Girth", 60, "inch"));
        ks.add(new KeyStatistic("Weight", 350, "lbs"));
        ks.add(new KeyStatistic("Engineers Maimed", 3, null));

        List<LegendItem> li = new ArrayList<LegendItem>();
        li.add(new LegendItem("Puppies Harmed", "/reports/legend/bluedash.png"));

        Chart c = new Chart("Bogus Chart", imageUrl, csvUrl, printerUrl, ks, "Date", "Terra", li);
        sis.add(c);

        Chart c1 = new Chart("Bogus Chart", imageUrl, csvUrl, printerUrl, ks, "Date", "Terra", li);
        sis.add(c1);

        return new SummarySection("Bogus Summary Section", sis);
    }

    private Section getBogusDetails()
    {
        List<ColumnDesc> cds = new ArrayList<ColumnDesc>();
        cds.add(new ColumnDesc("time", "Time", "Date"));
        cds.add(new ColumnDesc("site", "URL", "URL"));
        cds.add(new ColumnDesc("user", "User", "UserLink"));
        cds.add(new ColumnDesc("host", "Host", "HostLink"));
        cds.add(new ColumnDesc("email", "Email", "EmailLink"));

        Calendar c = Calendar.getInstance();

        List<List> data = new ArrayList<List>();
        for (int i = 0; i < 100; i++) {
            List l = new ArrayList();
            l.add(c.getTime());
            l.add("http://foo.bar/" + i);
            l.add("user" + i);
            l.add("10.0.0." + i);
            l.add("mail" + (Math.floor((Math.random()*1000)))+"@foo.bar");
            data.add(l);
            c.add(Calendar.DAY_OF_WEEK, -1);
        }

        return new DetailSection("Bogus Detail Section", cds, data);
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

    public boolean isReportsAvailable() {
        if (!isReportingEnabled())
            return false;
        File crd = new File(CURRENT_REPORT_DIR);
        if (!crd.isDirectory())
            return false;

        // note that Reporter creates env file
        File envFile = new File(CURRENT_REPORT_DIR, "settings.env");

        FileReader envFReader;
        try {
            envFReader = new FileReader(envFile);
        } catch (FileNotFoundException exn) {
            logger.error("report settings env file is missing: ", exn);
            return false;
        }

        BufferedReader envBReader = new BufferedReader(envFReader);
        ArrayList<String> envList = new ArrayList<String>();
        try {
            while (true == envBReader.ready()) {
                envList.add(envBReader.readLine());
            }
            envBReader.close();
            envFReader.close();
        } catch (IOException exn) {
            logger.error("cannot read or close report settings env file: ", exn);
            return false;
        }

        String daily = "export MV_EG_DAILY_REPORT=y";
        if (true == envList.contains(daily)) {
            return true;
        }

        String weekly = "export MV_EG_WEEKLY_REPORT=y";
        if (true == envList.contains(weekly)) {
            return true;
        }

        String monthly = "export MV_EG_MONTHLY_REPORT=y";
        if (true == envList.contains(monthly)) {
            return true;
        }

        return false;
    }
}
