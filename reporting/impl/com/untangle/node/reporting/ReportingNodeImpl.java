/*
 * $Id$
 */
package com.untangle.node.reporting;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.node.util.SimpleExec;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.node.Validator;
import com.untangle.uvm.AdminManager;
import com.untangle.uvm.User;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.PipeSpec;

public class ReportingNodeImpl extends AbstractNode implements ReportingNode
{
    private static final Logger logger = Logger.getLogger(ReportingNodeImpl.class);

    private static final String REPORTS_SCRIPT = System.getProperty("uvm.home") + "/bin/reporting-generate-reports.py";
    private static int MAX_FLUSH_FREQUENCY;

    static {
        String shortSyncStr = System.getProperty("uvm.event.short_sync");
        if (shortSyncStr != null) {
            try {
                MAX_FLUSH_FREQUENCY = Integer.valueOf(shortSyncStr); 
            }
            catch (Exception e) {
                logger.warn("Invalid Flush Frequency: " + shortSyncStr);
                MAX_FLUSH_FREQUENCY = 30*1000; /* 30 seconds */
            }
        } else {
            MAX_FLUSH_FREQUENCY = 30*1000; /* 30 seconds */
        }
    }
    
    private ReportingSettings settings;

    private long lastFlushTime = 0;
    
    public ReportingNodeImpl() {}

    public void setReportingSettings(final ReportingSettings settings)
    {
        ReportingNodeImpl.this.settings = settings;

        TransactionWork<Void> tw = new TransactionWork<Void>() {
            public boolean doWork(Session s)
            {
                s.saveOrUpdate(settings);
                return true;
            }

            public Void getResult() {
                return null;
            };
        };
        getNodeContext().runTransaction(tw);
    }

    public ReportingSettings getReportingSettings()
    {
        return settings;
    }

    public void runDailyReport() throws Exception
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date()); // now
        cal.add(Calendar.DATE, 1); // tomorrow
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String ts = df.format(cal.getTime());
        boolean failed = false;
        SimpleExec.SimpleExecResult result = SimpleExec.exec(REPORTS_SCRIPT, new String[] {"-r", "1", "-m", "-d", ts},
                                                             null,//env
                                                             null,//rootDir
                                                             true,//stdout
                                                             true,//stderr
                                                             1000*900); // 15 minutes timeout

        if (result.exitCode != 0) {
            throw new Exception("Unable to run daily reports: \nReturn code: " +
                                result.exitCode + ", stdout \"" +
                                new String(result.stdOut) + "\", stderr \"" +
                                new String(result.stdErr) + "\"");
        }
    }

    public void flushEvents()
    {
        flushEvents(false);
    }

    public void flushEvents(boolean force)
    {
        long currentTime  = System.currentTimeMillis();

        if (!force && currentTime - lastFlushTime < MAX_FLUSH_FREQUENCY) {
            logger.info("Ignoring flushEvents call (not enough time has elasped)");
        } else {
            try {
                lastFlushTime = System.currentTimeMillis();
            
                logger.info("Flushing queued events...");

                LocalUvmContextFactory.context().loggingManager().forceFlush();

                logger.info("Running incremental report...");
            
                SimpleExec.SimpleExecResult result = SimpleExec.exec(REPORTS_SCRIPT, new String[] { "-m", "-i" },
                                                                     null,//env
                                                                     null,//rootDir
                                                                     true,//stdout
                                                                     true,//stderr
                                                                     1000*30*60); // 30 minutes timeout

                /* set the time again, this is because the process itself can take a while */
                lastFlushTime = System.currentTimeMillis();

                if (result.exitCode != 0) {
                    throw new Exception("Unable to run daily reports: \nReturn code: " +
                                        result.exitCode + ", stdout \"" +
                                        new String(result.stdOut) + "\", stderr \"" +
                                        new String(result.stdErr) + "\"");
                }
            } catch (Exception e) {
                logger.warn("Failed to run incremental report: ", e);
            }
        }
    }
    
    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return new PipeSpec[0];
    }

    protected void postInit(String[] args)
    {
        TransactionWork<Void> tw = new TransactionWork<Void>() {
            public boolean doWork(Session s) {
                Query q = s
                .createQuery("from ReportingSettings ts where ts.nodeId = :nodeId");
                q.setParameter("nodeId", getNodeId());
                settings = (ReportingSettings) q.uniqueResult();

                if (null == settings) {
                    settings = initSettings();
                    s.save(settings.getSchedule());
                    s.merge(settings);
                }

                if (null == settings.getSchedule()) {
                    /* You have to save the schedule before continuing */
                    settings.setSchedule(new Schedule());
                    s.save(settings.getSchedule());
                    s.merge(settings);
                }

                return true;
            }

            public Void getResult() {
                return null;
            }
        };

        getNodeContext().runTransaction(tw);

        //run incremental reports to create the schemas for the first time        
        flushEvents(); 
    }

    protected void preStart()
    {
        if (this.settings == null) {
            String[] args = {""};
            postInit(args);
        }
    }

    private ReportingSettings initSettings()
    {
        ReportingSettings settings = new ReportingSettings();
        settings.setNodeId(getNodeId());

        loadReportingUsers(settings);

        return settings;
    }

    public void initializeSettings()
    {
        setReportingSettings(initSettings());
    }

    public Validator getValidator() {
        return new ReportingValidator();
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getReportingSettings();
    }

    public void setSettings(Object settings)
    {
        setReportingSettings((ReportingSettings)settings);
    }

    /*
     * Add admin users to the list of reporting users, and set them up for
     * emailed reports.
     *
     * The list is maintained in the DB as a comma-separated string, so we
     * split it first, deal with the resulting HashSet, and spit out another
     * comma-separated string at the very end of this method. */
    private void loadReportingUsers(ReportingSettings s)
    {
        AdminManager adminManager = LocalUvmContextFactory.context().adminManager();
        String reportEmail = adminManager.getMailSettings().getReportEmail();
        Set<String> res = new HashSet<String>();
        if ((reportEmail != null) && (!reportEmail.isEmpty())) {
            reportEmail = reportEmail.trim();
            res.addAll(Arrays.asList(reportEmail.split(",")));
        }

        /* add in all other admins with an email */
        for (User user : adminManager.getAdminSettings().getUsers()) {
            String email = user.getEmail();
            if ((email != null) && (!email.equals("[no email]")
                    && (!email.isEmpty()))) {
                res.add(email);
            }
        }

        // assemble back the comma-separated string
        StringBuilder sb = new StringBuilder();
        for ( String email : res ) {
            if ( sb.length() > 0 ) {
                sb.append(",");
            }
            sb.append(email);
        }

        reportEmail = sb.toString();
        // modify the passed-in ReportingSettings, so the users we gathered
        // are now known to the report node
        s.setReportingUsers(reportEmail);
        // also sign them up for emailed reports
        adminManager.getMailSettings().setReportEmail(reportEmail);
    }
}
