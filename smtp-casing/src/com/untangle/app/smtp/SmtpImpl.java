/**
 * $Id$
 */
package com.untangle.app.smtp;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.app.smtp.quarantine.Quarantine;
import com.untangle.app.smtp.quarantine.QuarantineMaintenenceView;
import com.untangle.app.smtp.quarantine.QuarantineAppView;
import com.untangle.app.smtp.quarantine.QuarantineSettings;
import com.untangle.app.smtp.quarantine.QuarantineUserView;
import com.untangle.app.smtp.safelist.SafelistAdminView;
import com.untangle.app.smtp.safelist.SafelistManager;
import com.untangle.app.smtp.safelist.SafelistManipulation;
import com.untangle.app.smtp.safelist.SafelistAppView;
import com.untangle.app.smtp.safelist.SafelistSettings;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.SessionEventHandler;
import com.untangle.uvm.vnet.ForkedEventHandler;

/**
 * SMTP app implementation.
 */
public class SmtpImpl extends AppBase implements MailExport
{
    private static final long ONE_GB = (1024L * 1024L * 1024L);

    // the safelist that applies to all users
    public static final String GLOBAL_SAFELIST_NAME = "GLOBAL";

    private final SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
    private final Logger logger = Logger.getLogger(SmtpImpl.class);

    private SessionEventHandler clientSideHandler = new ForkedEventHandler( new SmtpClientParserEventHandler(), new SmtpClientUnparserEventHandler() );
    private SessionEventHandler serverSideHandler = new ForkedEventHandler( new SmtpServerUnparserEventHandler(), new SmtpServerParserEventHandler() );
    
    private final PipelineConnector clientSideConnector = UvmContextFactory.context().pipelineFoundry().create( "smtp-client-side", this, null, clientSideHandler, Fitting.SMTP_STREAM, Fitting.SMTP_TOKENS, Affinity.CLIENT, -1000, false, null );
    private final PipelineConnector serverSideConnector = UvmContextFactory.context().pipelineFoundry().create( "smtp-server-side", this, null, serverSideHandler, Fitting.SMTP_TOKENS, Fitting.SMTP_STREAM, Affinity.SERVER,  1000, false, "smtp-client-side" );
    private final PipelineConnector[] connectors = new PipelineConnector[] { clientSideConnector, serverSideConnector };
    
    private SmtpSettings settings;

    private static Quarantine quarantine;
    private static SafelistManager safelistMangr;

    private static boolean deployedWebApp = false;

    // constructors -----------------------------------------------------------

    /**
     * Initialzie SmtpImpl.
     * @param  appSettings   Application settings.
     * @param  appProperties Application properties.
     * @return               Instance of SmtpImpl.
     */
    public SmtpImpl( com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super(appSettings, appProperties);

        createSingletonsIfRequired();

        MailExportFactory.factory().registerExport(this);
    }

    /**
     * Create quarantine and SafelistManager instances.
     */
    private static synchronized void createSingletonsIfRequired()
    {
        if ( quarantine == null) {
            quarantine = new Quarantine();
        }
        if (safelistMangr == null) {
            safelistMangr = new SafelistManager( quarantine );
        }
    }

    /**
     * Create quarantine web instance.
     * @param logger Logger to use.
     */
    private static synchronized void deployWebAppIfRequired( Logger logger )
    {
        if ( ! deployedWebApp ) {
            if (null != UvmContextFactory.context().tomcatManager().loadServlet("/quarantine", "quarantine")) {
                logger.debug("Deployed Quarantine web app");
            } else {
                logger.error("Unable to deploy Quarantine web app");
            }
            deployedWebApp = true;
        }
    }

    /**
     * Remove quarantine web instance.
     * @param logger Logger to use.
     */
    private static synchronized void unDeployWebAppIfRequired( Logger logger )
    {
        if ( deployedWebApp ) {
            if (UvmContextFactory.context().tomcatManager().unloadServlet("/quarantine")) {
                logger.debug("Unloaded Quarantine web app");
            } else {
                logger.error("Unable to unload Quarantine web app");
            }
            deployedWebApp = false;
        }
    }

    /**
     * Set default SMTP settings.
     */
    private void initializeSmtpSettings()
    {
        SmtpSettings ns = new SmtpSettings();
        ns.setSmtpEnabled(true);
        ns.setSmtpTimeout(1000 * 60 * 4);

        QuarantineSettings qs = new QuarantineSettings();
        qs.setDigestHourOfDay(6); // 6 am
        qs.setDigestMinuteOfDay(0); // 6 am
        byte[] binaryKey = new byte[8];
        new java.util.Random().nextBytes(binaryKey);
        qs.initBinaryKey(binaryKey);
        qs.setMaxMailIntern(QuarantineSettings.WEEK * 2);
        qs.setMaxIdleInbox(QuarantineSettings.WEEK * 4);
        ns.setQuarantineSettings(qs);

        ArrayList<SafelistSettings> ss = new ArrayList<SafelistSettings>();

        ns.setSafelistSettings(ss);

        setSmtpSettings(ns);
    }

    // SmtpApp methods --------------------------------------------------------

    /**
     * Return current SMTP settings.
     * @return SmtpSettings of current SMTP settings.
     */
    public SmtpSettings getSmtpSettings()
    {
        return settings;
    }

    /**
     * Set SMTP settings.
     * @param newSettings New SmtpSettings to use.
     */
    public void setSmtpSettings(final SmtpSettings newSettings)
    {
        String appID = this.getAppSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/smtp/settings_" + appID
                + ".js";

        try {
            settingsManager.save( settingsFile, newSettings );
        } catch (Exception exn) {
            logger.error("setSmtpSettings()", exn);
            return;
        }

        this.settings = newSettings;

        reconfigure();
        SmtpImpl.quarantine.setSettings(this, settings.getQuarantineSettings());
        SmtpImpl.safelistMangr.setSettings(this, settings);
    }

    /**
     * Set SMTP settings withou safelists.
     * @param settings New SmtpSettings to use.
     */
    public void setSmtpSettingsWithoutSafelists(final SmtpSettings settings)
    {
        // if the settings object being passed does not have a valid secret
        // key then we copy the key from the existing settings
        if (settings.getQuarantineSettings().getSecretKey() == null)
            settings.getQuarantineSettings().initBinaryKey(this.settings.getQuarantineSettings().grabBinaryKey());

        // get initial safelists
        settings.setSafelistSettings(this.settings.getSafelistSettings());
        setSmtpSettings(settings);
    }

    /**
     * Return quarantine instance.
     * @return QuarantineUserView instance.
     */
    public QuarantineUserView getQuarantineUserView()
    {
        return SmtpImpl.quarantine;
    }

    /**
     * Return quarantine maintenance instance.
     * @return QuarantineMaintenenceView instance.
     */
    public QuarantineMaintenenceView getQuarantineMaintenenceView()
    {
        return SmtpImpl.quarantine;
    }

    /**
     * Return SafelistManipulation instance.
     * @return SafelistManipulation instance.
     */
    public SafelistManipulation getSafelistManipulation()
    {
        return SmtpImpl.safelistMangr;
    }

    /**
     * Return SafelistAdminView instance.
     * @return SafelistAdminView instance.
     */
    public SafelistAdminView getSafelistAdminView()
    {
        return SmtpImpl.safelistMangr;
    }

    /**
     * Send quarantine digests.
     */
    public void sendQuarantineDigests()
    {
        if ( SmtpImpl.quarantine != null )
            SmtpImpl.quarantine.sendQuarantineDigests();
    }

    /**
     * Return minimum allocated store size (1 GB).
     * @param  inGB if true, compare in GB, otherwise in bytes.
     * @return      Size in GB.
     */
    public long getMinAllocatedStoreSize(boolean inGB)
    {
        if (false == inGB) {
            return ONE_GB;
        }
        return ONE_GB / ONE_GB;
    }

    /**
     * Return maximum allocated store size (30 GB).
     * @param  inGB if true, compare in GB, otherwise in bytes.
     * @return      Size in GB.
     */
    public long getMaxAllocatedStoreSize(boolean inGB)
    {
        if (false == inGB) {
            return (30 * ONE_GB);
        }
        return 30 * (ONE_GB / ONE_GB);
    }

    /**
     * Initialize authentication token for an account.
     * @param  account Account to create token for.
     * @return         String of token.
     */
    public String createAuthToken(String account)
    {
        return SmtpImpl.quarantine.createAuthToken(account);
    }

    // MailExport methods -----------------------------------------------------

    /**
     * Return SMTP settings.
     * @return SmtpSettings.
     */
    public SmtpSettings getExportSettings()
    {
        return getSmtpSettings();
    }

    /**
     * Return QuarantineAppView.
     * @return QuarantineAppView instance.
     */
    public QuarantineAppView getQuarantineAppView()
    {
        return SmtpImpl.quarantine;
    }

    /**
     * Return SafelistAppView.
     * @return SafelistAppView instance.
     */
    public SafelistAppView getSafelistAppView()
    {
        return SmtpImpl.safelistMangr;
    }

    /**
     * Reconfigure SMTP.
     */
    private void reconfigure()
    {
        if ( settings != null ) {
            for ( PipelineConnector connector : this.connectors ) 
                connector.setEnabled( settings.isSmtpEnabled() );
        }
    }

    // App methods -----------------------------------------------------------

    /**
     * Handle app predestroy.
     *
     * * Remove web app
     * * Close quarantine.
     */
    @Override
    protected void preDestroy()
    {
        super.preDestroy();

        logger.debug("preDestroy()");

        unDeployWebAppIfRequired(logger);

        SmtpImpl.quarantine.close();
    }

    /**
     * Post app initialization routines.
     */
    protected void postInit()
    {
        String appID = this.getAppSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/smtp/settings_" + appID + ".js";

        SmtpSettings readSettings = null;
        logger.info("Loading settings from " + settingsFile);

        try {
            // first we try to read our json settings
            readSettings = settingsManager.load(SmtpSettings.class, settingsFile);
        } catch (Exception exn) {
            logger.error("postInit()", exn);
        }

        try {
            // still no settings found so init with defaults
            if (readSettings == null) {
                logger.warn("No settings found... initializing with defaults");
                initializeSmtpSettings();
            }
            // otherwise apply the loaded or imported settings from the file
            else {
                logger.info("Loaded settings from " + settingsFile);

                settings = readSettings;
                SmtpImpl.quarantine.setSettings(this, settings.getQuarantineSettings());
                SmtpImpl.safelistMangr.setSettings(this, settings);
                reconfigure();
            }
        } catch (Exception exn) {
            logger.error("Could not apply app settings", exn);
        }

        // At this point the settings have either been loaded from disk
        // or initialized to defaults so now we do all the other setup
        try {
            // create the safelist that applies to all
            SmtpImpl.safelistMangr.createSafelist( GLOBAL_SAFELIST_NAME );
        } catch (Exception exn) {
            logger.error("Could not create global safelist", exn);
        }

        deployWebAppIfRequired(logger);
        SmtpImpl.quarantine.open();
    }

    /**
     * Get the pineliene connector.
     *
     * @return PipelineConector
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Return smtp settings.
     * @return Smtp Settings object.
     */
    public Object getSettings()
    {
        return getSmtpSettings();
    }

    /**
     * Write smtp settings.
     * @param settings Object of SMTP settings.
     */
    public void setSettings(Object settings)
    {
        setSmtpSettings((SmtpSettings) settings);
    }

    /**
     * Run SMTP app tests.
     * @return Result of tests.
     */
    public String runTests()
    {
        try {
            return runTests(System.getProperty("uvm.lib.dir") + "/" + getAppProperties().getName());
        } catch (Exception e) {
            return e.toString();
        }
    }
    
    /**
     * Get tests to run.
     * @return List of test names.
     */
    public List<String> getTests()
    {
        try {
            return getTests(System.getProperty("uvm.lib.dir") + "/" + getAppProperties().getName());
        } catch (Exception e) {
            return new ArrayList<String>();
        }
    }
    
    /**
     * Get tests to run from a path.
     * @param path String of path to use.
     * @return List of test names.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public List<String> getTests(String path)
    {
        List<String> result = new ArrayList<String>();
        try {
            File test = new File(path);
            if (test.isDirectory()) {
                //boolean success = true;
                for (File f : test.listFiles()) {
                    //System.out.println(f.getAbsolutePath());
                    result.addAll(getTests(f.getAbsolutePath()));
                }
            } else {
                if (test.getName().endsWith(".class")) {
                    String name = test.getAbsolutePath().substring(test.getAbsolutePath().lastIndexOf("com.untangle.app"));
                    name = name.substring(0, name.length() - 6);
                    name = name.replaceAll("/", ".");
                    Class cls = getClass().getClassLoader().loadClass(name);
                    Method method = null;
                    try {
                        method = cls.getMethod("runTest", String[].class);
                        result.add(test.getAbsolutePath());
                    } catch (NoSuchMethodException e) {
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception:",e);
        }
        return result;
    }

    /**
     * Run tests from a path.
     * @param path String of path to use.
     * @return Result of tests.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public String runTests(String path)
    {
        String result = "";
        try {
            File test = new File(path);
            if (test.isDirectory()) {
                //boolean success = true;
                for (File f : test.listFiles()) {
                    //System.out.println(f.getAbsolutePath());
                    result = result + runTests(f.getAbsolutePath());
                }
            } else {
                if (test.getName().endsWith(".class")) {
                    String name = test.getAbsolutePath().substring(test.getAbsolutePath().lastIndexOf("com.untangle.app"));
                    name = name.substring(0, name.length() - 6);
                    name = name.replaceAll("/", ".");
                    Class cls = getClass().getClassLoader().loadClass(name);
                    Method method = null;
                    try {
                        method = cls.getMethod("runTest", String[].class);
                        String[] args = { "" };
                        String testResult = (String)method.invoke(cls, (Object) args);
                        result = "\n ------- " + name + " -------\n ";
                        result += testResult;
                        // return (Boolean) result;
                    } catch (NoSuchMethodException e) {
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception:",e);
            
            for ( Throwable cause = e.getCause() ; cause != null ; cause = cause.getCause()) {
                logger.warn( "Cause:", cause );
            }
        }
        return result;
    }

}
