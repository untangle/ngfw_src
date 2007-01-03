/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmException;
import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.ReportingManager;
import com.untangle.mvvm.reporting.Reporter;
import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.tran.LocalTransformManager;
import com.untangle.mvvm.tran.TransformState;
import org.apache.log4j.Logger;

class ReportingManagerImpl implements ReportingManager
{
    private static final String BUNNICULA_WEB = System.getProperty( "bunnicula.web.dir" );

    private static final String WEB_REPORTS_DIR = BUNNICULA_WEB + "/reports";
    private static final String CURRENT_REPORT_DIR = WEB_REPORTS_DIR + "/current";

    private final Logger logger = Logger.getLogger(getClass());

    private static ReportingManagerImpl REPORTING_MANAGER = new ReportingManagerImpl();

    private enum RunState {
        START,                  // Not yet prepared 
        PREPARING,              // Currently preparing reports
        READY,                  // Prepared and Ready to run reports
        RUNNING                // Running report.
        // Goes back to START after RUNNING, so no need for another state.
    };

    private volatile RunState state;

    // Prepare info
    private volatile String outputBaseDir;
    private volatile int daysToKeep;
    private volatile Date midnight ;

    // Run info
    private volatile Thread runThread;
    private Reporter reporter;

    private ReportingManagerImpl() {
        state = RunState.START;
        runThread = null;
        reporter = null;
    }

    static ReportingManagerImpl reportingManager()
    {
        return REPORTING_MANAGER;
    }

    public void prepareReports(String outputBaseDir, Date midnight, int daysToKeep)
        throws MvvmException
    {
        synchronized (this) {
            switch (state) {
            case PREPARING:
                throw new MvvmException("Already preparing reports");
            case RUNNING:
                throw new MvvmException("Reports are currently running");
            case START:
            case READY:
                break;
            }
            this.outputBaseDir = outputBaseDir;
            this.daysToKeep = daysToKeep;
            this.midnight = midnight;
            logger.debug("Now PREPARING");
            state = RunState.PREPARING;
            notifyAll();
            reporter = new Reporter(outputBaseDir, midnight, daysToKeep);
        }
        try {
            reporter.prepare();
            logger.debug("Finished, now READY ");
        } catch (Exception x) {
            logger.error("Exception preparing reports", x);
        } finally {
            synchronized (this) {
                state = RunState.READY;
                notifyAll();
            }
        }
    }

    public boolean isRunning()
    {
        synchronized (this) {
            return (state == RunState.RUNNING);
        }
    }

    public void startReports()
        throws MvvmException
    {   
        synchronized (this) {
            switch (state) {
            case START:
                throw new MvvmException("Haven't prepared yet");
            case PREPARING:
                throw new MvvmException("Still preparing");
            case RUNNING:
                throw new MvvmException("Already started, need to stop before starting again.");
            case READY:
                break;
            }
            logger.debug("Now RUNNING");
            state = RunState.RUNNING;
            notifyAll();
            Runnable task = new Runnable() {
                    public void run() {
                        try {
                            reporter.run();
                        } catch (Exception x) {
                            logger.error("Exception running reports", x);
                        } finally {
                            logger.debug("Run finished.  Back to START.");
                            synchronized (this) {
                                state = RunState.START;
                                notifyAll();
                                reporter = null;
                                runThread = null;
                            }
                        }
                    }
                };
            runThread = MvvmContextFactory.context().newThread(task, "Reports");
        }
        runThread.start();
    }

    /**
     * Guaranteed death
     *
     */
    public void stopReports()
        throws MvvmException
    {
        synchronized (this) {
            switch (state) {
            case START:
                throw new MvvmException("Haven't begun to prepare reports");
            case PREPARING:
                throw new MvvmException("Can't stop while preparing, wait til done");
            case RUNNING:
                reporter.setNeedToDie();
                runThread.interrupt();
                break;
            case READY:
                return;
            }
            try {
                this.wait(1000);
            } catch (InterruptedException x) {
                // Can't happen.
            }
            if (state != RunState.START)
                throw new MvvmException("Unable to stop reports, ended in state " +
                                        state.toString());
        }
    }

    public boolean isReportingEnabled() {
        MvvmLocalContext mvvm = MvvmContextFactory.context();
        LocalTransformManager transformManager = mvvm.transformManager();
        List<Tid> tids = transformManager.transformInstances("reporting-transform");
        if(tids == null || tids.size() == 0)
            return false;
        // What if more than one? Shouldn't happen. XX
        TransformContext context = transformManager.transformContext(tids.get(0));
        if (context == null)
            return false;
        TransformState state = context.getRunState();
        return (state == TransformState.RUNNING);
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
