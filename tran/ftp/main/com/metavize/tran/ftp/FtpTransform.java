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
package com.metavize.tran.ftp;


import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.CasingPipeSpec;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;

public class FtpTransform extends AbstractTransform
{
    private final PipeSpec[] pipeSpecs = new PipeSpec[] {
        new CasingPipeSpec("ftp", this, FtpCasingFactory.factory(),
                           Fitting.FTP_STREAM, Fitting.FTP_TOKENS)
    };

    // constructors -----------------------------------------------------------

    public FtpTransform() { }

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        throw new UnsupportedOperationException("bad move");
    }

    public void setSettings(Object settings)
    {
        throw new UnsupportedOperationException("bad move");
    }
}
