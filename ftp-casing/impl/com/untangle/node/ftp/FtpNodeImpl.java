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
package com.untangle.node.ftp;

import com.untangle.uvm.tapi.AbstractNode;
import com.untangle.uvm.tapi.CasingPipeSpec;
import com.untangle.uvm.tapi.Fitting;
import com.untangle.uvm.tapi.PipeSpec;
import com.untangle.uvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class FtpNodeImpl extends AbstractNode
    implements FtpNode
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

    public FtpNodeImpl() { }

    // FtpNode methods ---------------------------------------------------

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
                    FtpNodeImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        reconfigure();
    }

    // Node methods ------------------------------------------------------

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
        getNodeContext().runTransaction(tw);
    }

    // AbstractNode methods ----------------------------------------------

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
