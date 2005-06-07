/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.spam;

import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.Interface;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tapi.SoloTransform;
import com.metavize.mvvm.tapi.Subscription;
import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.mvvm.tran.PortRange;
import com.metavize.tran.token.TokenAdaptor;
import org.apache.log4j.Logger;

public class SpamImpl extends SoloTransform implements Spam
{
    private static final Logger logger = Logger.getLogger(SpamImpl.class);

    private final SpamFactory factory = new SpamFactory();
    private final PipeSpec pipeSpec;

    // constructors -----------------------------------------------------------

    public SpamImpl()
    {
        Subscription s = new Subscription(Protocol.TCP,
                                          Interface.ANY, Interface.ANY,
                                          IPMaddr.anyAddr, PortRange.ANY,
                                          IPMaddr.anyAddr, PortRange.ANY);

        pipeSpec = new SoloPipeSpec("spam", s, Fitting.SMTP_TOKENS,
                                    Affinity.CLIENT, 0);
    }

    // SoloTransform methods --------------------------------------------------

    public void reconfigure() { }

    protected void initializeSettings() { }

    protected PipeSpec getPipeSpec()
    {
        return pipeSpec;
    }

    protected void postInit(String[] args) { }

    protected void preStart()
    {
        getMPipe().setSessionEventListener(new TokenAdaptor(factory));
    }

    protected void postDestroy() { }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return null;
    }

    public void setSettings(Object settings) { }
}
