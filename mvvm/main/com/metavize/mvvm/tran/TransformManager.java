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

package com.metavize.mvvm.tran;

import java.util.List;
import java.util.Map;

import com.metavize.mvvm.policy.Policy;
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
     * @return list of all transform ids.
     */
    List<Tid> transformInstances();

    /**
     * Transform instances by name.
     *
     * @param name name of the transform.
     * @return tids of corresponding transforms.
     */
    List<Tid> transformInstances(String name);

    /**
     * Transform instances by policy.
     *
     * @param policy policy of transform.
     * @return tids of corresponding transforms.
     */
    List<Tid> transformInstances(Policy policy);

    /**
     * Transform instances by name policy.
     *
     * @param name name of transform.
     * @param policy policy of transform.
     * @return tids of corresponding transforms.
     */
    List<Tid> transformInstances(String name, Policy policy);

    /**
     * Create a new transform instance under the given policy.
     *
     * @param name of the transform.
     * @param policy the policy this instance is applied to.
     * @return the <code>tid</code> of the instance.
     * @exception DeployException if the instance cannot be created.
     */
    Tid instantiate(String name, Policy policy) throws DeployException;

    /**
     * Create a new transform instance under the given policy.
     *
     * @param name of the transform.
     * @param policy the policy this instance is applied to.
     * @param args transform args.
     * @return the <code>tid</code> of the instance.
     * @exception DeployException if the instance cannot be created.
     */
    Tid instantiate(String name, Policy policy, String[] args)
        throws DeployException;

    /**
     * Create a new transform instance.
     *
     * @param name of the transform.
     * @param args transform args.
     * @return the <code>tid</code> of the instance.
     * @exception DeployException if the instance cannot be created.
     */
    Tid instantiate(String name, String[] args) throws DeployException;

    /**
     * Create a new transform instance under the default policy.
     *
     * @param name of the transform.
     * @return the <code>tid</code> of the instance.
     * @exception DeployException if the instance cannot be created.
     */
    Tid instantiate(String name) throws DeployException;

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

    /**
     * Return the TransformContext for the thread.
     *
     * @return the TransformContext for the current thread, or null if
     * none.
     */
    TransformContext threadContext();

    /**
     * Get the statistics and counts for all transforms in one call.
     *
     * @return a <code>Map</code> from Tid to TransformStats for all
     * transforms in RUNNING state.
     */
    Map<Tid, TransformStats> allTransformStats();
}
