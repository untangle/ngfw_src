/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: FtpTransform.java 1258 2005-07-07 04:02:17Z amread $
 */
package com.metavize.tran.ftp;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.CasingPipeSpec;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class FtpTransformImpl extends AbstractTransform
    implements FtpTransform
{
    private final PipeSpec ctlPipeSpec = new CasingPipeSpec
        ("ftp", this, FtpCasingFactory.factory(),
         Fitting.FTP_CTL_STREAM, Fitting.FTP_CTL_TOKENS);

    private final PipeSpec dataPipeSpec = new CasingPipeSpec
        ("ftp", this, FtpCasingFactory.factory(),
         Fitting.FTP_DATA_STREAM, Fitting.FTP_DATA_TOKENS);

    private final PipeSpec[] pipeSpecs = new PipeSpec[]
        { ctlPipeSpec, dataPipeSpec };
    private final Logger logger = Logger.getLogger(getClass());

    private FtpSettings settings;

    // constructors -----------------------------------------------------------

    public FtpTransformImpl() { }

    // FtpTransform methods ---------------------------------------------------

    public FtpSettings getFtpSettings()
    {
        return settings;
    }

    public void setFtpSettings(FtpSettings settings)
    {
        Session s = getTransformContext().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.merge(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get FtpSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close hibernate session", exn);
            }
        }

        reconfigure();
    }

    // Transform methods ------------------------------------------------------

    public void reconfigure()
    {
        if (null != settings) {
            ctlPipeSpec.setEnabled(settings.isEnabled());
            dataPipeSpec.setEnabled(settings.isEnabled());
        }
    }

    protected void initializeSettings()
    {
        settings = new FtpSettings();
        settings.setTid(getTid());
        setFtpSettings(settings);
    }

    protected void postInit(String[] args)
    {
        Session s = getTransformContext().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery
                ("from FtpSettings hbs where hbs.tid = :tid");
            q.setParameter("tid", getTid());
            settings = (FtpSettings)q.uniqueResult();

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get FtpSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }

        if (null == settings) {
            initializeSettings();
        }

        reconfigure();
    }

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getFtpSettings();
    }

    public void setSettings(Object settings)
    {
        setFtpSettings((FtpSettings)settings);
    }
}
