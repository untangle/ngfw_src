/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.util.List;
import java.util.Map;

import com.metavize.mvvm.policy.Policy;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.DeployException;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformManager;
import com.metavize.mvvm.tran.TransformStats;
import com.metavize.mvvm.tran.UndeployException;

class RemoteTransformManagerImpl implements TransformManager
{
    private final TransformManagerImpl transformManager;

    RemoteTransformManagerImpl(TransformManagerImpl transformManager)
    {
        this.transformManager = transformManager;
    }

    public List<Tid> transformInstances()
    {
        return transformManager.transformInstances();
    }

    public List<Tid> transformInstances(String name)
    {
        return transformManager.transformInstances(name);
    }

    public List<Tid> transformInstances(Policy policy)
    {
        return transformManager.transformInstances(policy);
    }

    public List<Tid> transformInstancesVisible(Policy policy)
    {
        return transformManager.transformInstancesVisible(policy);
    }

    public List<Tid> transformInstances(String name, Policy policy)
    {
        return transformManager.transformInstances(name, policy);
    }

    public Tid instantiate(String name, Policy policy) throws DeployException
    {
        return transformManager.instantiate(name, policy);
    }

    public Tid instantiate(String name, Policy policy, String[] args)
        throws DeployException
    {
        return transformManager.instantiate(name, policy, args);
    }

    public Tid instantiate(String name, String[] args) throws DeployException
    {
        return transformManager.instantiate(name, args);
    }

    public Tid instantiate(String name) throws DeployException
    {
        return transformManager.instantiate(name);
    }

    public void destroy(Tid tid) throws UndeployException
    {
        transformManager.destroy(tid);
    }

    public TransformContext transformContext(Tid tid)
    {
        return transformManager.transformContext(tid);
    }

    public Map<Tid, TransformStats> allTransformStats()
    {
        return transformManager.allTransformStats();
    }
}