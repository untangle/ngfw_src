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

    private FtpTransformCommon common;

    // constructors -----------------------------------------------------------

    public FtpTransformImpl() { }

    // FtpTransform methods ---------------------------------------------------

    public FtpSettings getFtpSettings()
    {
        return null == common ? null : common.getFtpSettings();
    }

    public void setFtpSettings(FtpSettings settings)
    {
        if (null != common) {
            common.setFtpSettings(this, settings);
        }
    }

    // Transform methods ------------------------------------------------------

    public void reconfigure()
    {
        common.reconfigure();
    }

    protected void initializeSettings() { }

    protected void postInit(String[] args)
    {
        common = FtpTransformCommon.common(this);
        common.registerListener(this);
        doReconfigure(common.getFtpSettings());
    }

    protected void preDestroy()
    {
        common.deregisterListener(this);
        common = null;
    }

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // package protected methods ----------------------------------------------

    void doReconfigure(FtpSettings settings)
    {
        if (null != common) {
            ctlPipeSpec.setEnabled(settings.isEnabled());
            dataPipeSpec.setEnabled(settings.isEnabled());
        }
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
