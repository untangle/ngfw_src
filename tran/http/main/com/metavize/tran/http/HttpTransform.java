/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: HttpTransform.java,v 1.11 2005/01/30 09:20:30 amread Exp $
 */
package com.metavize.tran.http;

import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.Subscription;
import com.metavize.tran.token.CasingAdaptor;
import com.metavize.tran.token.CasingTransform;
import org.apache.log4j.Logger;


public class HttpTransform extends CasingTransform
{
    private final Logger logger = Logger.getLogger(HttpTransform.class);

    private final PipeSpec insidePipeSpec;
    private final PipeSpec outsidePipeSpec;

    // constructors -----------------------------------------------------------

    public HttpTransform()
    {
        System.out.println("HTTP_TRANSFORM: " + getTid() + " TCL: "
                           + Thread.currentThread().getContextClassLoader());

        // inside PipeSpec
        Subscription s = new Subscription(Protocol.TCP);
        insidePipeSpec = new PipeSpec("http-inside", Fitting.HTTP_STREAM,
                                      Fitting.HTTP_TOKENS, s);

        // outside PipeSpec
        outsidePipeSpec = new PipeSpec("http-outside", Fitting.HTTP_TOKENS,
                                       Fitting.HTTP_STREAM, s);
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
