/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.logging;

/**
 * A logging context represents the context in which a log was
 * created. In our system, we associate a logging context with a
 * {@link org.apache.log4j.Logger} on creation based on the
 * <code>UvmLoggingContext</code> associated with the thread.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
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
     * property: <code>uvm.log.dir</code>.
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
