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

package com.metavize.tran.exploder;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tran.TransformException;
import org.apache.log4j.Logger;

public class ExploderImpl extends AbstractTransform implements Exploder
{
    private final Logger logger = Logger.getLogger(ExploderImpl.class);

    private final PipeSpec[] pipeSpecs = new PipeSpec[0];

    // constructors -----------------------------------------------------------

    public ExploderImpl()
    {
        logger.debug("<init>");
    }

    private void deployWebAppIfRequired(Logger logger) {
        if (MvvmContextFactory.context().loadWebApp("/browser", "browser")) {
            logger.debug("Deployed Browser web app");
        } else {
            logger.error("Unable to deploy Browser web app");
        }
    }

    private void unDeployWebAppIfRequired(Logger logger) {
        if (MvvmContextFactory.context().unloadWebApp("/browser")) {
            logger.debug("Unloaded Browser web app");
        } else {
            logger.error("Unable to unload Browser web app");
        }
    }

    // Exploder methods -------------------------------------------------------

    // Transform methods ------------------------------------------------------

    public void reconfigure()
    {
    }

    protected void initializeSettings() { }

    @Override
        protected void preDestroy() throws TransformException {
        super.preDestroy();
        logger.debug("preDestroy()");
        unDeployWebAppIfRequired(logger);
    }

    protected void postInit(String[] args)
    {
        logger.debug("postInit()");

        deployWebAppIfRequired(logger);
    }

    // AbstractTransform methods ----------------------------------------------

    @Override
        protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings() { return null; }

    public void setSettings(Object settings) { }
}
