/**
 * $Id$
 */
package com.untangle.node.web_filter;

import java.net.InetAddress;
import java.util.List;
import java.util.LinkedList;

import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.Token;
import com.untangle.node.http.HeaderToken;

/**
 * The base implementation of the Web Filter.
 * The web filter lite and web filter implementation inherit this
 */
public abstract class WebFilterBase extends NodeBase implements WebFilter
{
    private static final String STAT_SCAN = "scan";
    private static final String STAT_BLOCK = "block";
    private static final String STAT_PASS = "pass";
    
    protected static final Logger logger = Logger.getLogger(WebFilterBase.class);
    
    protected final PipelineConnector connector;
    protected final PipelineConnector[] connectors;

    protected final WebFilterBaseReplacementGenerator replacementGenerator;

    protected volatile WebFilterSettings settings;

    protected final UnblockedSitesMonitor unblockedSitesMonitor;

    public WebFilterBase( NodeSettings nodeSettings, NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );
        
        this.replacementGenerator = buildReplacementGenerator();

        this.addMetric(new NodeMetric(STAT_SCAN, I18nUtil.marktr("Pages scanned")));
        this.addMetric(new NodeMetric(STAT_BLOCK, I18nUtil.marktr("Pages blocked")));
        this.addMetric(new NodeMetric(STAT_PASS, I18nUtil.marktr("Pages passed")));

        this.connector = UvmContextFactory.context().pipelineFoundry().create("web-filter", this, null, new WebFilterBaseHandler( this ), Fitting.HTTP_TOKENS, Fitting.HTTP_TOKENS, Affinity.CLIENT, 3, isPremium());
        this.connectors = new PipelineConnector[] { connector };
        
        String nodeName = this.getName();
        
        this.unblockedSitesMonitor = new UnblockedSitesMonitor(this);
    }

    public String getUnblockMode()
    {
        return settings.getUnblockMode();
    }

    public boolean isHttpsEnabledSni()
    {
        return settings.getEnableHttpsSni();
    }

    public boolean isHttpsEnabledSniCertFallback()
    {
        return settings.getEnableHttpsSniCertFallback();
    }

    public boolean isHttpsEnabledSniIpFallback()
    {
        return settings.getEnableHttpsSniIpFallback();
    }

    public WebFilterBlockDetails getDetails( String nonce )
    {
        return replacementGenerator.getNonceData(nonce);
    }

    public boolean unblockSite( String nonce, boolean global )
    {
        WebFilterBlockDetails bd = replacementGenerator.removeNonce(nonce);

        if (WebFilterSettings.UNBLOCK_MODE_NONE.equals(settings.getUnblockMode())) {
            logger.debug("attempting to unblock in WebFilterSettings.UNBLOCK_MODE_NONE");
            return false;
        } else if (WebFilterSettings.UNBLOCK_MODE_HOST.equals(settings.getUnblockMode())) {
            if (global) {
                logger.debug("attempting to unblock global in WebFilterSettings.UNBLOCK_MODE_HOST");
                return false;
            }
        } else if (WebFilterSettings.UNBLOCK_MODE_GLOBAL.equals(settings.getUnblockMode())) {
            // its all good
        }
        else  {
            logger.error("missing case: " + settings.getUnblockMode());
        }

        if (null == bd) {
            logger.debug("no BlockDetails for nonce");
            return false;
        } else if (global) {
            String site = bd.getWhitelistHost();
            if (null == site) {
                logger.warn("cannot unblock null host");
                return false;
            } else {
                logger.warn("permanently unblocking site: " + site);
                GenericRule sr = new GenericRule(site, site, "user unblocked", "unblocked by user", true);
                settings.getPassedUrls().add(sr);
                _setSettings(settings);

                return true;
            }
        } else {
            String site = bd.getWhitelistHost();
            if (null == site) {
                logger.warn("cannot unblock null host");
                return false;
            } else {
                logger.info("Temporarily unblocking site: " + site);
                InetAddress addr = bd.getClientAddress();

                unblockedSitesMonitor.addUnblockedSite(addr, site);
                getDecisionEngine().addUnblockedSite(addr, site);

                return true;
            }
        }
    }

    public void flushAllUnblockedSites()
    {
        logger.warn("Flushing all Unblocked sites...");
        getDecisionEngine().removeAllUnblockedSites();
    }
    
    public WebFilterSettings getSettings()
    {
        return this.settings;
    }
    
    public void setSettings( WebFilterSettings settings )
    {
        _setSettings(settings);
    }

    public List<GenericRule> getCategories()
    {
        return settings.getCategories();
    }

    public void setCategories( List<GenericRule> newCategories )
    {
        this.settings.setCategories(newCategories);

        _setSettings(this.settings);
    }

    public List<GenericRule> getBlockedExtensions()
    {
        return settings.getBlockedExtensions();
    }

    public void setBlockedExtensions( List<GenericRule> blockedExtensions )
    {
        this.settings.setBlockedExtensions(blockedExtensions);

        _setSettings(this.settings);
    }

    public List<GenericRule> getBlockedMimeTypes()
    {
        return settings.getBlockedMimeTypes();
    }

    public void setBlockedMimeTypes( List<GenericRule> blockedMimeTypes )
    {
        this.settings.setBlockedMimeTypes(blockedMimeTypes);

        _setSettings(this.settings);
    }

    public List<GenericRule> getBlockedUrls()
    {
        return settings.getBlockedUrls();
    }

    public void setBlockedUrls( List<GenericRule> blockedUrls )
    {
        this.settings.setBlockedUrls(blockedUrls);

        _setSettings(this.settings);
    }

    public List<GenericRule> getPassedClients() 
    {
        return settings.getPassedClients();
    }

    public void setPassedClients( List<GenericRule> passedClients )
    {
        this.settings.setPassedClients(passedClients);

        _setSettings(this.settings);
    }

    public List<GenericRule> getPassedUrls()
    {
        return settings.getPassedUrls();
    }

    public void setPassedUrls( List<GenericRule> passedUrls )
    {
        this.settings.setPassedUrls(passedUrls);

        _setSettings(this.settings);
    }

    public abstract DecisionEngine getDecisionEngine();

    public abstract String getNodeTitle();
    public abstract String getName();
    public abstract String getAppName();
    public abstract boolean isPremium();

    public Token[] generateResponse( String nonce, NodeTCPSession session, String uri, HeaderToken header )
    {
        return replacementGenerator.generateResponse( nonce, session, uri, header );
    }

    public abstract void initializeSettings( WebFilterSettings settings );

    public void initializeCommonSettings( WebFilterSettings settings )
    {
        if (logger.isDebugEnabled()) {
            logger.debug(getNodeSettings() + " init settings");
        }

        settings.setBlockedExtensions(_buildDefaultFileExtensionList());

        settings.setBlockedMimeTypes(_buildDefaultMimeTypeList());
    }

    public void incrementScanCount()
    {
        this.incrementMetric(STAT_SCAN);
    }

    public void incrementBlockCount()
    {
        this.incrementMetric(STAT_BLOCK);
    }

    public void incrementPassCount()
    {
        this.incrementMetric(STAT_PASS);
    }

    protected WebFilterBaseReplacementGenerator buildReplacementGenerator()
    {
        return new WebFilterBaseReplacementGenerator(getNodeSettings());
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        WebFilterSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-" + this.getAppName() + "/" + "settings_" + nodeID + ".js";
        
        try {
            readSettings = settingsManager.load( WebFilterSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            WebFilterSettings settings = new WebFilterSettings();

            this.initializeCommonSettings(settings);
            this.initializeSettings(settings);

            _setSettings(settings);

        }
        else {
            logger.info("Loading Settings...");

            // UPDATE settings if necessary
            
            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }
    }


    @Override
    protected void preStart( boolean isPermanentTransition )
    {
        getDecisionEngine().removeAllUnblockedSites();
        unblockedSitesMonitor.start();
    }

    @Override
    protected void postStop( boolean isPermanentTransition )
    {
        unblockedSitesMonitor.stop();
        getDecisionEngine().removeAllUnblockedSites();
    }

    protected String generateNonce( WebFilterBlockDetails details )
    {
        return replacementGenerator.generateNonce(details);
    }

    protected Token[] generateResponse( String nonce, NodeTCPSession session )
    {
        return replacementGenerator.generateResponse( nonce, session );
    }

    /**
     * Set the current settings to new Settings
     * And save the settings to disk
     */
    protected void _setSettings( WebFilterSettings newSettings )
    {
        /**
         * Prepare settings for saving
         * This makes sure certain things are always true, such as flagged == true if blocked == true
         */
        if (newSettings.getCategories() != null) {
            for (GenericRule rule : newSettings.getCategories()) {
                if (rule.getBlocked()) rule.setFlagged(Boolean.TRUE);
            }
        }
        if (newSettings.getBlockedUrls() != null) {
            for (GenericRule rule : newSettings.getBlockedUrls()) {
                if (rule.getBlocked()) rule.setFlagged(Boolean.TRUE);
            }
        }
        if (newSettings.getBlockedMimeTypes() != null) {
            for (GenericRule rule : newSettings.getBlockedMimeTypes()) {
                if (rule.getBlocked()) rule.setFlagged(Boolean.TRUE);
            }
        }
        if (newSettings.getBlockedExtensions() != null) {
            for (GenericRule rule : newSettings.getBlockedExtensions()) {
                if (rule.getBlocked()) rule.setFlagged(Boolean.TRUE);
            }
        }

        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "untangle-node-" + this.getAppName() + "/" + "settings_" + nodeID + ".js", newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}
    }

    private List<GenericRule> _buildDefaultMimeTypeList()
    {
        List<GenericRule> mimeTypes = new LinkedList<GenericRule>();
        mimeTypes.add(new GenericRule("application/octet-stream", "unspecified data", "byte stream", "application/octet-stream mime type", null, Boolean.FALSE, Boolean.FALSE));

        mimeTypes.add(new GenericRule("application/x-msdownload", "Microsoft download", "executable", "application/x-msdownload mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/exe", "executable", "executable", "application/exe mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-exe", "executable", "executable", "application/x-exe mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/dos-exe", "DOS executable", "executable", "application/dos-exe mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-winexe", "Windows executable", "executable", "application/x-winexe mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/msdos-windows", "MS-DOS executable", "executable", "application/msdos-windows mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-msdos-program", "MS-DOS program", "executable", "application/x-msdos-program mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-oleobject", "Microsoft OLE Object", "executable", "application/x-oleobject mime type", null, Boolean.FALSE, Boolean.FALSE));

        mimeTypes.add(new GenericRule("application/x-java-applet", "Java Applet", "executable", "application/x-java-applet mime type", null, Boolean.FALSE, Boolean.FALSE));

        mimeTypes.add(new GenericRule("audio/mpegurl", "MPEG audio URLs", "audio", "audio/mpegurl mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-mpegurl", "MPEG audio URLs", "audio", "audio/x-mpegurl mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/mp3", "MP3 audio", "audio", "audio/mp3 mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-mp3", "MP3 audio", "audio", "audio/x-mp3 mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/mpeg", "MPEG audio", "audio", "audio/mpeg mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/mpg", "MPEG audio", "audio", "audio/mpg mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-mpeg", "MPEG audio", "audio", "audio/x-mpeg mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-mpg", "MPEG audio", "audio", "audio/x-mpg mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-ogg", "Ogg Vorbis", "audio", "application/x-ogg mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/m4a", "MPEG 4 audio", "audio", "audio/m4a mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/mp2", "MP2 audio", "audio", "audio/mp2 mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/mp1", "MP1 audio", "audio", "audio/mp1 mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/ogg", "Ogg Vorbis", "audio", "application/ogg mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/wav", "Microsoft WAV", "audio", "audio/wav mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-wav", "Microsoft WAV", "audio", "audio/x-wav mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-pn-wav", "Microsoft WAV", "audio", "audio/x-pn-wav mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/aac", "Advanced Audio Coding", "audio", "audio/aac mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/midi", "MIDI audio", "audio", "audio/midi mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/mpeg", "MPEG audio", "audio", "audio/mpeg mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/aiff", "AIFF audio", "audio", "audio/aiff mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-aiff", "AIFF audio", "audio", "audio/x-aiff mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-pn-aiff", "AIFF audio", "audio", "audio/x-pn-aiff mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-pn-windows-acm", "Windows ACM", "audio", "audio/x-pn-windows-acm mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-pn-windows-pcm", "Windows PCM", "audio", "audio/x-pn-windows-pcm mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/basic", "8-bit u-law PCM", "audio", "audio/basic mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-pn-au", "Sun audio", "audio", "audio/x-pn-au mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/3gpp", "3GPP", "audio", "audio/3gpp mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/3gpp-encrypted", "encrypted 3GPP", "audio", "audio/3gpp-encrypted mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/scpls", "streaming mp3 playlists", "audio", "audio/scpls mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-scpls", "streaming mp3 playlists", "audio", "audio/x-scpls mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/smil", "SMIL", "audio", "application/smil mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/sdp", "Streaming Download Project", "audio", "application/sdp mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-sdp", "Streaming Download Project", "audio", "application/x-sdp mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/amr", "AMR codec", "audio", "audio/amr mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/amr-encrypted", "AMR encrypted codec", "audio", "audio/amr-encrypted mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/amr-wb", "AMR-WB codec", "audio", "audio/amr-wb mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/amr-wb-encrypted", "AMR-WB encrypted codec", "audio", "audio/amr-wb-encrypted mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-rn-3gpp-amr", "3GPP codec", "audio", "audio/x-rn-3gpp-amr mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-rn-3gpp-amr-encrypted", "3GPP-AMR encrypted codec", "audio", "audio/x-rn-3gpp-amr-encrypted mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-rn-3gpp-amr-wb", "3gpp-AMR-WB codec", "audio", "audio/x-rn-3gpp-amr-wb mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-rn-3gpp-amr-wb-encrypted", "3gpp-AMR_WB encrypted codec", "audio", "audio/x-rn-3gpp-amr-wb-encrypted mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/streamingmedia", "Streaming Media", "audio", "application/streamingmedia mime type", null, Boolean.FALSE, Boolean.FALSE));

        mimeTypes.add(new GenericRule("video/mpeg", "MPEG video", "video", "video/mpeg mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-ms-wma", "Windows Media", "video", "audio/x-ms-wma mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("video/quicktime", "QuickTime", "video", "video/quicktime mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("video/x-ms-asf", "Microsoft ASF", "video", "video/x-ms-asf mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("video/x-msvideo", "Microsoft AVI", "video", "video/x-msvideo mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("video/x-sgi-mov", "SGI movie", "video", "video/x-sgi-mov mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("video/3gpp", "3GPP video", "video", "video/3gpp mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("video/3gpp-encrypted", "3GPP encrypted video", "video", "video/3gpp-encrypted mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("video/3gpp2", "3GPP2 video", "video", "video/3gpp2 mime type", null, Boolean.FALSE, Boolean.FALSE));

        mimeTypes.add(new GenericRule("audio/x-realaudio", "RealAudio", "audio", "audio/x-realaudio mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("text/vnd.rn-realtext", "RealText", "text", "text/vnd.rn-realtext mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/vnd.rn-realaudio", "RealAudio", "audio", "audio/vnd.rn-realaudio mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-pn-realaudio", "RealAudio plug-in", "audio", "audio/x-pn-realaudio mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("image/vnd.rn-realpix", "RealPix", "image", "image/vnd.rn-realpix mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/vnd.rn-realmedia", "RealMedia", "video", "application/vnd.rn-realmedia mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/vnd.rn-realmedia-vbr", "RealMedia VBR", "video", "application/vnd.rn-realmedia-vbr mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/vnd.rn-realmedia-secure", "secure RealMedia", "video", "application/vnd.rn-realmedia-secure mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/vnd.rn-realaudio-secure", "secure RealAudio", "audio", "application/vnd.rn-realaudio-secure mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/x-realaudio-secure", "secure RealAudio", "audio", "audio/x-realaudio-secure mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("video/vnd.rn-realvideo-secure", "secure RealVideo", "video", "video/vnd.rn-realvideo-secure mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("video/vnd.rn-realvideo", "RealVideo", "video", "video/vnd.rn-realvideo mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/vnd.rn-realsystem-rmj", "RealSystem media", "video", "application/vnd.rn-realsystem-rmj mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/vnd.rn-realsystem-rmx", "RealSystem secure media", "video", "application/vnd.rn-realsystem-rmx mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("audio/rn-mpeg", "MPEG audio", "audio", "audio/rn-mpeg mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-shockwave-flash", "Macromedia Shockwave", "multimedia", "application/x-shockwave-flash mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-director", "Macromedia Shockwave", "multimedia", "application/x-director mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-authorware-bin", "Macromedia Authorware binary", "multimedia", "application/x-authorware-bin mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-authorware-map", "Macromedia Authorware shocked file", "multimedia", "application/x-authorware-map mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-authorware-seg", "Macromedia Authorware shocked packet", "multimedia", "application/x-authorware-seg mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/futuresplash", "Macromedia FutureSplash", "multimedia", "application/futuresplash mime type", null, Boolean.FALSE, Boolean.FALSE));

        mimeTypes.add(new GenericRule("application/zip", "ZIP", "archive", "application/zip mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-lzh", "LZH archive", "archive", "application/x-lzh mime type", null, Boolean.FALSE, Boolean.FALSE));

        mimeTypes.add(new GenericRule("image/gif", "Graphics Interchange Format", "image", "image/gif mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("image/png", "Portable Network Graphics", "image", "image/png mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("image/jpeg", "JPEG", "image", "image/jpeg mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("image/bmp", "Microsoft BMP", "image", "image/bmp mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("image/tiff", "Tagged Image File Format", "image", "image/tiff mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("image/x-freehand", "Macromedia Freehand", "image", "image/x-freehand mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("image/x-cmu-raster", "CMU Raster", "image", "image/x-cmu-raster mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("image/x-rgb", "RGB image", "image", "image/x-rgb mime type", null, Boolean.FALSE, Boolean.FALSE));

        mimeTypes.add(new GenericRule("text/css", "cascading style sheet", "text", "text/css mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("text/html", "HTML", "text", "text/html mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("text/plain", "plain text", "text", "text/plain mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("text/richtext", "rich text", "text", "text/richtext mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("text/tab-separated-values", "tab separated values", "text", "text/tab-separated-values mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("text/xml", "XML", "text", "text/xml mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("text/xsl", "XSL", "text", "text/xsl mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("text/x-sgml", "SGML", "text", "text/x-sgml mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("text/x-vcard", "vCard", "text", "text/x-vcard mime type", null, Boolean.FALSE, Boolean.FALSE));

        mimeTypes.add(new GenericRule("application/mac-binhex40", "Macintosh BinHex", "archive", "application/mac-binhex40 mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-stuffit", "Macintosh Stuffit archive", "archive", "application/x-stuffit mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/macwriteii", "MacWrite Document", "document", "application/macwriteii mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/applefile", "Macintosh File", "archive", "application/applefile mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/mac-compactpro", "Macintosh Compact Pro", "archive", "application/mac-compactpro mime type", null, Boolean.FALSE, Boolean.FALSE));

        mimeTypes.add(new GenericRule("application/x-bzip2", "block compressed", "compressed", "application/x-bzip2 mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-shar", "shell archive", "archive", "application/x-shar mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-gtar", "gzipped tar archive", "archive", "application/x-gtar mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-gzip", "gzip compressed", "compressed", "application/x-gzip mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-tar", "4.3BSD tar archive", "archive", "application/x-tar mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-ustar", "POSIX tar archive", "archive", "application/x-ustar mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-cpio", "old cpio archive", "archive", "application/x-cpio mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-bcpio", "POSIX cpio archive", "archive", "application/x-bcpio mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-sv4crc", "System V cpio with CRC", "archive", "application/x-sv4crc mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-compress", "UNIX compressed", "compressed", "application/x-compress mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-sv4cpio", "System V cpio", "archive", "application/x-sv4cpio mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-sh", "UNIX shell script", "executable", "application/x-sh mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-csh", "UNIX csh script", "executable", "application/x-csh mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-tcl", "Tcl script", "executable", "application/x-tcl mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/x-javascript", "JavaScript", "executable", "application/x-javascript mime type", null, Boolean.FALSE, Boolean.FALSE));

        mimeTypes.add(new GenericRule("application/x-excel", "Microsoft Excel", "document", "application/x-excel mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/mspowerpoint", "Microsoft Powerpoint", "document", "application/mspowerpoint mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/msword", "Microsoft Word", "document", "application/msword mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/wordperfect5.1", "Word Perfect", "document", "application/wordperfect5.1 mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/rtf", "Rich Text Format", "document", "application/rtf mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/pdf", "Adobe Acrobat", "document", "application/pdf mime type", null, Boolean.FALSE, Boolean.FALSE));
        mimeTypes.add(new GenericRule("application/postscript", "Postscript", "document", "application/postscript mime type", null, Boolean.FALSE, Boolean.FALSE));

        return mimeTypes;
    }

    private List<GenericRule> _buildDefaultFileExtensionList()
    {
        List<GenericRule> fileExtensions = new LinkedList<GenericRule>();

        // this third column is more of a description than a category, the way the client is using it
        // the second column is being used as the "category"
        fileExtensions.add(new GenericRule("exe", "executable", "an executable file format" , ".exe file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("ocx", "executable", "an executable file format", ".ocx file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("dll", "executable", "an executable file format", ".dll file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("cab", "executable", "an ActiveX executable file format", ".cab file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("bin", "executable", "an executable file format", ".bin file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("com", "executable", "an executable file format", ".com file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("jpg", "image", "an image file format", ".jpg file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("png", "image", "an image file format", ".png file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("gif", "image", "an image file format", ".gif file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("jar", "java", "a Java file format", ".jar file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("class", "java", "a Java file format", ".class file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("swf", "flash", "the flash file format", ".swf file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("mp3", "audio", "an audio file format", ".mp3 file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("wav", "audio", "an audio file format", ".wav file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("wmf", "audio", "an audio file format", ".wmf file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("mpg", "video", "a video file format", ".mpg file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("mov", "video", "a video file format", ".mov file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("avi", "video", "a video file format", ".avi file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("hqx", "archive", "an archived file format", ".hqx file", null, Boolean.FALSE, Boolean.FALSE));
        fileExtensions.add(new GenericRule("cpt", "compression", "a compressed file format", ".cpt file", null, Boolean.FALSE, Boolean.FALSE));

        return fileExtensions;
    }

    /**
     * This is a utility function to reassure that all the current categories are in the settings
     * Returns true if the category was added, false otherwise
     */
    private boolean addCategory(List<GenericRule> categories, String string, String name, String category, String description, boolean enabled, boolean blocked, boolean flagged)
    {
        if (categories == null) {
            logger.warn("Invalid arguments: categories is null");
            return false;
        }
        if (string == null) {
            logger.warn("Invalid arguments: string is null");
            return false;
        }
        if (name == null) {
            logger.warn("Invalid arguments: name is null");
            return false;
        }
            
        for (GenericRule rule : categories) {
            if (rule.getString().equals(string))
                return false;
        }

        boolean newBlocked = blocked;
        boolean newFlagged = flagged;

        logger.info("Adding Category: ( " + string + ", " + name + ", " + newBlocked + ", " + newFlagged + " )");
        categories.add(new GenericRule(string, name, category, description, enabled, newBlocked, newFlagged));
        return true;
    }

    /**
     * Adds the new v3 categories to the list
     * returns true if any were added
     */
    protected boolean addCategories(List<GenericRule> categories)
    {
        boolean added = false;
        
        added |= addCategory(categories, "200", "Abortion", null, "Web pages that discuss abortion from a historical, medical, legal, or other not overtly biased point of view.", true, false, false);
        added |= addCategory(categories, "201", "Abortion - Pro Choice", null, "Web pages that push the pro-choice viewpoint or otherwise overtly encourage abortions.", true, false, false);
        added |= addCategory(categories, "202", "Abortion - Pro Life", null, "Web pages that condemn abortion or otherwise overtly push a pro-life agenda.", true, false, false);
        added |= addCategory(categories, "243", "Advocacy Groups & Trade Associations", null, "Web pages dedicated to industry trade groups, lobbyists, unions, special interest groups, professional organizations and other associations comprised of members with common goals.", true, false, false);
        added |= addCategory(categories, "203", "Agriculture", null, "Web pages devoted to the science, art, and business of cultivating soil, producing crops, raising livestock, and products, services, tips, tricks, etc. related to farming.", true, false, false);
        added |= addCategory(categories, "16" , "Alcohol", null, "Web pages that promote, advocate or sell alcohol including beer, wine and hard liquor.", true, false, false);
        added |= addCategory(categories, "12" , "Anonymizer", null, "Web pages that promote proxies and anonymizers for surfing websites with the intent of circumventing filters.", true, true, true);
        added |= addCategory(categories, "204", "Architecture & Construction", null, "Web pages which involve construction, contractors, structural design, architecture and all businesses or services related to the design, building or engineering of structures and environments.", true, false, false);
        added |= addCategory(categories, "205", "Arts", null, "Web pages related to the development or display of the visual arts.", true, false, false);
        added |= addCategory(categories, "206", "Astrology & Horoscopes", null, "Web pages related to astrology, horoscopes, divination according to the stars, or the zodiac.", true, false, false);
        added |= addCategory(categories, "207", "Atheism & Agnosticism", null, "Web pages that pursue an anti-religion agenda or that challenge religious, spiritual, metaphysical, or supernatural beliefs.", true, false, false);
        added |= addCategory(categories, "208", "Auctions & Marketplaces", null, "Web pages devoted to person to person selling or trading of goods and services through classifieds, online auctions, or other means not including \"traditional\" online business-to-consumer models.", true, false, false);
        added |= addCategory(categories, "209", "Banking", null, "Web pages operated by or all about banks and credit unions, particularly online banking web applications, but excludes online brokerages.", true, false, false);
        added |= addCategory(categories, "210", "Biotechnology", null, "Web pages which include genetics research, biotechnology firms and research institutions.", true, false, false);
        added |= addCategory(categories, "211", "Botnet", null, "Web pages or compromised web servers running software that is used by hackers to send spam, phishing attacks and denial of service attacks.", true, true, true);
        added |= addCategory(categories, "212", "Businesses & Services (General)", null, "Web pages that include Businesses and Services, generally used unless there is a more specific category that better describes the actual business or service.", true, false, false);
        added |= addCategory(categories, "213", "Cartoons, Anime & Comic Books", null, "Web pages dedicated to animated TV shows and movies or to comic books and graphic novels.", true, false, false);
        added |= addCategory(categories, "214", "Catalogs", null, "Web pages that have product listings and catalogs but do not have an online shopping option.", true, false, false);
        added |= addCategory(categories, "216", "Chat", null, "Web pages with real-time chat rooms and messaging allowing strangers and friends to chat in groups both in public and private chats.", true, false, false);
        added |= addCategory(categories, "217", "Child Abuse Images", null, "Web pages that show the physical or sexual abuse / exploitation of children.", true, true, true);
        added |= addCategory(categories, "297", "Child Inappropriate", null, "Includes tasteless content and material such as web pages that show cruelty (e.g. to animals), bathroom humor, tasteless material or other material potentially inappropriate for children.", true, false, false);
        added |= addCategory(categories, "218", "Command and Control Centers", null, "Internet servers used to send commands to infected machines called \"bots.\"", true, true, true);
        added |= addCategory(categories, "306", "Community Forums", null, "Web pages dedicated to forums, newsgroups, email archives, bulletin boards, and other community-driven content.", true, false, false);
        added |= addCategory(categories, "1"  , "Compromised", null, "Web pages that have been compromised by someone other than the site owner, which appear to be legitimate, but house malicious code.", true, true, true);
        added |= addCategory(categories, "219", "Content Servers", null, "Web servers without any navigable web pages typically used to host images and other media files with the purpose of improving web page performance and site scalability.", true, false, false);
        added |= addCategory(categories, "220", "Contests & Surveys", null, "Web pages devoted to online sweepstakes, contests, giveaways and raffles typically designed to obtain consumer information and demographics, but also used as part of various marketing efforts.", true, false, false);
        added |= addCategory(categories, "222", "Coupons", null, "Web pages dedicated to listing promotional codes, coupons, etc., either for printing for retail use or codes for online shopping.", true, false, false);
        added |= addCategory(categories, "223", "Criminal Skills", null, "Web pages providing information on how to perpetrate illegal activities such as burglary, murder, bomb-making, lock picking, etc.", true, false, false);
        added |= addCategory(categories, "23" , "Dating", null, "Web pages that promote relationships such as dating and marriage.", true, false, false);
        added |= addCategory(categories, "225", "Educational Institutions", null, "Web pages for schools with an online presence including Universities, private and public schools and other real-world places of learning.", true, false, false);
        added |= addCategory(categories, "226", "Educational Materials & Studies", null, "Web pages with academic publications, journals, published research findings, curriculum, online learning courses and materials or study guides.", true, false, false);
        added |= addCategory(categories, "227", "Entertainment News & Celebrity Sites", null, "Web pages including news and gossip about celebrities, television shows, movies and show business in general.", true, false, false);
        added |= addCategory(categories, "228", "Entertainment Venues & Events", null, "Web pages devoted to venues used for entertainment including comedy clubs, night clubs, discos, festivals, theaters, playhouses, etc.", true, false, false);
        added |= addCategory(categories, "229", "Fashion & Beauty", null, "Web pages devoted to fashion and beauty information and tips. Includes web pages that market products or services related to fashion including clothing, jewelry, cosmetics and perfume.", true, false, false);
        added |= addCategory(categories, "230", "File Repositories", null, "Web pages including collections of shareware, freeware, open source, and other software downloads.", true, false, false);
        added |= addCategory(categories, "233", "Finance (General)", null, "Includes web pages that discuss economics, investing strategies, money management, retirement planning and tax planning.", true, false, false);
        added |= addCategory(categories, "231", "Fitness & Recreation", null, "Web pages with tips and information on fitness or recreational activities.", true, false, false);
        added |= addCategory(categories, "310", "Food & Restaurants", null, "Web pages related to food from restaurants and dining, to cooking and recipes.", true, false, false);
        added |= addCategory(categories, "20" , "Gambling", null, "Web pages which promote gambling, lotteries, casinos and betting agencies involving chance.", true, false, false);
        added |= addCategory(categories, "21" , "Games", null, "Web pages consisting of computer games, game producers and online gaming.", true, false, false);
        added |= addCategory(categories, "232", "Gay, Lesbian or Bisexual", null, "Web pages that cater to or discuss the gay, lesbian, bisexual or transgender lifestyle.", true, false, false);
        added |= addCategory(categories, "235", "Government Sponsored", null, "Web pages devoted to Government organizations, departments, or agencies. Includes police, fire (when employed by a city), elections commissions, elected representatives, government sponsored programs and research.", true, false, false);
        added |= addCategory(categories, "237", "Hacking", null, "Web pages with information or tools specifically intended to assist in online crime such as the unauthorized access to computers, but also pages with tools and information that enables fraud and other online crime.", true, false, false);
        added |= addCategory(categories, "3"  , "Hate Speech", null, "Web pages that promote extreme right/left wing groups, sexism, racism, religious hate and other discrimination.", true, false, false);
        added |= addCategory(categories, "238", "Health & Medical", null, "Web pages dedicated to personal health, medical services, medical equipment, procedures, mental health, finding and researching doctors, hospitals and clinics.", true, false, false);
        added |= addCategory(categories, "239", "Hobbies & Leisure", null, "Web pages which include tips and information about crafts, and hobbies such as sewing, stamp collecting, model airplane building, etc.", true, false, false);
        added |= addCategory(categories, "241", "Home & Office Furnishings", null, "Web pages that include furniture makers, retail furniture outlets, desks, couches, chairs, cabinets, etc.", true, false, false);
        added |= addCategory(categories, "240", "Home, Garden & Family", null, "Web pages which cover activities in the home and pertaining to the family. Includes tips and information about parenting, interior decorating , gardening, cleaning, family and entertaining.", true, false, false);
        added |= addCategory(categories, "37" , "Humor", null, "Web pages which include comics, jokes and other humorous content.", true, false, false);
        added |= addCategory(categories, "4"  , "Illegal Drugs", null, "Web pages that promote the use or information of common illegal drugs and the misuse of prescription drugs and compounds.", true, false, false);
        added |= addCategory(categories, "305", "Image Search", null, "Web pages and internet search engines used to search pictures and photos found across the Internet where the returned results include thumbnails of the found images.", true, false, false);
        added |= addCategory(categories, "244", "Information Security", null, "Web pages and companies that provide computer and network security services, hardware, software or information.", true, false, false);
        added |= addCategory(categories, "245", "Instant Messenger", null, "Instant messaging software and web pages that typically involve staying in touch with a list of \"buddies\" via messaging services.", true, false, false);
        added |= addCategory(categories, "246", "Insurance", null, "Web pages the cover any type of insurance, insurance company, or government insurance program from Medicare to car insurance to life insurance.", true, false, false);
        added |= addCategory(categories, "247", "Internet Phone & VOIP", null, "Web pages that allow users to make calls via the web or to download software that allows users to make calls over the Internet.", true, false, false);
        added |= addCategory(categories, "51" , "Job Search", null, "Web pages devoted to job searches or agencies, career planning and human resources.", true, false, false);
        added |= addCategory(categories, "248", "Kid's Pages", null, "Web pages specifically intended for young children (under 10) including entertainment, games, and recreational pages built with young children in mind.", true, false, false);
        added |= addCategory(categories, "311", "Legislation, Politics & Law", null, "Web pages covering legislation, the legislative process, politics, political parties, elections, elected officials and opinions on these topics.", true, false, false);
        added |= addCategory(categories, "249", "Lingerie, Suggestive & Pinup", null, "Web pages that refer specifically to photos and videos where the person who is the subject of the photo is wearing sexually provocative clothing such as lingerie.", true, false, false);
        added |= addCategory(categories, "250", "Literature & Books", null, "Web pages for published writings including fiction and non-fiction novels, poems and biographies.", true, false, false);
        added |= addCategory(categories, "251", "Login Screens", null, "Web pages which are used to login to a wide variety of services where the actual service is not known, but could be any of several categories (e.g. Yahoo and Google login pages).", true, false, false);
        added |= addCategory(categories, "252", "Malware Call-Home", null, "Web pages identified as spyware which report information back to a particular URL.", true, true, true);
        added |= addCategory(categories, "253", "Malware Distribution Point", null, "Web pages that host viruses, exploits, and other malware.", true, true, true);
        added |= addCategory(categories, "254", "Manufacturing", null, "Web pages devoted to businesses involved in manufacturing and industrial production.", true, false, false);
        added |= addCategory(categories, "255", "Marijuana", null, "Web pages about the plant or about smoking the marijuana plant. Includes web pages on legalizing marijuana and using marijuana for medicinal purposes, marijuana facts and info pages.", true, false, false);
        added |= addCategory(categories, "308", "Marketing Services", null, "Web pages dedicated to advertising agencies and other marketing services that don't include online banner ads.", true, false, false);
        added |= addCategory(categories, "30" , "Military", null, "Web pages sponsored by the armed forces and government controlled agencies.", true, false, false);
        added |= addCategory(categories, "54" , "Miscellaneous", null, "Web pages that do not clearly fall into any other category.", true, false, false);
        added |= addCategory(categories, "256", "Mobile Phones", null, "Web pages which contain content for Mobile phone manufacturers and mobile phone companies' websites. Also includes sites that sell mobile phones and accessories.", true, false, false);
        added |= addCategory(categories, "309", "Motorized Vehicles", null, "Web pages which contain information about motorized vehicles including selling, promotion, or discussion. Includes motorized vehicle manufacturers and sites dedicated to the buying and selling of those vehicles.", true, false, false);
        added |= addCategory(categories, "38" , "Music", null, "Web pages that include internet radio and streaming media, musicians, bands, MP3 and media downloads.", true, false, false);
        added |= addCategory(categories, "236", "Nature & Conservation", null, "Web pages with information on environmental issues, sustainable living, ecology, nature and the environment.", true, false, false);
        added |= addCategory(categories, "39" , "News", null, "Web pages with general news information such as newspapers and magazines.", true, false, false);
        added |= addCategory(categories, "257", "No Content Found", null, "Web pages which contain no discernable content which can be used for classification purposes.", true, false, false);
        added |= addCategory(categories, "258", "Non-traditional Religion & Occult", null, "Web pages for religions outside of the mainstream or not in the top ten religions practiced in the world. Also includes occult and supernatural, extraterrestrial, folk religions, mysticism, cults and sects.", true, false, false);
        added |= addCategory(categories, "7"  , "Nudity", null, "Web pages that display full or partial nudity with no sexual references or intent.", true, false, false);
        added |= addCategory(categories, "259", "Nutrition & Diet", null, "Web pages on losing weight and eating healthy, diet plans, weight loss programs and food allergies.", true, false, false);
        added |= addCategory(categories, "49" , "Online Ads", null, "Companies, web pages, and sites responsible for hosting online advertisements including advertising graphics, banners, and pop-up content. Also includes web pages that host source code for dynamically generated ads and pop-ups.", true, false, false);
        added |= addCategory(categories, "260", "Online Financial Tools & Quotes", null, "Web pages for investment quotes, online portfolio tracking, financial calculation tools such as mortgage calculators, online tax preparation software, online bill payment and online budget tracking software.", true, false, false);
        added |= addCategory(categories, "261", "Online Information Management", null, "Web pages devoted to online personal information managers such as web applications that manage to-do lists, calendars, address books, etc.", true, false, false);
        added |= addCategory(categories, "262", "Online Shopping", null, "Websites and web pages that provide a means to purchase online.", true, false, false);
        added |= addCategory(categories, "263", "Online Stock Trading", null, "Investment brokerage web pages that allow online trading of stocks, mutual funds and other securities.", true, false, false);
        added |= addCategory(categories, "99" , "Parked", null, "Web pages that have been purchased to reserve the name but do not have any real content.", true, false, false);
        added |= addCategory(categories, "264", "Parks, Rec Facilities & Gyms", null, "Web pages which include parks and other areas designated for recreational activities such as swimming, skateboarding, rock climbing, as well as for non-professional sports such as community athletic fields.", true, false, false);
        added |= addCategory(categories, "265", "Pay To Surf", null, "Web sites that offer cash to users who install their software which displays ads and tracks browsing habits effectively allowing users to be paid while surfing the web.", true, false, false);
        added |= addCategory(categories, "266", "Peer-to-Peer", null, "Web pages that provide peer-to-peer (P2P) file sharing software.", true, false, false);
        added |= addCategory(categories, "312", "Personal Pages & Blogs", null, "Web pages including blogs, or a format for individuals to share news, opinions, and information about themselves. Also includes personal web pages about an individual or that individual's family.", true, false, false);
        added |= addCategory(categories, "267", "Personal Storage", null, "Web sites used for remote storage of files, sharing of large files, and remote Internet backups.", true, false, false);
        added |= addCategory(categories, "268", "Pets & Animals", null, "Web pages with information or products and services for pets and other animals including birds, fish, and insects.", true, false, false);
        added |= addCategory(categories, "18" , "Pharmacy", null, "Web pages which include prescribed medications and information about approved drugs and their medical use.", true, false, false);
        added |= addCategory(categories, "269", "Philanthropic Organizations", null, "Web pages with information regarding charities and other non-profit philanthropic organizations and foundations dedicated to altruistic activities.", true, false, false);
        added |= addCategory(categories, "5"  , "Phishing/Fraud", null, "Manipulated web pages and emails used for fraudulent purposes, also known as phishing.", true, true, true);
        added |= addCategory(categories, "270", "Photo Sharing", null, "Web pages that host digital photographs or allow users to upload, search, and exchange photos and images online.", true, false, false);
        added |= addCategory(categories, "273", "Physical Security", null, "Web pages devoted to businesses and services related to security products or other security aspects excluding computer security.", true, false, false);
        added |= addCategory(categories, "271", "Piracy & Copyright Theft", null, "Web pages that provide access to illegally obtained files such as pirated software (aka warez), pirated movies, pirated music, etc.", true, false, false);
        added |= addCategory(categories, "272", "Pornography", null, "Web pages which contain images or videos depicting sexual acts, sexual arousal, or explicit nude imagery intended to be sexual in nature.", true, true, true);
        added |= addCategory(categories, "47" , "Portal Sites", null, "General web pages with customized personal portals, including white/yellow pages.", true, false, false);
        added |= addCategory(categories, "274", "Private IP Address", null, "Web pages for Private IP addresses are those reserved for use internally in corporations or homes.", true, false, false);
        added |= addCategory(categories, "275", "Product Reviews & Price Comparisons", null, "Web pages dedicated to helping consumers comparison shop or choose products or stores, but don't offer online purchasing options.", true, false, false);
        added |= addCategory(categories, "276", "Profanity", null, "Web pages that use either frequent profanity or serious profanity.", true, false, false);
        added |= addCategory(categories, "277", "Professional Networking", null, "Social networking web pages intended for professionals and business relationship building.", true, false, false);
        added |= addCategory(categories, "278", "R-Rated", null, "Web pages whose primary purpose and majority of content is child appropriate, but who have regular or irregular sections of the site with sexually themed, non-educational material.", true, false, false);
        added |= addCategory(categories, "52" , "Real Estate", null, "Web pages possessing information about renting, purchasing, selling or financing real estate including homes, apartments, office space, etc.", true, false, false);
        added |= addCategory(categories, "279", "Redirect", null, "Web pages that redirect to other pages on other web sites.", true, false, false);
        added |= addCategory(categories, "280", "Reference Materials & Maps", null, "Web pages which contain reference materials and are specific to data compilations and reference shelf material such as atlases, dictionaries, encyclopedias, census and other reference data.", true, false, false);
        added |= addCategory(categories, "281", "Religions", null, "Web pages which cover main-stream popular religions world-wide as well as general religion topics and theology.", true, false, false);
        added |= addCategory(categories, "307", "Remote Access", null, "Web pages that provide remote access to private computers or networks, internal network file shares, and internal web applications.", true, false, false);
        added |= addCategory(categories, "282", "Retirement Homes & Assisted Living", null, "Web pages containing information on retirement homes and communities including nursing care and hospice care.", true, false, false);
        added |= addCategory(categories, "283", "School Cheating", null, "Web pages that contain test answers, pre-written term papers and essays, full math problem solvers that show the work and similar web sites that can be used to cheat on homework and tests.", true, false, false);
        added |= addCategory(categories, "48" , "Search Engines", null, "Web pages supporting the searching of web, newsgroups, pictures, directories, and other online content.", true, false, false);
        added |= addCategory(categories, "284", "Self-help & Addiction", null, "Web pages which include sites with information and help on gambling, drug, and alcohol addiction as well as sites helping with eating disorders such as anorexia, bulimia, and over-eating.", true, false, false);
        added |= addCategory(categories, "285", "Sex & Erotic", null, "Web pages with sexual content or products or services related to sex, but without nudity or other explicit pictures on the page.", true, true, true);
        added |= addCategory(categories, "286", "Sex Education & Pregnancy", null, "Web pages with educational materials and clinical explanations of sex, safe sex, birth control, pregnancy, and similar topics aimed at teens and children.", true, false, false);
        added |= addCategory(categories, "287", "Shipping & Logistics", null, "Web pages that promote management of inventory including transportation, warehousing, distribution, storage, order fulfillment and shipping.", true, false, false);
        added |= addCategory(categories, "288", "Social and Affiliation Organizations", null, "Web pages built around communities of people where users \"connect\" to other users.", true, false, false);
        added |= addCategory(categories, "45" , "Social Networking", null, "Social networking web pages and online communities built around communities of people where users \"connect\" to other users.", true, false, false);
        added |= addCategory(categories, "289", "Software, Hardware & Electronics", null, "Web pages with information about or makers of computer equipment, computer software, hardware, peripherals, data networks, computer services and electronics.", true, false, false);
        added |= addCategory(categories, "53" , "Spam", null, "Products and web pages promoted through spam techniques.", true, false, false);
        added |= addCategory(categories, "313", "Sport Fighting", null, "Web pages dedicated to training and contests involving fighting disciplines and multi-person combat sports such as martial arts, boxing, wrestling, and fencing.", true, false, false);
        added |= addCategory(categories, "290", "Sport Hunting", null, "Web pages covering recreational hunting of live animals.", true, false, false);
        added |= addCategory(categories, "291", "Sports", null, "Web pages covering competitive sports in which multiple people or teams compete in both athletic (e.g. football) and non-athletic competitions (e.g. billiards).", true, false, false);
        added |= addCategory(categories, "292", "Spyware & Questionable Software", null, "Web pages containing software that reports information back to a central server such as spyware or keystroke loggers.", true, true, true);
        added |= addCategory(categories, "293", "Streaming & Downloadable Audio", null, "Web pages with repositories of music or that provide streaming music or other audio files that may pose a bandwidth risk to companies.", true, false, false);
        added |= addCategory(categories, "294", "Streaming & Downloadable Video", null, "Web pages with repositories of videos or that provide in-browser streaming videos that may pose a bandwidth risk to companies.", true, false, false);
        added |= addCategory(categories, "295", "Supplements & Compounds", null, "Web pages containing information on vitamins and other over-the-counter unregulated supplements and compounds.", true, false, false);
        added |= addCategory(categories, "296", "Swimsuits", null, "Web pages containing pictures of people wearing swimsuits. Does not include pictures of swimsuits on manikins or by themselves.", true, false, false);
        added |= addCategory(categories, "234", "Technology (General)", null, "Web pages which include web design, internet standards (such as RFCs), protocol specifications, and other broad technology discussions or news.", true, false, false);
        added |= addCategory(categories, "298", "Television & Movies", null, "Web pages about television shows and movies including reviews, show times, plot summaries, discussions, teasers, marketing sites, etc.", true, false, false);
        added |= addCategory(categories, "316", "Text Messaging & SMS", null, "Web pages used to send or receive simple message service (SMS) text messages between a web page and a mobile phone.", true, false, false);
        added |= addCategory(categories, "19" , "Tobacco", null, "Web pages promoting the use of tobacco related products (cigarettes, cigars, pipes).", true, false, false);
        added |= addCategory(categories, "299", "Torrent Repository", null, "Web pages that host repositories of torrent files, which are the instruction file for allowing a bit torrent client to download large files from peers.", true, false, false);
        added |= addCategory(categories, "300", "Toys", null, "Web pages dedicated to manufacturers of toys, including toy selling or marketing sites.", true, false, false);
        added |= addCategory(categories, "15" , "Translator", null, "Web pages which translate languages from one to another.", true, false, false);
        added |= addCategory(categories, "28" , "Travel", null, "Web pages which provide travel and tourism information, online booking or travel services such as airlines, car rentals, and hotels.", true, false, false);
        added |= addCategory(categories, "301", "Unreachable", null, "Web pages that give an error such as, \"Network Timeout\", \"The server at example.com is taking too long to respond,\" or \"Address Not Found\".", true, false, false);
        added |= addCategory(categories, "10" , "Violence", null, "Web pages that promote questionable activities such as violence and militancy.", true, false, false);
        added |= addCategory(categories, "11" , "Weapons", null, "Web pages that include guns and weapons when not used in a violent manner.", true, false, false);
        added |= addCategory(categories, "302", "Web Hosting, ISP & Telco", null, "Web pages for web hosting and blog hosting sites, Internet Service Providers (ISPs) and telecommunications (phone) companies.", true, false, false);
        added |= addCategory(categories, "46" , "Web-based Email", null, "Web pages which enable users to send and/or receive email through a web accessible email account.", true, false, false);
        added |= addCategory(categories, "303", "Web-based Greeting Cards", null, "Web pages that allow users to send or receive online greeting cards.", true, false, false);
        added |= addCategory(categories, "304", "Wikis", null, "Web pages or websites in which a community maintains a set of informational documents where anyone in the community can update the content.", true, false, false);

        return added;
    }
}
