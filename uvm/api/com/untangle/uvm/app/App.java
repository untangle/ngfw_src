/**
 * $Id$
 */
package com.untangle.uvm.app;

import java.util.List;

import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.logging.LogEvent;

/**
 * Interface for a app instance, provides public runtime control
 * methods for manipulating the instance's state.
 */
public interface App
{
    /**
     * Get the app Settings
     * This returns the generic settings that all apps share
     * getSettings returns the app-specific settings
     */
    AppSettings getAppSettings();

    /**
     * Get the app immutable Properties 
     */
    AppProperties getAppProperties();

    /**
     * Get the current run state of this app
     */
    AppSettings.AppState getRunState();

    /**
     * Connects to PipelineConnector and starts. The app instance reads its
     * configuration each time this method is called. A call to this method
     * is only valid when the instance is in the
     * {@link AppState#INITIALIZED} state. After successful return,
     * the instance will be in the {@link AppState#RUNNING} state.
     *
     * @exception IllegalStateException if not called in the {@link
     * AppState#INITIALIZED} state.
     */
    void start() throws Exception;

    /**
     * Stops app and disconnects from the PipelineConnector. A call to
     * this method is only valid when the instance is in the {@link
     * AppState#RUNNING} state. After successful return, the
     * instance will be in the {@link AppState#INITIALIZED}
     * state.
     *
     * @exception IllegalStateException if not called in the {@link
     * AppState#RUNNING} state.
     */
    void stop() throws Exception;

    /**
     * Retrieve a list of sessions currently being processed by this app
     */
    List<SessionTuple> liveSessions();

    /**
     * Retrieve a list of app sessions currently being processed by this app
     */
    List<AppSession> liveAppSessions();
    
    /**
     * Log an event
     * This is just a convenience method for different parts of the app to log events
     */
    void logEvent(LogEvent evt);

    /**
     * Return statistics tracked for this app (if any)
     */
    List<AppMetric> getMetrics();

    /**
     * Returns the validity of the license for this app
     * Override to implement custom license logic
     * @return True if license is valid, false otherwise
     */
    boolean isLicenseValid();
}
