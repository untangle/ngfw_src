/**
 * $Id$
 */
package com.untangle.node.reporting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.Validator;
import com.untangle.uvm.node.script.ScriptRunner;
import com.untangle.uvm.node.script.ScriptRunner.ScriptException;
import com.untangle.uvm.AdminManager;
import com.untangle.uvm.User;
import com.untangle.uvm.logging.LogWorker;
import com.untangle.uvm.logging.LogWorkerFacility;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.PipeSpec;

public class ReportingNodeImpl extends AbstractNode implements ReportingNode, LogWorkerFacility
{
    private static final Logger logger = Logger.getLogger(ReportingNodeImpl.class);

    private static final String  REPORTS_SCRIPT = System.getProperty("uvm.home") + "/bin/reporting-generate-reports.py";
    private static final String  REPORTER_LOG_FILE = "/var/log/uvm/reporter.log";
    private static final long    REPORTER_LOG_FILE_READ_TIMEOUT = 180 * 1000; /* 180 seconds */
    private static final Pattern REPORTER_LOG_PROGRESS_PATTERN = Pattern.compile(".*PROGRESS\\s*\\[(.*)\\]");

    private static final String CRON_STRING = "* * * root /usr/share/untangle/bin/reporting-generate-reports.py -d $(date \"+\\%Y-\\%m-\\%d\") > /dev/null 2>&1";
    private static final File CRON_FILE = new File("/etc/cron.d/untangle-reports-nightly");

    private static LogWorkerImpl logWorker = null;

    private String currentStatus = "";

    private ReportingSettings settings;

    private long lastFlushTime = 0;
    
    public ReportingNodeImpl()
    {
        if (logWorker == null)
            logWorker = new LogWorkerImpl(this);
    }

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

        writeCronFile();
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

        int exitCode = -1;
        logger.info("Running daily report...");
        boolean tryAgain = false;
        int tries = 0;

        synchronized (this) {
            do {
                tries++;
                tryAgain = false;
            
                try {
                    String args[] = { REPORTS_SCRIPT, "-r", "1", "-m", "-d", ts };
                    Process proc = UvmContextFactory.context().execManager().execEvil(args);
                    tailLog(REPORTER_LOG_FILE, REPORTER_LOG_FILE_READ_TIMEOUT, proc);
                    exitCode = proc.waitFor();
                    proc.destroy();
                } catch (Exception e) {
                    logger.error("Unable to run daily reports", e );
                }
                /* exitCode == 1 means another reports process is running, just wait and try again. */
                if (exitCode == 1)  {
                    logger.warn("Report process already running. Waiting and then trying again...");
                    tryAgain = true;
                    Thread.sleep(10000); // sleep 10 seconds
                }
            }
            while (tryAgain && tries < 20); // try max 20 times (20 * 10 seconds = 200 seconds)
        }        
        if (exitCode != 0) {
            if (exitCode == 1) 
                throw new Exception("A reports process is already running. Please try again later.");
            else
                throw new Exception("Unable to create daily reports. (Exit code: " + exitCode + ")");
        }
    }

    public String getCurrentStatus()
    {
        return this.currentStatus;
    }

    public void flushEvents()
    {
        long currentTime  = System.currentTimeMillis();
        
        int exitCode = -1;
        this.currentStatus = "";
            
        logger.info("Flushing queued events...");
        UvmContextFactory.context().loggingManager().forceFlush();

        logger.info("Running incremental report...");
        synchronized (this) {
            try {
                String args[] = { REPORTS_SCRIPT, "-m", "-i", "-r", "1" };
                Process proc = UvmContextFactory.context().execManager().execEvil(args);
                tailLog(REPORTER_LOG_FILE, REPORTER_LOG_FILE_READ_TIMEOUT, proc);
                exitCode = proc.waitFor();
                proc.destroy();
            } catch (Exception e) {
                logger.error("Unable to run incremental reports", e );
            }
        }

        this.currentStatus = "";
        lastFlushTime = System.currentTimeMillis();
                
        if (exitCode != 0) {
            logger.warn("Incremental reports exited with non-zero return code: " + exitCode);
        }
    }
    
    public void initializeSettings()
    {
        setReportingSettings(initSettings());
    }

    public Validator getValidator() {
        return new ReportingValidator();
    }

    public Object getSettings()
    {
        return getReportingSettings();
    }

    public void setSettings(Object settings)
    {
        setReportingSettings((ReportingSettings)settings);
    }

    public LogWorker getLogWorker()
    {
        return this.logWorker;
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


        if (!CRON_FILE.exists())
            writeCronFile();
    }

    protected void preStart()
    {
        if (this.settings == null) {
            String[] args = {""};
            postInit(args);
        }

        this.logWorker.start();
    }

    protected void postStop()
    {
        this.logWorker.stop();
    }

    
    private ReportingSettings initSettings()
    {
        ReportingSettings settings = new ReportingSettings();
        settings.setNodeId(getNodeId());

        loadReportingUsers(settings);

        return settings;
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
        AdminManager adminManager = UvmContextFactory.context().adminManager();
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

    private void tailLog(String logFile, long timeout, Process proc)
    {
        try {
            File file = new File(logFile);
            if (!file.exists()) {
                logger.warn(logFile + " not found.");
            }

            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(file.length()); // seek to end
                
            String line = null;
            while ((line = readLine(raf, timeout, proc)) != null) {
                Matcher match = REPORTER_LOG_PROGRESS_PATTERN.matcher(line);
                if (match.matches()) {
                    currentStatus = match.group(1);
                    logger.info("update currentStatus: " + currentStatus);
                }
            }
        } catch (IOException e) {
            logger.error("Unable to read log file.", e);
        }
        
        logger.info("tailLog() complete");
    }

    /**
     * This attempts to a read line
     *
     * It returns a line if successful
     * null if the process exits
     * null if the timeout expires
     */
    private String readLine(RandomAccessFile raf, long timeout, Process proc)
    {
        long lastActivity = -1;

        try {
            while (true) {
                long currentTime = System.currentTimeMillis();
                if (0 > lastActivity) {
                    lastActivity = currentTime;
                }
                String line = raf.readLine();

                if (line == null) {
                    try {
                        if (currentTime - lastActivity > timeout) {
                            // just end the thread adding TimeoutEvent
                            logger.warn("readLine timing out: " + (currentTime - lastActivity));
                            return null;
                        } else {
                            Thread.sleep(100);

                            if (isProcessComplete(proc)) 
                                return null; // if no more is coming just return
                        }
                    } catch (InterruptedException exn) { }
                } else {
                    lastActivity = currentTime;
                    return line;
                } 
            }
        } catch (IOException exn) {
            logger.warn("could not read apt.log", exn);
            throw new RuntimeException("could not read apt-log", exn);
        } catch (Exception exn) {
            logger.warn("could not read apt.log", exn);
            throw new RuntimeException("could not read apt-log", exn);
        }
    }

    private boolean isProcessComplete(Process proc)
    {
        try {
            int foo = proc.exitValue();
            return true;
        } catch (java.lang.IllegalThreadStateException e) {
            return false;
        }
    }

    private void writeCronFile()
    {
        // write the cron file for nightly runs
        String conf = settings.getNightlyMinute() + " " + settings.getNightlyHour() + " " + CRON_STRING;
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(CRON_FILE));
            out.write(conf, 0, conf.length());
            out.write("\n");
        } catch (IOException ex) {
            logger.error("Unable to write file", ex);
            return;
        }
        try {
            out.close();
        } catch (IOException ex) {
            logger.error("Unable to close file", ex);
            return;
        }
    }

}
