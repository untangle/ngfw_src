/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.reporting;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class ReportingTransformImpl extends AbstractTransform implements ReportingTransform
{
    private final Logger logger = Logger.getLogger(getClass());

    private ReportingSettings settings;

    public ReportingTransformImpl() {}

    public void setReportingSettings(final ReportingSettings settings)
    {
        ReportingTransformImpl.this.settings = settings;

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Schedule sched = settings.getSchedule();
                    s.saveOrUpdate(settings);
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);
    }

    public ReportingSettings getReportingSettings()
    {
        return settings;
    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return new PipeSpec[0];
    }

    protected void postInit(String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from ReportingSettings ts where ts.tid = :tid");
                    q.setParameter("tid", getTid());
                    settings = (ReportingSettings)q.uniqueResult();

                    boolean bSave = false;

                    if (null == settings) {
                        // should never happen
                        settings = initSettings();
                        bSave = true;
                    } else {
                        Schedule sched = settings.getSchedule();
                        if (null == sched) {
                            // create and save schedule on initial conversion
                            settings.setSchedule(new Schedule());
                            bSave = true;
                        }
                    }

                    if (true == bSave) {
                        s.merge(settings);
                    }

                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);
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
        settings.setTid(getTid());

        return settings;
    }

    public void initializeSettings()
    {
        setReportingSettings(initSettings());
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
}
