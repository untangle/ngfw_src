/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
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
import com.metavize.mvvm.tapi.TransformContextFactory;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class ReportingTransformImpl extends AbstractTransform implements ReportingTransform
{
    private static final Logger logger = Logger
        .getLogger(ReportingTransformImpl.class);

    private ReportingSettings settings;

    public ReportingTransformImpl() {}

    public void setReportingSettings(ReportingSettings settings)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not save ReportingSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close session", exn);
            }
        }
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
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery
                ("from ReportingSettings ts where ts.tid = :tid");
            q.setParameter("tid", getTid());
            settings = (ReportingSettings)q.uniqueResult();

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get ReportingSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("Could not close settings", exn);
            }
        }
    }

    protected void preStart()
    {
        if (this.settings == null) {
            String[] args = {""};
            postInit(args);
        }
    }

    protected void initializeSettings()
    {
        ReportingSettings settings = new ReportingSettings();
        settings.setTid(getTid());
        setReportingSettings(settings);
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
