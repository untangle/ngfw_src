/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;


import com.metavize.mvvm.logging.LoggingManager;
import com.metavize.mvvm.security.AdminManager;
import com.metavize.mvvm.security.MvvmLogin;
import com.metavize.mvvm.tapi.MPipeManager;
import com.metavize.mvvm.tapi.PipelineFoundry;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformManager;
import net.sf.hibernate.Session;
import org.apache.log4j.Logger;

/**
 * Provides an interface to get all local MVVM components from an MVVM
 * instance.  This interface is accessible locally.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public interface MvvmLocalContext
{
    public class MvvmState implements Serializable
    {
        private static final long serialVersionUID = -101624240450519097L;

        /**
         * Booted, but not initialized. This is a transient state, just
         * after the world has been instantiated, but before
         * MvvmLocalContext.init() has been called.
         *
         */
        public static final MvvmState LOADED = new MvvmState("loaded");

        /**
         * Initialized, but not running. We've run init() but not yet started
         * the transforms or Tomcat.
         *
         */
        public static final MvvmState INITIALIZED = new MvvmState("initialized");

        /**
         * Running.
         *
         */
        public static final MvvmState RUNNING = new MvvmState("running");

        /**
         * Destroyed, this instance should not be used.
         *
         */
        public static final MvvmState DESTROYED = new MvvmState("destroyed");

        private static final Map INSTANCES = new HashMap();

        static {
            INSTANCES.put(LOADED.toString(), LOADED);
            INSTANCES.put(INITIALIZED.toString(), INITIALIZED);
            INSTANCES.put(RUNNING.toString(), RUNNING);
            INSTANCES.put(DESTROYED.toString(), DESTROYED);
        }

        private String state;

        public static MvvmState getInstance(String state)
        {
            return (MvvmState)INSTANCES.get(state);
        }

        private MvvmState(String state) { this.state = state; }

        public String toString() { return state; }

        // Serialization ----------------------------------------------------------
        Object readResolve()
        {
            return getInstance(state);
        }
    }

    /**
     * Gets the current state of the MVVM
     *
     * @return a <code>MvvmState</code> enumerated value
     */
    MvvmState state();

    /**
     * Get the <code>ToolboxManager</code> singleton.
     *
     * @return a <code>ToolboxManager</code> value
     */
    ToolboxManager toolboxManager();

    /**
     * Get the <code>TransformManager</code> singleton.
     *
     * @return a <code>TransformManager</code> value
     */
    TransformManager transformManager();

    /**
     * Get the <code>LoggingManager</code> singleton.
     *
     * @return a <code>LoggingManager</code> value
     */
    LoggingManager loggingManager();

    /**
     * Get the <code>AdminManager</code> singleton.
     *
     * @return a <code>AdminManager</code> value
     */
    AdminManager adminManager();

    ArgonManager argonManager();

    NetworkingManager networkingManager();

    MailSender mailSender();

    /**
     * Save settings to local hard drive.
     *
     * @exception IOException if the save was unsuccessful.
     */
    void localBackup() throws IOException;

    /**
     * Save settings to USB key drive.
     *
     * @exception IOException if the save was unsuccessful.
     */
    void usbBackup() throws IOException;

    void shutdown();

    // debugging / performance management
    void doFullGC();

    /**
     * Get the <code>MPipeManager</code> singleton.
     *
     * @return a <code>MPipeManager</code> value
     */
    MPipeManager mPipeManager();

    /**
     * The pipeline compiler.
     *
     * @return a <code>PipelineFoundry</code> value
     */
    PipelineFoundry pipelineFoundry();

    /**
     * Returns an <code>mvvmLogin</code> method here.
     *
     * @param isLocal a <code>boolean</code> true if the invoker is on
     * the local host.
     * @return a <code>MvvmLogin</code> value
     */
    MvvmLogin mvvmLogin(boolean isLocal);

    TransformContext transformContext(ClassLoader cl);

    /**
     * Get a new Hibernate <code>Session</code>. This is session is
     * only good for persisting classes loaded by the MVVM's
     * ClassLoader.
     *
     * @return a new Hibernate <code>Session</code>.
     */
    Session openSession();

    Logger eventLogger();
}
