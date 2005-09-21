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

public class HttpTransformImpl extends AbstractTransform
    implements HttpTransform
{
    private final Logger logger = Logger.getLogger(HttpTransformImpl.class);

    private final CasingPipeSpec pipeSpec = new CasingPipeSpec
        ("http", this, new HttpCasingFactory(this),
         Fitting.HTTP_STREAM, Fitting.HTTP_TOKENS);

    private final PipeSpec[] pipeSpecs = new PipeSpec[] { pipeSpec };

    private HttpTransformCommon common;

    // constructors -----------------------------------------------------------

    public HttpTransformImpl() { }

    // HttpTransform methods --------------------------------------------------

    public HttpSettings getHttpSettings()
    {
        return null == common ? null : common.getHttpSettings();
    }

    public void setHttpSettings(HttpSettings settings)
    {
        if (null != common) {
            common.setHttpSettings(this, settings);
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
        common = HttpTransformCommon.common(this);
        common.registerListener(this);
        doReconfigure(common.getHttpSettings());
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

    void doReconfigure(HttpSettings settings)
    {
        if (null != common) {
            pipeSpec.setEnabled(settings.isEnabled());
            pipeSpec.setReleaseParseExceptions(!settings.isNonHttpBlocked());
        }
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
