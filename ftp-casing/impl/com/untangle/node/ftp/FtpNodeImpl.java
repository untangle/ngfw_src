/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.ftp;

import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.CasingPipeSpec;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;

/**
 * FTP node implementation.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
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
        TransactionWork<Object> tw = new TransactionWork<Object>()
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
        TransactionWork<Object> tw = new TransactionWork<Object>()
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
