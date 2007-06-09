/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: UvmRepositorySelector.java 8515 2007-01-03 00:13:24Z amread $
 */

package com.untangle.uvm.logging;

/**
 * A logging context represents the context in which a log was
 * created. In our system, we associate a logging context with a
 * {@link org.apache.log4j.Logger} on creation based on the
 * <code>UvmLoggingContext</code> associated with the thread.
 *
 * @author <a href="mailto:amread@nyx.net">Aaron Read</a>
 * @version 1.0
 */
public interface UvmLoggingContext
{
    /**
     * Name of the log4j configuration file for this context. The
     * configuration file should be in the classpath and the
     * configName should be suitable for {@link
     * ClassLoader.getResource()}.
     *
     * @return configuration file name.
     */
    String getConfigName();

    /**
     * The name of the log file for this logging context. This file
     * will be created in the directory specified in the system
     * property: <code>bunnicula.log.dir</code>.
     *
     * @return log filename.
     */
    String getFileName();

    /**
     * Descriptive name of this logging context.
     *
     * @return context name.
     */
    String getName();
}
