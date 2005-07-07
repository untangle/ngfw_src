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

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.tran.token.TokenAdaptor;
import org.apache.log4j.Logger;

public class SpamImpl extends AbstractTransform implements Spam
{
    private static final Logger logger = Logger.getLogger(SpamImpl.class);

    private final SpamFactory factory = new SpamFactory();
    private final SoloPipeSpec pipeSpec = new SoloPipeSpec
        ("spam", this, new TokenAdaptor(factory), Fitting.SMTP_TOKENS,
         Affinity.CLIENT, 0);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { pipeSpec };

    // constructors -----------------------------------------------------------

    public SpamImpl() { }

    // SoloTransform methods --------------------------------------------------

    public void reconfigure() { }

    protected void initializeSettings() { }

    // MultiTransform methods -------------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return null;
    }

    public void setSettings(Object settings) { }
}
