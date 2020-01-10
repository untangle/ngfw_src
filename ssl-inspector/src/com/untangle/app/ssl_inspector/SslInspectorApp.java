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

package com.untangle.app.ssl_inspector;

import javax.net.ssl.TrustManagerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.app.License;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.ForkedEventHandler;
import com.untangle.uvm.vnet.SessionEventHandler;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.servlet.UploadHandler;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

/**
 * The SSL Inspector application performs MitM decryption/encryption of secure
 * communications to allow the unencrypted traffic to pass through and benefit
 * from all of the other applications.
 * 
 * @author mahotz
 * 
 */

public class SslInspectorApp extends AppBase
{
    private final Logger logger = Logger.getLogger(SslInspectorApp.class);

    private static HashSet<String> brokenServerList = new HashSet<>();

    private PipelineConnector clientWebConnector = null;
    private PipelineConnector serverWebConnector = null;
    private PipelineConnector clientMailConnector = null;
    private PipelineConnector serverMailConnector = null;
    private PipelineConnector[] connectors = null;

    private final String OAUTH_DOMAIN_CONFIG = System.getProperty("uvm.home") + "/conf/ut-oauth.conf";

    protected static final String STAT_COUNTER = "COUNTER";
    protected static final String STAT_INSPECTED = "INSPECTED";
    protected static final String STAT_IGNORED = "IGNORED";
    protected static final String STAT_BLOCKED = "BLOCKED";
    protected static final String STAT_UNTRUSTED = "UNTRUSTED";
    protected static final String STAT_ABANDONED = "ABANDONED";

    protected ArrayList<oauthDomain> oauthConfigList;
    private TrustManagerFactory trustFactory;
    private SslInspectorSettings settings;

    private static final File CRON_FILE = new File("/etc/cron.daily/ssl-inspector-cleanup");
    private static String keyStorePath = "/var/cache/untangle-ssl";

    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param appProperties
     *        The application properties
     */
    public SslInspectorApp(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super(appSettings, appProperties);

        this.addMetric(new AppMetric(STAT_INSPECTED, I18nUtil.marktr("Sessions inspected")));
        this.addMetric(new AppMetric(STAT_IGNORED, I18nUtil.marktr("Sessions ignored")));
        this.addMetric(new AppMetric(STAT_UNTRUSTED, I18nUtil.marktr("Sessions untrusted")));
        this.addMetric(new AppMetric(STAT_ABANDONED, I18nUtil.marktr("Sessions abandoned")));
        this.addMetric(new AppMetric(STAT_COUNTER, I18nUtil.marktr("Total sessions")));
        this.addMetric(new AppMetric(STAT_BLOCKED, I18nUtil.marktr("Sessions blocked")));

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

    /**
     * Called before the application is initialized
     */
    @Override
    protected void preInit()
    {
        String appID = this.getAppSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/ssl-inspector/settings_" + appID + ".js";

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

                // appy the settings to the app
                reconfigure();
            }
        }

        catch (Exception exn) {
            logger.error("Could not apply app settings", exn);
        }
    }

    /**
     * Called before the application is started.
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStart(boolean isPermanentTransition)
    {
        // load the list of OAuth domains
        loadOAuthList();
    }

    /**
     * Called when the application is uninstalled.
     */
    @Override
    protected void uninstall()
    {
        removeCertCache();
    }

    /**
     * Return the application pipeline connectors.
     * 
     * @return The application pipeline connectors.
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Get the application settings
     * 
     * @return The settings for the application instance
     */
    public SslInspectorSettings getSettings()
    {
        return settings;
    }

    /**
     * Set the application settings
     * 
     * @param newSettings
     *        The new application settings
     */
    public void setSettings(final SslInspectorSettings newSettings)
    {
        String appID = this.getAppSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/ssl-inspector/settings_" + appID + ".js";

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

    /**
     * Returns the list of certificates and authorities we trust when connecting
     * to secure servers as a client.
     * 
     * @return Our list of trusted certificates and authorities
     */
    public List<TrustedCertificate> getTrustCatalog()
    {
        List<TrustedCertificate> trustCatalog;

        try {
            trustCatalog = TrustCatalog.getTrustCatalog();
        }

        catch (Exception exn) {
            logger.error("Exception loading catalog of trusted certificates", exn);
            trustCatalog = new LinkedList<>();
        }

        return (trustCatalog);
    }

    /**
     * Used to remove a user added trusted certificate
     * 
     * @param certAlias
     *        The alias of the certificate to remove
     */
    public void removeTrustedCertificate(String certAlias)
    {
        TrustCatalog.removeTrustedCertificate(certAlias);
        reconfigure();
    }

    /**
     * Returns our trust manager factory which is loaded with the standard and
     * user added certificates and authorities we trust.
     * 
     * @return Our trust manager factory
     */
    public TrustManagerFactory getTrustFactory()
    {
        return (trustFactory);
    }

    /**
     * Check for a valid license
     * 
     * @return True if license is valid, otherwise false
     */
    public boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.SSL_INSPECTOR)) return true;
        return false;
    }

    /**
     * This function is called whenever settings change
     */
    private void reconfigure()
    {
        UvmContextFactory.context().servletFileManager().registerUploadHandler(new CertificateUploadHandler());

        if (settings == null) return;

        // enable the SMTPS connectors using the app enabled flag and SMTPS enabled flag
        clientMailConnector.setEnabled(settings.isEnabled() && settings.getProcessEncryptedMailTraffic());
        serverMailConnector.setEnabled(settings.isEnabled() && settings.getProcessEncryptedMailTraffic());

        // enable th HTTPS connectors using the app enabled flag and HTTPS enabled flag 
        clientWebConnector.setEnabled(settings.isEnabled() && settings.getProcessEncryptedWebTraffic());
        serverWebConnector.setEnabled(settings.isEnabled() && settings.getProcessEncryptedWebTraffic());

        // if any inspection is enabled, write out the cert cleanup cron job, otherwise remove it
        if (serverWebConnector.isEnabled() || clientWebConnector.isEnabled() ||
            serverMailConnector.isEnabled() || clientMailConnector.isEnabled()) {
            writeCronFile();
        } else {
            removeCertCache();
        }

        if (settings.getJavaxDebug() == true) System.setProperty("javax.net.debug", "all");

        if (settings.getServerBlindTrust() == false) {
            try {
                trustFactory = TrustCatalog.createTrustFactory();
            }

            catch (Exception exn) {
                logger.error("Exception initializing server trust factory", exn);
            }
        } else {
            logger.warn("Configured to blindly trust all remote server certificates");
        }
    }

    /**
     * Creates our list of default rules
     * 
     * @return A list of default rules
     */
    private LinkedList<SslInspectorRule> generateDefaultRules()
    {
        LinkedList<SslInspectorRule> defaultRules = new LinkedList<>();
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

    /**
     * Creates a rule from passed parameters
     * 
     * @param ruleNumber
     * @param ruleDescription
     * @param matcherOneType
     * @param matcherOneString
     * @param matcherTwoType
     * @param matcherTwoString
     * @param matcherThreeType
     * @param matcherThreeString
     * @param actionType
     * @param isLive
     * @return A rule created using the passed parameters
     */
    private SslInspectorRule createDefaultRule(int ruleNumber, String ruleDescription, SslInspectorRuleCondition.ConditionType matcherOneType, String matcherOneString, SslInspectorRuleCondition.ConditionType matcherTwoType, String matcherTwoString, SslInspectorRuleCondition.ConditionType matcherThreeType, String matcherThreeString, SslInspectorRuleAction.ActionType actionType, boolean isLive)
    {
        SslInspectorRule rule;
        LinkedList<SslInspectorRuleCondition> matchers;
        SslInspectorRuleCondition ruleMatcher;
        SslInspectorRuleAction action;

        rule = new SslInspectorRule();
        matchers = new LinkedList<>();

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

    /**
     * This is the handler for uploading trusted certificates
     * 
     * @author mahotz
     * 
     */
    private class CertificateUploadHandler implements UploadHandler
    {
        /**
         * @return The path name for this upload handler
         */
        @Override
        public String getName()
        {
            return "trusted_cert";
        }

        /**
         * Called when a certificate is uploaded
         * 
         * @param fileItem
         * @param argument
         * @return The result
         * @throws Exception
         */
        @Override
        public ExecManagerResult handleFile(FileItem fileItem, String argument) throws Exception
        {
            logger.debug("CertificateUploadHandler FILE=" + fileItem.getName() + " ARG=" + argument);
            ExecManagerResult result = TrustCatalog.addTrustedCertificate(argument, fileItem.get());
            reconfigure();
            return result;
        }
    }

    /**
     * Some servers are misconfigured and may return the TLS unrecognized_name
     * warning when an misconfigured SNI name is included in the ClientHello
     * message. Browsers normally ignore, but it causes Java to fail the
     * handshake. Since we're a casing we can't disconnect and start the
     * handshake over, so instead we keep a list of servers and names that
     * generate the error. When a match is found, we'll disable SNI during
     * subsequent handshake attempts. Most clients seem to try more than once,
     * so the second attempt should succeed.
     * 
     * @param brokenServer
     *        The address of the broken server
     */
    protected void addBrokenServer(String brokenServer)
    {
        if ((brokenServerList.contains(brokenServer)) == false) {
            brokenServerList.add(brokenServer);
        }
    }

    /**
     * Checks the broken server list for the argumented address
     * 
     * @param brokenServer
     *        The address to match
     * @return True if the server is in our list, otherwise false
     */
    protected boolean checkBrokenServer(String brokenServer)
    {
        return (brokenServerList.contains(brokenServer));
    }

    /**
     * Holds the details of an OAuth domain
     */
    protected class oauthDomain
    {
        String provider;
        String match;
        String name;
    }

    /**
     * Loads the list of OAuth domains that must be allowed for client auth from
     * a config file in the format PROVIDER|MATCH|NAME
     * 
     * @return The list of oauthDomain's
     */
    public void loadOAuthList()
    {
        oauthConfigList = new ArrayList<oauthDomain>();
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(OAUTH_DOMAIN_CONFIG))) {
            while ((line = br.readLine()) != null) {
                // ignore lines that start with a comment character
                if (line.startsWith("#")) continue;
                // ignore lines too short to be valid
                if (line.length() < 5) continue;
                String[] values = line.split(java.util.regex.Pattern.quote("|"), 3);
                // ignore lines that don't have exactly three fields
                if (values.length != 3) continue;
                oauthDomain rec = new oauthDomain();
                rec.provider = values[0].toLowerCase();
                rec.match = values[1].toLowerCase();
                rec.name = values[2].toLowerCase();
                oauthConfigList.add(rec);
                logger.debug("Loaded OAuth config: " + rec.provider + " | " + rec.match + " | " + rec.name);
            }
        } catch (Exception exn) {
            logger.warn("Exception loading OAuth domains:" + OAUTH_DOMAIN_CONFIG, exn);
        }
    }

    /**
     * Write ssl inspector's cert cache cleanup cronjob file.
     */
    private void writeCronFile()
    {
        /**
         * write the cron file for nightly runs that will
         * remove all files in /var/cache/untangle-ssl that are older than 6 months
         */
        String cronStr = "#!/bin/sh\n\n" +
                         "if [ -d /var/cache/untangle-ssl ] ; then\n" +
                         "    /usr/bin/find " + keyStorePath + " -mtime +182 -exec rm {} +\n" +
                         "fi\n\n" +
                         "exit 0\n";
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(CRON_FILE));
            out.write(cronStr, 0, cronStr.length());
            out.write("\n");
        } catch (IOException ex) {
            logger.error("Unable to write file", ex);
            return;
        }finally{
            if(out != null){
                try {
                    out.close();
                } catch (IOException ex) {
                    logger.error("Unable to close file", ex);
                }
            }
        }

        // Make it executable
        UvmContextFactory.context().execManager().execResult( "chmod 755 " + CRON_FILE );
    }

    /**
     * Clear the cert cache and delete ssl inspector's cert cache cleanup cronjob file.
     */
    private void removeCertCache()
    {
        UvmContextFactory.context().execManager().execResult( "rm -rf " + keyStorePath + "/* ; rm -f " + CRON_FILE );
    }
}
