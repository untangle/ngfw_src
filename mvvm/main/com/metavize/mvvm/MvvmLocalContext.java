/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: MvvmLocalContext.java,v 1.13 2005/01/30 09:20:32 amread Exp $
 */

package com.metavize.mvvm;

import com.metavize.mvvm.security.MvvmLogin;
import com.metavize.mvvm.tapi.MPipeManager;
import com.metavize.mvvm.tapi.PipelineFoundry;
import com.metavize.mvvm.tran.TransformContext;
import net.sf.hibernate.Session;
import org.apache.log4j.Logger;

/**
 * Provides an interface to get all local MVVM components from an MVVM
 * instance.  This interface is accessible ONLY locally.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public interface MvvmLocalContext extends MvvmContext
{
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
     * Describe <code>mvvmLogin</code> method here.
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
