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

package com.metavize.tran.http;


import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.CasingPipeSpec;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.Query;

public class HttpTransformImpl extends AbstractTransform
    implements HttpTransform
{
    private final Logger logger = Logger.getLogger(HttpTransformImpl.class);

    private final CasingPipeSpec pipeSpec = new CasingPipeSpec
        ("http", this, new HttpCasingFactory(this),
         Fitting.HTTP_STREAM, Fitting.HTTP_TOKENS);

    private final PipeSpec[] pipeSpecs = new PipeSpec[] { pipeSpec };

    private HttpSettings settings;

    // constructors -----------------------------------------------------------

    public HttpTransformImpl() { }

    // HttpTransform methods --------------------------------------------------

    public HttpSettings getHttpSettings()
    {
        return settings;
    }

    public void setHttpSettings(HttpSettings settings)
    {
        Session s = getTransformContext().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdate(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get HttpSettings", exn);
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
            pipeSpec.setEnabled(settings.isEnabled());
            pipeSpec.setReleaseParseExceptions(!settings.isNonHttpBlocked());
        }
    }

    protected void initializeSettings() { }

    protected void postInit(String[] args)
    {
        Session s = getTransformContext().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from HttpSettings hbs");
            settings = (HttpSettings)q.uniqueResult();

            if (null == settings) {
                settings = new HttpSettings();
                s.save(settings);
            }

            reconfigure();

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get HttpSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }
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
        return getHttpSettings();
    }

    public void setSettings(Object settings)
    {
        setHttpSettings((HttpSettings)settings);
    }
}
