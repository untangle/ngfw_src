/*
 * Copyright (c) 2003, 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: TransformManager.java,v 1.4 2004/12/25 11:30:17 amread Exp $
 */

package com.metavize.mvvm.tran;

import com.metavize.mvvm.security.Tid;

/**
 * Manages transform instances in the pipeline.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public interface TransformManager
{
    /**
     * Get <code>Tid</code>s of transforms in the pipeline.
     *
     * @return a <code>Tid[]</code> value
     */
    Tid[] transformInstances();

    Tid[] transformInstances(String name);

    /**
     * Create a new transform instance.
     *
     * @param name of the transform.
     * @return the <code>tid</code> of the instance.
     */
    Tid instantiate(String name) throws DeployException;

    /**
     * Create a new transform instance.
     *
     * @param name of the transform.
     * @param args transform args.
     * @return the <code>tid</code> of the instance.
     */
    Tid instantiate(String name, String[] args) throws DeployException;

    /**
     * Remove transform instance from the pipeline.
     *
     * @param tid <code>Tid</code> of instance to be destroyed.
     * @exception UndeployException if detruction fails.
     */
    void destroy(Tid tid) throws UndeployException;

    /**
     * Get the <code>TransformContext</code> for a transform instance.
     *
     * @param tid <code>Tid</code> of the instance.
     * @return the instance's <code>TransformContext</code>.
     */
    TransformContext transformContext(Tid tid);
}
