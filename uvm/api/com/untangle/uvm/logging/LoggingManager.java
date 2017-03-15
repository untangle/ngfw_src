/**
 * $Id$
 */
package com.untangle.uvm.logging;

import com.untangle.uvm.logging.LogEvent;

/**
 * Manager for Logging.
 */
public interface LoggingManager
{
    /**
     * resetAllLogs re-reads all log configuration files (jboss,
     * uvm, and all node instances). This allows changing logging levels,
     * etc. The old output files will be erased and new files begun.
     */
    void resetAllLogs();

    /**
     * Set the logging context of this thread to the "uvm" configuration
     * log4j log calls after this will go to the uvm.log file
     */
    void setLoggingUvm();

    /**
     * Set the logging context of this thread to the "app" configuration
     * log4j log calls after this will go to the associated node-nodeId.log file
     */
    void setLoggingApp(Long appId);
}
