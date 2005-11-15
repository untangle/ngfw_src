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

import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.CasingPipeSpec;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class HttpTransformImpl extends AbstractTransform
    implements HttpTransform
{
    private final Logger logger = Logger.getLogger(HttpTransformImpl.class);

    private final CasingPipeSpec pipeSpec = new CasingPipeSpec
        ("http", this, new HttpCasingFactory(this),
         Fitting.HTTP_STREAM, Fitting.HTTP_TOKENS);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { pipeSpec };

    private final EventLogger eventLogger;

    private HttpSettings settings;

    // constructors -----------------------------------------------------------

    public HttpTransformImpl()
    {
        this.eventLogger = new EventLogger(getTransformContext());
    }

    // HttpTransform methods --------------------------------------------------

    public HttpSettings getHttpSettings()
    {
        return settings;
    }

    public void setHttpSettings(final HttpSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(settings);
                    HttpTransformImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

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

    protected void preStart()
    {
        eventLogger.start();
    }

    protected void postStop()
    {
        eventLogger.stop();
    }

    protected void postInit(String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from HttpSettings hbs");
                    settings = (HttpSettings)q.uniqueResult();

                    if (null == settings) {
                        settings = new HttpSettings();
                        s.save(settings);
                    }

                    reconfigure();
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);
    }

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // package protected methods ----------------------------------------------

    void log(LogEvent le)
    {
        eventLogger.log(le);
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
