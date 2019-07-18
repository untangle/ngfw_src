/**
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

import org.json.JSONObject;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.app.AppSettings.AppState;
import com.untangle.uvm.logging.LogEvent;

/**
 * A base class for app instances, both normal and casing.
 * 
 */
public abstract class AppBase implements App
{
    private static final Logger staticLogger = Logger.getLogger(AppBase.class);
    private final Logger logger = Logger.getLogger(AppBase.class);

    /**
     * These are the (generic) settings for this app The app usually stores more
     * app-specific settings in "settings" This holds the generic AppSettings
     * that all apps have.
     */
    private AppSettings appSettings;

    /**
     * These are the properties for this app
     */
    private AppProperties appProperties;

    /**
     * This stores a set of parents of this app Parents are any apps that this
     * app depends on to operate properly
     */
    private Set<AppBase> parents = new HashSet<>();

    /**
     * This stores a set of children to this app Children are any apps that
     * depend on this app to operate properly
     */
    private Set<App> children = new HashSet<>();

    /**
     * These store this app's metrics (for display in the UI) The hash map is
     * for fast lookups The list is to maintain order for the UI
     */
    private Map<String, AppMetric> metrics = new ConcurrentHashMap<>();
    private List<AppMetric> metricList = new ArrayList<>();

    private AppSettings.AppState currentState;

    /**
     * Constructor
     */
    protected AppBase()
    {
        currentState = AppState.LOADED;
    }

    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param appProperties
     *        The application properties
     */
    protected AppBase(AppSettings appSettings, AppProperties appProperties)
    {
        this.appSettings = appSettings;
        this.appProperties = appProperties;

        currentState = AppState.LOADED;
    }

    /**
     * Get the pipeline connectors
     * 
     * @return The pipeline connectors
     */
    protected abstract PipelineConnector[] getConnectors();

    /**
     * Register the pipeline connectors
     */
    protected void connectPipelineConnectors()
    {
        if (getConnectors() != null) {
            for (PipelineConnector connector : getConnectors()) {
                UvmContextFactory.context().pipelineFoundry().registerPipelineConnector(connector);
            }
        }
    }

    /**
     * Unregister the pipeline connectors
     */
    protected void disconnectPipelineConnectors()
    {
        if (getConnectors() != null) {
            for (PipelineConnector connector : getConnectors()) {
                UvmContextFactory.context().pipelineFoundry().deregisterPipelineConnector(connector);
                connector.destroy();
            }
        }
    }

    /**
     * The the application run state
     * 
     * @return The run state
     */
    public final AppState getRunState()
    {
        return currentState;
    }

    /**
     * Initialization the application
     */
    public final void init()
    {
        init(true);
    }

    /**
     * Start the application
     */
    public final void start()
    {
        start(true);
    }

    /**
     * Stop the application
     */
    public final void stop()
    {
        stop(true);
    }

    /**
     * Get the application settings
     * 
     * @return The application settings
     */
    public AppSettings getAppSettings()
    {
        return appSettings;
    }

    /**
     * Set the application settings
     * 
     * @param appSettings
     *        The new settings
     */
    public void setAppSettings(AppSettings appSettings)
    {
        this.appSettings = appSettings;
    }

    /**
     * Get the application properties
     * 
     * @return The application properties
     */
    public AppProperties getAppProperties()
    {
        return appProperties;
    }

    /**
     * Set the application properties
     * 
     * @param appProperties
     *        The new properties
     */
    public void setAppProperties(AppProperties appProperties)
    {
        this.appProperties = appProperties;
    }

    /**
     * Add a parent
     * 
     * @param parent
     *        The parent
     */
    public void addParent(AppBase parent)
    {
        parents.add(parent);
        parent.addChild(this);
    }

    /**
     * Called when the app is new, initial settings should be created and saved
     * in this method.
     */
    public void initializeSettings()
    {
    }

    /**
     * Resume the application state
     * 
     * @param appState
     *        The state
     */
    public void resumeState(AppState appState)
    {
        switch (appState)
        {
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

    /**
     * Destroy the application
     */
    public void destroy()
    {
        uninstall();

        destroy(true);
    }

    /**
     * Stop the application if it's running
     */
    public void stopIfRunning()
    {
        UvmContextFactory.context().loggingManager().setLoggingApp(appSettings.getId());

        switch (currentState)
        {
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

    /**
     * Enable the application
     */
    public void enable()
    {
        switch (currentState)
        {
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

    /**
     * Log an event
     * 
     * @param evt
     *        The event to log
     */
    public void logEvent(LogEvent evt)
    {
        String tag = appProperties.getDisplayName().replaceAll("\\s+", "_") + " [" + appSettings.getId() + "]:";
        evt.setTag(tag);

        UvmContextFactory.context().logEvent(evt);
    }

    /**
     * Load the class
     * 
     * @param appProperties
     *        The applicatin properties
     * @param appSettings
     *        The application settings
     * @param isNew
     *        True for new, false for existing
     * @return The application
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static final App loadClass(AppProperties appProperties, AppSettings appSettings, boolean isNew) throws Exception
    {
        if (appProperties == null || appSettings == null) throw new Exception("Invalid Arguments: null");

        try {
            AppBase app;

            Set<App> parentApps = new HashSet<>();
            if (appProperties.getParents() != null) {
                for (String parent : appProperties.getParents()) {
                    parentApps.add(startParent(parent, appSettings.getPolicyId()));
                }
            }

            UvmContextFactory.context().loggingManager().setLoggingApp(appSettings.getId());

            String appSettingsName = appSettings.getAppName();
            staticLogger.debug("setting app " + appSettingsName + " log4j repository");

            String className = appProperties.getClassName();
            java.lang.reflect.Constructor constructor = Class.forName(className).getConstructor(new Class<?>[] { AppSettings.class, AppProperties.class });
            app = (AppBase) constructor.newInstance(appSettings, appProperties);

            app.setAppProperties(appProperties);
            app.setAppSettings(appSettings);

            for (App parentApp : parentApps) {
                app.addParent((AppBase) parentApp);
            }

            if (isNew) {
                app.initializeSettings();
                app.init();
            } else {
                try {
                    app.resumeState(appSettings.getTargetState());
                } catch (Exception exn) {
                    staticLogger.error("Exception during app resumeState", exn);
                    if (exn.getCause() != null) staticLogger.error("Cause", exn.getCause());
                    // still return the initialized app
                }
            }

            return app;

        } catch (Exception exn) {
            staticLogger.error("Exception during app initialization", exn);
            if (exn.getCause() != null) staticLogger.error("Cause", exn.getCause());
            throw exn;
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    /**
     * Destroy the class
     * 
     * @throws Exception
     */
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

    /**
     * Get a list of live sessions
     * 
     * @return The list
     */
    public List<SessionTuple> liveSessions()
    {
        List<SessionTuple> sessions = new LinkedList<>();

        for (AppSession sess : liveAppSessions()) {
            SessionTuple tuple = new SessionTuple(sess.getProtocol(), sess.getClientAddr(), sess.getServerAddr(), sess.getClientPort(), sess.getServerPort());
            sessions.add(tuple);
        }

        return sessions;
    }

    /**
     * Get a list of live app sessions
     * 
     * @return The list
     */
    public List<AppSession> liveAppSessions()
    {
        List<AppSession> sessions = new LinkedList<>();

        if (getConnectors() != null) {
            for (PipelineConnector connector : getConnectors()) {
                for (AppSession sess : connector.liveSessions()) {
                    /*
                     * create a new sessiontupleimpl so the list will be
                     * serialized properly
                     */
                    sessions.add(sess);
                }
            }
        }

        return sessions;
    }

    /**
     * Get the application metrics
     * 
     * @return The metrics
     */
    public List<AppMetric> getMetrics()
    {
        return metricList;
    }

    /**
     * Get a specific application metric
     * 
     * @param name
     *        The metric name
     * @return The metric
     */
    public AppMetric getMetric(String name)
    {
        AppMetric metric = metrics.get(name);
        if (metric == null)
            logger.debug("Metric not found: " + name);
        return metric;
    }

    /**
     * Decrement a metric
     * 
     * @param name
     *        The metric name
     */
    public void decrementMetric(String name)
    {
        adjustMetric(name, -1L);
    }

    /**
     * Increment a metric
     * 
     * @param name
     *        The metric name
     */
    public void incrementMetric(String name)
    {
        adjustMetric(name, 1L);
    }

    /**
     * Set a metric
     * 
     * @param name
     *        The metric name
     * @param newValue
     *        The new value
     */
    public void setMetric(String name, Long newValue)
    {
        if (name == null) {
            logger.warn("Invalid stat: " + name);
            return;
        }

        AppMetric metric = metrics.get(name);
        if (metric == null) {
            logger.warn("AppMetric not found: " + name);
            return;
        }
        metric.setValue(newValue);
    }

    /**
     * Adjust a metric
     * 
     * @param name
     *        The metric name
     * @param adjustmentValue
     *        The adjustment value
     */
    public void adjustMetric(String name, Long adjustmentValue)
    {
        if (name == null) {
            logger.warn("Invalid stat: " + name);
            return;
        }

        AppMetric metric = metrics.get(name);
        if (metric == null) {
            logger.warn("AppMetric not found: " + name);
            return;
        }

        Long value = metric.getValue();
        if (value == null) value = 0L;
        value = value + adjustmentValue;
        metric.setValue(value);
    }

    /**
     * Add a metric
     * 
     * @param metric
     *        The metric
     */
    public void addMetric(AppMetric metric)
    {
        if (metrics.get(metric.getName()) != null) {
            //logger.warn("addMetric(): Metric already exists: \"" + metric.getName() + "\" - ignoring");
            return;
        }
        this.metrics.put(metric.getName(), metric);
        this.metricList.add(metric);
    }

    /**
     * Remove a metric
     * 
     * @param metric
     *        The metric
     */
    public void removeMetric(AppMetric metric)
    {
        if (metric == null) {
            logger.warn("Invalid argument: null");
            return;
        }
        if (metrics.get(metric.getName()) == null) {
            logger.warn("Invalid argument: metric not found");
            return;
        }

        this.metrics.remove(metric.getName());
        this.metricList.remove(metric);
    }

    /**
     * Get the status
     * 
     * @return The status
     */
    public String getStatus()
    {
        return "";
    }

    /**
     * Generate a string representation
     * 
     * @return The string
     */
    public String toString()
    {
        return "App[" + getAppSettings().getId() + "," + getAppSettings().getAppName() + "]";
    }

    /**
     * See if the license if valid
     * 
     * @return True if valid, otherwise false
     */
    public boolean isLicenseValid()
    {
        return UvmContextFactory.context().licenseManager().isLicenseValid(appProperties.getName());
    }

    /**
     * Return restricted list of JSONObjects from this app.
     * @param key String of key to return;
     * @return List of apps.
     */
    public List<JSONObject> getReportInfo(String key){
        return null;
    }

    // protected methods -------------------------------------------------

    /**
     * Called when the app is being uninstalled, rather than just being taken
     * down with the UVM.
     */
    protected void uninstall()
    {
    }

    /**
     * Called before initialization
     * 
     * initialization occurs when an app is instantiated or on startup
     */
    protected void preInit()
    {
    }

    /**
     * Called after initialization
     * 
     * initialization occurs when an app is instantiated or on startup
     */
    protected void postInit()
    {
    }

    /**
     * Called just after connecting to PipelineConnector, but before starting.
     * 
     * isPermanentTransition is true if this is the permanent (saved) This can
     * be used to determine if this app is being started permanently
     * 
     * @param isPermanentTransition
     */
    protected void preStart(boolean isPermanentTransition)
    {
    }

    /**
     * Called just after starting PipelineConnector and making subscriptions.
     * 
     * isPermanentTransition is true if this is the permanent (saved) This can
     * be used to determine if this app is being started permanently
     * 
     * @param isPermanentTransition
     * 
     */
    protected void postStart(boolean isPermanentTransition)
    {
    }

    /**
     * Called just before stopping PipelineConnector and disconnecting.
     * 
     * isPermanentTransition is true if this is the permanent (saved) This can
     * be used to determine if this app is being stopped permanently
     * 
     * @param isPermanentTransition
     */
    protected void preStop(boolean isPermanentTransition)
    {
    }

    /**
     * Called after stopping PipelineConnector and disconnecting.
     * 
     * isPermanentTransition is true if this is the permanent (saved) This can
     * be used to determine if this app is being stopped permanently
     * 
     * @param isPermanentTransition
     */
    protected void postStop(boolean isPermanentTransition)
    {
    }

    /**
     * Called just before this instance becomes invalid.
     * 
     */
    protected void preDestroy()
    {
    }

    /**
     * Same as <code>postDestroy</code>, except now officially in the
     * {@link AppState#DESTROYED} state.
     */
    protected void postDestroy()
    {
    }

    /**
     * This kills/resets all of the matching sessions (runs against all sessions
     * globally)
     * 
     * @param matcher
     *        The matcher
     */
    protected void killMatchingSessionsGlobal(SessionMatcher matcher)
    {
        if (matcher == null) return;

        UvmContextFactory.context().netcapManager().shutdownMatches(matcher);
    }

    /**
     * This kills/resets all of the matching sessions for this app's sessions
     * This includes "released" sessions that we processed previously by one of
     * this app's pipespecs
     * 
     * @param matcher
     *        The matcher
     */
    protected void killMatchingSessions(SessionMatcher matcher)
    {
        logger.info("killMatchingSessions()");
        if (matcher == null) return;
        if (getConnectors() == null) return;

        for (PipelineConnector connector : getConnectors())
            UvmContextFactory.context().netcapManager().shutdownMatches(matcher, connector);
    }

    /**
     * This kills all this app's sessions (for all its pipespecs)
     */
    public void killAllSessions()
    {
        killMatchingSessions(new SessionMatcher()
        {
            /**
             * Session matcher for all sessions
             * 
             * @param policyId
             * @param protocol
             * @param clientIntf
             * @param serverIntf
             * @param clientAddr
             * @param serverAddr
             * @param clientPort
             * @param serverPort
             * @param attachments
             * @return Always true to match everything
             */
            public boolean isMatch(Integer policyId, short protocol, int clientIntf, int serverIntf, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort, Map<String, Object> attachments)
            {
                return true;
            }
        });
    }

    /**
     * Add a child
     * 
     * @param child
     *        The child
     */
    private void addChild(App child)
    {
        children.add(child);
    }

    /**
     * Remove a child
     * 
     * @param child
     *        The child
     * @return True if found and removed, otherwise false
     */
    private boolean removeChild(App child)
    {
        return children.remove(child);
    }

    /**
     * Change the application state
     * 
     * @param appState
     *        The new state
     * @param saveNewTargetState
     *        True to save the state, otherwise false
     */
    private void changeState(AppState appState, boolean saveNewTargetState)
    {
        if (saveNewTargetState) {
            UvmContextFactory.context().appManager().saveTargetState(this, appState);

            UvmContextFactory.context().pipelineFoundry().clearCache();
        }

        this.currentState = appState;
    }

    /**
     * Initialize the application
     * 
     * @param saveNewTargetState
     *        True to save the state, otherwise false
     */
    private void init(boolean saveNewTargetState)
    {
        if (currentState != AppState.LOADED) {
            logger.warn("Init called in state: " + currentState);
            return;
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingApp(this.appSettings.getId());

            // if no valid license exists, request a trial license
            try {
                if (!UvmContextFactory.context().licenseManager().isLicenseValid(appProperties.getName())) {
                    logger.info("No valid license for: " + appProperties.getName());
                    logger.info("Requesting trial for: " + appProperties.getName());
                    UvmContextFactory.context().licenseManager().requestTrialLicense(appProperties.getName());
                }
            } catch (Exception e) {
                logger.warn("Exception fetching trial license. Ignoring...", e);
            }

            preInit();
            changeState(AppState.INITIALIZED, saveNewTargetState);
            postInit();

        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    /**
     * Start the application
     * 
     * @param saveNewTargetState
     *        True to save the state, otherwise false
     */
    private void start(boolean saveNewTargetState)
    {
        if (AppState.INITIALIZED != getRunState()) {
            logger.warn("Start called in state: " + getRunState());
            return;
        }

        for (AppBase parent : parents) {
            if (AppState.INITIALIZED == parent.getRunState()) {
                try {
                    UvmContextFactory.context().loggingManager().setLoggingApp(parent.getAppSettings().getId());
                    if (parent.getRunState() == AppState.INITIALIZED) parent.start(false);
                } finally {
                    UvmContextFactory.context().loggingManager().setLoggingApp(appSettings.getId());
                }
            }
        }

        // save new settings first (if this fails, the state should not be changed)
        try {
            changeState(AppState.RUNNING, saveNewTargetState);
        } catch (Exception e) {
            logger.warn("Failed to start app", e);
            return;
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingApp(this.appSettings.getId());
            logger.info("Starting   " + this.getAppProperties().getName() + " [" + this.getAppSettings().getId() + "]" + " ...");

            try {
                preStart(saveNewTargetState);
            } catch (Exception e) {
                logger.warn("Exception in preStart(). Reverting to INITIALIZED state.", e);
                changeState(AppState.INITIALIZED, saveNewTargetState);
                throw e;
            }

            connectPipelineConnectors();

            try {
                postStart(saveNewTargetState);
            } catch (Exception e) {
                logger.warn("Exception in postStart().", e);
            }

            logger.info("Started    " + this.getAppProperties().getName() + " [" + this.getAppSettings().getId() + "]" + " ...");
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    /**
     * Stop the application
     * 
     * @param saveNewTargetState
     *        True to save the state, otherwise false
     */
    private void stop(boolean saveNewTargetState)
    {
        if (AppState.RUNNING != getRunState()) {
            logger.warn("Stop called in state: " + getRunState());
            return;
        }

        // save new settings first (if this fails, the state should not be changed)
        try {
            changeState(AppState.INITIALIZED, saveNewTargetState);
        } catch (Exception e) {
            logger.warn("Failed to stop app", e);
            return;
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingApp(this.appSettings.getId());
            logger.info("Stopping   " + this.getAppProperties().getName() + " [" + this.getAppSettings().getId() + "]" + " ...");
            preStop(saveNewTargetState);
            disconnectPipelineConnectors();
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }

        for (AppBase parent : parents) {
            if (AppState.RUNNING == parent.getRunState()) {
                try {
                    UvmContextFactory.context().loggingManager().setLoggingApp(parent.getAppSettings().getId());
                    parent.stopIfNotRequiredByChildren();
                } finally {
                    UvmContextFactory.context().loggingManager().setLoggingApp(appSettings.getId());
                }
            }
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingApp(this.appSettings.getId());
            postStop(saveNewTargetState);
            logger.info("Stopped    " + this.getAppProperties().getName() + " [" + this.getAppSettings().getId() + "]" + " ...");
        } finally {
            UvmContextFactory.context().loggingManager().setLoggingUvm();
        }
    }

    /**
     * Destroy the application
     * 
     * @param saveNewTargetState
     *        True to save the state, otherwise false
     */
    private void destroy(boolean saveNewTargetState)
    {
        if (currentState == AppState.DESTROYED) {
            logger.warn("Ignoring destroy(): Already in state DESTROYED");
            return;
        }

        try {
            UvmContextFactory.context().loggingManager().setLoggingApp(this.appSettings.getId());
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

    /**
     * Stop the application if not required by any children
     */
    private final void stopIfNotRequiredByChildren()
    {
        if (getRunState() != AppState.RUNNING) return;

        /**
         * Return if any children are still running
         */
        for (App app : children) {
            if (app.getRunState() == AppState.RUNNING) return;
        }

        stop(false);
    }

    /**
     * Start the parent
     * 
     * @param parent
     *        The parent name
     * @param policyId
     *        The policy ID
     * @return The parent or null if there is no parent
     * @throws Exception
     */
    private final static App startParent(String parent, Integer policyId) throws Exception
    {
        if (null == parent) {
            return null;
        }

        staticLogger.debug("Starting required parent: " + parent);

        App parentApp = getParentApp(parent, policyId);

        if (parentApp == null) {
            staticLogger.debug("Parent does not exist, instantiating");

            parentApp = UvmContextFactory.context().appManager().instantiate(parent, policyId);
        }

        if (parentApp == null) {
            throw new Exception("could not create parent: " + parent);
        } else {
            return parentApp;
        }
    }

    /**
     * Get the parent
     * 
     * @param parent
     *        The parent name
     * @param childPolicyId
     *        The child policy ID
     * @return The parent appor null if there is no parent
     */
    private final static App getParentApp(String parent, Integer childPolicyId)
    {
        for (App app : UvmContextFactory.context().appManager().appInstances(parent)) {
            Integer policyId = app.getAppSettings().getPolicyId();
            if (policyId == null || policyId.equals(childPolicyId)) return app;
        }

        return null;
    }
}
