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

import java.util.HashSet;
import java.util.Set;

import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.Subscription;
import com.metavize.tran.token.CasingAdaptor;
import com.metavize.tran.token.CasingTransform;

public class FtpTransform extends CasingTransform
{
    private final PipeSpec insidePipeSpec;
    private final PipeSpec outsidePipeSpec;

    // constructors -----------------------------------------------------------

    public FtpTransform()
    {
        // inside PipeSpec
        Subscription s = new Subscription(Protocol.TCP);
        Set subs = new HashSet();
        subs.add(s);
        insidePipeSpec = new FtpPipeSpec(subs);

        // outside PipeSpec
        outsidePipeSpec = insidePipeSpec;
    }

    // CasingTransform methods ------------------------------------------------

    protected PipeSpec getInsidePipeSpec()
    {
        return insidePipeSpec;
    }

    protected PipeSpec getOutsidePipeSpec()
    {
        return outsidePipeSpec;
    }

    // lifecycle methods ------------------------------------------------------

    protected void preStart()
    {
        // inside
        CasingAdaptor ih = new CasingAdaptor(FtpCasingFactory.factory(),
                                             true);
        getInsideMPipe().setSessionEventListener(ih);

        // outside
        CasingAdaptor oh = new CasingAdaptor(FtpCasingFactory.factory(),
                                             false);
        getOutsideMPipe().setSessionEventListener(oh);

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
