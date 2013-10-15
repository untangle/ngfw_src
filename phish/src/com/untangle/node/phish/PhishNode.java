/**
 * $Id$
 */
package com.untangle.node.phish;

import org.apache.log4j.Logger;

import com.untangle.node.spam.SpamNodeImpl;
import com.untangle.node.spam.SpamSettings;
import com.untangle.node.token.TokenAdaptor;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.node.clam.ClamDaemonController;

public class PhishNode extends SpamNodeImpl implements Phish
{
    private final Logger logger = Logger.getLogger(getClass());

    // We want to make sure that phish is before spam,
    // before virus in the pipeline (towards the client for smtp,
    // server for pop/imap).
    private final PipeSpec[] pipeSpecs = new PipeSpec[] {
        new SoloPipeSpec("phish-smtp", this, new TokenAdaptor(this, new PhishSmtpFactory(this)), Fitting.SMTP_TOKENS, Affinity.CLIENT, 12)
    };

    // constructors -----------------------------------------------------------

    public PhishNode( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties, new PhishScanner() );
    }

    // private methods --------------------------------------------------------

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
    
    // public methods ---------------------------------------------------------

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
        tmpSpamSettings.getSmtpConfig().setBlockSuperSpam(false);

        setSettings(tmpSpamSettings);
        initSpamDnsblList(tmpSpamSettings);
    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
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
        ClamDaemonController.getInstance().incrementUsageCount();
        super.preStart();
    }

    @Override
    protected void postStop()
    {
        ClamDaemonController.getInstance().decrementUsageCount();
        super.postStop();
    }
    
    @Override
    public boolean startSpamAssassinDaemon()
    {
        return false; // does not apply to clamphish
    }

    @Override
    public boolean stopSpamAssassinDaemon()
    {
        return false; // does not apply to clamphish
    }

    @Override
    public boolean restartSpamAssassinDaemon()
    {
        return false; // does not apply to clamphish
    }
}
