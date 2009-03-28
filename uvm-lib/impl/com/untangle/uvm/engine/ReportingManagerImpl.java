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
import java.lang.Math;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.node.NodeContext;
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
import com.untangle.uvm.reports.Section;
import com.untangle.uvm.reports.SummaryItem;
import com.untangle.uvm.reports.SummarySection;
import com.untangle.uvm.reports.TableOfContents;
import com.untangle.uvm.reports.User;
import com.untangle.uvm.security.Tid;
import org.apache.log4j.Logger;

class RemoteReportingManagerImpl implements RemoteReportingManager
{
    private static final String BUNNICULA_REPORTS
        = System.getProperty("bunnicula.reports.dir");

    private static final File REPORTS_DIR = new File(BUNNICULA_REPORTS);


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

        for (String s : REPORTS_DIR.list()) {
            String[] split = s.split("-");
            if (split.length == 3) {
                try {
                    int year = Integer.decode(split[0]);
                    int month = Integer.decode(split[1]);
                    int day = Integer.decode(split[2]);
                    l.add(new Date(year, month, day));
                } catch (NumberFormatException exn) {
                    logger.debug("not a date: " + s);
                }
            } else {
                logger.debug("not a date: " + s);
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

    public boolean isReportsAvailable()
    {
        return true;
    }
}
