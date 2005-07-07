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

package com.metavize.mvvm.tapi;

import java.net.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.metavize.mvvm.tapi.event.SessionEventListener;


/**
 * Service-provider & manager class for MetaPipes.
 *
 * <p>A <code>MPipeManager</code> is a concrete subclass of this class
 * that has a zero-argument constructor and implements the abstract
 * methods herein.  A given Meta Node virtual machine maintains a single
 * system-wide default manager instance, which is returned by the {@link
 * #manager manager} method.  The first invocation of that method will locate
 * and cache the default provider as specified below.
 * @author <a href="mailto:jdi@SLAB"></a>
 * @version 1.0
 */
public abstract class MPipeManager {

    // public static final int DEFAULT_XENON_PORT = 1050;

    // We have to use our own lock since 'this' might be used for something
    // else by our concrete subclass.
    private static final Object lock = new Object();

    private static MPipeManager manager = null;

    /**
     * Initializes a new instance of this class.  </p>
     *
     * @throws  SecurityException
     *          If a security manager has been installed and it denies
     *          {@link RuntimePermission}<tt>("mpipelineManager")</tt>
     */
    protected MPipeManager() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(new RuntimePermission("mpipelineManager"));
        // Nothing much to be done here.
    }


    /**
     * Returns the system-wide default meta pipeline manager for this Meta Node
     * virtual machine.
     *
     * <p> The first invocation of this method finds the default provider
     * by: </p>
     *
     * <ol>
     *
     *   Need some configuration file/param means here.
     *
     *   <li><p> Finally, if no provider has been specified by any of the above
     *   means then the system-default xenon manager class is instantiated and the
     *   result is returned.  </p></li>
     *
     * </ol>
     *
     * <p> Subsequent invocations of this method return the manager that was
     * returned by the first invocation.  </p>
     *
     * @return  The system-wide default MetaPipeline manager
     */
    public static MPipeManager manager() {
        synchronized (lock) {
            if (manager != null)
                return manager;
            return (MPipeManager)AccessController
                .doPrivileged(new PrivilegedAction() {
                        public Object run() {
                            // Put config loading here. XX
                            manager = com.metavize.mvvm.tapi.impl.DefaultMPipeManager.create();
                            return manager;
                        }
                    });
        }
    }


    /**
     * The <code>plumbLocal</code> activates a new section of MetaPipe
     * for the given transform.  No attempt is made to limit tranforms
     * to only one active MPipe at this level (e.g. casings have two).
     *
     * Remote doesn't exist yet. XX
     */
    public abstract MPipe plumbLocal(PipeSpec pipeSpec,
                                     SessionEventListener listener);

    /**
     * The <code>connectLocal</code> method connects the MetaSmith to a XENON
     * on the local machine running on the given port.
     * No remote XENON may be contacted in this way.
     *
     */
    // public abstract Xenon connectLocal(int port);

    /**
     * The <code>connect</code> method connects the MetaSmith to the
     * XENON running on the given machine on the default port.
     */
    // public abstract Xenon connect(InetAddress xenonAddress);

    /**
     * The <code>connect</code> method connects the MetaSmith to the
     * XENON running on the given machine on the given port.
     */
    // public abstract Xenon connect(InetAddress xenonAddress, int port);

    /**
     * Returns all live <code>MPipe</code>s managed by this manager.  There will be one for each
     * transform that is in the RUNNING state.
     *
     * @return a <code>MPipe[]</code> value
     */
    // public abstract MPipe[] mpipes();

    /*
     * MVVM Context calls in here when restarting the whole mvvm, after destroying all
     * the transforms.  We just make sure there are no leftover MPipes.
     *
     */
    public void destroy() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(new RuntimePermission("MPipelineManager"));
        manager = null;
    }
}
