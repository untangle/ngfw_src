/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.ftp;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.CasingPipeSpec;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

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

    public void setFtpSettings(final FtpSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(settings);
                    FtpTransformImpl.this.settings = settings;
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
            ctlPipeSpec.setEnabled(settings.isEnabled());
            dataPipeSpec.setEnabled(settings.isEnabled());
        }
    }

    public void initializeSettings() { }

    protected void postInit(String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from FtpSettings fs");
                    settings = (FtpSettings)q.uniqueResult();

                    if (null == settings) {
                        settings = new FtpSettings();
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
