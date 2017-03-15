/**
 * $Id: SslInspectorApp.java,v 1.00 2017/03/03 19:29:08 dmorris Exp $
 * 
 * Copyright (c) 2003-2017 Untangle, Inc.
 *
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Untangle.
 */

package com.untangle.node.ssl_inspector;

import javax.net.ssl.TrustManagerFactory;

import java.util.HashSet;
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

    private static HashSet<String> brokenServerList = new HashSet<String>();

    private PipelineConnector clientWebConnector = null;
    private PipelineConnector serverWebConnector = null;
    private PipelineConnector clientMailConnector = null;
    private PipelineConnector serverMailConnector = null;
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

        SessionEventHandler clientWebHandler = new ForkedEventHandler(clientParser, clientUnparser);
        SessionEventHandler serverWebHandler = new ForkedEventHandler(serverUnparser, serverParser);
        this.clientWebConnector = UvmContextFactory.context().pipelineFoundry().create("ssl-web-client", this, null, clientWebHandler, Fitting.HTTPS_STREAM, Fitting.HTTP_STREAM, Affinity.CLIENT, -1100, true, null);
        this.serverWebConnector = UvmContextFactory.context().pipelineFoundry().create("ssl-web-server", this, null, serverWebHandler, Fitting.HTTP_STREAM, Fitting.HTTPS_STREAM, Affinity.SERVER, 1100, true, "ssl-web-client");

        SessionEventHandler clientMailHandler = new ForkedEventHandler(clientParser, clientUnparser);
        SessionEventHandler serverMailHandler = new ForkedEventHandler(serverUnparser, serverParser);
        this.clientMailConnector = UvmContextFactory.context().pipelineFoundry().create("ssl-mail-client", this, null, clientMailHandler, Fitting.SMTP_STREAM, Fitting.SMTP_STREAM, Affinity.CLIENT, -1100, true, null);
        this.serverMailConnector = UvmContextFactory.context().pipelineFoundry().create("ssl-mail-server", this, null, serverMailHandler, Fitting.SMTP_STREAM, Fitting.SMTP_STREAM, Affinity.SERVER, 1100, true, "ssl-mail-client");

        this.connectors = new PipelineConnector[] { clientWebConnector, serverWebConnector, clientMailConnector, serverMailConnector };

        TrustCatalog.staticInitialization(logger);
    }

    // overriden functions ----------------------------------------------------

    @Override
    protected void preInit()
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/ssl-inspector/settings_" + nodeID + ".js";

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

                // no version really means version one
                if (readSettings.getVersion() == null) readSettings.setVersion(new Integer(1));

                // between v1 and v2 we added a new default rule to scan secure SMTP traffic
                if (readSettings.getVersion().intValue() < 2) {
                    SslInspectorRule addRule = createDefaultRule(0, "Inspect SMTP + STARTTLS", SslInspectorRuleCondition.ConditionType.PROTOCOL, "TCP", SslInspectorRuleCondition.ConditionType.SRC_INTF, "wan", SslInspectorRuleCondition.ConditionType.DST_PORT, "25", SslInspectorRuleAction.ActionType.INSPECT, true);                    
                    readSettings.getIgnoreRules().addFirst(addRule);
                    int idx = 1;
                    for (SslInspectorRule rule : readSettings.getIgnoreRules()) {
                        rule.setRuleId(idx++);
                    }

                    // calling the setter here will write our changes, update the version
                    // and handle the reconfigure so we can return directly from here so
                    // we don't call reconfigure twice in a row
                    setSettings(readSettings);
                    return;
                }

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
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    // public functions -------------------------------------------------------

    public SslInspectorSettings getSettings()
    {
        return settings;
    }

    public void setSettings(final SslInspectorSettings newSettings)
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/ssl-inspector/settings_" + nodeID + ".js";

        try {
            newSettings.setVersion(new Integer(2));
            UvmContextFactory.context().settingsManager().save(settingsFile, newSettings);
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
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.SSL_INSPECTOR)) return true;
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.SSL_INSPECTOR_OLDNAME)) return true;
        return false;
    }

    // private functions ------------------------------------------------------

    private void reconfigure()
    {
        UvmContextFactory.context().servletFileManager().registerUploadHandler(new CertificateUploadHandler());

        if (settings == null) return;

        // enable the SMTPS connectors using the node enabled flag and SMTPS enabled flag
        clientMailConnector.setEnabled(settings.isEnabled() && settings.getProcessEncryptedMailTraffic());
        serverMailConnector.setEnabled(settings.isEnabled() && settings.getProcessEncryptedMailTraffic());

        // enable th HTTPS connectors using the node enabled flag and HTTPS enabled flag 
        clientWebConnector.setEnabled(settings.isEnabled() && settings.getProcessEncryptedWebTraffic());
        serverWebConnector.setEnabled(settings.isEnabled() && settings.getProcessEncryptedWebTraffic());

        if (settings.getJavaxDebug() == true) System.setProperty("javax.net.debug", "all");

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

        defaultRules.add(createDefaultRule(ruleNumber++, "Ignore Microsoft Update", SslInspectorRuleCondition.ConditionType.SSL_INSPECTOR_SUBJECT_DN, "*update.microsoft*", null, null, null, null, SslInspectorRuleAction.ActionType.IGNORE, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Ignore GotoMeeting", SslInspectorRuleCondition.ConditionType.SSL_INSPECTOR_SUBJECT_DN, "*citrix*", null, null, null, null, SslInspectorRuleAction.ActionType.IGNORE, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Ignore Dropbox", SslInspectorRuleCondition.ConditionType.SSL_INSPECTOR_SUBJECT_DN, "*dropbox*", null, null, null, null, SslInspectorRuleAction.ActionType.IGNORE, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect All Traffic", null, null, null, null, null, null, SslInspectorRuleAction.ActionType.INSPECT, false));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect Inbound SMTP + STARTTLS", SslInspectorRuleCondition.ConditionType.PROTOCOL, "TCP", SslInspectorRuleCondition.ConditionType.SRC_INTF, "wan", SslInspectorRuleCondition.ConditionType.DST_PORT, "25", SslInspectorRuleAction.ActionType.INSPECT, false));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect Outbound SMTP + STARTTLS", SslInspectorRuleCondition.ConditionType.PROTOCOL, "TCP", SslInspectorRuleCondition.ConditionType.DST_INTF, "wan", SslInspectorRuleCondition.ConditionType.DST_PORT, "25", SslInspectorRuleAction.ActionType.INSPECT, false));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect YouTube", SslInspectorRuleCondition.ConditionType.SSL_INSPECTOR_SNI_HOSTNAME, "*youtube.com", null, null, null, null, SslInspectorRuleAction.ActionType.INSPECT, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect Google", SslInspectorRuleCondition.ConditionType.SSL_INSPECTOR_SUBJECT_DN, "*Google*", null, null, null, null, SslInspectorRuleAction.ActionType.INSPECT, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect Facebook", SslInspectorRuleCondition.ConditionType.SSL_INSPECTOR_SUBJECT_DN, "*Facebook*", null, null, null, null, SslInspectorRuleAction.ActionType.INSPECT, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect Wikipedia", SslInspectorRuleCondition.ConditionType.SSL_INSPECTOR_SUBJECT_DN, "*Wikimedia*", null, null, null, null, SslInspectorRuleAction.ActionType.INSPECT, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect Twitter", SslInspectorRuleCondition.ConditionType.SSL_INSPECTOR_SUBJECT_DN, "*Twitter*", null, null, null, null, SslInspectorRuleAction.ActionType.INSPECT, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect Yahoo", SslInspectorRuleCondition.ConditionType.SSL_INSPECTOR_SUBJECT_DN, "*Yahoo*", null, null, null, null, SslInspectorRuleAction.ActionType.INSPECT, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect Bing", SslInspectorRuleCondition.ConditionType.SSL_INSPECTOR_SNI_HOSTNAME, "*bing.com", null, null, null, null, SslInspectorRuleAction.ActionType.INSPECT, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Inspect Ask", SslInspectorRuleCondition.ConditionType.SSL_INSPECTOR_SNI_HOSTNAME, "*ask.com", null, null, null, null, SslInspectorRuleAction.ActionType.INSPECT, true));
        defaultRules.add(createDefaultRule(ruleNumber++, "Ignore Other Traffic", null, null, null, null, null, null, SslInspectorRuleAction.ActionType.IGNORE, true));

        return defaultRules;
    }

    private SslInspectorRule createDefaultRule(int ruleNumber, String ruleDescription,
            SslInspectorRuleCondition.ConditionType matcherOneType, String matcherOneString,
            SslInspectorRuleCondition.ConditionType matcherTwoType, String matcherTwoString,
            SslInspectorRuleCondition.ConditionType matcherThreeType, String matcherThreeString,            
            SslInspectorRuleAction.ActionType actionType, boolean isLive)
    {
        SslInspectorRule rule;
        LinkedList<SslInspectorRuleCondition> matchers;
        SslInspectorRuleCondition ruleMatcher;
        SslInspectorRuleAction action;

        rule = new SslInspectorRule();
        matchers = new LinkedList<SslInspectorRuleCondition>();

        if (matcherOneString != null) {
            ruleMatcher = new SslInspectorRuleCondition(matcherOneType, matcherOneString);
            matchers.add(ruleMatcher);
        }

        if (matcherTwoString != null) {
            ruleMatcher = new SslInspectorRuleCondition(matcherTwoType, matcherTwoString);
            matchers.add(ruleMatcher);
        }

        if (matcherThreeString != null) {
            ruleMatcher = new SslInspectorRuleCondition(matcherThreeType, matcherThreeString);
            matchers.add(ruleMatcher);
        }

        action = new SslInspectorRuleAction(actionType, Boolean.TRUE);
        rule.setDescription(ruleDescription);
        rule.setConditions(matchers);
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

    /*
     * Some servers are misconfigured and may return the TLS unrecognized_name
     * warning when an misconfigured SNI name is included in the ClientHello
     * message. Browsers normally ignore, but it causes Java to fail the
     * handshake. Since we're a casing we can't disconnect and start the
     * handshake over, so instead we keep a list of servers and names that
     * generate the error. When a match is found, we'll disable SNI during
     * subsequent handshake attempts. Most clients seem to try more than once,
     * so the second attempt will succeed.
     */

    protected void addBrokenServer(String brokenServer)
    {
        if ((brokenServerList.contains(brokenServer)) == false) {
            brokenServerList.add(brokenServer);
        }
    }

    protected boolean checkBrokenServer(String brokenServer)
    {
        return (brokenServerList.contains(brokenServer));
    }
}
