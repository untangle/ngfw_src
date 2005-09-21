/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.logging;


import com.metavize.mvvm.security.Tid;



/**
 * Manager for the Logging, which holds Mackages. A Mackage is all
 * data concerning a Transform that is not related to any particular
 * transform instance.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public interface LoggingManager
{
    /**
     * <code>resetAllLogs</code> re-reads all log configuration files (jboss,
     * mvvm, and all transform instances). This allows changing logging levels,
     * etc. The old output files will be erased and new files begun.
     *
     */
    void resetAllLogs();

    // XXX a version that limits to n last messages
    LogEvent[] userLogs(Tid tid);
    String[] userLogStrings(Tid tid);
}
