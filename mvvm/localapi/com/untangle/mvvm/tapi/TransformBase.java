 /*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.tapi;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.localapi.SessionMatcher;
import com.untangle.mvvm.localapi.SessionMatcherFactory;
import com.untangle.mvvm.policy.Policy;
import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tapi.MPipe;
import com.untangle.mvvm.tran.LocalTransformManager;
import com.untangle.mvvm.tran.Transform;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.tran.TransformDesc;
import com.untangle.mvvm.tran.TransformException;
import com.untangle.mvvm.tran.TransformManager;
import com.untangle.mvvm.tran.TransformStartException;
import com.untangle.mvvm.tran.TransformState;
import com.untangle.mvvm.tran.TransformStats;
import com.untangle.mvvm.tran.TransformStopException;
import com.untangle.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * A base class for transform instances, both normal and casing.
 *
 * @author Aaron Read <amread@untangle.com>
 * @version 1.0
 */
public abstract class TransformBase implements Transform
{
    private final Logger logger = Logger.getLogger(TransformBase.class);

    private final TransformContext transformContext;
    private final Tid tid;
    private final Set<TransformBase> parents = new HashSet<TransformBase>();
    private final Set<Transform> children = new HashSet<Transform>();
    private final LocalTransformManager transformManager;
    private final List<TransformListener> transformListeners
        = new LinkedList<TransformListener>();

    private final Object stateChangeLock = new Object();

    private TransformState runState;
    private boolean wasStarted = false;
    private TransformStats stats = new TransformStats();

    protected TransformBase()
    {
        transformManager = MvvmContextFactory.context().transformManager();
        transformContext = transformManager.threadContext();
        tid = transformContext.getTid();

        runState = TransformState.LOADED;
    }

    // abstract methods --------------------------------------------------------

    protected abstract void connectMPipe();
    protected abstract void disconnectMPipe();

    // Transform methods -------------------------------------------------------

    public final TransformState getRunState()
    {
        return runState;
    }

    public final boolean neverStarted()
    {
        if (wasStarted || TransformState.RUNNING == runState) {
            return false;
        } else {
            TransactionWork<Long> tw = new TransactionWork<Long>()
                {
                    Long result;

                    // XXX move this shit to TransformContext
                    public boolean doWork(Session s)
                    {
                        Query q = s.createQuery("SELECT count(sc) FROM TransformStateChange sc WHERE sc.tid = :tid AND sc.state = 'running'");
                        q.setParameter("tid", tid);
                        result = (Long)q.uniqueResult();

                        return true;
                    }

                    public Long getResult()
                    {
                        return result;
                    }
                };
            MvvmContextFactory.context().runTransaction(tw);

            return 0 == tw.getResult();
        }
    }

    public final void start()
        throws TransformStartException, IllegalStateException
    {
        synchronized (stateChangeLock) {
            start(true);
        }
    }

    public final void stop()
        throws TransformStopException, IllegalStateException
    {
        synchronized (stateChangeLock) {
            stop(true);
        }
    }

    public TransformContext getTransformContext()
    {
        return transformContext;
    }

    public Tid getTid()
    {
        return tid;
    }

    public Policy getPolicy()
    {
        return tid.getPolicy();
    }

    public TransformDesc getTransformDesc()
    {
        return transformContext.getTransformDesc();
    }

    public TransformStats getStats() throws IllegalStateException
    {
        if (TransformState.RUNNING != getRunState()) {
            throw new IllegalStateException("Stats called in state: "
                                            + getRunState());
        }
        return stats;
    }

    // TransformBase methods ---------------------------------------------------

    public void addTransformListener(TransformListener tl) {
        synchronized (transformListeners) {
            transformListeners.add(tl);
        }
    }

    public void removeTransformListener(TransformListener tl) {
        synchronized (transformListeners) {
            transformListeners.remove(tl);
        }
    }

    public void addParent(TransformBase parent)
    {
        parents.add(parent);
        parent.addChild(this);
    }

    /**
     * Called when the transform is new, initial settings should be
     * created and saved in this method.
     */
    public void initializeSettings() { }

    public void init(String[] args)
        throws TransformException, IllegalStateException
    {
        synchronized (stateChangeLock) {
            init(true, args);
        }
    }

    public void disable()
        throws TransformException, IllegalStateException
    {
        if (TransformState.LOADED == runState
            || TransformState.DESTROYED == runState) {
            throw new IllegalStateException("disabling in: " + runState);
        } else if (TransformState.RUNNING == runState) {
            stop(false);
        }
        changeState(TransformState.DISABLED, true);
    }

    public void resumeState(TransformState ts, String[] args)
        throws TransformException
    {
        if (TransformState.LOADED == ts) {
            logger.debug("leaving transform in LOADED state");
        } else if (TransformState.INITIALIZED == ts) {
            logger.debug("bringing into INITIALIZED state");
            init(false, args);
        } else if (TransformState.RUNNING == ts) {
            logger.debug("bringing into RUNNING state: " + tid);
            init(false, args);
            start(false);
        } else if (TransformState.DESTROYED == ts) {
            logger.debug("bringing into DESTROYED state: " + tid);
            runState = TransformState.DESTROYED;
        } else if (TransformState.DISABLED == ts) {
            logger.debug("bringing into DISABLED state: " + tid);
            init(false, args);
            runState = TransformState.DISABLED;
        } else {
            logger.warn("unknown state: " + ts);
        }
    }

    public void destroy()
        throws TransformException, IllegalStateException
    {
        uninstall();

        synchronized (stateChangeLock) {
            destroy(true);
        }
    }

    /**
     * Unloads the transform for MVVM shutdown, does not change
     * transform's target state.
     *
     * XXX it is incorrect to unload a casing if the child is loaded,
     * enforce that here.
     */
    public void unload()
    {
        try {
            if (runState == TransformState.LOADED) {
                destroy(false); // XXX
            } else if (runState == TransformState.INITIALIZED) {
                destroy(false);
            } else if (runState == TransformState.RUNNING) {
                stop(false);
                destroy(false);
            } else if (runState == TransformState.DISABLED) {
                destroy(false);
            }
        } catch (TransformException exn) {
            logger.warn("could not unload", exn);
        }
    }

    public void enable()
        throws TransformException, IllegalStateException
    {
        if (TransformState.LOADED == runState
            || TransformState.DESTROYED == runState) {
                throw new IllegalStateException("enabling in: " + runState);
        } else if (TransformState.RUNNING == runState
           || TransformState.INITIALIZED == runState) {
        // We're already fine.
    } else {
        // DISABLED
            changeState(TransformState.INITIALIZED, true);
        }
    }

    // protected no-op methods -------------------------------------------------

    /**
     * Called when the transform is being uninstalled, rather than
     * just being taken down with the MVVM.
     */
    protected void uninstall() { }

    /**
     * Called as the instance is created, but is not configured.
     *
     * @param args[] the transform-specific arguments.
     */
    protected void preInit(String args[]) throws TransformException { }

    /**
     * Same as <code>preInit</code>, except now officially in the
     * {@link TransformState#INITIALIZED} state.
     *
     * @param args[] the transform-specific arguments.
     */
    protected void postInit(String args[]) throws TransformException { }

    /**
     * Called just after connecting to MPipe, but before starting.
     *
     */
    protected void preStart() throws TransformStartException
    { }

    /**
     * Called just after starting MPipe and making subscriptions.
     *
     */
    protected void postStart() throws TransformStartException
    { }

    /**
     * Called just before stopping MPipe and disconnecting.
     *
     */
    protected void preStop() throws TransformStopException { }

    /**
     * Called after stopping MPipe and disconnecting.
     *
     */
    protected void postStop() throws TransformStopException { }

    /**
     * Called just before this instance becomes invalid.
     *
     */
    protected void preDestroy() throws TransformException { }

    /**
     * Same as <code>postDestroy</code>, except now officially in the
     * {@link TransformState#DESTROYED} state.
     *
     * @param args[] a <code>String</code> value
     */
    protected void postDestroy() throws TransformException { }

    // private methods ---------------------------------------------------------

    private void addChild(Transform child)
    {
        children.add(child);
    }

    private boolean removeChild(Transform child)
    {
        return children.remove(child);
    }

    private void changeState(TransformState ts, boolean syncState)
    {
        changeState(ts, syncState, null);
    }

    private void changeState(TransformState ts, boolean syncState,
                             String[] args)
    {
        runState = ts;

        if (syncState) {
            if (TransformState.RUNNING == ts) {
                wasStarted = true;
            }

            TransformStateChangeEvent te = new TransformStateChangeEvent(this, ts, args);
            synchronized (transformListeners) {
                for (TransformListener tl : transformListeners) {
                    tl.stateChange(te);
                }
            }
        }
    }

    // XXX i am worried about races in the lifecycle methods

    private void init(boolean syncState, String[] args)
        throws TransformException, IllegalStateException
    {
        if (TransformState.LOADED != runState) {
            throw new IllegalStateException("Init called in state: " + runState);
        }

        try {
            transformManager.registerThreadContext(transformContext);
            preInit(args);
            changeState(TransformState.INITIALIZED, syncState, args);

            postInit(args); // XXX if exception, state == ?
        } finally {
            transformManager.deregisterThreadContext();
        }
    }

    private void start(boolean syncState) throws TransformStartException
    {
        if (TransformState.INITIALIZED != getRunState()) {
            throw new IllegalStateException("Start called in state: "
                                            + getRunState());
        }

        for (TransformBase parent : parents) {
            if (TransformState.INITIALIZED == parent.getRunState()) {
                ClassLoader parentCl = parent.getTransformContext()
                    .getClassLoader();

                Thread ct = Thread.currentThread();
                ClassLoader oldCl = ct.getContextClassLoader();
                // Entering TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                ct.setContextClassLoader(parentCl);
                try {
                    TransformContext pCtx = parent.getTransformContext();
                    transformManager.registerThreadContext(pCtx);
                    parent.parentStart();
                } finally {
                    transformManager.deregisterThreadContext();
                    ct.setContextClassLoader(oldCl);
                    // Left TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                }
            }
        }

        try {
            transformManager.registerThreadContext(transformContext);
            preStart();

            connectMPipe();

            changeState(TransformState.RUNNING, syncState);
            postStart(); // XXX if exception, state == ?
        } finally {
            transformManager.deregisterThreadContext();
        }
        logger.info("started transform");
    }

    private void stop(boolean syncState)
        throws TransformStopException, IllegalStateException
    {
        if (TransformState.RUNNING != getRunState()) {
            throw new IllegalStateException("Stop called in state: "
                                            + getRunState());
        }

        try {
            transformManager.registerThreadContext(transformContext);
            preStop();
            disconnectMPipe();
            changeState(TransformState.INITIALIZED, syncState);
        } finally {
            transformManager.deregisterThreadContext();
        }

        for (TransformBase parent : parents) {
            if (TransformState.RUNNING == parent.getRunState()) {
                ClassLoader parentCl = parent.getTransformContext()
                    .getClassLoader();

                Thread ct = Thread.currentThread();
                ClassLoader oldCl = ct.getContextClassLoader();
                // Entering TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                ct.setContextClassLoader(parentCl);

                try {
                    TransformContext pCtx = parent.getTransformContext();
                    transformManager.registerThreadContext(pCtx);
                    parent.parentStop();
                } finally {
                    transformManager.deregisterThreadContext();
                    ct.setContextClassLoader(oldCl);
                    // Left TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                }
            }
        }

        try {
            transformManager.registerThreadContext(transformContext);
            postStop(); // XXX if exception, state == ?
        } finally {
            transformManager.deregisterThreadContext();
        }
        logger.info("stopped transform");
    }

    private void destroy(boolean syncState)
        throws TransformException, IllegalStateException
    {
        if (TransformState.INITIALIZED != runState
            && TransformState.LOADED != runState
            && TransformState.DISABLED != runState) {
            throw new IllegalStateException("Destroy in state: " + runState);
        }

        try {
            transformManager.registerThreadContext(transformContext);
            preDestroy();
            for (TransformBase p : parents) {
                p.removeChild(this);
            }
            parents.clear();
            changeState(TransformState.DESTROYED, syncState);

            postDestroy(); // XXX if exception, state == ?
        } finally {
            transformManager.deregisterThreadContext();
        }
    }

    private void parentStart() throws TransformStartException
    {
        if (TransformState.INITIALIZED == getRunState()) {
            start();
        }
    }
    private void parentStop()
        throws TransformStopException, IllegalStateException
    {
        boolean childrenStopped = true;

        if (TransformState.RUNNING == getRunState()) {
            for (Transform tran : children) {
                if (TransformState.RUNNING == tran.getRunState()) {
                    childrenStopped = false;
                    break;
                }
            }
        } else {
            childrenStopped = false;
        }

        if (childrenStopped) {
            stop();
        }
    }

    /**
     * This shutdowns all of the related/matching sessions for this
     * transform.  By default, this won't kill any sessions, override
     * sessionMatcher to actually kill sessions
     */
    protected void shutdownMatchingSessions()
    {
        MvvmContextFactory.context().argonManager()
            .shutdownMatches(sessionMatcher());
    }

    protected SessionMatcher sessionMatcher()
    {
        /* By default use the session matcher that doesn't match anything */
        return SessionMatcherFactory.getNullInstance();
    }

    public long incrementCount(int i)
    {
        return incrementCount(i, 1);
    }

    public long incrementCount(int i, long delta)
    {
        return stats.incrementCount(i, delta);
    }
}
