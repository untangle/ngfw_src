/*
 * $Id$
 */
package com.untangle.node.cpd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.catalina.Valve;
import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.node.cpd.CPDSettings.AuthenticationType;
import com.untangle.uvm.LocalAppServerManager;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmException;
import com.untangle.uvm.UvmState;
import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.IntfMatcher;
import com.untangle.uvm.node.IPMatcher;
import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.OutsideValve;
import com.untangle.uvm.util.JsonClient.ConnectionException;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.SettingsManager;

public class CPDImpl extends NodeBase implements CPD
{
    private static final String SETTINGS_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/cpd-convert-settings.py";
    private static int deployCount = 0;

    private final CustomUploadHandler uploadHandler = new CustomUploadHandler();
    private final Logger logger = Logger.getLogger(CPDImpl.class);

    private final PipeSpec[] pipeSpecs;

    private final CPDManager manager = new CPDManager(this);

    private static final String STAT_BLOCK = "block";
    private static final String STAT_AUTHORIZE = "authorize";

    public static final String DEFAULT_USERNAME = "captive portal user";

    private CPDIpUsernameMapAssistant assistant = null;

    private CPDSettings settings;

    private EventLogQuery loginEventQuery;
    private EventLogQuery blockEventQuery;

    private final SettingsManager settingsManager = UvmContextFactory.context().settingsManager();

    public CPDImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        this.loginEventQuery = new EventLogQuery(I18nUtil.marktr("Login Events"),
                                                 "SELECT * FROM reports.n_cpd_login_events evt ORDER BY time_stamp DESC");

        this.blockEventQuery = new EventLogQuery(I18nUtil.marktr("Block Events"),
                                                 "SELECT * FROM reports.n_cpd_block_events evt ORDER BY time_stamp DESC");

        this.settings = new CPDSettings();
        this.pipeSpecs = new PipeSpec[0];

        this.addMetric(new NodeMetric(STAT_BLOCK, I18nUtil.marktr("Sessions blocked")));
        this.addMetric(new NodeMetric(STAT_AUTHORIZE, I18nUtil.marktr("Clients authorized")));
    }

    public void initializeSettings()
    {
        logger.info("Initializing Settings...");

        CPDSettings settings = new CPDSettings();
        /* Create a set of default capture rules */
        List<CaptureRule> rules = new LinkedList<CaptureRule>();
        rules.add(new CaptureRule(false, true,
                                  "Require a login for traffic on the an interface (example rule)",
                                  new IntfMatcher("2"),
                                  IPMatcher.getAnyMatcher(), IPMatcher.getAnyMatcher(),
                                  CaptureRule.START_OF_DAY, CaptureRule.END_OF_DAY, CaptureRule.ALL_DAYS));

        rules.add(new CaptureRule(false, true,
                                  "Require a login between 8:00 AM and 5 PM on on interface. (example rule)",
                                  new IntfMatcher("2"),
                                  IPMatcher.getAnyMatcher(), IPMatcher.getAnyMatcher(),
                                  "8:00", "17:00", CaptureRule.ALL_DAYS));

        settings.setCaptureRules(rules);

        BrandingManager brand = UvmContextFactory.context().brandingManager();

        settings.setBasicLoginPageTitle("Captive Portal");
        settings.setBasicLoginPageWelcome("Welcome to the " + brand.getCompanyName() + " Captive Portal");
        settings.setBasicLoginUsername("Username:");
        settings.setBasicLoginPassword("Password:");
        settings.setBasicLoginMessageText("Please enter your username and password to connect to the Internet.");
        settings.setBasicLoginFooter("If you have any questions, please contact your network administrator.");
        settings.setBasicMessagePageTitle("Captive Portal");
        settings.setBasicMessagePageWelcome("Welcome to the " + brand.getCompanyName() + " Captive Portal");
        settings.setBasicMessageMessageText("Click Continue to connect to the Internet.");
        settings.setBasicMessageAgreeBox(false);
        settings.setBasicMessageAgreeText("Clicking here means you agree to the terms above.");
        settings.setBasicMessageFooter("If you have any questions, please contact your network administrator.");

        try {
            setSettings(settings);
        } catch (Exception e) {
            logger.error( "Unable to initialize the settings", e );
            throw new IllegalStateException("Error initializing cpd", e);
        }
    }

    // CPDNode methods --------------------------------------------------

    @Override
    public void setSettings(final CPDSettings settings) 
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsBase = System.getProperty("uvm.settings.dir") + "/untangle-node-cpd/settings_" + nodeID;
        String settingsFile = settingsBase + ".js";

        if ( settings == this.settings )
        {
            throw new IllegalArgumentException("Unable to update original settings, set this.settings to null first.");
        }

        this.settings = settings;
        try {
            settingsManager.save(CPDSettings.class, settingsBase, settings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Unable to save settings: ", e);
        }

        reconfigure();
    }

    public CPDSettings getSettings()
    {
        return this.settings;
    }

    @Override
    public List<HostDatabaseEntry> getCaptiveStatus()
    {
        List<HostDatabaseEntry> captiveStatus = null;

        if (assistant != null)
            captiveStatus = assistant.getCaptiveStatus();

        if (captiveStatus == null) {
            return new LinkedList<HostDatabaseEntry>();
        } else {
            return captiveStatus;
        }
    }

    @Override
    public List<CaptureRule> getCaptureRules()
    {
        List<CaptureRule> captureRules = this.settings.getCaptureRules();
        if ( captureRules == null ) {
            captureRules = new LinkedList<CaptureRule>();
        }

        return captureRules;
    }

    @Override
    public void setCaptureRules( final List<CaptureRule> captureRules ) 
    {
        this.settings.setCaptureRules( captureRules );
        reconfigure();
    }

    @Override
    public List<PassedAddress> getPassedClients()
    {
        return this.settings.getPassedClients();
    }

    @Override
    public void setPassedClients( final List<PassedAddress> newValue ) 
    {
        this.settings.setPassedClients( newValue );
        reconfigure();
    }

    @Override
    public List<PassedAddress> getPassedServers()
    {
        return this.settings.getPassedServers();
    }

    @Override
    public void setPassedServers( final List<PassedAddress> newValue ) 
    {
        this.settings.setPassedServers( newValue );
        reconfigure();
    }

    @Override
    public EventLogQuery[] getLoginEventQueries()
    {
        return new EventLogQuery[] { this.loginEventQuery };
    }

    @Override
    public EventLogQuery[] getBlockEventQueries()
    {
        return new EventLogQuery[] { this.blockEventQuery };
    }

    @Override
    public void incrementCount(BlingerType blingerType, long delta )
    {
        switch ( blingerType ) {
        case BLOCK:
            this.adjustMetric(STAT_BLOCK, delta);
            break;

        case AUTHORIZE:
            this.adjustMetric(STAT_AUTHORIZE, delta);
            break;
        }
    }

    @Override
    public boolean authenticate( String address, String username, String password, String credentials )
    {
        boolean isAuthenticated = false;
        if ( this.getRunState() ==  NodeSettings.NodeState.RUNNING ) {
            /* Enforcing this here so the user can't pick another username at login. */
            if ( this.settings.getAuthenticationType() == AuthenticationType.NONE) {
                username = this.DEFAULT_USERNAME;
            }
            /* This is split out for debugging */
            isAuthenticated = this.manager.authenticate(address, username, password, credentials);

            /* Update the CPD Phone Book cache */
            if (isAuthenticated) {
                try {
                    /* if no auth is required, don't count the "default user" as a user */
                    if ( this.settings.getAuthenticationType() != AuthenticationType.NONE && this.assistant != null)
                        assistant.addCache(InetAddress.getByName(address),username);
                } catch (UnknownHostException e) {
                    logger.warn("Add Cache failed",e);
                }
            }
        }
        return isAuthenticated;
    }

    @Override
    public boolean logout( String address )
    {
        boolean isLoggedOut = false;
        if ( this.getRunState() == NodeSettings.NodeState.RUNNING ) {
            isLoggedOut = this.manager.logout( address );
        }

        /* Update the CPD Phone Book cache */
        if (isLoggedOut) {
            try {
                if (assistant != null)
                    assistant.removeCache(InetAddress.getByName(address));
            } catch (UnknownHostException e) {
                logger.warn("Remove Cache failed",e);
            }
        }

        return isLoggedOut;
    }

    // NodeBase methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs() {
        return pipeSpecs;
    }

    // lifecycle --------------------------------------------------------------

    @Override
    protected void preStart() 
    {
        this.assistant = new CPDIpUsernameMapAssistant(this);

        /* Check if there is at least one enabled capture rule */
        boolean hasCaptureRule = false;
        for ( CaptureRule rule : this.settings.getCaptureRules()) {
            if ( rule.getLive() && rule.getCapture()) {
                hasCaptureRule = true;
                break;
            }
        }

        if ( !hasCaptureRule ) {
            Map<String,String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle-node-cpd");
            I18nUtil i18nUtil = new I18nUtil(i18nMap);
            throw new RuntimeException( i18nUtil.tr( "You must create and enable at least one Capture Rule before turning on the Captive Portal" ));
        }
        reconfigure(true);

        UvmContextFactory.context().uploadManager().registerHandler(this.uploadHandler);

        super.preStart();
     }

    @Override
    protected void preStop() 
    {
        try {
            /* Only stop if requested by the user (not during shutdown). */
            if ( UvmContextFactory.context().state() != UvmState.DESTROYED ) {
                this.manager.clearHostDatabase();

                /* Flush all of the entries that are in the phonebook XXX */
                //UvmContextFactory.context().localIpUsernameMap().flushEntries();
            }
        } catch (JSONException e) {
            logger.warn( "Unable to convert the JSON while clearing the host database, continuing.", e);
        } catch (ConnectionException e) {
            logger.warn( "Unable to clear host database settings, continuing.", e);
        }

        try {
            this.manager.setConfig(this.settings, false);
        } catch (JSONException e) {
            logger.warn( "Unable to convert the JSON while disabling the configuration, continuing.", e);
        } catch (IOException e) {
            logger.warn( "Unable to write settings, continuing.", e);
        }
        this.manager.stop();
    }

    @Override
    protected void postStop() 
    {
        super.postStop();

        if (this.assistant != null) {
            this.assistant.destroy();
            this.assistant = null;
        }
    }

    protected void postInit()
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsBase = System.getProperty("uvm.settings.dir") + "/untangle-node-cpd/settings_" + nodeID;
        String settingsFile = settingsBase + ".js";

        CPDSettings readSettings = null;
        logger.info("Loading settings from " + settingsFile );

        try
        {
			// first we try to read our json settings
            readSettings = settingsManager.load( CPDSettings.class, settingsBase );
        }

        catch (Exception exn)
        {
            logger.error("postInit()",exn);
        }

        // if no settings found try importing from the database
        if (readSettings == null)
        {
            logger.info("No json settings found... attempting to import from database");

            try
            {
                String convertCmd = SETTINGS_CONVERSION_SCRIPT + " " + nodeID.toString() + " " + settingsFile;
                logger.info("Running: " + convertCmd);
                UvmContextFactory.context().execManager().exec( convertCmd );
            }

            catch (Exception exn)
            {
                logger.error("Conversion script failed", exn);
            }

            try
            {
				// try to read the settings created by the conversion script
                readSettings = settingsManager.load( CPDSettings.class, settingsBase );
            }

            catch (Exception exn)
            {
                logger.error("Could not read node settings", exn);
            }
        }

        try
        {
			// still no settings found so init with defaults
            if (readSettings == null)
            {
                logger.warn("No database or json settings found... initializing with defaults");
                initializeSettings();
            }

            // otherwise apply the loaded or imported settings from the file
            else
            {
                logger.info("Loaded settings from " + settingsFile);
                setSettings(readSettings);
            }
        }

        catch (Exception exn)
        {
            logger.error("Could not apply node settings",exn);
        }

        UvmContextFactory.context().uploadManager().registerHandler(this.uploadHandler);
        deployWebAppIfRequired(this.logger);
    }

    @Override
    protected void preDestroy() 
    {
        UvmContextFactory.context().uploadManager().unregisterHandler(this.uploadHandler.getName());

        unDeployWebAppIfRequired(this.logger);

        if (this.assistant != null) {
            this.assistant.destroy();
            this.assistant = null;
        }

        super.preDestroy();
    }

    @Override
    protected void uninstall()
    {
        UvmContextFactory.context().uploadManager().unregisterHandler(this.uploadHandler.getName());

        super.uninstall();
    }

    // private methods -------------------------------------------------------
    private void reconfigure() 
    {
        reconfigure(false);
    }

    private void reconfigure(boolean force) 
    {
        if ( force || this.getRunState() == NodeSettings.NodeState.RUNNING) {
            try {
                this.manager.setConfig(this.settings, true);
            } catch (JSONException e) {
                throw new RuntimeException( "Unable to convert the JSON while setting the configuration.", e);
            } catch (IOException e) {
                throw new RuntimeException( "Unable to write settings.", e);
            }

            try {
                this.manager.start();
            } catch ( Exception e ) {
                throw new RuntimeException( "Unable to start CPD", e );
            }
        } else {
            try {
                this.manager.setConfig(this.settings, false);
            } catch (JSONException e) {
                logger.warn( "Unable to convert the JSON while disabling the configuration, continuing.", e);
            } catch (IOException e) {
                logger.warn( "Unable to write settings, continuing.", e);
            }

            this.manager.stop();
        }
    }

    private class CustomUploadHandler implements UploadHandler
    {
        @Override
        public String getName() {
            return "cpd-custom-page";
        }

        @Override
        public String handleFile(FileItem fileItem) throws Exception {
            File temp = File.createTempFile("untangle-cpd", ".zip");
            fileItem.write(temp);
            manager.loadCustomPage(temp.getAbsolutePath());
            return "Successfully uploaded a custom portal page";

        }
    }

    protected static synchronized void deployWebAppIfRequired(Logger logger)
    {
        if (0 != deployCount++) {
            return;
        }

        UvmContext mctx = UvmContextFactory.context();
        LocalAppServerManager asm = mctx.localAppServerManager();

        Valve v = new OutsideValve()
            {
                protected boolean isInsecureAccessAllowed()
                {
                    return true;
                }

                /* Unified way to determine which parameter to check */
                protected boolean isOutsideAccessAllowed()
                {
                    return false;
                }
            };

        if (null != asm.loadInsecureApp("/users", "users", v)) {
            logger.debug("Deployed authentication WebApp");
        } else {
            logger.error("Unable to deploy authentication WebApp");
        }
    }

    protected static synchronized void unDeployWebAppIfRequired(Logger logger) {
        if (0 != --deployCount) {
            return;
        }

        UvmContext mctx = UvmContextFactory.context();
        LocalAppServerManager asm = mctx.localAppServerManager();

        if (asm.unloadWebApp("/users")) {
            logger.debug("Unloaded authentication webapp");
        } else {
            logger.warn("Uanble to unload authentication WebApp");
        }
    }
}
