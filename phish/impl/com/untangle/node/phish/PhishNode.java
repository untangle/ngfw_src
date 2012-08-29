/*
 * $HeadURL$
 */
package com.untangle.node.phish;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import org.apache.catalina.Valve;
import org.apache.log4j.Logger;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import com.untangle.node.spam.SpamNodeImpl;
import com.untangle.node.spam.SpamSettings;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenAdaptor;
import com.untangle.uvm.AppServerManager;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.OutsideValve;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.SettingsManager;

public class PhishNode extends SpamNodeImpl implements Phish
{
    private static final String SETTINGS_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/phish-convert-settings.py";
    
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * This stores a list of hashed suffix/prefix expressions representing sites that should be blocked because they are hosting phish pages
     * MORE INFO: http://code.google.com/apis/safebrowsing/developers_guide_v2.html
     */
    private static HashSet<String> googlePhish = new HashSet<String>();

    /**
     * This stores a list of suffix/prefix regulare expressions representing sites that should be blocked because they are hosting malware pages
     * MORE INFO: http://code.google.com/apis/safebrowsing/developers_guide_v2.html
     */
    private static HashSet<String> googleMalware = new HashSet<String>();

    private static int deployCount = 0;

    private EventLogQuery httpBlockedEventQuery;
    
    // We want to make sure that phish is before spam,
    // before virus in the pipeline (towards the client for smtp,
    // server for pop/imap).
    private final PipeSpec[] pipeSpecs = new PipeSpec[] {
        new SoloPipeSpec("phish-smtp", this, new TokenAdaptor(this, new PhishSmtpFactory(this)), Fitting.SMTP_TOKENS, Affinity.CLIENT, 12),
        new SoloPipeSpec("phish-pop",  this, new TokenAdaptor(this, new PhishPopFactory(this)),  Fitting.POP_TOKENS,  Affinity.SERVER, 12),
        new SoloPipeSpec("phish-imap", this, new TokenAdaptor(this, new PhishImapFactory(this)), Fitting.IMAP_TOKENS, Affinity.SERVER, 12),
        new SoloPipeSpec("phish-http", this, new TokenAdaptor(this, new PhishHttpFactory(this)), Fitting.HTTP_TOKENS, Affinity.CLIENT, 12)
    };

    private final Map<InetAddress, Set<String>> unblockedSites = new HashMap<InetAddress, Set<String>>();
    private final PhishReplacementGenerator replacementGenerator;

    // constructors -----------------------------------------------------------

    public PhishNode( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties, new PhishScanner() );

        replacementGenerator = new PhishReplacementGenerator(getNodeSettings());

        this.httpBlockedEventQuery = new EventLogQuery(I18nUtil.marktr("Blocked Web Events"),
                                                       "SELECT * from reports.n_http_events " +
                                                     " WHERE phish_action = 'B'" + 
                                                     " AND policy_id = :policyId" + 
                                                     " ORDER BY time_stamp DESC");
        
        synchronized (PhishNode.class) {
            updateMalwareList();
        }
    }

    // private methods --------------------------------------------------------

    private void readNodeSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsBase = System.getProperty("uvm.settings.dir") + "/untangle-node-phish/settings_" + nodeID;
        String settingsFile = settingsBase + ".js";
        PhishSettings readSettings = null;
        
        logger.info("Loading settings from " + settingsFile);
        
        try {
            readSettings =  settingsManager.load( PhishSettings.class, settingsBase);
        } catch (Exception exn) {
            logger.error("Could not read node settings", exn);
        }

        // if no settings found try getting them from the database
        if (readSettings == null) {
            logger.warn("No json settings found... attempting to import from database");
            
            try {
                String convertCmd = SETTINGS_CONVERSION_SCRIPT + " " + nodeID.toString() + " " + settingsFile;
                logger.warn("Running: " + convertCmd);
                UvmContextFactory.context().execManager().exec( convertCmd );
            } catch (Exception exn) {
                logger.error("Conversion script failed", exn);
            }

            try {
                readSettings = settingsManager.load( PhishSettings.class, settingsBase);
            } catch (Exception exn) {
                logger.error("Could not read node settings", exn);
            }
            
            if (readSettings != null) logger.warn("Database settings successfully imported");
        }

        try {
            if (readSettings == null) {
                logger.warn("No database or json settings found... initializing with defaults");
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

    public EventLogQuery[] getHttpEventQueries()
    {
        return new EventLogQuery[] { this.httpBlockedEventQuery };
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
        String settingsBase = System.getProperty("uvm.settings.dir") + "/untangle-node-phish/settings_" + nodeID;

        try {
            settingsManager.save( PhishSettings.class, settingsBase, newSettings);
        } catch (Exception exn) {
            logger.error("Could not save PhishNode settings", exn);
            return;
        }

        super.setSettings(newSettings);
    }

    public PhishBlockDetails getBlockDetails(String nonce)
    {
        return replacementGenerator.getNonceData(nonce);
    }

    public String getUnblockMode()
    {
        return "Host";
    }

    public boolean unblockSite(String nonce, boolean global)
    {
        PhishBlockDetails bd = replacementGenerator.getNonceData(nonce);

        // XXX we do not do global right now
        String site = bd.getWhitelistHost();
        if (null == site) {
            logger.warn("cannot unblock null host");
            return false;
        } else {
            logger.warn("temporarily unblocking site: " + site);
            InetAddress addr = bd.getClientAddress();

            synchronized (this) {
                Set<String> wl = unblockedSites.get(addr);
                if (null == wl) {
                    wl = new HashSet<String>();
                    unblockedSites.put(addr, wl);
                }
                wl.add(site);
            }

            return true;
        }
    }

    public void initializeSettings()
    {
        logger.info("Initializing Settings");

        PhishSettings tmpSpamSettings = new PhishSettings();
        tmpSpamSettings.setEnableGooglePhishList(true);
        configureSpamSettings(tmpSpamSettings);
        tmpSpamSettings.getSmtpConfig().setBlockSuperSpam(false);

        setSettings(tmpSpamSettings);
        initSpamDnsblList(tmpSpamSettings);
    }

    // protected methods ------------------------------------------------------

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
    protected void postInit()
    {
        deployWebAppIfRequired(logger);
    }

    @Override
    protected void postDestroy()
    {
        unDeployWebAppIfRequired(logger);
    }

    @Override
    public boolean startSpamAssassinDaemon() {
        return false; // does not apply to clamphish
    }

    @Override
    public boolean stopSpamAssassinDaemon() {
        return false; // does not apply to clamphish
    }

    @Override
    public boolean restartSpamAssassinDaemon() {
        return false; // does not apply to clamphish
    }

    // package private methods ------------------------------------------------

    Token[] generateResponse(PhishBlockDetails bd, NodeTCPSession session, boolean persistent)
    {
        return replacementGenerator.generateResponse(bd, session, persistent);
    }

    boolean isDomainUnblocked(String host, InetAddress clientAddr)
    {
        Set<String> l = unblockedSites.get(clientAddr);
        if (null != l) {
            for (String d = host; null != d; d = nextHost(d)) {
                if (l.contains(d)) {
                    return true;
                }
            }
        }

        return false;
    }

    protected HashSet<String> getMalwareList()
    {
        return googleMalware;
    }

    protected HashSet<String> getPhishList()
    {
        return googlePhish;
    }
    
    // private methods -------------------------------------------------------

    private static synchronized void deployWebAppIfRequired(Logger logger)
    {
        if (0 != deployCount++) {
            return;
        }

        UvmContext mctx = UvmContextFactory.context();
        AppServerManager asm = mctx.appServerManager();

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

        if (null != asm.loadInsecureApp("/phish", "phish", v)) {
            logger.debug("Deployed phish WebApp");
        } else {
            logger.error("Unable to deploy phish WebApp");
        }
    }

    private static synchronized void unDeployWebAppIfRequired(Logger logger)
    {
        if (0 != --deployCount) {
            return;
        }

        UvmContext mctx = UvmContextFactory.context();
        AppServerManager asm = mctx.appServerManager();

        if (asm.unloadWebApp("/phish")) {
            logger.debug("Unloaded phish WebApp");
        } else {
            logger.warn("Unable to unload phish WebApp");
        }
    }

    private String nextHost(String host)
    {
        int i = host.indexOf('.');
        if (0 > i || i == host.lastIndexOf('.')) {
            return null;  /* skip TLD */
        }

        return host.substring(i + 1);
    }

    private void updateMalwareList()
    {
        String googleUrlString = "http://safebrowsing.clients.google.com/safebrowsing" + "/download?"
            + "client=api"
            + "&appver=1.5.2"
            + "&pver=2.2"
            + "&apikey=ABQIAAAAcF3DrVo7y87-tH8HDXqeYBTJqIcXJiJ1Klr7Vk1tUUBxWLpa4w";
        //String googlePhishRequestBody   = "googpub-phish-shavar;";
        String googleMalwareRequestBody = "googpub-malware-shavar;";

        try {
            HttpClient hc = new HttpClient();
            HttpMethod get = new GetMethod(googleUrlString);
            get.setRequestHeader("Accept-Encoding", "gzip");
            get.setQueryString(googleMalwareRequestBody);
            hc.executeMethod(get);

            InputStream is = null;
            Header contentEncoding = get.getResponseHeader("Content-Encoding");
            if (contentEncoding != null && contentEncoding.getValue() != null && contentEncoding.getValue().equals("gzip")) {
                is = new GZIPInputStream(get.getResponseBodyAsStream());
            }
            else {
                is = get.getResponseBodyAsStream();
            }
        } catch (IOException e) {
            logger.error("Failed to update Malware URL list", e);
        }
        
//         try {
//             UrlList ul = new EncryptedUrlList(URL_BASE, "phish", "goog-black-hash", m, null);
//             urlDatabase.addBlacklist("goog-black-hash", ul);
//         } catch (IOException exn) {
//             logger.warn("could not open database", exn);
//         }

//         try {
//             UrlList ul = new EncryptedUrlList(URL_BASE, "phish", "goog-malware-hash", m, null);
//             urlDatabase.addBlacklist("goog-malware-hash", ul);
//         } catch (IOException exn) {
//             logger.warn("could not open database", exn);
//         }

//         urlDatabase.updateAll(true);
    }
}
