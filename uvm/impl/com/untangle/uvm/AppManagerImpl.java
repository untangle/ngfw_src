/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;

import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppManagerSettings;
import com.untangle.uvm.app.LicenseManager;
import com.untangle.uvm.app.PolicyManager;
import com.untangle.uvm.app.AppManager;
import com.untangle.uvm.app.App;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.License;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.app.AppBase;

/**
 * Implements AppManager.
 */
public class AppManagerImpl implements AppManager
{
    private final static String APP_MANAGER_SETTINGS_FILE = System.getProperty("uvm.settings.dir") + "/untangle-vm/apps.js";

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Stores a map of all currently loaded apps from their appId to the App
     * instance
     */
    private final Map<Long, App> loadedAppsMap = new ConcurrentHashMap<>();

    /**
     * Stores a map of all yet to be loaded apps from their appId to their
     * AppSettings
     */
    private final Map<Long, AppSettings> unloadedAppsMap = new ConcurrentHashMap<>();

    /**
     * This stores the count of apps currently being loaded
     */
    private ConcurrentHashMap<Long, AppProperties> appsBeingLoaded = new ConcurrentHashMap<>();

    private AppManagerSettings settings = null;

    private Semaphore startSemaphore = new Semaphore(0);

    private boolean live = true;

    /**
     * Constructor
     */
    public AppManagerImpl()
    {
    }

    /**
     * Get the settings
     * 
     * @return The settings
     */
    public AppManagerSettings getSettings()
    {
        return this.settings;
    }

    /**
     * Saves the state of the specified application
     * 
     * @param app
     *        The application
     * @param appState
     *        The application state
     */
    public void saveTargetState(App app, AppSettings.AppState appState)
    {
        if (app == null) {
            logger.error("Invalid argument saveTargetState(): app is null");
            return;
        }
        if (appState == null) {
            logger.error("Invalid argument saveTargetState(): appState is null");
            return;
        }

        AppSettings.AppState originalState = null;

        for (AppSettings appSettings : this.settings.getApps()) {
            if (appSettings.getId() == app.getAppSettings().getId()) {
                if (appState != appSettings.getTargetState()) {
                    originalState = appSettings.getTargetState();
                    appSettings.setTargetState(appState);
                    break;
                } else {
                    logger.info("ignore saveTargetState(): already in state " + appState);
                }
            }
        }

        try {
            this._setSettings(this.settings);
        } catch (Exception e) {
            //roll back changes
            if (originalState != null) {
                for (AppSettings appSettings : this.settings.getApps()) {
                    if (appSettings.getId() == app.getAppSettings().getId()) {
                        appSettings.setTargetState(originalState);
                        break;
                    }
                }
            }
            throw e;
        }
    }

    /**
     * Get a list of all application instances
     * 
     * @return The list of all application instances
     */
    public List<App> appInstances()
    {
        List<App> appList = new ArrayList<>(loadedAppsMap.values());

        // sort by view position, for convenience
        Collections.sort(appList, new Comparator<App>()
        {
            /**
             * view position compare function
             * 
             * @param tci1
             *        App one
             * @param tci2
             *        App two
             * @return Comparison result
             */
            public int compare(App tci1, App tci2)
            {
                int rpi1 = tci1.getAppProperties().getViewPosition();
                int rpi2 = tci2.getAppProperties().getViewPosition();
                if (rpi1 == rpi2) {
                    return tci1.getAppProperties().getName().compareToIgnoreCase(tci2.getAppProperties().getName());
                } else if (rpi1 < rpi2) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

        return appList;
    }

    /**
     * Get the list of all node instances
     * 
     * @return The list of all node instances
     */
    public List<App> nodeInstances()
    {
        logger.warn("deprecated method called.", new Exception());
        return appInstances();
    }

    /**
     * Get the list of all application instance ID's
     * 
     * @return The list of all application instance ID's
     */
    public List<Long> appInstancesIds()
    {
        return appToIdList(appInstances());
    }

    /**
     * Get the list of all node instance ID's
     * 
     * @return The list of all node instance ID's
     */

    public List<Long> nodeInstancesIds()
    {
        logger.warn("deprecated method called.", new Exception());
        return appInstancesIds();
    }

    /**
     * Get the list of instances for a specified application
     * 
     * @param appName
     *        The application
     * @return The list of instances
     */
    public List<App> appInstances(String appName)
    {
        appName = fixupName(appName); // handle old names

        List<App> list = new LinkedList<>();

        for (App app : loadedAppsMap.values()) {
            if (app.getAppProperties().getName().equals(appName)) {
                list.add(app);
            }
        }

        return list;
    }

    /**
     * Get the list of instances for a specified node
     * 
     * @param appName
     *        The application
     * @return The list of instances
     */
    public List<App> nodeInstances(String appName)
    {
        logger.warn("deprecated method called.", new Exception());
        return appInstances(appName);
    }

    /**
     * Get the list of all application instances, including parents, for the
     * specified application.
     * 
     * @param name
     *        The application name
     * @param policyId
     *        The policy ID
     * @return The application list
     */
    public List<App> appInstances(String name, Integer policyId)
    {
        return appInstances(name, policyId, true);
    }

    /**
     * Get the list of all node instances, including parents, for the specified
     * application.
     * 
     * @param name
     *        The application name
     * @param policyId
     *        The policy ID
     * @return The application list
     */
    public List<App> nodeInstances(String name, Integer policyId)
    {
        logger.warn("deprecated method called.", new Exception());
        return appInstances(name, policyId, true);
    }

    /**
     * Get the list of all instances for a specified application
     * 
     * @param name
     *        The application name
     * @param policyId
     *        The policy ID
     * @param parents
     *        True to include parents, otherwise false
     * @return The list
     */
    public List<App> appInstances(String name, Integer policyId, boolean parents)
    {
        name = fixupName(name); // handle old names

        List<App> list = new ArrayList<>(loadedAppsMap.size());

        for (App app : getAppsForPolicy(policyId, parents)) {
            String appName = app.getAppProperties().getName();

            if (appName.equals(name)) {
                list.add(app);
            }
        }

        return list;
    }

    /**
     * Get the list of all instances for a specified application
     * 
     * @param name
     *        The application name
     * @param policyId
     *        The policy ID
     * @param parents
     *        True to include parents, otherwise false
     * @return The list
     */
    public List<App> nodeInstances(String name, Integer policyId, boolean parents)
    {
        logger.warn("deprecated method called.", new Exception());
        return appInstances(name, policyId, parents);
    }

    /**
     * Get the list of applications for a specified policy
     * 
     * @param policyId
     *        The policy
     * @return The list
     */
    public List<App> appInstances(Integer policyId)
    {
        return getAppsForPolicy(policyId);
    }

    /**
     * Get the list of applications for a specified policy
     * 
     * @param policyId
     *        The policy
     * @return The list
     */
    public List<App> nodeInstances(Integer policyId)
    {
        logger.warn("deprecated method called.", new Exception());
        return appInstances(policyId);
    }

    /**
     * Get the list of application instance ID's for a specified policy
     * 
     * @param policyId
     *        The policy
     * @return The list
     */
    public List<Long> appInstancesIds(Integer policyId)
    {
        return appToIdList(appInstances(policyId));
    }

    /**
     * Get the list of application instance ID's for a specified policy
     * 
     * @param policyId
     *        The policy
     * @return The list
     */
    public List<Long> nodeInstancesIds(Integer policyId)
    {
        logger.warn("deprecated method called.", new Exception());
        return appInstancesIds(policyId);
    }

    /**
     * Get the list of visible apps for a specified policy
     * 
     * @param policyId
     *        The policy
     * @return The list of visible apps
     */
    protected List<App> visibleApps(Integer policyId)
    {
        List<App> loadedApps = appInstances();
        List<App> list = new ArrayList<>(loadedApps.size());

        for (App app : getAppsForPolicy(policyId)) {
            if (!app.getAppProperties().getInvisible()) {
                list.add(app);
            }
        }

        for (App app : getAppsForPolicy(null /* services */)) {
            if (!app.getAppProperties().getInvisible()) {
                list.add(app);
            }
        }

        return list;
    }

    /**
     * Get the list of visible nodes for a specified policy
     * 
     * @param policyId
     *        The policy
     * @return The list of visible nodes
     */
    protected List<App> visibleNodes(Integer policyId)
    {
        logger.warn("deprecated method called.", new Exception());
        return visibleApps(policyId);
    }

    /**
     * Get the app for the specified application ID
     * 
     * @param appId
     *        The application ID
     * @return The App
     */
    public App app(Long appId)
    {
        return loadedAppsMap.get(appId);
    }

    /**
     * Get the app for the specified application ID
     * 
     * @param appId
     *        The application ID
     * @return The App
     */
    public App node(Long appId)
    {
        logger.warn("deprecated method called.", new Exception());
        return app(appId);
    }

    /**
     * Get the app for the specified application name
     * 
     * @param name
     *        The application name
     * @return The App
     */
    public App app(String name)
    {
        name = fixupName(name); // handle old names

        List<App> apps = appInstances(name);
        if (apps.size() > 0) {
            return apps.get(0);
        }
        return null;
    }

    /**
     * Get the app for the specified application name
     * 
     * @param name
     *        The application name
     * @return The App
     */
    public App node(String name)
    {
        logger.warn("deprecated method called.", new Exception());
        return app(name);
    }

    /**
     * Instantiate a new instance of the specified application using the default
     * policy
     * 
     * @param appName
     *        The application name to instantiate
     * @return The App
     * @throws Exception
     */
    public App instantiate(String appName) throws Exception
    {
        return instantiate(appName, 1 /* Default Policy ID */);
    }

    /**
     * Instantiate a new instance of the specified application using the
     * specified policy
     * 
     * @param appName
     *        The application name to instantiate
     * @param policyId
     *        The policy
     * @return The App
     * @throws Exception
     */
    public App instantiate(String appName, Integer policyId) throws Exception
    {
        appName = fixupName(appName); // handle old names

        logger.info("instantiate( name:" + appName + " , policy:" + policyId + " )");

        if (!UvmContextFactory.context().licenseManager().isLicenseValid(appName)) {
            logger.info("No valid license for: " + appName);
            logger.info("Requesting trial for: " + appName);
            UvmContextFactory.context().licenseManager().requestTrialLicense(appName);
        }

        App app = null;
        AppProperties appProperties = null;
        AppSettings appSettings = null;

        synchronized (this) {
            if (!live) throw new Exception("AppManager is shut down");

            logger.info("initializing app: " + appName);
            appProperties = getAppProperties(appName);

            if (appProperties == null) {
                logger.error("Missing app properties for " + appName);
                throw new Exception("Missing app properties for " + appName);
            }

            if (!checkArchitecture(appProperties.getSupportedArchitectures())) {
                throw new Exception("Unsupported Architecture " + System.getProperty("os.arch"));
            }

            if (appProperties.getMinimumMemory() != null) {
                Long requiredMemory = appProperties.getMinimumMemory();
                Long actualMemory = UvmContextFactory.context().metricManager().getMemTotal();
                if (actualMemory < requiredMemory) {
                    float requiredGig = ((float) ((double) requiredMemory / (1024 * 1024))) / (1024.0f);
                    float actualGig = ((float) ((double) actualMemory / (1024 * 1024))) / (1024.0f);
                    String message = "This app requires more memory (required: " + (Math.round(10.0 * requiredGig) / 10.0) + "G actual: " + (Math.round(10.0 * actualGig) / 10.0) + "G)";
                    throw new Exception(message);
                }
            }

            if (appProperties.getType() == AppProperties.Type.SERVICE) policyId = null;

            if (appInstances(appName, policyId, false).size() >= 1) throw new Exception("Too many instances of " + appName + " in policy " + policyId + ".");
            for (AppSettings n2 : getSettings().getApps()) {
                String appName1 = appName;
                String appName2 = n2.getAppName();
                Integer policyId1 = policyId;
                Integer policyId2 = n2.getPolicyId();
                /**
                 * If the app name and policies are equal, they are dupes
                 */
                if (appName1.equals(appName2) && ((policyId1 == policyId2) || (policyId1 != null && policyId1.equals(policyId2)))) throw new Exception("Too many instances of " + appName + " in policy " + policyId + ".");
            }

            appSettings = createNewAppSettings(policyId, appName);

            /**
             * Check all the basics
             */
            if (appSettings == null) throw new Exception("Null appSettings: " + appName);
            if (appProperties == null) throw new Exception("Null appProperties: " + appName);

            app = AppBase.loadClass(appProperties, appSettings, true);

            if (app != null) {
                loadedAppsMap.put(appSettings.getId(), app);
                saveNewAppSettings(appSettings);
            } else {
                logger.error("Failed to initialize app: " + appProperties.getName());
            }

        }

        /**
         * If AutoStart is true, go ahead and start app
         */
        if (app != null && appProperties != null && appProperties.getAutoStart()) {
            app.start();
        }

        // Full System GC so the JVM gives memory back
        UvmContextFactory.context().gc();

        UvmContextFactory.context().hookManager().callCallbacks( HookManager.APPLICATION_INSTANTIATE, appName, app);

        return app;
    }

    /**
     * Destroy the app instance for a specified application ID
     * 
     * @param appId
     *        The application ID
     * @throws Exception
     */
    public void destroy(Long appId) throws Exception
    {
        destroy(app(appId));
    }

    /**
     * Destroy the appinstance for the specified application *
     * 
     * @param app
     *        The application
     * @throws Exception
     */
    public void destroy(App app) throws Exception
    {
        if (app == null) {
            throw new Exception("App " + app + " not found");
        }

        String appName = app.getAppSettings().getAppName();

        synchronized (this) {
            AppBase appBase = (AppBase) app;
            appBase.destroyClass();

            /**
             * Remove from map and list and save settings
             */
            loadedAppsMap.remove(app.getAppSettings().getId());
            for (Iterator<AppSettings> iter = this.settings.getApps().iterator(); iter.hasNext();) {
                AppSettings appSettings = iter.next();
                if (appSettings.getId().equals(app.getAppSettings().getId())) iter.remove();
            }
            this._setSettings(this.settings);
        }

        // Full System GC so the JVM gives memory back for unloaded memory/classes
        // This is necessary because the G1 does not actually account for MaxHeapFreeRatio
        // except during an full GC.
        UvmContextFactory.context().gc();

        UvmContextFactory.context().hookManager().callCallbacks( HookManager.APPLICATION_DESTROY, appName);

        return;
    }

    /**
     * Get a map of all application states
     * 
     * @return Map of all application states
     */
    public Map<Long, AppSettings.AppState> allAppStates()
    {
        HashMap<Long, AppSettings.AppState> result = new HashMap<>();
        for (App app : loadedAppsMap.values()) {
            result.put(app.getAppSettings().getId(), app.getRunState());
        }

        return result;
    }

    /**
     * Get a map of all application states
     * 
     * @return The map of all application states
     */
    public Map<Long, AppSettings.AppState> allNodeStates()
    {
        logger.warn("deprecated method called.", new Exception());
        return allAppStates();
    }

    /**
     * Check if a specified application has been instantiated
     * 
     * @param appName
     *        The application name
     * @return True if instantiated, otherwise false
     */
    public boolean isInstantiated(String appName)
    {
        return (this.app(appName) != null);
    }

    /**
     * Get a map of all application settings
     * 
     * @return Map of all application settings
     */
    public Map<Long, AppSettings> getAllAppSettings()
    {
        HashMap<Long, AppSettings> result = new HashMap<>();
        for (App app : loadedAppsMap.values()) {
            result.put(app.getAppSettings().getId(), app.getAppSettings());
        }
        return result;
    }

    /**
     * Get a map of all application settings
     * 
     * @return Map of all application settings
     */
    public Map<Long, AppSettings> allNodeSettings()
    {
        logger.warn("deprecated method called.", new Exception());
        return getAllAppSettings();
    }

    /**
     * Get a map of all application properties
     * 
     * @return Map of all application properties
     */
    public Map<Long, AppProperties> getAllAppPropertiesMap()
    {
        HashMap<Long, AppProperties> result = new HashMap<>();
        for (App app : loadedAppsMap.values()) {
            result.put(app.getAppSettings().getId(), app.getAppProperties());
        }
        return result;
    }

    /**
     * Get a map of all application properties
     * 
     * @return Map of all application properties
     */
    public Map<Long, AppProperties> allNodeProperties()
    {
        logger.warn("deprecated method called.", new Exception());
        return getAllAppPropertiesMap();
    }

    /**
     * Get a list of all application properties
     * 
     * @return List of all application properties
     */
    public List<AppProperties> getAllAppProperties()
    {
        LinkedList<AppProperties> appProps = new LinkedList<>();

        File rootDir = new File(System.getProperty("uvm.lib.dir"));

        findAllAppProperties(appProps, rootDir);

        return appProps;
    }

    /**
     * Get a list of all application properties
     * 
     * @return List of all application properties
     */
    public List<AppProperties> getAllNodeProperties()
    {
        logger.warn("deprecated method called.", new Exception());
        return getAllAppProperties();
    }

    /**
     * Get the applications view for the specified policy ID
     * 
     * @param policyId
     *        The policy ID
     * @return The applications view
     */
    public AppsView getAppsView(Integer policyId)
    {
        AppManagerImpl nm = (AppManagerImpl) UvmContextFactory.context().appManager();
        LicenseManager lm = UvmContextFactory.context().licenseManager();

        /* This stores a list of installable apps. (for this rack) */
        Map<String, String> installableAppsMap = new HashMap<>();
        /* This stores a list of all licenses */
        Map<String, License> licenseMap = new HashMap<>();

        /**
         * Build the license map
         */
        List<App> visibleApps = nm.visibleApps(policyId);
        for (App app : visibleApps) {
            String n = app.getAppProperties().getName();
            //exactMatch = false so we accept any license that starts with n
            licenseMap.put(n, lm.getLicense(n, false));
        }

        /**
         * Build the rack state
         */
        Map<Long, AppSettings.AppState> runStates = nm.allAppStates();

        /**
         * Iterate through apps
         */
        for (AppProperties appProps : nm.getAllAppProperties()) {
            if (appProps.getInvisible()) continue;

            if (!checkArchitecture(appProps.getSupportedArchitectures())) {
                logger.debug("Hiding " + appProps.getDisplayName() + ". " + System.getProperty("os.arch") + " is not a supported architecture.");
                continue;
            }

            installableAppsMap.put(appProps.getDisplayName(), appProps.getName());
        }

        /**
         * Build the appMetrics (stats in the UI) Remove visible installableApps
         * from installableApps
         */
        Map<Long, List<AppMetric>> appMetrics = new HashMap<>(visibleApps.size());
        for (App visibleApp : visibleApps) {
            Long appId = visibleApp.getAppSettings().getId();
            Integer appPolicyId = visibleApp.getAppSettings().getPolicyId();
            appMetrics.put(appId, visibleApp.getMetrics());

            if (appPolicyId == null || appPolicyId.equals(policyId)) {
                installableAppsMap.remove(visibleApp.getAppProperties().getDisplayName());
            }
        }

        /**
         * SPECIAL CASE: Web Filter Lite is being deprecated - hide it
         */
        installableAppsMap.remove("Web Filter Lite"); /*
                                                       * hide web filter lite
                                                       * from left hand nav
                                                       */

        /**
         * SPECIAL CASE: If Web Filter is installed in this rack OR licensed for
         * non-trial, hide Web Monitor
         */
        List<App> webFilterApps = UvmContextFactory.context().appManager().appInstances("web-filter", policyId);
        if (webFilterApps != null && webFilterApps.size() > 0) {
            installableAppsMap.remove("Web Monitor"); /*
                                                       * hide web monitor from
                                                       * left hand nav
                                                       */
        }
        if (!UvmContextFactory.context().isDevel()) {
            License webFilterLicense = lm.getLicense(License.WEB_FILTER);
            if (webFilterLicense != null && webFilterLicense.getValid() && !webFilterLicense.getTrial()) {
                installableAppsMap.remove("Web Monitor"); /*
                                                           * hide web monitor
                                                           * from left hand nav
                                                           */
            }
        }

        /**
         * SPECIAL CASE: If Spam Blocker is installed in this rack OR licensed
         * for non-trial, hide Spam Blocker Lite
         */
        List<App> spamBlockerApps = UvmContextFactory.context().appManager().appInstances("spam-blocker", policyId);
        if (spamBlockerApps != null && spamBlockerApps.size() > 0) {
            installableAppsMap.remove("Spam Blocker Lite"); /*
                                                             * hide spam blocker
                                                             * lite from left
                                                             * hand nav
                                                             */
        }
        if (!UvmContextFactory.context().isDevel()) {
            License spamBlockerLicense = lm.getLicense(License.SPAM_BLOCKER);
            if (spamBlockerLicense != null && spamBlockerLicense.getValid() && !spamBlockerLicense.getTrial()) {
                installableAppsMap.remove("Spam Blocker Lite"); /*
                                                                 * hide spam
                                                                 * blocker lite
                                                                 * from left
                                                                 * hand nav
                                                                 */
            }
        }

        /**
         * Build the list of apps to show on the left hand nav
         */
        logger.debug("Building apps panel:");
        List<String> installableApps = new ArrayList<>(installableAppsMap.values());
        Collections.sort(installableApps);

        List<AppProperties> appProperties = new LinkedList<>();
        for (AppProperties appProps : nm.getAllAppProperties()) {
            if (!appProps.getInvisible()) { /* add only visible apps */
                appProperties.add(appProps);
            }
        }
        List<AppSettings> appSettings = new LinkedList<>();
        for (App app : visibleApps) {
            appSettings.add(app.getAppSettings());
        }

        return new AppsView(policyId, installableApps, appSettings, appProperties, appMetrics, licenseMap, runStates);
    }

    /**
     * Get an array of the AppsView for all policies
     * 
     * @return The array of AppsView objects
     */
    public AppsView[] getAppsViews()
    {
        PolicyManager policyManager = (PolicyManager) UvmContextFactory.context().appManager().app("policy-manager");

        if (policyManager == null) {
            AppsView[] views = new AppsView[] { getAppsView(1) };
            return views;
        }

        int policyIds[] = policyManager.getPolicyIds();
        AppsView[] views = new AppsView[policyIds.length];

        for (int i = 0; i < policyIds.length; i++) {
            views[i] = getAppsView(policyIds[i]);
        }

        return views;
    }

    /**
     * Called during initialization
     */
    protected void init()
    {
        loadSettings();

        restartUnloaded();

        if (logger.isDebugEnabled()) {
            logger.debug("Fininshed restarting apps:");
            for (App app : loadedAppsMap.values()) {
                logger.info(app.getAppSettings().getId() + " " + app.getAppSettings().getAppName());
            }
        }

        startAutoLoad();

        logger.info("Initialized AppManager");
    }

    /**
     * Called during shutdown
     */
    protected synchronized void destroy()
    {
        List<Runnable> tasks = new ArrayList<>();

        for (final App app : loadedAppsMap.values()) {
            Runnable r = new Runnable()
            {
                /**
                 * The runnable function
                 */
                public void run()
                {
                    String name = app.getAppProperties().getName();
                    Long id = app.getAppSettings().getId();

                    logger.info("Stopping  : " + name + " [" + id + "]");

                    long startTime = System.currentTimeMillis();
                    ((AppBase) app).stopIfRunning();
                    long endTime = System.currentTimeMillis();

                    logger.info("Stopped   : " + name + " [" + id + "] [" + (((float) (endTime - startTime)) / 1000.0f) + " seconds]");

                    loadedAppsMap.remove(app.getAppSettings().getId());
                }
            };
            tasks.add(r);
        }

        List<Thread> threads = new ArrayList<>(tasks.size());
        try {
            for (Iterator<Runnable> taskIterator = tasks.iterator(); taskIterator.hasNext();) {
                Thread t = UvmContextFactory.context().newThread(taskIterator.next(), "STOP_THREAD");
                threads.add(t);
                t.start();
            }
            // Must wait for them to start before we can go on to next wave.
            for (Thread t : threads)
                t.join();
        } catch (InterruptedException exn) {
            logger.error("Interrupted while starting apps");
        }

        logger.info("AppManager destroyed");
    }

    /**
     * Called to start apps marked for autoload
     */
    protected void startAutoLoad()
    {
        for (AppProperties appProps : getAllAppProperties()) {
            if (!appProps.getAutoLoad()) continue;

            List<App> list = appInstances(appProps.getName());

            /**
             * If a app is "autoLoad" and is not loaded, instantiate it
             */
            if (list.size() == 0) {
                try {
                    logger.info("Auto-loading new app: " + appProps.getName());
                    App app = instantiate(appProps.getName());

                    if (appProps.getAutoStart()) {
                        app.start();
                    }

                } catch (Exception exn) {
                    logger.warn("could not deploy: " + appProps.getName(), exn);
                    continue;
                }
            }
        }
    }

    /**
     * Find all application properties files
     * 
     * @param appProps
     *        The list of properties
     * @param searchDir
     *        The search directory
     */
    private void findAllAppProperties(List<AppProperties> appProps, File searchDir)
    {
        if (!searchDir.exists()) return;

        File[] fileList = searchDir.listFiles();
        if (fileList == null) return;

        for (File f : fileList) {
            if (f.isDirectory()) {
                findAllAppProperties(appProps, f);
            } else {
                if ("appProperties.json".equals(f.getName())) {
                    try {
                        AppProperties np = getAppPropertiesFilename(f.getAbsolutePath());
                        appProps.add(np);
                    } catch (Exception e) {
                        logger.warn("Ignoring bad app properties: " + f.getAbsolutePath(), e);
                    }
                }
            }

        }
    }

    /**
     * Restart unloaded applications
     */
    private void restartUnloaded()
    {
        long t0 = System.currentTimeMillis();
        int passCount = 0;

        if (!live) {
            throw new RuntimeException("AppManager is shut down");
        }

        logger.info("Restarting unloaded apps...");

        for (AppSettings appSettings : settings.getApps()) {
            logger.debug("Restarting unloaded apps: " + appSettings.getAppName() + " [" + appSettings.getId() + "]");
            if (unloadedAppsMap.get(appSettings.getId()) != null) {
                logger.error("DUPLICATE APP ID: " + appSettings.getId());
                logger.error("DUPLICATE APPS: " + unloadedAppsMap.get(appSettings.getId()).getAppName() + " " + appSettings.getAppName());
            } else {
                unloadedAppsMap.put(appSettings.getId(), appSettings);
            }
        }

        while (unloadedAppsMap.size() > 0 || appsBeingLoaded.size() > 0) {
            passCount++;
            List<AppSettings> loadable = getLoadable();
            String appsLoadingStr = "";
            if (appsBeingLoaded.size() > 0) {
                appsLoadingStr = " loading: ";
                for (Long appId : appsBeingLoaded.keySet()) {
                    appsLoadingStr += appId + " ";
                }
            }
            logger.info("Loading pass[" + passCount + "]: " + "loadable.size(): " + loadable.size() + " unloadedAppsMap.size(): " + unloadedAppsMap.size() + " appsBeingLoaded: " + appsBeingLoaded.size() + appsLoadingStr);

            if (appsBeingLoaded.size() < 1 && loadable.size() == 0 && unloadedAppsMap.size() > 0) {
                // if nothing is being loaded and nothing is loadeable but there is more to be loaded
                // then something is wrong. This should never happen
                logger.error("No apps loadable but not finished! Continuing...");
                break;
            }

            if (loadable.size() > 0) {
                for (AppSettings ns : loadable)
                    logger.info("Loading in this pass[" + passCount + "]: " + ns.getAppName() + " [" + ns.getId() + "]");
                startUnloaded(loadable);
            }

            try {
                startSemaphore.tryAcquire(300, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                continue;
            }
        }
        logger.info("Loaded! pass[" + passCount + "]: unloadedAppsMap.size(): " + unloadedAppsMap.size() + " appsBeingLoaded: " + appsBeingLoaded.size());

        long t1 = System.currentTimeMillis();
        logger.info("Time to restart apps: " + (t1 - t0) + " millis");
    }

    private static int startThreadNum = 0;

    /**
     * Start unloaded applications
     * 
     * @param startQueue
     *        The start queue
     */
    private void startUnloaded(List<AppSettings> startQueue)
    {
        List<Runnable> restarters = new ArrayList<>(startQueue.size());

        for (final AppSettings appSettings : startQueue) {
            final String name = appSettings.getAppName();
            final AppProperties appProps = getAppProperties(appSettings);

            if (name == null) {
                logger.error("Unable to load app \"" + name + "\": NULL name.");
            } else if (appProps == null) {
                logger.error("Unable to load app \"" + name + "\": NULL app properties.");
            } else {
                Runnable r = new Runnable()
                {
                    /**
                     * The runnable function
                     */
                    public void run()
                    {
                        AppBase app = null;
                        try {
                            logger.info("Restarting: " + name + " [" + appSettings.getId() + "]");
                            long startTime = System.currentTimeMillis();
                            app = (AppBase) AppBase.loadClass(appProps, appSettings, false);
                            long endTime = System.currentTimeMillis();
                            logger.info("Restarted : " + name + " [" + appSettings.getId() + "] [" + (((float) (endTime - startTime)) / 1000.0f) + " seconds]");

                            // add to loaded apps
                            loadedAppsMap.put(appSettings.getId(), app);

                        } catch (Exception exn) {
                            logger.error("Could not restart: " + name, exn);
                        } catch (LinkageError err) {
                            logger.error("Could not restart: " + name, err);
                        } finally {

                            // alert the main thread that a app is done loading
                            appsBeingLoaded.remove(appSettings.getId());
                            startSemaphore.release();

                        }
                        if (app == null) {
                            logger.error("Failed to load app:" + name);
                            loadedAppsMap.remove(appSettings);
                        }
                    }
                };
                // remove from unloaded apps
                appsBeingLoaded.put(appSettings.getId(), appProps);
                unloadedAppsMap.remove(appSettings.getId());

                restarters.add(r);
            }
        }

        List<Thread> threads = new ArrayList<>(restarters.size());

        for (Iterator<Runnable> iter = restarters.iterator(); iter.hasNext();) {
            Thread t = UvmContextFactory.context().newThread(iter.next(), "START_" + startThreadNum++);
            threads.add(t);
            t.start();
        }
    }

    /**
     * Get list of loadable applications
     * 
     * @return The list
     */
    private List<AppSettings> getLoadable()
    {
        List<AppSettings> loadable = new ArrayList<>(unloadedAppsMap.size());
        Set<String> thisPass = new HashSet<>(unloadedAppsMap.size());

        for (Iterator<AppSettings> i = unloadedAppsMap.values().iterator(); i.hasNext();) {
            AppSettings appSettings = i.next();
            if (appSettings == null) {
                logger.error("Invalid settings: " + appSettings);
                i.remove(); // remove from unloadedAppsMap because we can never load this one
                continue;
            }
            logger.debug("Checking loadable status for " + appSettings.getAppName() + "...");
            String name = appSettings.getAppName();
            if (name == null) {
                logger.error("Missing name for: " + appSettings);
                i.remove(); // remove from unloadedAppsMap because we can never load this one
                continue;
            }
            AppProperties appProps = getAppProperties(name);
            if (appProps == null) {
                logger.error("Missing properties for: " + appSettings);
                i.remove(); // remove from unloadedAppsMap because we can never load this one
                continue;
            }

            List<String> parents = appProps.getParents();
            boolean parentsLoaded = true;
            if (parents != null) {
                for (String parent : parents) {
                    if (!isLoaded(parent)) {
                        parentsLoaded = false;
                        break;
                    }
                }
            }

            // all parents loaded and another instance of this
            // app not loading this pass or already loaded in
            // previous pass (prevents classloader race).
            if (parentsLoaded && !thisPass.contains(name)) {
                logger.debug(appProps.getName() + " is loadable.");
                loadable.add(appSettings);
                thisPass.add(name);
            }
        }

        return loadable;
    }

    /**
     * Determine if an application is loaded
     * 
     * @param appName
     *        The application
     * @return True if loaded, otherwise false
     */
    private boolean isLoaded(String appName)
    {
        if (appName == null) {
            logger.warn("Invalid arguments");
            return false;
        }

        for (App n : loadedAppsMap.values()) {
            String name = n.getAppSettings().getAppName();
            if (appName.equals(name)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Load our settings
     * 
     * @return The settings
     */
    private AppManagerSettings loadSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        AppManagerSettings readSettings = null;
        String settingsFileName = APP_MANAGER_SETTINGS_FILE;

        try {
            readSettings = settingsManager.load(AppManagerSettings.class, settingsFileName);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:", e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            this.initializeSettings();
        } else {
            logger.debug("Loading Settings...");

            // UPDATE settings if necessary

            // look for and remove old apps that no longer exist
            LinkedList<AppSettings> cleanList = new LinkedList<>();
            for (AppSettings item : readSettings.getApps()) {
                if (item.getAppName().equals("webfilter-lite")) continue;
                if (item.getAppName().equals("ips")) continue;
                if (item.getAppName().equals("idps")) continue;
                cleanList.add(item);
            }

            // if we removed anything update the app list and save
            if (cleanList.size() != readSettings.getApps().size()) {
                readSettings.setApps(cleanList);
                this._setSettings(readSettings);
            }

            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        return this.settings;
    }

    /**
     * Initialize our setting
     */
    private void initializeSettings()
    {
        logger.info("Initializing Settings...");

        AppManagerSettings newSettings = new AppManagerSettings();

        this._setSettings(newSettings);
    }

    /**
     * Get AppProperties from the app settings
     * 
     * @param appSettings
     * @return
     */
    private AppProperties getAppProperties(AppSettings appSettings)
    {
        return getAppProperties(appSettings.getAppName());
    }

    /**
     * Get AppProperties from the app name (ie "firewall")
     * 
     * @param name
     *        The application name
     * @return The properties
     */
    private AppProperties getAppProperties(String name)
    {
        String fileName = System.getProperty("uvm.lib.dir") + "/" + name + "/" + "appProperties.json";
        return getAppPropertiesFilename(fileName);
    }

    /**
     * Get AppProperties from the full path file name
     * 
     * @param fileName
     *        The file name
     * @return The properties
     */
    private AppProperties getAppPropertiesFilename(String fileName)
    {
        AppProperties appProperties = null;

        try {
            appProperties = UvmContextFactory.context().settingsManager().load(AppProperties.class, fileName);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:", e);
        }

        return appProperties;
    }

    /**
     * Create new application settings
     * 
     * @param policyId
     *        The policy ID
     * @param appName
     *        The application name
     * @return The new settings
     */
    private AppSettings createNewAppSettings(Integer policyId, String appName)
    {
        long newAppId = settings.getNextAppId();

        /**
         * Keep incrementing until you get a free app ID This is just a safety
         * mechanism in case nextAppId gets lost This is a hack that we adde for
         * a messed up 13.0 conversion
         */
        App app = null;
        if (newAppId < appInstances().size()) newAppId = 100;
        while ((app = app(newAppId)) != null) {
            newAppId++;
        }

        /**
         * Increment the next app Id (not saved until later)
         */
        settings.setNextAppId(newAppId + 1);

        return new AppSettings(newAppId, policyId, appName);
    }

    /**
     * Save new application settings
     * 
     * @param appSettings
     *        The application settings
     */
    private void saveNewAppSettings(AppSettings appSettings)
    {
        List<AppSettings> apps = settings.getApps();
        apps.add(appSettings);
        _setSettings(settings);
        return;
    }

    /**
     * Get the list of parent policies
     * 
     * @param policyId
     *        The child policy
     * @return The list of parent policies
     */
    private List<Integer> getParentPolicies(Integer policyId)
    {
        PolicyManager policyManager = (PolicyManager) UvmContextFactory.context().appManager().app("policy-manager");
        List<Integer> parentList = new ArrayList<>();
        if (policyManager == null) return parentList;

        for (Integer parentId = policyManager.getParentPolicyId(policyId); parentId != null; parentId = policyManager.getParentPolicyId(parentId)) {
            parentList.add(parentId);
        }

        return parentList;
    }

    /**
     * Get the list of apps for a policy
     * 
     * @param policyId
     *        The policy ID
     * @return The list of apps
     */
    private List<App> getAppsForPolicy(Integer policyId)
    {
        return getAppsForPolicy(policyId, true);
    }

    /**
     * Get the list of apps for a policy
     * 
     * @param policyId
     *        The policy
     * @param parents
     *        True to include parents, otherwise false
     * @return The list of apps
     */
    private List<App> getAppsForPolicy(Integer policyId, boolean parents)
    {
        List<Integer> parentPolicies = null;

        if (parents && policyId != null) parentPolicies = getParentPolicies(policyId);
        else parentPolicies = new ArrayList<>();

        /*
         * This is a list of loadedAppsMap. Each index of the first list
         * corresponds to its policy in the policies array. Each index in the
         * second list is a appSettings of the apps in the policy
         * parentAppSettingsArray[0] == list of loadedAppsMap in
         * parentPolicies[0] parentAppSettingsArray[1] == list of loadedAppsMap
         * in parentPolicies[1] ... parentAppSettingsArray[n] == list of
         * loadedAppsMap in parentPolicies[n] Policies are ordered
         * parentAppSettingsArray[0] is the first parent, etc
         */
        List<List<App>> parentAppArray = new ArrayList<>(parentPolicies.size());
        List<App> thisPolicyApps = new ArrayList<>();
        for (int i = 0; i < parentPolicies.size(); i++) {
            parentAppArray.add(new ArrayList<App>());
        }

        /*
         * Fill in the inner list, at the end each of these is the list of apps
         * in the policy.
         */
        for (App app : loadedAppsMap.values()) {
            Integer appPolicyId = app.getAppSettings().getPolicyId();

            /**
             * If its in the parent policy list - add it Otherwise it its in the
             * policy - add it
             */
            int i = parentPolicies.indexOf(appPolicyId);
            if (i >= 0) {
                parentAppArray.get(i).add(app);
            } else if (appPolicyId == null && policyId == null) {
                thisPolicyApps.add(app);
            } else if (appPolicyId != null && policyId != null && appPolicyId.equals(policyId)) {
                thisPolicyApps.add(app);
            }
        }

        /*
         * Add all the loadedAppsMap from the current policy And all the apps
         * from the parent IFF they don't already exists will only add the first
         * entry (which will be most specific app.
         */
        List<App> finalList = thisPolicyApps;
        Set<String> names = new HashSet<>();

        for (App app : thisPolicyApps) {
            String n = app.getAppSettings().getAppName();
            if (!names.contains(n)) names.add(n);
        }
        for (List<App> parentPolicyList : parentAppArray) {
            if (parentPolicyList != null) {
                for (App app : parentPolicyList) {
                    String n = app.getAppSettings().getAppName();
                    if (!names.contains(n)) {
                        names.add(n);
                        finalList.add(app);
                    }
                }
            }
        }

        return finalList;
    }

    /**
     * Private function to save our settings
     * 
     * @param newSettings
     *        The new settings
     */
    private synchronized void _setSettings(AppManagerSettings newSettings)
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save(APP_MANAGER_SETTINGS_FILE, newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.", e);
            throw new RuntimeException(e);
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {
            logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));
        } catch (Exception e) {
        }
    }

    /**
     * Convert a list of apps to a list of app ID's
     * 
     * @param apps
     *        The list of apps
     * @return The list of ID's
     */
    private List<Long> appToIdList( List<App> apps )
    {
        if ( apps == null ) return null;
        List<Long> idList = apps.stream().map( a -> a.getAppSettings().getId() ).collect(Collectors.toCollection(ArrayList::new));
        return idList;
    }

    /**
     * Compare two policies
     * 
     * @param policyId1
     *        Policy one
     * @param policyId2
     *        Policy two
     * @return The comparison result
     */
    private boolean policyEquals(Integer policyId1, Integer policyId2)
    {
        return ((policyId1 == policyId2) || (policyId1 != null && policyId1.equals(policyId2)));
    }

    /**
     * Check to see if we are running on a supported architecture
     * 
     * @param supportedArchitectures
     *        List of supported architectures
     * @return True if current architecture is in the supplied list, otherwise
     *         false
     */
    private boolean checkArchitecture(List<String> supportedArchitectures)
    {
        boolean foundArch = false;
        String arch = System.getProperty("os.arch");

        if (supportedArchitectures == null) return true;

        for (String supportedArchitecture : supportedArchitectures) {
            if ("any".equals(supportedArchitecture)) {
                return true;
            }
            if (arch.equals(supportedArchitecture)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Function to fix names that were created on older versions of the product
     * to match the names used in the current version.
     * 
     * @param name
     *        The original name
     * @return The fixed name
     */
    private String fixupName(String name)
    {
        if (name == null) return null;
        name = name.replaceAll("untangle-node-", "").replaceAll("untangle-casing-", "");
        if (name.contains("untangle-base")) name = name.replaceAll("untangle-base-", "") + "-base";
        return name;
    }
}
