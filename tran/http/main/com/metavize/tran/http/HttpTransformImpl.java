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

import java.util.HashSet;
import java.util.Set;

import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.Subscription;
import com.metavize.tran.token.CasingAdaptor;
import com.metavize.tran.token.CasingTransform;
import org.apache.log4j.Logger;

public class HttpTransformImpl extends CasingTransform
{
    private final Logger logger = Logger.getLogger(HttpTransformImpl.class);

    private final PipeSpec insidePipeSpec;
    private final PipeSpec outsidePipeSpec;

    // constructors -----------------------------------------------------------

    public HttpTransformImpl()
    {
        // inside PipeSpec
        Subscription s = new Subscription(Protocol.TCP);
        Set subs = new HashSet();
        subs.add(s);
        insidePipeSpec = new HttpPipeSpec(subs);

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
        CasingAdaptor ih = new CasingAdaptor(HttpCasingFactory.factory(),
                                             true);
        getInsideMPipe().setSessionEventListener(ih);

        // outside
        CasingAdaptor oh = new CasingAdaptor(HttpCasingFactory.factory(),
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
