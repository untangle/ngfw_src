/*
 * $Id$
 */
package com.untangle.uvm.app;

import java.util.List;

import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.logging.LogEvent;

/**
 * Interface for a node instance, provides public runtime control
 * methods for manipulating the instance's state.
 */
public interface App
{
    /**
     * Get the node Settings
     * This returns the generic settings that all nodes share
     * getSettings returns the node-specific settings
     */
    AppSettings getAppSettings();

    /**
     * Get the node immutable Properties 
     */
    AppProperties getAppProperties();

    /**
     * Get the current run state of this node
     */
    AppSettings.AppState getRunState();

    /**
     * Connects to PipelineConnector and starts. The node instance reads its
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
     * Stops node and disconnects from the PipelineConnector. A call to
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
     * Retrieve a list of sessions currently being processed by this node
     */
    List<SessionTuple> liveSessions();

    /**
     * Retrieve a list of node sessions currently being processed by this node
     */
    List<AppSession> liveAppSessions();
    
    /**
     * Log an event
     * This is just a convenience method for different parts of the node to log events
     */
    void logEvent(LogEvent evt);

    /**
     * Return statistics tracked for this node (if any)
     */
    List<AppMetric> getMetrics();
}
