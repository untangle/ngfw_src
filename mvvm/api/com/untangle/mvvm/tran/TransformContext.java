/*
 * Copyright (c) 2003-2006 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.tran;

import java.io.InputStream;

import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tapi.IPSessionDesc;
import com.untangle.mvvm.toolbox.MackageDesc;
import com.untangle.mvvm.util.TransactionWork;

/**
 * Holds the context for a Transform instance.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface TransformContext
{
    /**
     * Get the Tid for this instance.
     *
     * @return the transform id.
     */
    Tid getTid();

    /**
     * Get the transform for this context.
     *
     * @return this context's transform.
     */
    Transform transform();

    /**
     * Returns desc from mvvm-transform.xml.
     *
     * @return the TransformDesc.
     */
    TransformDesc getTransformDesc();

    /**
     * Returns the transform preferences.
     *
     * @return the TransformPreferences.
     */
    TransformPreferences getTransformPreferences();

    /**
     * Get the {@link MackageDesc} corresponding to this instance.
     *
     * @return the MackageDesc.
     */
    MackageDesc getMackageDesc();

    // XXX should be LocalTransformContext ------------------------------------

    // XXX
    boolean runTransaction(TransactionWork tw);

    // XXX
    ClassLoader getClassLoader();

    InputStream getResourceAsStream(String resource);

    // call-through methods ---------------------------------------------------

    IPSessionDesc[] liveSessionDescs();

    TransformState getRunState();

    TransformStats getStats();
}
