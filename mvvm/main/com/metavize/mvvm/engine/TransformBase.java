/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: TransformBase.java,v 1.13 2005/03/22 03:48:36 amread Exp $
 */

package com.metavize.mvvm.engine;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.argon.SessionMatcher;
import com.metavize.mvvm.argon.SessionMatcherFactory;
import com.metavize.mvvm.argon.VectronTable;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tran.Transform;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformDesc;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.tran.TransformState;
import com.metavize.mvvm.tran.TransformStats;
import com.metavize.mvvm.tran.TransformStopException;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

/**
 * A base class for transform instances, both normal and casing.
 *
 * @author Aaron Read <amread@metavize.com>
 * @version 1.0
 */
public abstract class TransformBase implements Transform
{
    private Logger logger = Logger.getLogger(TransformBase.class);

    private final TransformContext transformContext;
    private final Tid tid;
    private final Set children = Collections.synchronizedSet(new HashSet());

    private TransformBase parent;
    private TransformState runState;
    private TransformStats stats = new TransformStats();

    protected TransformBase()
    {
        transformContext = TransformContextFactory.context();
        tid = transformContext.getTid();

        runState = TransformState.LOADED;
    }

    // abstract methods ------------------------------------------------------

    protected abstract void connectMPipe();
    protected abstract void disconnectMPipe();

    // lifecycle methods -----------------------------------------------------

    public final TransformState getRunState()
    {
        return runState;
    }

    public final void start()
        throws TransformStartException, IllegalStateException
    {
        start(true);
    }

    public final void stop()
        throws TransformStopException, IllegalStateException
    {
        stop(true);
    }

    public void reconfigure() throws TransformException { }

    // public methods --------------------------------------------------------

    public TransformContext getTransformContext()
    {
        return transformContext;
    }

    public Tid getTid()
    {
        return tid;
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

    // protected no-op methods ------------------------------------------------

    /**
     * Called when the transform is new, initial settings should be
     * created and saved in this method.
     */
    protected void initializeSettings() { }

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

    // package protected methods ----------------------------------------------

    void resumeState() throws TransformException
    {
        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery
                ("from TransformDesc td where td.tid = :tid");
            q.setParameter("tid", tid);
            TransformDesc td = (TransformDesc)q.uniqueResult();

            resumeState(td);

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get TransformDesc", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close session", exn);
            }
        }
    }

    void init(String[] args)
        throws TransformException, IllegalStateException
    {
        init(true, args);
    }

    void destroy()
        throws TransformException, IllegalStateException
    {
        destroy(true);
    }

    /**
     * Unloads the transform for MVVM shutdown, does not change
     * transform's target state.
     *
     * XXX it is incorrect to unload a casing if the child is loaded,
     * enforce that here.
     */
    void unload()
    {
        try {
            if (runState == TransformState.LOADED) {
                destroy(false); // XXX
            } else if (runState == TransformState.INITIALIZED) {
                destroy(false);
            } else if (runState == TransformState.RUNNING) {
                stop(false);
                destroy(false);
            }
        } catch (TransformException exn) {
            logger.warn("could not unload", exn);
        }
    }

    void setParent(TransformBase parent)
    {
        this.parent = parent;
        if (null != parent) {
            this.parent.addChild(this);
        }
    }

    // private methods --------------------------------------------------------

    private void addChild(Transform child)
    {
        children.add(child);
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
            Session s = MvvmContextFactory.context().openSession();
            try {
                Transaction tx = s.beginTransaction();

                Query q = s.createQuery
                    ("from TransformDesc td where td.tid = :tid");
                q.setParameter("tid", tid);
                TransformDesc td = (TransformDesc)q.uniqueResult();

                td.setTargetState(ts);
                if (null != args) {
                    td.setArgs(args);
                }

                tx.commit();
            } catch (HibernateException exn) {
                logger.warn("could not get TransformDesc", exn);
            } finally {
                try {
                    s.close();
                } catch (HibernateException exn) {
                    logger.warn("could not close Session", exn);
                }
            }
        }
    }

    // XXX i am worried about races in the lifecycle methods

    private void init(boolean syncState, String[] args)
        throws TransformException, IllegalStateException
    {
        if (TransformState.LOADED != runState) {
            throw new IllegalStateException("Init called in state: "
                                            + runState);
        }

        preInit(args);
        changeState(TransformState.INITIALIZED, syncState, args);

        postInit(args); // XXX if exception, state == ?
    }

    private void start(boolean syncState) throws TransformStartException
    {
        if (TransformState.INITIALIZED != getRunState()) {
            throw new IllegalStateException("Start called in state: "
                                            + getRunState());
        }

        if (syncState && null != parent) {
            Thread ct = Thread.currentThread();
            // Entering TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            ClassLoader oldCl = ct.getContextClassLoader();
            ct.setContextClassLoader(parent.getClass().getClassLoader());

            try {
                parent.parentStart();
            } finally {
                ct.setContextClassLoader(oldCl);
                // Left TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            }
        }

        connectMPipe();

        preStart();

        changeState(TransformState.RUNNING, syncState);
        postStart(); // XXX if exception, state == ?
        logger.info("started transform");
    }

    private void stop(boolean syncState)
        throws TransformStopException, IllegalStateException
    {
        if (TransformState.RUNNING != getRunState()) {
            throw new IllegalStateException("Stop called in state: "
                                            + getRunState());
        }
        preStop();

        disconnectMPipe();
        changeState(TransformState.INITIALIZED, syncState);

        if (syncState && null != parent) {
            Thread ct = Thread.currentThread();
            // Entering TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            ClassLoader oldCl = ct.getContextClassLoader();
            ct.setContextClassLoader(parent.getClass().getClassLoader());

            try {
                parent.parentStop();
            } finally {
                ct.setContextClassLoader(oldCl);
                // Left TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            }
        }

        postStop(); // XXX if exception, state == ?
        logger.info("stopped transform");
    }

    private void destroy(boolean syncState)
        throws TransformException, IllegalStateException
    {
        // XXX this method should only be callable by transformmanager.

        if (TransformState.INITIALIZED != runState
            && TransformState.LOADED != runState) {
            throw new IllegalStateException("Destroy in state: " + runState);
        }
        preDestroy();
        changeState(TransformState.DESTROYED, syncState);

        postDestroy(); // XXX if exception, state == ?
    }

    private void resumeState(TransformDesc td) throws TransformException
    {
        if (TransformState.LOADED == td.getTargetState()) {
            logger.debug("leaving transform in LOADED state");
        } else if (TransformState.INITIALIZED == td.getTargetState()) {
            logger.debug("bringing into INITIALIZED state");
            init(false, td.getArgArray());
        } else if (TransformState.RUNNING == td.getTargetState()) {
            logger.debug("bringing into RUNNING state: " + tid);
            init(false, td.getArgArray());
            start(false);
        } else if (TransformState.DESTROYED == td.getTargetState()) {
            logger.debug("bringing into DESTROYED state: " + tid);
            runState = TransformState.DESTROYED;
        } else {
            logger.warn("unknown state: " + td.getTargetState());
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
            for (Iterator i = children.iterator(); i.hasNext(); ) {
                Transform tran = (Transform)i.next();
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
     * This shutdowns all of the related/matching sessions for this transform.
     * By default, this won't kill any sessions, override sessionMatcher to
     * actually kill sessions
     */
    protected void shutdownMatchingSessions()
    {
        VectronTable.getInstance().shutdownMatches( sessionMatcher());
    }

    protected SessionMatcher sessionMatcher()
    {
        /* By default use the session matcher that doesn't match anything */
        return SessionMatcherFactory.getNullInstance();
    }
}
