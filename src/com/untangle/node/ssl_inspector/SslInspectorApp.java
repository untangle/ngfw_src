/*
 * $Id: SslInspectorApp.java 41228 2015-09-11 22:45:38Z dmorris $
 */

package com.untangle.node.ssl_inspector;

import javax.net.ssl.TrustManagerFactory;

import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.node.PortRange;
import com.untangle.uvm.node.License;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.Subscription;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.ForkedEventHandler;
import com.untangle.uvm.vnet.SessionEventHandler;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.servlet.UploadHandler;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

public class SslInspectorApp extends NodeBase
{
    private final Logger logger = Logger.getLogger(SslInspectorApp.class);

    private PipelineConnector clientSideConnector = null;
    private PipelineConnector serverSideConnector = null;
    private PipelineConnector[] connectors = null;

    protected static final String STAT_COUNTER = "COUNTER";
    protected static final String STAT_INSPECTED = "INSPECTED";
    protected static final String STAT_IGNORED = "IGNORED";
    protected static final String STAT_BLOCKED = "BLOCKED";
    protected static final String STAT_UNTRUSTED = "UNTRUSTED";
    protected static final String STAT_ABANDONED = "ABANDONED";

    private TrustManagerFactory trustFactory;
    private SslInspectorSettings settings;

    public SslInspectorApp(com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties)
    {
        super(nodeSettings, nodeProperties);

        this.addMetric(new NodeMetric(STAT_INSPECTED, I18nUtil.marktr("Sessions inspected")));
        this.addMetric(new NodeMetric(STAT_IGNORED, I18nUtil.marktr("Sessions ignored")));
        this.addMetric(new NodeMetric(STAT_UNTRUSTED, I18nUtil.marktr("Sessions untrusted")));
        this.addMetric(new NodeMetric(STAT_ABANDONED, I18nUtil.marktr("Sessions abandoned")));
        this.addMetric(new NodeMetric(STAT_COUNTER, I18nUtil.marktr("Total sessions")));
        this.addMetric(new NodeMetric(STAT_BLOCKED, I18nUtil.marktr("Sessions blocked")));

        SslInspectorParserEventHandler clientParser = new SslInspectorParserEventHandler(true, this);
        SslInspectorParserEventHandler serverParser = new SslInspectorParserEventHandler(false, this);
        SslInspectorUnparserEventHandler clientUnparser = new SslInspectorUnparserEventHandler(true, this);
        SslInspectorUnparserEventHandler serverUnparser = new SslInspectorUnparserEventHandler(false, this);

        // tell the client side parser about the client-side unparser and vice versa
        clientParser.setUnparser(clientUnparser);
        clientUnparser.setParser(clientParser);
        // tell the server side parser about the server-side unparser and vice versa
        serverParser.setUnparser(serverUnparser);
        serverUnparser.setParser(serverParser);

        SessionEventHandler clientSideHandler = new ForkedEventHandler(clientParser, clientUnparser);
        SessionEventHandler serverSideHandler = new ForkedEventHandler(serverUnparser, serverParser);

        this.clientSideConnector = UvmContextFactory.context().pipelineFoundry().create("ssl-client-side", this, null, clientSideHandler, Fitting.HTTPS_STREAM, Fitting.HTTP_STREAM, Affinity.CLIENT, -1100);
        this.serverSideConnector = UvmContextFactory.context().pipelineFoundry().create("ssl-server-side", this, null, serverSideHandler, Fitting.HTTP_STREAM, Fitting.HTTPS_STREAM, Affinity.SERVER, 1100);
        this.connectors = new PipelineConnector[] { clientSideConnector, serverSideConnector };

        TrustCatalog.staticInitialization(logger);
    }

    // overriden functions ----------------------------------------------------

    @Override
    protected void preInit()
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-casing-ssl-inspector/settings_" + nodeID + ".js";

        SslInspectorSettings readSettings = null;
        logger.info("Loading settings from " + settingsFile);

        try {
            // first we try to read our json settings
            readSettings = UvmContextFactory.context().settingsManager().load(SslInspectorSettings.class, settingsFile);

            // no settings found so init with defaults and save
            if (readSettings == null) {
                logger.warn("No settings found... initializing with defaults");
                SslInspectorSettings makeSettings = new SslInspectorSettings();
                makeSettings.setIgnoreRules(generateDefaultRules());
                setSettings(makeSettings);
            }

            // otherwise apply the loaded or imported settings from the file
            else {
                logger.info("Loaded settings from " + settingsFile);
                this.settings = readSettings;

                // appy the settings to the node
                reconfigure();
            }
        }

        catch (Exception exn) {
            logger.error("Could not apply node settings", exn);
        }
    }

    @Override
    protected void preStart()
    {
        // check for a valid license
        if (isLicenseValid() != true)
            throw (new RuntimeException("Unable to start ssl node: invalid license"));
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    @Override
    protected void connectPipelineConnectors()
    {
        UvmContextFactory.context().pipelineFoundry().registerCasing(clientSideConnector, serverSideConnector);
    }

    @Override
    protected void disconnectPipelineConnectors()
    {
        UvmContextFactory.context().pipelineFoundry().deregisterCasing(clientSideConnector, serverSideConnector);
    }

    // public functions -------------------------------------------------------

    public SslInspectorSettings getSettings()
    {
        return settings;
    }

    public void setSettings(final SslInspectorSettings newSettings)
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-casing-ssl-inspector/settings_" + nodeID + ".js";

        try {
            UvmContextFactory.context().settingsManager().save( settingsFile, newSettings );
        } catch (Exception exn) {
            logger.error("setSettings()", exn);
            return;
        }

        this.settings = newSettings;
        reconfigure();
    }

    public List<TrustedCertificate> getTrustCatalog()
    {
        List<TrustedCertificate> trustCatalog;

        try {
            trustCatalog = TrustCatalog.getTrustCatalog();
        }

        catch (Exception exn) {
            logger.error("Exception loading catalog of trusted certificates", exn);
            trustCatalog = new LinkedList<TrustedCertificate>();
        }

        return (trustCatalog);
    }

    public void removeTrustedCertificate(String certAlias)
    {
        TrustCatalog.removeTrustedCertificate(certAlias);
        reconfigure();
    }

    public TrustManagerFactory getTrustFactory()
    {
        return (trustFactory);
    }

    public boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.SSL_INSPECTOR))
            return true;
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.SSL_INSPECTOR_OLDNAME))
            return true;
        return false;
    }

    // private functions ------------------------------------------------------

    private void reconfigure()
    {
        UvmContextFactory.context().servletFileManager().registerUploadHandler(new CertificateUploadHandler());

        if (settings == null)
            return;

        for (PipelineConnector connector : this.connectors)
            connector.setEnabled(settings.isEnabled());

        if (settings.getJavaxDebug() == true)
            System.setProperty("javax.net.debug", "all");

        if (settings.getServerBlindTrust() == false) {
            try {
                trustFactory = TrustCatalog.createTrustFactory();
            }

            catch (Exception exn) {
                logger.error("Exception initializing server trust factory", exn);
            }
        } else {
            logger.info("Configured to blindly trust all remote server certificates");
        }
    }

    private LinkedList<SslInspectorRule> generateDefaultRules()
    {
        LinkedList<SslInspectorRule> defaultRules = new LinkedList<SslInspectorRule>();
        int ruleNumber = 1;

        defaultRules.add(createDefaultRule(ruleNumber++, "Ignore Microsoft Update", SslInspectorRuleMatcher.MatcherType.SSL_INSPECTOR_SUBJECT_DN, "*update.microsoft*", SslInspectorRuleAction.ActionType.IGNORE, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Ignore GotoMeeting", SslInspectorRuleMatcher.MatcherType.SSL_INSPECTOR_SNI_HOSTNAME, "*gotomeeting.com", SslInspectorRuleAction.ActionType.IGNORE, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Ignore Dropbox", SslInspectorRuleMatcher.MatcherType.SSL_INSPECTOR_SUBJECT_DN, "*dropbox*", SslInspectorRuleAction.ActionType.IGNORE, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect All Traffic", null, null, SslInspectorRuleAction.ActionType.INSPECT, false));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect YouTube Traffic", SslInspectorRuleMatcher.MatcherType.SSL_INSPECTOR_SNI_HOSTNAME, "*youtube.com", SslInspectorRuleAction.ActionType.INSPECT, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect Google Traffic", SslInspectorRuleMatcher.MatcherType.SSL_INSPECTOR_SUBJECT_DN, "*Google*", SslInspectorRuleAction.ActionType.INSPECT, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect Facebook Traffic", SslInspectorRuleMatcher.MatcherType.SSL_INSPECTOR_SUBJECT_DN, "*Facebook*", SslInspectorRuleAction.ActionType.INSPECT, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect Wikipedia Traffic", SslInspectorRuleMatcher.MatcherType.SSL_INSPECTOR_SUBJECT_DN, "*Wikimedia*", SslInspectorRuleAction.ActionType.INSPECT, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect Twitter Traffic", SslInspectorRuleMatcher.MatcherType.SSL_INSPECTOR_SUBJECT_DN, "*Twitter*", SslInspectorRuleAction.ActionType.INSPECT, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect Yahoo Traffic", SslInspectorRuleMatcher.MatcherType.SSL_INSPECTOR_SUBJECT_DN, "*Yahoo*", SslInspectorRuleAction.ActionType.INSPECT, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect Bing Traffic", SslInspectorRuleMatcher.MatcherType.SSL_INSPECTOR_SNI_HOSTNAME, "*bing.com", SslInspectorRuleAction.ActionType.INSPECT, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect Ask Traffic", SslInspectorRuleMatcher.MatcherType.SSL_INSPECTOR_SNI_HOSTNAME, "*ask.com", SslInspectorRuleAction.ActionType.INSPECT, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Ignore Other Traffic", null, null, SslInspectorRuleAction.ActionType.IGNORE, true));

        return defaultRules;
    }

    private SslInspectorRule createDefaultRule(int ruleNumber, String ruleDescription, SslInspectorRuleMatcher.MatcherType matcherType, String matcherString, SslInspectorRuleAction.ActionType actionType, boolean isLive)
    {
        SslInspectorRule rule;
        LinkedList<SslInspectorRuleMatcher> matchers;
        SslInspectorRuleMatcher ruleMatcher;
        SslInspectorRuleAction action;

        rule = new SslInspectorRule();
        matchers = new LinkedList<SslInspectorRuleMatcher>();

        if (matcherString != null) {
            ruleMatcher = new SslInspectorRuleMatcher(matcherType, matcherString);
            matchers.add(ruleMatcher);
        }

        action = new SslInspectorRuleAction(actionType, Boolean.TRUE);
        rule.setDescription(ruleDescription);
        rule.setMatchers(matchers);
        rule.setRuleId(ruleNumber);
        rule.setAction(action);
        rule.setLive(isLive);

        return (rule);
    }

    private class CertificateUploadHandler implements UploadHandler
    {
        @Override
        public String getName()
        {
            return "trusted_cert";
        }

        @Override
        public ExecManagerResult handleFile(FileItem fileItem, String argument) throws Exception
        {
            logger.debug("CertificateUploadHandler FILE=" + fileItem.getName() + " ARG=" + argument);
            ExecManagerResult result = TrustCatalog.addTrustedCertificate(argument, fileItem.get());
            reconfigure();
            return result;
        }
    }
}
