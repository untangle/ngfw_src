/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: TransformContext.java,v 1.8 2005/03/25 02:08:09 jdi Exp $
 */

package com.metavize.mvvm.tran;

import com.metavize.mvvm.MackageDesc;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tapi.IPSessionDesc;
import net.sf.hibernate.Session;

/**
 * Holds the context for a Transform instance.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
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
     * Get the {@link TransformDesc} for this instance.
     *
     * @return the TransformDesc.
     */
    TransformDesc getTransformDesc();

    /**
     * Get the {@link MackageDesc} corresponding to this instance.
     *
     * @return the MackageDesc.
     */
    MackageDesc getMackageDesc();

    Session openSession();

    // call-through methods ---------------------------------------------------

    IPSessionDesc[] liveSessionDescs();

    TransformState getRunState();

    TransformStats getStats();
}
