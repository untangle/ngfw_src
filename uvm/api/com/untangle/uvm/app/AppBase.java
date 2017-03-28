/*
 * $Id$
 */
package com.untangle.uvm.app;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.MetricManager;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.app.AppSettings.AppState;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.logging.LogEvent;

/**
 * A base class for app instances, both normal and casing.
 *
 */
public abstract class AppBase implements App
{
    private static final Logger staticLogger = Logger.getLogger(AppBase.class);
    private        final Logger logger       = Logger.getLogger(AppBase.class);

    /**
     * These are the (generic) settings for this app
     * The app usually stores more app-specific settings in "settings"
     * This holds the generic AppSettings that all apps have.
     */
    private AppSettings appSettings;

    /**
     * These are the properties for this app
     */
    private AppProperties appProperties;

    /**
     * This stores a set of parents of this app
     * Parents are any apps that this app depends on to operate properly
     */
    private Set<AppBase> parents = new HashSet<AppBase>();

    /**
     * This stores a set of children to this app
     * Children are any apps that depend on this app to operate properly
     */
    private Set<App> children = new HashSet<App>();

    /**
     * These store this app's metrics (for display in the UI)
     * The hash map is for fast lookups
     * The list is to maintain order for the UI
     */
    private Map<String, AppMetric> metrics = new ConcurrentHashMap<String, AppMetric>();
    private List<AppMetric> metricList = new ArrayList<AppMetric>();
        
    private AppSettings.AppState currentState;

    protected AppBase( )
    {
        currentState = AppState.LOADED;
    }

    protected AppBase( AppSettings appSettings, AppProperties appProperties )
    {
        this.appSettings = appSettings;
        this.appProperties = appProperties;

        currentState = AppState.LOADED;
    }

    protected abstract PipelineConnector[] getConnectors();

    protected void connectPipelineConnectors()
    {
        if ( getConnectors() != null ) {
            for ( PipelineConnector connector : getConnectors() ) {
                UvmContextFactory.context().pipelineFoundry().registerPipelineConnector( connector );
            }
        }
    }

    protected void disconnectPipelineConnectors()
    {
        if ( getConnectors() != null ) {
            for ( PipelineConnector connector : getConnectors() ) {
                UvmContextFactory.context().pipelineFoundry().deregisterPipelineConnector( connector );
                connector.destroy();
            }
        }
    }
    
    public final AppState getRunState()
    {
        return currentState;
    }

    public final void init()
    {
        init(true);
    }
    
    public final void start() 
    {
        start(true);
    }

    public final void stop() 
    {
        stop(true);
    }

    public AppSettings getAppSettings()
    {
        return appSettings;
    }

    public void setAppSettings( AppSettings appSettings )
    {
        this.appSettings = appSettings;
    }

    public AppProperties getAppProperties()
    {
        return appProperties;
    }

    public void setAppProperties( AppProperties appProperties )
    {
        this.appProperties = appProperties;
    }

    public void addParent( AppBase parent )
    {
        parents.add(parent);
        parent.addChild(this);
    }

    /**
     * Called when the app is new, initial settings should be
     * created and saved in this method.
     */
    public void initializeSettings() { }

    public void resumeState( AppState appState ) 
    {
        switch ( appState ) {
        case LOADED:
            logger.debug("leaving app in LOADED state");
            break;
        case INITIALIZED:
            logger.debug("bringing into INITIALIZED state");
            init(false);
            break;
        case RUNNING:
            logger.debug("bringing into RUNNING state: " + appSettings);
            init(false);
            start(false);
            break;
        case DESTROYED:
            logger.debug("bringing into DESTROYED state: " + appSettings);
            currentState = AppState.DESTROYED;
            break;
        default:
            logger.warn("unknown state: " + appState);
        }
    }

    public void destroy()
    {
        uninstall();

        destroy(true);
    }

    public void stopIfRunning()
    {
        UvmContextFactory.context().loggingManager().setLoggingApp(appSettings.getId());

        switch ( currentState ) {
        case RUNNING:
            stop(false);
            break;
        case LOADED:
            break;
        case INITIALIZED:
            break;
        default:
            break;
        }

        UvmContextFactory.context().loggingManager().setLoggingUvm();
    }

    public void enable()
    {
        switch ( currentState ) {
        case LOADED:
            logger.warn("enabling in: " + currentState);
            break;
        case DESTROYED:
            logger.warn("enabling in: " + currentState);
            break;
        case RUNNING:
            break; /* do nothing */
        case INITIALIZED:
            break; /* do nothing */
        default:
            changeState(AppState.INITIALIZED, true);
        }            
    }

    public void logEvent( LogEvent evt )
    {
        String tag = appProperties.getDisplayName().replaceAll("\\s+","_") + " [" + appSettings.getId() + "]:";
        evt.setTag(tag);
        
        UvmContextFactory.context().logEvent(evt);
    }

    @SuppressWarnings("rawtypes")
    public static final App loadClass( AppProperties appProperties, AppSettings appSettings, boolean isNew ) throws Exception
    {
        if ( appProperties == null || appSettings == null )
            throw new Exception("Invalid Arguments: null");

        try {
            AppBase app;

            Set<App> parentApps = new HashSet<App>();
            if (appProperties.getParents() != null) {
                for (String parent : appProperties.getParents()) {
                    parentApps.add(startParent(parent, appSettings.getPolicyId()));
                }
            }

            UvmContextFactory.context().loggingManager().setLoggingApp(appSettings.getId());

            String appSettingsName = appSettings.getAppName();
            staticLogger.debug("setting app " + appSettingsName + " log4j repository");

            String className = appProperties.getClassName();
            java.lang.reflect.Constructor constructor = Class.forName(className).getConstructor(new Class<?>[]{AppSettings.class, AppProperties.class});
            app = (AppBase)constructor.newInstance( appSettings, appProperties );

            app.setAppProperties( appProperties );
            app.setAppSettings( appSettings );
                
            for (App parentApp : parentApps) {
                app.addParent((AppBase)parentApp);
            }

            if (isNew) {
                app.initializeSettings( );
                app.init();
            } else {
                try {
                    app.resumeState(appSettings.getTargetState());
                }
                catch (Exception exn) {
                    staticLogger.error("Exception during app resumeState", exn);
                    if ( exn.getCause() != null )
                        staticLogger.error("Cause", exn.getCause() );
                    // still return the initialized app
                }
            }
            
            return app;

        } catch (Exception exn) {
            staticLogger.error("Exception during app initialization", exn);
            if ( exn.getCause() != null )
                staticLogger.error("Cause", exn.getCause() );
            throw exn;
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    public final void destroyClass() throws Exception
    {
        try {
            UvmContextFactory.context().loggingManager().setLoggingApp(appSettings.getId());
            if (this.getRunState() == AppSettings.AppState.RUNNING) {
                this.stop();
            }
            this.destroy();
        } catch (Exception exn) {
            throw new Exception(exn);
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    public List<SessionTuple> liveSessions()
    {
        List<SessionTuple> sessions = new LinkedList<SessionTuple>();

        for (AppSession sess : liveAppSessions()) {
            SessionTuple tuple = new SessionTuple( sess.getProtocol(),
                                                   sess.getClientAddr(), sess.getServerAddr(),
                                                   sess.getClientPort(), sess.getServerPort() );
            sessions.add( tuple );
        }

        return sessions;
    }

    public List<AppSession> liveAppSessions()
    {
        List<AppSession> sessions = new LinkedList<AppSession>();

        if ( getConnectors() != null ) {
            for ( PipelineConnector connector : getConnectors() ) {
                for ( AppSession sess : connector.liveSessions() ) {
                    /* create a new sessiontupleimpl so the list will be serialized properly */
                    sessions.add( sess );
                }
            }
        }

        return sessions;
    }

    public List<AppMetric> getMetrics()
    {
        return metricList;
    }

    public AppMetric getMetric( String name )
    {
        AppMetric metric = metrics.get( name );
        if ( metric == null )
            logger.warn("Metric not found: " + name);
        return metric;
    }
    
    public void decrementMetric( String name )
    {
        adjustMetric( name, -1L );
    }

    public void incrementMetric( String name )
    {
        adjustMetric( name, 1L );
    }

    public void setMetric( String name, Long newValue )
    {
        if ( name == null ) {
            logger.warn( "Invalid stat: " + name );
            return;
        }
        
        AppMetric metric = metrics.get(name);
        if (metric == null) {
            logger.warn("AppMetric not found: " + name);
            return;
        }
        metric.setValue( newValue );
    }

    public void adjustMetric( String name, Long adjustmentValue )
    {
        if ( name == null ) {
            logger.warn( "Invalid stat: " + name );
            return;
        }
        
        AppMetric metric = metrics.get(name);
        if (metric == null) {
            logger.warn("AppMetric not found: " + name);
            return;
        }

        Long value = metric.getValue();
        if (value == null)
            value = 0L;
        value = value + adjustmentValue;
        metric.setValue( value );
    }

    public void addMetric( AppMetric metric )
    {
        if (metrics.get(metric.getName()) != null) {
            //logger.warn("addMetric(): Metric already exists: \"" + metric.getName() + "\" - ignoring");
            return;
        }
        this.metrics.put( metric.getName(), metric );
        this.metricList.add( metric );
    }

    public void removeMetric( AppMetric metric )
    {
        if ( metric == null ) {
            logger.warn("Invalid argument: null");
            return;
        }
        if (metrics.get(metric.getName()) == null) {
            logger.warn("Invalid argument: metric not found");
            return;
        }        

        this.metrics.remove( metric.getName() );
        this.metricList.remove( metric );
    }
    
    public String toString()
    {
        return "App[" + getAppSettings().getId() + "," + getAppSettings().getAppName() + "]";
    }
    
    // protected methods -------------------------------------------------

    /**
     * Called when the app is being uninstalled, rather than
     * just being taken down with the UVM.
     */
    protected void uninstall() { }

    /**
     * Called before initialization
     *
     * initialization occurs when an app is instantiated or on startup
     */
    protected void preInit() { } 

    /**
     * Called after initialization
     *
     * initialization occurs when an app is instantiated or on startup
     */
    protected void postInit() { } 

    /**
     * Called just after connecting to PipelineConnector, but before starting.
     *
     * isPermanentTransition is true if this is the permanent (saved)
     * This can be used to determine if this app is being started permanently
     */
    protected void preStart( boolean isPermanentTransition ) { } 

    /**
     * Called just after starting PipelineConnector and making subscriptions.
     *
     * isPermanentTransition is true if this is the permanent (saved)
     * This can be used to determine if this app is being started permanently
     */
    protected void postStart( boolean isPermanentTransition ) { } 

    /**
     * Called just before stopping PipelineConnector and disconnecting.
     *
     * isPermanentTransition is true if this is the permanent (saved)
     * This can be used to determine if this app is being stopped permanently
     */
    protected void preStop( boolean isPermanentTransition ) { } 

    /**
     * Called after stopping PipelineConnector and disconnecting.
     *
     * isPermanentTransition is true if this is the permanent (saved)
     * This can be used to determine if this app is being stopped permanently
     */
    protected void postStop( boolean isPermanentTransition ) { }

    /**
     * Called just before this instance becomes invalid.
     *
     */
    protected void preDestroy()  { }

    /**
     * Same as <code>postDestroy</code>, except now officially in the
     * {@link AppState#DESTROYED} state.
     */
    protected void postDestroy() { }

    /**
     * This kills/resets all of the matching sessions (runs against all sessions globally)
     */
    protected void killMatchingSessionsGlobal( SessionMatcher matcher )
    {
        if (matcher == null)
            return;

        UvmContextFactory.context().netcapManager().shutdownMatches( matcher );
    }

    /**
     * This kills/resets all of the matching sessions for this app's sessions
     * This includes "released" sessions that we processed previously by one of this app's pipespecs
     */
    protected void killMatchingSessions( SessionMatcher matcher )
    {
        logger.info("killMatchingSessions()");
        if ( matcher == null )
            return;
        if ( getConnectors() == null )
            return;
        
        for ( PipelineConnector connector : getConnectors() )
            UvmContextFactory.context().netcapManager().shutdownMatches( matcher, connector );
    }
    
    /**
     * This kills all this app's sessions (for all its pipespecs)
     */
    public void killAllSessions()
    {
        killMatchingSessions(new SessionMatcher() {
                public boolean isMatch( Integer policyId, short protocol, int clientIntf, int serverIntf, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort, Map<String,Object> attachments ) { return true; }
            });
    }

    private void addChild( App child )
    {
        children.add(child);
    }

    private boolean removeChild( App child )
    {
        return children.remove(child);
    }

    private void changeState( AppState appState, boolean saveNewTargetState )
    {
        if ( saveNewTargetState ) {
            UvmContextFactory.context().appManager().saveTargetState( this, appState );

            UvmContextFactory.context().pipelineFoundry().clearCache();
        }

        this.currentState = appState;
    }

    private void init( boolean saveNewTargetState ) 
    {
        if ( currentState != AppState.LOADED ) {
            logger.warn("Init called in state: " + currentState);
            return;
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingApp( this.appSettings.getId()) ;

            // if no valid license exists, request a trial license
            try {
                if ( ! UvmContextFactory.context().licenseManager().isLicenseValid( appProperties.getName() ) ) {
                    logger.info("No valid license for: " + appProperties.getName());
                    logger.info("Requesting trial for: " + appProperties.getName());
                    UvmContextFactory.context().licenseManager().requestTrialLicense( appProperties.getName() );
                }
            } catch (Exception e) {
                logger.warn( "Exception fetching trial license. Ignoring...", e );
            }
            
            preInit();
            changeState( AppState.INITIALIZED, saveNewTargetState );
            postInit();

        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    private void start( boolean saveNewTargetState ) 
    {
        if (AppState.INITIALIZED != getRunState()) {
            logger.warn("Start called in state: " + getRunState());
            return;
        }

        for (AppBase parent : parents) {
            if (AppState.INITIALIZED == parent.getRunState()) {
                try {
                    UvmContextFactory.context().loggingManager().setLoggingApp( parent.getAppSettings().getId() );
                    if (parent.getRunState() == AppState.INITIALIZED) 
                        parent.start( false );
                } finally {
                    UvmContextFactory.context().loggingManager().setLoggingApp( appSettings.getId() );
                }
            }
        }

        // save new settings first (if this fails, the state should not be changed)
        try {
            changeState(AppState.RUNNING, saveNewTargetState);
        } catch (Exception e) {
            logger.warn("Failed to start app",e);
            return;
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingApp( this.appSettings.getId() );
            logger.info("Starting   " + this.getAppProperties().getName() + " [" + this.getAppSettings().getId() + "]" + " ...");

            try {
                preStart( saveNewTargetState );
            } catch (Exception e) {
                logger.warn("Exception in preStart(). Reverting to INITIALIZED state.", e);
                changeState(AppState.INITIALIZED, saveNewTargetState);
                throw e;
            }

            connectPipelineConnectors();

            try {
                postStart( saveNewTargetState ); 
            } catch (Exception e) {
                logger.warn("Exception in postStart().", e);
            }

            logger.info("Started    " + this.getAppProperties().getName() + " [" + this.getAppSettings().getId() + "]" + " ...");
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    private void stop( boolean saveNewTargetState ) 
    {
        if (AppState.RUNNING != getRunState()) {
            logger.warn("Stop called in state: " + getRunState());
            return;
        }

        // save new settings first (if this fails, the state should not be changed)
        try {
            changeState(AppState.INITIALIZED, saveNewTargetState);
        } catch (Exception e) {
            logger.warn("Failed to stop app",e);
            return;
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingApp( this.appSettings.getId() );
            logger.info("Stopping   " + this.getAppProperties().getName() + " [" + this.getAppSettings().getId() + "]" + " ...");
            preStop( saveNewTargetState );
            disconnectPipelineConnectors();
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }

        for (AppBase parent : parents) {
            if (AppState.RUNNING == parent.getRunState()) {
                try {
                    UvmContextFactory.context().loggingManager().setLoggingApp( parent.getAppSettings().getId() );
                    parent.stopIfNotRequiredByChildren();
                } finally {
                    UvmContextFactory.context().loggingManager().setLoggingApp( appSettings.getId() );
                }
            }
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingApp( this.appSettings.getId() );
            postStop( saveNewTargetState ); 
            logger.info("Stopped    " + this.getAppProperties().getName() + " [" + this.getAppSettings().getId() + "]" + " ...");
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    private void destroy( boolean saveNewTargetState )  
    {
        if (currentState == AppState.DESTROYED) {
            logger.warn("Ignoring destroy(): Already in state DESTROYED");
            return;
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingApp( this.appSettings.getId() );
            logger.info("Destroying " + this.getAppProperties().getName() + " [" + this.getAppSettings().getId() + "]" + " ...");
            preDestroy();
            for (AppBase p : parents) {
                p.removeChild(this);
            }
            parents.clear();
            changeState(AppState.DESTROYED, saveNewTargetState);

            postDestroy();
            logger.info("Destroyed  " + this.getAppProperties().getName() + " [" + this.getAppSettings().getId() + "]" + " ...");
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    private final void stopIfNotRequiredByChildren() 
    {
        if (getRunState() != AppState.RUNNING)
            return;

        /**
         * Return if any children are still running
         */
        for (App app : children) {
            if (app.getRunState() == AppState.RUNNING) 
                return;
        } 

        stop( false );
    }
    
    private final static App startParent( String parent, Integer policyId ) throws Exception
    {
        if (null == parent) {
            return null;
        }

        staticLogger.debug( "Starting required parent: " + parent );

        App parentApp = getParentApp( parent, policyId );

        if ( parentApp == null ) {
            staticLogger.debug("Parent does not exist, instantiating");

            parentApp = UvmContextFactory.context().appManager().instantiate(parent, policyId);
        }

        if ( parentApp == null ) {
            throw new Exception("could not create parent: " + parent);
        } else {
            return parentApp;
        }
    }

    private final static App getParentApp( String parent, Integer childPolicyId )
    {
        for (App app : UvmContextFactory.context().appManager().appInstances(parent)) {
            Integer policyId = app.getAppSettings().getPolicyId();
            if ( policyId == null || policyId.equals( childPolicyId ) )
                return app;
        }

        return null;
    }
    
}
