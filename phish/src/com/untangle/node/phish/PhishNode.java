/**
 * $Id$
 */
package com.untangle.node.phish;

import org.apache.log4j.Logger;

import com.untangle.node.spam.SpamNodeImpl;
import com.untangle.node.spam.SpamSettings;
import com.untangle.uvm.DaemonManager;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;

public class PhishNode extends SpamNodeImpl implements Phish
{
    private final Logger logger = Logger.getLogger(getClass());

    // We want to make sure that phish is before spam,
    // before virus in the pipeline (towards the client for smtp).
    protected final PipelineConnector connector = UvmContextFactory.context().pipelineFoundry().create("phish-smtp", this, null, new PhishSmtpHandler( this ), Fitting.SMTP_TOKENS, Fitting.SMTP_TOKENS, Affinity.CLIENT, 12);
    protected final PipelineConnector[] connectors = new PipelineConnector[] { connector };

    public PhishNode( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties, new PhishScanner() );
    }

    private void readNodeSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-node-phish/settings_" + nodeID + ".js";
        PhishSettings readSettings = null;
        
        logger.info("Loading settings from " + settingsFile);
        
        try {
            readSettings =  settingsManager.load( PhishSettings.class, settingsFile);
        } catch (Exception exn) {
            logger.error("Could not read node settings", exn);
        }

        try {
            if (readSettings == null) {
                logger.warn("No settings found... initializing with defaults");
                initializeSettings();
            }
            else {
                this.spamSettings = readSettings;
            }
        } catch (Exception exn) {
            logger.error("Could not apply node settings", exn);
        }
    }
    
    public PhishSettings getSettings()
    {
        return (PhishSettings)super.getSettings();
    }

    public void setSettings(PhishSettings newSettings)
    {
        logger.info("setSettings()");

        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-node-phish/settings_" + nodeID + ".js";

        try {
            settingsManager.save( PhishSettings.class, settingsFile, newSettings);
        } catch (Exception exn) {
            logger.error("Could not save PhishNode settings", exn);
            return;
        }

        super.setSettings(newSettings);
    }

    public void initializeSettings()
    {
        logger.info("Initializing Settings");

        PhishSettings tmpSpamSettings = new PhishSettings();
        configureSpamSettings(tmpSpamSettings);
        tmpSpamSettings.getSmtpConfig().setBlockSuperSpam(false); // no such thing as 'super' phish
        tmpSpamSettings.getSmtpConfig().setAllowTls(true); // allow TLS in phishing by default
        
        setSettings(tmpSpamSettings);
        initSpamDnsblList(tmpSpamSettings);
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    @Override
    protected void preInit()
    {
        readNodeSettings();
        SpamSettings ps = getSettings();
        ps.getSmtpConfig().setBlockSuperSpam(false);
        initSpamDnsblList(ps);
    }

    @Override
    protected void preStart()
    {
        UvmContextFactory.context().daemonManager().incrementUsageCount( "clamav-daemon" );
        UvmContextFactory.context().daemonManager().incrementUsageCount( "clamav-freshclam" );
        super.preStart();
    }

    @Override
    protected void postStop()
    {
        UvmContextFactory.context().daemonManager().decrementUsageCount( "clamav-daemon" );
        UvmContextFactory.context().daemonManager().decrementUsageCount( "clamav-freshclam" );
        super.postStop();
    }
}
