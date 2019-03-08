/**
* $Id$
 */
package com.untangle.uvm;

import java.util.Set;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;

import com.untangle.uvm.app.App;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.Subscription;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.SessionEventHandler;

/**
 * PipelineConnectorImpl is the implementation of a single PipelineConnector. Status and control of a pipe happen here. Events are handled in Dispatcher instead.
 */
public class PipelineConnectorImpl implements PipelineConnector
{
    /**
     * Active Sessions for this agent
     */
    private Set<AppSession> activeSessions = java.util.Collections.newSetFromMap(new ConcurrentHashMap<AppSession,Boolean>());

    private boolean enabled = true;

    private final Dispatcher dispatcher;
    private final String name;
    private final App app;
    private final Subscription subscription;
    private final SessionEventHandler listener;
    private final Fitting inputFitting;
    private final Fitting outputFitting;
    private final Affinity affinity;
    private final Integer affinityStrength;
    private final boolean premium;

    /**
     * A buddy is another pipelineConnector that this connector must not be adjacent to.
     * If it is, both this one and the buddy will be removed from the pipeline.
     * Also, this pipeline connector will not be inserted into the pipeline if the buddy is not present.
     *
     * This is used for casings
     * For example, the "http-server-side" pipeline connector has a buddy of "http-client-side"
     * This assures two things
     * 1) That http-server-side is only inserted if http-client-side exists somewhere in the pipeline before it
     * 2) That if http-server-side is inserted immediately after http-client-side, then both are removed since there is nothin "in the middle"
     */
    private final String buddy; 
    
    
    protected static final Logger logger = Logger.getLogger( PipelineConnectorImpl.class );
    
    /**
     * PipelineConnectorImpl constructor
     * @param name
     * @param app
     * @param subscription
     * @param listener
     * @param inputFitting
     * @param outputFitting
     * @param affinity
     * @param affinityStrength
     * @param premium
     */
    public PipelineConnectorImpl( String name, App app, Subscription subscription, SessionEventHandler listener, Fitting inputFitting, Fitting outputFitting, Affinity affinity, Integer affinityStrength, boolean premium )
    {
        this( name, app, subscription, listener, inputFitting, outputFitting, affinity, affinityStrength, premium, null );
    }

    /**
     * PipelineConnectorImpl constructor
     * @param name
     * @param app
     * @param subscription
     * @param listener
     * @param inputFitting
     * @param outputFitting
     * @param affinity
     * @param affinityStrength
     * @param premium
     * @param buddy
     */
    public PipelineConnectorImpl( String name, App app, Subscription subscription, SessionEventHandler listener, Fitting inputFitting, Fitting outputFitting, Affinity affinity, Integer affinityStrength, boolean premium, String buddy )
    {
        this.name = name;
        this.app = app;
        this.subscription = subscription;
        this.listener = listener;
        this.inputFitting = inputFitting;
        this.outputFitting = outputFitting;
        this.affinity = affinity;
        this.affinityStrength = affinityStrength;
        this.premium = premium;
        this.buddy = buddy;
        
        dispatcher = new Dispatcher(this);
        if (listener != null)
            dispatcher.setSessionEventHandler( listener );
    }
    
    /**
     * isEnabled
     * @return enabled
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * setEnabled
     * @param newValue
     */
    public void setEnabled(boolean newValue)
    {
        this.enabled = newValue;
    }

    /**
     * getName
     * @return name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * getBuddy
     * @return buddy
     */
    public String getBuddy()
    {
        return this.buddy;
    }

    /**
     * getApp
     * @return app
     */
    public App getApp()
    {
        return this.app;
    }

    /**
     * app (alias for getApp)
     * @return app
     */
    public App app()
    {
        return this.app;
    }

    /**
     * getAffinity
     * @return affinity
     */
    public Affinity getAffinity()
    {
        return this.affinity;
    }

    /**
     * getAffinityStrength
     * @return affinityStrength
     */
    public Integer getAffinityStrength()
    {
        return this.affinityStrength;
    }

    /**
     * getDispatcher
     * @return dispatcher
     */
    public Dispatcher getDispatcher()
    {
        return dispatcher;
    }

    /**
     * isPremium
     * @return premium
     */
    public boolean isPremium()
    {
        return premium;
    }
    
    /**
     * getInputFitting returns the input fitting
     * (the type of input this PipelineConnector takes)
     * This is used to calculate which PipelineConnectors can be connected
     * after this PipelineConnector
     * @return Fitting
     */
    public Fitting getInputFitting()
    {
        return inputFitting;
    }

    /**
     * getOutputFitting returns the output fitting
     * (the type of output this PipelineConnector outputs)
     * This is used to calculate which PipelineConnectors can be connected
     * after this PipelineConnector
     * @return Fitting
     */
    public Fitting getOutputFitting()
    {
        return outputFitting;
    }

    /**
     * get the AppProperties for the app that owns this PipelineConnector
     * @return AppProperties
     */
    public AppProperties appProperties()
    {
        return app().getAppProperties();
    }

    /**
     * Gets a list of the session IDs currently being processed by this PipelineConnector
     * @return list
     */
    public long[] liveSessionIds()
    {
        if (dispatcher == null)
            return new long[0];
        return dispatcher.liveSessionIds();
    }

    /**
     * Gets the AppSessions currently being processed by this PipelineConnector
     * @return list
     */
    public List<AppSession> liveSessions()
    {
        if (dispatcher != null)
            return dispatcher.liveSessions();
        else
            return null;
    }
    
    /**
     * This is called by the App (or AppManager?) to disconnect
     * from a live PipelineConnector. Since it is live we must be sure to shut down the
     * Dispatcher nicely (in comparison to shutdown, below).
     */
    public void destroy()
    {
        try {
            this.dispatcher.killAllSessions();
        } catch (Exception x) {
            logger.info("Exception destroying PipelineConnector", x);
        }

        /* Remove all of the active sessions */
        activeSessions.clear();
    }


    /**
     * Add a session to the map of active sessions.
     * @param session
     * @return True if the session was added, false if the agent is dead, or the session
     *   has already been added.
     */
    public boolean addSession( AppSession session )
    {
        return activeSessions.add( session );
    }

    /**
     * Remove a session from the map of active sessions associated with this netcap agent.
     * @param session
     * @return True if the session was removed, false if the session was not in the list 
     *   of active session.
     */
    public boolean removeSession( AppSession session )
    {
        return activeSessions.remove( session );
    }

    /**
     * returns true if this PipelineConnector is subscribed to this session attributes
     * @param tuple
     * @return true if subscribed, false otherwise
     */
    public boolean matches( com.untangle.uvm.app.SessionTuple tuple )
    {
        if ( !enabled ) {
            return false;
        }
        
        if ( subscription != null && ! subscription.matches( tuple ) ) {
            return false;
        }

        return true;
    }
    
    /**
     * toString
     * @return string
     */
    public String toString()
    {
        return this.name;
    }
}
