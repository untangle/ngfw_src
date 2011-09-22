/*
 * $Id$
 */
package com.untangle.uvm.logging;


/**
 * Manager for Logging.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
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

    void logError(String errorText);

    /**
     * Force currently queued hibernate events to be flushed to the DB
     */
    void forceFlush();
    
}
