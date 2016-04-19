/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Set;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.LanguageSettings;
import com.untangle.uvm.AdminManager;
import com.untangle.uvm.AdminSettings;
import com.untangle.uvm.AdminUserSettings;
import com.untangle.uvm.ExecManagerResult;

/**
 * Remote interface for administrative user management.
 */
public class AdminManagerImpl implements AdminManager
{
    private static final String INITIAL_USER_DESCRIPTION = "System Administrator";
    private static final String INITIAL_USER_LOGIN = "admin";
    private static final String INITIAL_USER_PASSWORD = "passwd";

    private static final String KERNEL_VERSION_SCRIPT = "/bin/uname -r";
    private static final String REBOOT_COUNT_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-reboot-count.sh";
    
    private final Logger logger = Logger.getLogger(this.getClass());

    private AdminSettings settings;

    protected AdminManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        AdminSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "admin.js";

        try {
            readSettings = settingsManager.load( AdminSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            AdminSettings newSettings = new AdminSettings();
            newSettings.setVersion(2L); /* as of 12.0, we start with version 2 */
            newSettings.addUser(new AdminUserSettings(INITIAL_USER_LOGIN, INITIAL_USER_PASSWORD, INITIAL_USER_DESCRIPTION, ""));
            this.setSettings(newSettings);
        }
        else {
            logger.debug("Loading Settings...");

            this.settings = readSettings;
            this.applyToSystem();
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        /**
         * If the settings file date is newer than the system files, re-sync them
         */
        if ( ! UvmContextFactory.context().isDevel() ) {
            File settingsFile = new File( settingsFileName );
            File shadowFile = new File("/etc/shadow");
            if (settingsFile.lastModified() > shadowFile.lastModified() ) {
                applyToSystem();
            }
        }

        /* run big 12.0 conversion */
        convertToNewNames();
        
        logger.info("Initialized AdminManager");
    }

    public AdminSettings getSettings()
    {
        return this.settings;
    }

    public void setSettings( final AdminSettings newSettings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "admin.js", newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

        this.applyToSystem();
    }

    public String getFullVersionAndRevision()
    {
        try {
            String version = UvmContextImpl.context().execManager().execOutput("dpkg-query -f '${Version}\\n' -W untangle-vm");

            if (version == null)
                return "";
            else
                return version.replaceAll("(\\r|\\n)", "");
        } catch (Exception e) {
            logger.warn("Unable to fetch version",e);
        }

        /**
         * that method probably timed out
         * fall back to this method
         */
        return UvmContextImpl.context().getFullVersion();
    }

    public String getModificationState()
    {
        File zshHistoryFile = new File("/root/.zsh_history");
        File blessedFile = new File(System.getProperty("uvm.conf.dir") + "/mods-blessed-flag");

        /* if there is no zsh_history file it obviously hasn't been modified */
        if (!zshHistoryFile.exists())
            return "none";

        /* if there is a zsh_history, but the blessed flag is newer these changes have been approved */
        if (blessedFile.exists() && blessedFile.lastModified() > zshHistoryFile.lastModified())
            return "blessed";

        ExecManagerResult result = UvmContextImpl.context().execManager().exec("cat /root/.zsh_history | /usr/bin/wc -l");
        int exitCode = result.getResult();
        String output = result.getOutput();

        output = output.replaceAll("(\\r|\\n)", "");
            
        if( exitCode == 0 ) {
            return new String("yes (" + output + ")");
        }

        return "UNKNOWN";
    }

    public String getRebootCount()
    {
        try {
            String count = UvmContextImpl.context().execManager().execOutput(REBOOT_COUNT_SCRIPT);
        
            if (count == null)
                return "";
            else
                return count.replaceAll("(\\r|\\n)", "");
        } catch (Exception e) {
            logger.warn("Unable to fetch version",e);
        }

        return "Unknown";
    }
    
    public String getKernelVersion()
    {
        try {
            String version = UvmContextImpl.context().execManager().execOutput(KERNEL_VERSION_SCRIPT);
        
            if (version == null)
                return "";
            else
                return version.replaceAll("(\\r|\\n)", "");
        } catch (Exception e) {
            logger.warn("Unable to fetch version",e);
        }

        return "Unknown";
    }
    
    public String getAdminEmail()
    {
        try {
            for ( AdminUserSettings user : getSettings().getUsers() ) {
                if ( "admin".equals( user.getUsername() ) )
                    return user.getEmailAddress();
            }
        } catch ( Exception e ) {
            logger.warn("Failed to find admin email", e);
        }
        return null;
    }
    
    private void applyToSystem() 
    {
        setRootPasswordToAdminPassword();
    }

    private void setRootPasswordToAdminPassword()
    {
        // Set root password to "admin" password
        for ( AdminUserSettings user : this.settings.getUsers() ) {
            if ( "admin".equals( user.getUsername() ) ) {
                String pass = user.trans_getPassword();
                if ( pass != null ) {
                    logger.info("Setting root password");
                    String cmd = "echo 'root:" + pass + "' | sudo chpasswd";
                    
                    // turn down logging so we dont log password
                    UvmContextImpl.context().execManager().setLevel(  org.apache.log4j.Level.DEBUG );

                    ExecManagerResult result = UvmContextImpl.context().execManager().exec( cmd );

                    // turn logging back up
                    UvmContextImpl.context().execManager().setLevel(  org.apache.log4j.Level.INFO );
                    
                    int exitCode = result.getResult();
                    if ( exitCode != 0 ) {
                        logger.warn( "Setting root password returned non-zero exit code: " + exitCode );
                    }
                }
            }
        }
    }

    /* 12.0 conversion */
    private void convertToNewNames()
    {
        String dirName;
        String oldName;
        String newName;
        String[] oldNames;
        String[] newNames;
        File dir;
        int i;

        /**
         * If we are on version 2 (or greater), the conversion has already taken place
         */
        if ( settings.getVersion() != null && settings.getVersion() >= 2 )
            return;

        // remove old IPS settings
        oldName = "untangle-node-ips";
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/rm -rf " + dirName);
        }

        // global renames
        oldNames = new String[] {"SITEFILTER",
                                 "CLASSD",
                                 "HTTPS_SNI_HOSTNAME",
                                 "HTTPS_SUBJECT_DN",
                                 "HTTPS_ISSUER_DN",
                                 "RuleMatcher",
                                 "\"matchers\":",
                                 "\"matcherType\":",
                                 "FailDEvent",
                                 "ClassDLogEvent"};
        newNames = new String[] {"WEB_FILTER",
                                 "APPLICATION_CONTROL",
                                 "SSL_INSPECTOR_SNI_HOSTNAME",
                                 "SSL_INSPECTOR_SUBJECT_DN",
                                 "SSL_INSPECTOR_ISSUER_DN",
                                 "RuleCondition",
                                 "\"conditions\":",
                                 "\"conditionType\":",
                                 "WanFailoverEvent",
                                 "ApplicationControlLogEvent"};
        String expressions = "";
        for ( i = 0 ; i < oldNames.length ; i++ ) {
            String oldStr = oldNames[i];
            String newStr = newNames[i];
            expressions = expressions + " " + "-e 's/" + oldStr + "/" + newStr + "/g'";
        }
        UvmContextFactory.context().execManager().execResult("find " + System.getProperty("uvm.settings.dir") + " -type f -name '*.js' -o -path './untangle-node-idps' -prune | xargs /bin/sed " + expressions + " -i ");

        // rename sitefilter to web-filter
        oldName = "untangle-node-sitefilter";
        newName = "untangle-node-web-filter";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.sitefilter.SiteFilterImpl",
                                 "com.untangle.node.webfilter.WebFilterSettings"};
        newNames = new String[] {"com.untangle.node.web_filter.WebFilterApp",
                                 "com.untangle.node.web_filter.WebFilterSettings"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename webfilter to web-filter-lite
        oldName = "untangle-node-webfilter";
        newName = "untangle-node-web-filter-lite";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.web_filter.WebFilterImpl",
                                 "com.untangle.node.webfilter"};
        newNames = new String[] {"com.untangle.node.web_filter_lite.WebFilterLiteApp",
                                 "com.untangle.node.web_filter"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename virusblocker to virus-blocker
        oldName = "untangle-node-virusblocker";
        newName = "untangle-node-virus-blocker";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.virusblocker.VirusBlockerNode",
                                 "com.untangle.node.virus.VirusSettings" };
        newNames = new String[] {"com.untangle.node.virus_blocker.VirusBlockerApp",
                                 "com.untangle.node.virus_blocker.VirusSettings"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename clam to virus-blocker-lite
        oldName = "untangle-node-clam";
        newName = "untangle-node-virus-blocker-lite";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.clam.ClamNode",
                                 "com.untangle.node.virus.VirusSettings"};
        newNames = new String[] {"com.untangle.node.virus_blocker_lite.VirusBlockerLiteApp",
                                 "com.untangle.node.virus_blocker.VirusSettings"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename spamblocker to spam-blocker
        oldName = "untangle-node-spamblocker";
        newName = "untangle-node-spam-blocker";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.spamblocker.SpamBlockerNode",
                                 "com.untangle.node.spam."};
        newNames = new String[] {"com.untangle.node.spam_blocker.SpamBlockerApp",
                                 "com.untangle.node.spam_blocker."};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename spamassassin to spam-blocker-lite
        oldName = "untangle-node-spamassassin";
        newName = "untangle-node-spam-blocker-lite";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.spamassassin.SpamAssassinNode",
                                 "com.untangle.node.spam."};
        newNames = new String[] {"com.untangle.node.spam_blocker_lite.SpamBlockerLiteApp",
                                 "com.untangle.node.spam_blocker."};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename phish to phish-blocker
        oldName = "untangle-node-phish";
        newName = "untangle-node-phish-blocker";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.phish.PhishNode",
                                 "com.untangle.node.phish.PhishSettings",
                                 "com.untangle.node.spam.SpamSmtpConfig"};
        newNames = new String[] {"com.untangle.node.phish_blocker.PhishBlockerApp",
                                 "com.untangle.node.phish_blocker.PhishBlockerSettings",
                                 "com.untangle.node.spam_blocker.SpamSmtpConfig"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename webcache to web-cache
        oldName = "untangle-node-webcache";
        newName = "untangle-node-web-cache";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.webcache.WebCacheNode",
                                 "com.untangle.node.webcache.WebCacheSettings",
                                 "com.untangle.node.webcache.WebCacheRule"};
        newNames = new String[] {"com.untangle.node.web_cache.WebCacheApp",
                                 "com.untangle.node.web_cache.WebCacheSettings",
                                 "com.untangle.node.web_cache.WebCacheRule"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename bandwidth to bandwidth-control
        oldName = "untangle-node-bandwidth";
        newName = "untangle-node-bandwidth-control";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.bandwidth.BandwidthNodeImpl",
                                 "com.untangle.node.bandwidth.BandwidthRule",
                                 "com.untangle.node.bandwidth.BandwidthRuleAction",
                                 "com.untangle.node.bandwidth.BandwidthRuleMatcher",
                                 "com.untangle.node.bandwidth.BandwidthSettings"};
        newNames = new String[] {"com.untangle.node.bandwidth_control.BandwidthControlApp",
                                 "com.untangle.node.bandwidth_control.BandwidthControlRule",
                                 "com.untangle.node.bandwidth_control.BandwidthControlRuleAction",
                                 "com.untangle.node.bandwidth_control.BandwidthControlRuleCondition",
                                 "com.untangle.node.bandwidth_control.BandwidthControlSettings"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename classd to application-control
        oldName = "untangle-node-classd";
        newName = "untangle-node-application-control";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.classd.ClassDNodeImpl",
                                 "com.untangle.node.classd.ClassDLogicRule",
                                 "com.untangle.node.classd.ClassDLogicRuleAction",
                                 "com.untangle.node.classd.ClassDLogicRuleMatcher",
                                 "com.untangle.node.classd.ClassDProtoRule",
                                 "com.untangle.node.classd.ClassDProtoRuleAction",
                                 "com.untangle.node.classd.ClassDSettings"};
        newNames = new String[] {"com.untangle.node.application_control.ApplicationControlApp",
                                 "com.untangle.node.application_control.ApplicationControlLogicRule",
                                 "com.untangle.node.application_control.ApplicationControlLogicRuleAction",
                                 "com.untangle.node.application_control.ApplicationControlLogicRuleCondition",
                                 "com.untangle.node.application_control.ApplicationControlProtoRule",
                                 "com.untangle.node.application_control.ApplicationControlProtoRuleAction",
                                 "com.untangle.node.application_control.ApplicationControlSettings"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename protofilter to application-control-lite
        oldName = "untangle-node-protofilter";
        newName = "untangle-node-application-control-lite";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.protofilter.ProtoFilterImpl",
                                 "com.untangle.node.protofilter.ProtoFilterSettings",
                                 "com.untangle.node.protofilter.ProtoFilterPattern"};
        newNames = new String[] {"com.untangle.node.application_control_lite.ApplicationControlLiteApp",
                                 "com.untangle.node.application_control_lite.ApplicationControlLiteSettings",
                                 "com.untangle.node.application_control_lite.ApplicationControlLitePattern"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename https-casing to ssl-inspector
        oldName = "untangle-casing-https";
        newName = "untangle-casing-ssl-inspector";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.https.HttpsNodeImpl",
                                 "com.untangle.node.https.HttpsRule",
                                 "com.untangle.node.https.HttpsRuleAction",
                                 "com.untangle.node.https.HttpsRuleMatcher",
                                 "com.untangle.node.https.HttpsSettings"};
        newNames = new String[] {"com.untangle.node.ssl_inspector.SslInspectorApp",
                                 "com.untangle.node.ssl_inspector.SslInspectorRule",
                                 "com.untangle.node.ssl_inspector.SslInspectorRuleAction",
                                 "com.untangle.node.ssl_inspector.SslInspectorRuleCondition",
                                 "com.untangle.node.ssl_inspector.SslInspectorSettings"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename capture to captive-portal
        oldName = "untangle-node-capture";
        newName = "untangle-node-captive-portal";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.capture.CaptureNodeImpl",
                                 "com.untangle.node.capture.CaptureRule",
                                 "com.untangle.node.capture.CaptureRuleMatcher",
                                 "com.untangle.node.capture.PassedAddress",
                                 "com.untangle.node.capture.CaptureSettings"};
        newNames = new String[] {"com.untangle.node.captive_portal.CaptivePortalApp",
                                 "com.untangle.node.captive_portal.CaptureRule",
                                 "com.untangle.node.captive_portal.CaptureRuleCondition",
                                 "com.untangle.node.captive_portal.PassedAddress",
                                 "com.untangle.node.captive_portal.CaptivePortalSettings"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }

            // rewrite .py files
            UvmContextFactory.context().execManager().execResult("find /usr/share/untangle/web/capture -type f | xargs /bin/sed -e 's/untangle-node-capture/untangle-node-captive-portal/g' -i ");

        }

        // ****************************
        // nothing changed for firewall
        // ****************************

        // rename adblocker to ad-blocker
        oldName = "untangle-node-adblocker";
        newName = "untangle-node-ad-blocker";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.adblocker.AdBlockerImpl",
                                 "com.untangle.node.adblocker.AdBlockerSettings"};
        newNames = new String[] {"com.untangle.node.ad_blocker.AdBlockerApp",
                                 "com.untangle.node.ad_blocker.AdBlockerSettings"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename reporting to reports
        oldName = "untangle-node-reporting";
        newName = "untangle-node-reports";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.reporting.ReportingNodeImpl",
                                 "com.untangle.node.reporting.ReportingSettings",
                                 "com.untangle.node.reporting.ReportingUser",
                                 "com.untangle.node.reporting.ReportingHostnameMapEntry",
                                 "com.untangle.node.reporting.ReportEntry",
                                 "com.untangle.node.reporting.SqlCondition",
                                 "com.untangle.node.reporting.AlertRule",
                                 "com.untangle.node.reporting.AlertRuleMatcher",
                                 "com.untangle.node.reporting",
                                 "reportingUsers"
        };
        newNames = new String[] {"com.untangle.node.reports.ReportsApp",
                                 "com.untangle.node.reports.ReportsSettings",
                                 "com.untangle.node.reports.ReportsUser",
                                 "com.untangle.node.reports.ReportsHostnameMapEntry",
                                 "com.untangle.node.reports.ReportEntry",
                                 "com.untangle.node.reports.SqlCondition",
                                 "com.untangle.node.reports.AlertRule",
                                 "com.untangle.node.reports.AlertRuleCondition",
                                 "com.untangle.node.reports",
                                 "reportsUsers"
        };
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename policy to policy-manager
        oldName = "untangle-node-policy";
        newName = "untangle-node-policy-manager";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.policy.PolicyManagerImpl",
                                 "com.untangle.node.policy.PolicyManagerSettings",
                                 "com.untangle.node.policy.PolicyRule",
                                 "com.untangle.node.policy.PolicyRuleMatcher",
                                 "com.untangle.node.policy.PolicySettings"};
        newNames = new String[] {"com.untangle.node.policy_manager.PolicyManagerApp",
                                 "com.untangle.node.policy_manager.PolicyManagerSettings",
                                 "com.untangle.node.policy_manager.PolicyRule",
                                 "com.untangle.node.policy_manager.PolicyRuleCondition",
                                 "com.untangle.node.policy_manager.PolicySettings",};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename adconnector to directory-connector
        oldName = "untangle-node-adconnector";
        newName = "untangle-node-directory-connector";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.adconnector.DirectoryConnectorImpl",
                                 "com.untangle.node.adconnector.ActiveDirectorySettings",
                                 "com.untangle.node.adconnector.RadiusSettings",
                                 "com.untangle.node.adconnector.DirectoryConnectorSettings"};
        newNames = new String[] {"com.untangle.node.directory_connector.DirectoryConnectorApp",
                                 "com.untangle.node.directory_connector.ActiveDirectorySettings",
                                 "com.untangle.node.directory_connector.RadiusSettings",                                 
                                 "com.untangle.node.directory_connector.DirectoryConnectorSettings"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename faild to wan-failover
        oldName = "untangle-node-faild";
        newName = "untangle-node-wan-failover";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.faild.FailDImpl",
                                 "com.untangle.node.faild.WanTestSettings",
                                 "com.untangle.node.faild.FailDSettings"};
        newNames = new String[] {"com.untangle.node.wan_failover.WanFailoverApp",
                                 "com.untangle.node.wan_failover.WanTestSettings",
                                 "com.untangle.node.wan_failover.WanFailoverSettings"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename splitd to wan-balancer
        oldName = "untangle-node-splitd";
        newName = "untangle-node-wan-balancer";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.splitd.SplitDImpl",
                                 "com.untangle.node.splitd.RouteRule",
                                 "com.untangle.node.splitd.RouteRuleAction",
                                 "com.untangle.node.splitd.SplitDSettings"};
        newNames = new String[] {"com.untangle.node.wan_balancer.WanBalancerApp",
                                 "com.untangle.node.wan_balancer.RouteRule",
                                 "com.untangle.node.wan_balancer.RouteRuleAction",
                                 "com.untangle.node.wan_balancer.WanBalancerSettings"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename ipsec to ipsec-vpn
        oldName = "untangle-node-ipsec";
        newName = "untangle-node-ipsec-vpn";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.ipsec.IPsecNodeImpl",
                                 "com.untangle.node.ipsec.IPsecTunnel",
                                 "com.untangle.node.ipsec.VirtualListen",
                                 "com.untangle.node.ipsec.IPsecSettings"};
        newNames = new String[] {"com.untangle.node.ipsec_vpn.IpsecVpnApp",
                                 "com.untangle.node.ipsec_vpn.IpsecVpnTunnel",
                                 "com.untangle.node.ipsec_vpn.VirtualListen",
                                 "com.untangle.node.ipsec_vpn.IpsecVpnSettings"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // ***************************
        // nothing changed for openvpn
        // ***************************

        // rename idps to intrusion-prevention
        oldName = "untangle-node-idps";
        newName = "untangle-node-intrusion-prevention";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {};
        newNames = new String[] {};

        // disabled
        // the below strings are not in the settings and this operation takes too long because many servers have 6gigs+ of idps settings
        // the idps settings are a bit different and do not store the java class hints. no need to convert them
        // disabled
        // oldNames = new String[] {"com.untangle.node.idps.IdpsNode"};
        // newNames = new String[] {"com.untangle.node.intrusion_prevention.IntrusionPreventionApp"};

        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            // copy only current settings (no backups)
            UvmContextFactory.context().execManager().execResult("mkdir " + System.getProperty("uvm.settings.dir") + "/untangle-node-intrusion-prevention");
            UvmContextFactory.context().execManager().execResult("find /usr/share/untangle/settings/untangle-node-idps -iregex '.*/settings_[0-9]*.js' -exec cp {} /usr/share/untangle/settings/untangle-node-intrusion-prevention/ \\;");
            UvmContextFactory.context().execManager().execResult("rm -f " + System.getProperty("uvm.settings.dir") + "/untangle-node-idps/*");
            UvmContextFactory.context().execManager().execResult("rmdir " + System.getProperty("uvm.settings.dir") + "/untangle-node-idps");

            for ( i = 0 ; i < oldNames.length ; i++ ) {
               String oldStr = oldNames[i];
               String newStr = newNames[i];
               UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename boxbackup to configuration-backup
        oldName = "untangle-node-boxbackup";
        newName = "untangle-node-configuration-backup";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.boxbackup.BoxBackupImpl",
                                 "com.untangle.node.boxbackup.BoxBackupSettings"};
        newNames = new String[] {"com.untangle.node.configuration_backup.ConfigurationBackupApp",
                                 "com.untangle.node.configuration_backup.ConfigurationBackupSettings"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename branding to branding-manager
        oldName = "untangle-node-branding";
        newName = "untangle-node-branding-manager";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.branding.BrandingNodeImpl",
                                 "com.untangle.node.branding.BrandingSettings"};
        newNames = new String[] {"com.untangle.node.branding_manager.BrandingManagerApp",
                                 "com.untangle.node.branding_manager.BrandingManagerSettings"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // rename support to live-support
        oldName = "untangle-node-support";
        newName = "untangle-node-live-support";
        updateNodesFile(oldName,newName);
        oldNames = new String[] {"com.untangle.node.support.SupportImpl"};
        newNames = new String[] {"com.untangle.node.live_support.LiveSupportApp"};
        dirName = System.getProperty("uvm.settings.dir") + "/" + oldName;
        dir = new File(dirName);
        if ( dir.exists() && dir.isDirectory() ) {
            UvmContextFactory.context().execManager().execResult("/bin/mv " + dir + " " + System.getProperty("uvm.settings.dir") + "/" + newName);
            for ( i = 0 ; i < oldNames.length ; i++ ) {
                String oldStr = oldNames[i];
                String newStr = newNames[i];
                UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldStr + "/" + newStr + "/g' -i " + System.getProperty("uvm.settings.dir") + "/" + newName + "/*");
            }
        }

        // set version at 2 so we don't re-run conversion
        settings.setVersion( 2L );
        this.setSettings( settings );


        // 12.0 special - try to upgrade spamassassin sigs now that the server is hopefully online
        try {
            if ( ! (new File("/var/lib/spamassassin/3.004000/updates_spamassassin_org.cf")).exists() ) {
                String output = UvmContextFactory.context().execManager().execOutput("/etc/cron.daily/spamassassin");
            }
        } catch (Exception e) {
            logger.warn("Exception",e);
        }
            

    }

    private void updateNodesFile(String oldName, String newName)
    {
        UvmContextFactory.context().execManager().execResult("/bin/sed -e 's/" + oldName + "/" + newName + "/g' -i " + System.getProperty("uvm.settings.dir") + "/untangle-vm/nodes.js");
    }
}
