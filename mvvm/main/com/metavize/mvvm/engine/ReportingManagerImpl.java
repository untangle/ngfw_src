/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: ReportingManagerImpl.java 1185 2005-06-27 21:26:25Z rbscott $
 */

package com.metavize.mvvm.engine;

import java.io.File;
import java.util.List;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.ReportingManager;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformManager;
import com.metavize.mvvm.tran.TransformState;
import org.apache.log4j.Logger;

class ReportingManagerImpl implements ReportingManager
{
    private static final String BUNNICULA_WEB = System.getProperty( "bunnicula.web.dir" );

    private static final String WEB_REPORTS_DIR = BUNNICULA_WEB + "/reports";
    private static final String CURRENT_REPORT_DIR = WEB_REPORTS_DIR + "/current";

    private static final Logger logger = Logger.getLogger( ReportingManagerImpl.class );

    private static ReportingManagerImpl REPORTING_MANAGER = new ReportingManagerImpl();

    private ReportingManagerImpl() { }

    static ReportingManagerImpl reportingManager()
    {
        return REPORTING_MANAGER;
    }

    public boolean isReportingEnabled() {
        MvvmLocalContext mvvm = MvvmContextFactory.context();
        TransformManager transformManager = mvvm.transformManager();
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
        File sum = new File(CURRENT_REPORT_DIR, "reporting-transform/sum-daily.html");
        return (sum.isFile());
    }
}
