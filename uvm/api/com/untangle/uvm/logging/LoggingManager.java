/*
 * $Id$
 */
package com.untangle.uvm.logging;

import com.untangle.uvm.logging.LogEvent;

/**
 * Manager for Logging.
 */
public interface LoggingManager
{
    LoggingSettings getLoggingSettings();

    void setLoggingSettings(LoggingSettings settings);

    /**
     * <code>resetAllLogs</code> re-reads all log configuration files (jboss,
     * uvm, and all node instances). This allows changing logging levels,
     * etc. The old output files will be erased and new files begun.
     */
    void resetAllLogs();

    void setLoggingNode(Long nodeId);

    void setLoggingUvm();

    void logError(String errorText);

    void logEvent(LogEvent evt);

    /**
     * Returns true if the schema conversion on startup are complete
     */
    boolean isConversionComplete();
}
