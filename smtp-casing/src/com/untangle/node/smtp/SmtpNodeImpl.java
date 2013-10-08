/**
 * $Id: SmtpNodeImpl.java 35194 2013-07-01 18:58:44Z dmorris $
 */
package com.untangle.node.smtp;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.quarantine.Quarantine;
import com.untangle.node.smtp.quarantine.QuarantineMaintenenceView;
import com.untangle.node.smtp.quarantine.QuarantineNodeView;
import com.untangle.node.smtp.quarantine.QuarantineSettings;
import com.untangle.node.smtp.quarantine.QuarantineUserView;
import com.untangle.node.smtp.safelist.SafelistAdminView;
import com.untangle.node.smtp.safelist.SafelistManager;
import com.untangle.node.smtp.safelist.SafelistManipulation;
import com.untangle.node.smtp.safelist.SafelistNodeView;
import com.untangle.node.smtp.safelist.SafelistSettings;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.CasingPipeSpec;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.PipeSpec;

public class SmtpNodeImpl extends NodeBase implements SmtpNode, MailExport
{
    public static final String PROTOCOL_NAME = "smtp";

    // the safelist that applies to all users
    public static final String GLOBAL_SAFELIST_NAME = "GLOBAL";

    private final SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
    private final Logger logger = Logger.getLogger(SmtpNodeImpl.class);

    private final CasingPipeSpec SMTP_PIPE_SPEC = new CasingPipeSpec(PROTOCOL_NAME, this, SmtpCasingFactory.factory(),
            Fitting.SMTP_STREAM, Fitting.SMTP_TOKENS);

    private final PipeSpec[] pipeSpecs = new PipeSpec[] { SMTP_PIPE_SPEC };

    private SmtpNodeSettings settings;
    private static Quarantine s_quarantine;// This will never be null for
                                           // *instances* of SmtpNodeImpl
    private static final long ONE_GB = (1024L * 1024L * 1024L);

    private static SafelistManager s_safelistMngr;
    private static boolean s_deployedWebApp = false;
    private static boolean s_unDeployedWebApp = false;

    // constructors -----------------------------------------------------------

    public SmtpNodeImpl(com.untangle.uvm.node.NodeSettings nodeSettings,
            com.untangle.uvm.node.NodeProperties nodeProperties) {
        super(nodeSettings, nodeProperties);

        createSingletonsIfRequired();

        MailExportFactory.factory().registerExport(this);
    }

    private static synchronized void createSingletonsIfRequired()
    {
        if (s_quarantine == null) {
            s_quarantine = new Quarantine();
        }
        if (s_safelistMngr == null) {
            s_safelistMngr = new SafelistManager(s_quarantine);
        }
    }

    private static synchronized void deployWebAppIfRequired(Logger logger)
    {
        if (!s_deployedWebApp) {
            if (null != UvmContextFactory.context().tomcatManager().loadServlet("/quarantine", "quarantine")) {
                logger.debug("Deployed Quarantine web app");
            } else {
                logger.error("Unable to deploy Quarantine web app");
            }
            s_deployedWebApp = true;
        }
    }

    private static synchronized void unDeployWebAppIfRequired(Logger logger)
    {
        if (!s_unDeployedWebApp) {
            if (UvmContextFactory.context().tomcatManager().unloadServlet("/quarantine")) {
                logger.debug("Unloaded Quarantine web app");
            } else {
                logger.error("Unable to unload Quarantine web app");
            }
            s_unDeployedWebApp = true;
        }
    }

    private void initializeSmtpNodeSettings()
    {
        SmtpNodeSettings ns = new SmtpNodeSettings();
        ns.setSmtpEnabled(true);
        ns.setSmtpTimeout(1000 * 60 * 4);
        ns.setSmtpAllowTLS(false);

        QuarantineSettings qs = new QuarantineSettings();
        qs.setMaxQuarantineTotalSz(10 * ONE_GB); // 10GB
        qs.setDigestHourOfDay(6); // 6 am
        qs.setDigestMinuteOfDay(0); // 6 am
        byte[] binaryKey = new byte[4];
        new java.util.Random().nextBytes(binaryKey);
        qs.initBinaryKey(binaryKey);
        qs.setMaxMailIntern(QuarantineSettings.WEEK * 2);
        qs.setMaxIdleInbox(QuarantineSettings.WEEK * 4);
        ns.setQuarantineSettings(qs);

        ArrayList<SafelistSettings> ss = new ArrayList<SafelistSettings>();

        ns.setSafelistSettings(ss);

        setSmtpNodeSettings(ns);
    }

    // SmtpNode methods --------------------------------------------------------

    public SmtpNodeSettings getSmtpNodeSettings()
    {
        return settings;
    }

    public void setSmtpNodeSettings(final SmtpNodeSettings newSettings)
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-casing-smtp/settings_" + nodeID
                + ".js";

        try {
            settingsManager.save(SmtpNodeSettings.class, settingsFile, newSettings);
        } catch (Exception exn) {
            logger.error("setSmtpNodeSettings()", exn);
            return;
        }

        this.settings = newSettings;

        reconfigure();
        s_quarantine.setSettings(this, settings.getQuarantineSettings());
        s_safelistMngr.setSettings(this, settings);
    }

    public void setSmtpNodeSettingsWithoutSafelists(final SmtpNodeSettings settings)
    {
        // if the settings object being passed does not have a valid secret
        // key then we copy the key from the existing settings
        if (settings.getQuarantineSettings().getSecretKey() == null)
            settings.getQuarantineSettings().initBinaryKey(this.settings.getQuarantineSettings().grabBinaryKey());

        // get initial safelists
        settings.setSafelistSettings(this.settings.getSafelistSettings());
        setSmtpNodeSettings(settings);
    }

    public QuarantineUserView getQuarantineUserView()
    {
        return s_quarantine;
    }

    public QuarantineMaintenenceView getQuarantineMaintenenceView()
    {
        return s_quarantine;
    }

    public SafelistManipulation getSafelistManipulation()
    {
        return s_safelistMngr;
    }

    public SafelistAdminView getSafelistAdminView()
    {
        return s_safelistMngr;
    }

    public long getMinAllocatedStoreSize(boolean inGB)
    {
        if (false == inGB) {
            return ONE_GB;
        }
        return ONE_GB / ONE_GB;
    }

    // max is arbitrarily set to 30 GB
    public long getMaxAllocatedStoreSize(boolean inGB)
    {
        if (false == inGB) {
            return (30 * ONE_GB);
        }
        return 30 * (ONE_GB / ONE_GB);
    }

    public String createAuthToken(String account)
    {
        return s_quarantine.createAuthToken(account);
    }

    // MailExport methods -----------------------------------------------------

    public SmtpNodeSettings getExportSettings()
    {
        return getSmtpNodeSettings();
    }

    public QuarantineNodeView getQuarantineNodeView()
    {
        return s_quarantine;
    }

    public SafelistNodeView getSafelistNodeView()
    {
        return s_safelistMngr;
    }

    private void reconfigure()
    {
        SMTP_PIPE_SPEC.setEnabled(settings.isSmtpEnabled());
    }

    // Node methods -----------------------------------------------------------

    @Override
    protected void preDestroy()
    {
        super.preDestroy();
        logger.debug("preDestroy()");
        unDeployWebAppIfRequired(logger);
        s_quarantine.close();
    }

    protected void postInit()
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-casing-smtp/settings_" + nodeID
                + ".js";

        SmtpNodeSettings readSettings = null;
        logger.info("Loading settings from " + settingsFile);

        try {
            // first we try to read our json settings
            readSettings = settingsManager.load(SmtpNodeSettings.class, settingsFile);
        } catch (Exception exn) {
            logger.error("postInit()", exn);
        }

        try {
            // still no settings found so init with defaults
            if (readSettings == null) {
                logger.warn("No settings found... initializing with defaults");
                initializeSmtpNodeSettings();
            }
            // otherwise apply the loaded or imported settings from the file
            else {
                logger.info("Loaded settings from " + settingsFile);

                settings = readSettings;
                s_quarantine.setSettings(this, settings.getQuarantineSettings());
                s_safelistMngr.setSettings(this, settings);
                reconfigure();
            }
        } catch (Exception exn) {
            logger.error("Could not apply node settings", exn);
        }

        // At this point the settings have either been loaded from disk
        // or initialized to defaults so now we do all the other setup
        try {
            // create the safelist that applies to all
            s_safelistMngr.createSafelist("GLOBAL");
        } catch (Exception exn) {
            logger.error("Could not create global safelist", exn);
        }

        deployWebAppIfRequired(logger);
        s_quarantine.open();
    }

    // NodeBase methods ---------------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    public Object getSettings()
    {
        return getSmtpNodeSettings();
    }

    public void setSettings(Object settings)
    {
        setSmtpNodeSettings((SmtpNodeSettings) settings);
    }

    public String runTests()
    {
        try {
            return runTests(System.getProperty("uvm.lib.dir") + "/" + getNodeProperties().getName());
        } catch (Exception e) {
            return e.toString();
        }
    }
    
    @Override
    public List<String> getTests()
    {
        try {
            return getTests(System.getProperty("uvm.lib.dir") + "/" + getNodeProperties().getName());
        } catch (Exception e) {
            return new ArrayList<String>();
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getTests(String path)
    {
        List<String> result = new ArrayList<String>();
        try {
            File test = new File(path);
            if (test.isDirectory()) {
                //boolean success = true;
                for (File f : test.listFiles()) {
                    System.out.println(f.getAbsolutePath());
                    result.addAll(getTests(f.getAbsolutePath()));
                }
            } else {
                if (test.getName().endsWith(".class")) {
                    String name = test.getAbsolutePath().substring(
                            test.getAbsolutePath().lastIndexOf("com/untangle/node"));
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
            e.printStackTrace();
        }
        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public String runTests(String path)
    {
        String result = "";
        try {
            File test = new File(path);
            if (test.isDirectory()) {
                //boolean success = true;
                for (File f : test.listFiles()) {
                    System.out.println(f.getAbsolutePath());
                    result = result + runTests(f.getAbsolutePath());
                }
            } else {
                if (test.getName().endsWith(".class")) {
                    String name = test.getAbsolutePath().substring(
                            test.getAbsolutePath().lastIndexOf("com/untangle/node"));
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
            e.printStackTrace();
        }
        return result;
    }

}
