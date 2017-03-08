/**
 * $Id$
 */
package com.untangle.node.web_cache; // IMPL

import java.util.Timer;
import java.util.Hashtable;
import java.util.LinkedList;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.nio.channels.Selector;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.node.PortRange;
import com.untangle.uvm.node.SessionTuple;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.Subscription;
import com.untangle.uvm.util.I18nUtil;

public class WebCacheApp extends NodeBase
{
    private final Logger logger = Logger.getLogger(getClass());
    private final Subscription webSub = new Subscription(Protocol.TCP,IPMaskedAddress.anyAddr,PortRange.ANY,IPMaskedAddress.anyAddr,new PortRange(80,80));

    protected static final String STAT_HIT = "hit";
    protected static final String STAT_MISS = "miss";
    protected static final String STAT_USER_BYPASS = "user-bypass";
    protected static final String STAT_SYSTEM_BYPASS = "system-bypass";

    private final String CLEAR_CACHE_SCRIPT = System.getProperty("uvm.home") + "/bin/web-cache-clear";

    private final WebCacheStreamHandler s_handler = new WebCacheStreamHandler(this);

    private final PipelineConnector connector;
    private final PipelineConnector[] connectors;

    protected WebCacheStatistics statistics;
    protected WebCacheSettings settings;
    protected boolean highLoadBypass = false;
    protected Timer timer;

    // size of the buffer we use to assemble the complete HTTP request from the client
    protected final int CLIENT_BUFFSIZE = 0x8000;

    // size of the buffer we use to stream cache hits from squid to client
    protected final int STREAM_BUFFSIZE = 0x4000;

    // milliseconds to wait for squid to respond (hit) or wakeup (miss) a client request
    protected final int SELECT_TIMEOUT = 5000;

    // set this value to enable raw socket logging
    protected final boolean SOCKET_DEBUG = false;

    public WebCacheApp( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        logger.debug("WebCache WebCacheApp()");

        this.addMetric(new NodeMetric(STAT_HIT, I18nUtil.marktr("Cache hits")));
        this.addMetric(new NodeMetric(STAT_MISS, I18nUtil.marktr("Cache misses")));
        this.addMetric(new NodeMetric(STAT_USER_BYPASS, I18nUtil.marktr("User Bypass")));
        this.addMetric(new NodeMetric(STAT_SYSTEM_BYPASS, I18nUtil.marktr("System Bypass")));

        this.connector = UvmContextFactory.context().pipelineFoundry().create("web-cache", this, webSub, s_handler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.SERVER, 16, true );
        this.connectors = new PipelineConnector[] { connector };

        statistics = new WebCacheStatistics();
    }

    public WebCacheStatistics getStatistics()
    {
        logger.debug("WebCache getStatistics()");
        return(statistics);
    }

    public WebCacheSettings getSettings()
    {
        logger.debug("WebCache getSettings()");
        return(settings);
    }

    public void setSettings(WebCacheSettings newSettings)
    {
        logger.debug("WebCache setSettings()");
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = getNodeSettings().getId().toString();

        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/untangle-node-web-cache/settings_" + nodeID + ".js" , newSettings );
        }
        catch (Throwable t) {
            WebCacheStackDump.error(logger,"WebCacheApp","setSettings()",t);
            return;
        }

        this.settings = newSettings;
    }

    public LinkedList<WebCacheRule> getRules()
    {
        logger.debug("WebCache getRules()");
        return(settings.getRules());
    }

    public void setRules(LinkedList<WebCacheRule> ruleList)
    {
        logger.debug("WebCache setRules()");
        settings.setRules(ruleList);
    }

    public int clearSquidCache()
    {
        logger.debug("WebCache ENTER clearSquidCache()");

            try
            {
                UvmContextFactory.context().execManager().exec( CLEAR_CACHE_SCRIPT );
            }

            catch (Throwable t)
            {
                WebCacheStackDump.error(logger,"WebCacheApp","clearSquidCache()",t);
                return(1);
            }

        logger.debug("WebCache LEAVE clearSquidCache()");
        return(0);
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    @Override
    protected void preStart( boolean isPermanentTransition )
    {
        UvmContextFactory.context().daemonManager().incrementUsageCount( "squid" );
        
        WebCacheParent.INSTANCE.connect();
        timer = new Timer();
        timer.schedule(new WebCacheTimer(this),60000,60000);
    }

    @Override
    protected void postStop( boolean isPermanentTransition )
    {
        UvmContextFactory.context().daemonManager().decrementUsageCount( "squid" );

        timer.cancel();
        WebCacheParent.INSTANCE.goodbye();
    }

    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = getNodeSettings().getId().toString();
        WebCacheSettings readSettings = null;

        try {
            readSettings = settingsManager.load( WebCacheSettings.class, System.getProperty("uvm.settings.dir") + "/untangle-node-web-cache/settings_" + nodeID + ".js" );

            if (readSettings == null) {
                settings = new WebCacheSettings();
                LinkedList<WebCacheRule> ruleList = new LinkedList<WebCacheRule>();
                ruleList.add(new WebCacheRule("maps.google.com",true));
                settings.setRules(ruleList);
            } else {
                this.settings = readSettings;
            }

            logger.debug("Settings:\n" + new org.json.JSONObject(this.settings).toString(2));
        }
        catch (Throwable t) {
            WebCacheStackDump.error(logger,"WebCacheApp","postInit()",t);
        }
    }

    public boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.WEB_CACHE))
            return true;
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.WEB_CACHE_OLDNAME))
            return true;
        return false;
    }
}
