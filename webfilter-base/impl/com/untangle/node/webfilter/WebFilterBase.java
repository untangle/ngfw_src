/**
 * $Id$
 */
package com.untangle.node.webfilter;

import java.net.InetAddress;
import java.util.List;
import java.util.LinkedList;
import org.json.JSONString;

import org.apache.log4j.Logger;

import com.untangle.uvm.SettingsManager;
import com.untangle.node.token.Header;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenAdaptor;
import com.untangle.node.util.PartialListUtil;
import com.untangle.uvm.LocalAppServerManager;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.ListEventFilter;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.message.BlingBlinger;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.node.IPMaskedAddressRule;
import com.untangle.uvm.node.IPMaskedAddressValidator;
import com.untangle.uvm.node.MimeType;
import com.untangle.uvm.node.MimeTypeRule;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.node.Rule;
import com.untangle.uvm.node.Validator;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.vnet.TCPSession;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.node.util.SimpleExec;

/**
 * The base implementation of the Web Filter.
 * The web filter lite and web filter implementation inherit this
 */
public abstract class WebFilterBase extends AbstractNode implements WebFilter
{
    private static final String SETTINGS_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/webfilter-base-convert-settings.py";

    protected static int deployCount = 0;

    protected final Logger logger = Logger.getLogger(getClass());
    
    protected final WebFilterFactory factory = new WebFilterFactory(this);

    protected final PipeSpec httpPipeSpec = new SoloPipeSpec("http-blocker", this, new TokenAdaptor(this, factory), Fitting.HTTP_TOKENS, Affinity.CLIENT, 0);
    protected final PipeSpec[] pipeSpecs = new PipeSpec[] { httpPipeSpec };

    protected final WebFilterReplacementGenerator replacementGenerator;

    protected final EventLogger<WebFilterEvent> eventLogger;
    protected final EventLogger<UnblockEvent> unblockEventLogger;

    protected volatile WebFilterSettings settings;

    protected final PartialListUtil listUtil = new PartialListUtil();

    protected final BlingBlinger scanBlinger;
    protected final BlingBlinger passBlinger;
    protected final BlingBlinger blockBlinger;
    protected final BlingBlinger passLogBlinger;

    protected final BypassMonitor bypassMonitor;

    // constructors -----------------------------------------------------------

    public WebFilterBase()
    {
        this.replacementGenerator = buildReplacementGenerator();
        NodeContext tctx = getNodeContext();
        eventLogger = EventLoggerFactory.factory().getEventLogger(tctx);
        SimpleEventFilter<WebFilterEvent> sef = new WebFilterBlockedFilter(this);
        eventLogger.addSimpleEventFilter(sef);
        sef = new WebFilterAllFilter(this);
        eventLogger.addSimpleEventFilter(sef);
        sef = new WebFilterPassedFilter(this);
        eventLogger.addSimpleEventFilter(sef);

        unblockEventLogger = EventLoggerFactory.factory().getEventLogger(tctx);
        WebFilterUnblockedFilter ueaf = new WebFilterUnblockedFilter(this);
        unblockEventLogger.addSimpleEventFilter(ueaf);

        MessageManager lmm = LocalUvmContextFactory.context().messageManager();
        Counters c = lmm.getCounters(getNodeId());
        scanBlinger = c.addActivity("scan", I18nUtil.marktr("Pages scanned"), null, I18nUtil.marktr("SCAN"));
        blockBlinger = c.addActivity("block", I18nUtil.marktr("Pages blocked"), null, I18nUtil.marktr("BLOCK"));
        passBlinger = c.addActivity("pass", I18nUtil.marktr("Pages passed"), null, I18nUtil.marktr("PASS"));
        passLogBlinger = c.addMetric("log", I18nUtil.marktr("Passed by policy"), null);
        lmm.setActiveMetricsIfNotSet(getNodeId(), scanBlinger, blockBlinger, passBlinger, passLogBlinger);

        bypassMonitor = new BypassMonitor(this);
    }

    // WebFilter methods ------------------------------------------------------

    public EventManager<WebFilterEvent> getEventManager()
    {
        return eventLogger;
    }

    public EventManager<UnblockEvent> getUnblockEventManager()
    {
        return unblockEventLogger;
    }


    public String getUnblockMode()
    {
        return settings.getUnblockMode();
    }

    public boolean isHttpsEnabled()
    {
        return settings.getEnableHttps();
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

                UnblockEvent ue = new UnblockEvent(bd.getClientAddress(), true,
                                                   bd.getFormattedUrl(),
                                                   getVendor(), getNodeId().getPolicy(),
                                                   bd.getUid());
                unblockEventLogger.log(ue);
                return true;
            }
        } else {
            String site = bd.getWhitelistHost();
            if (null == site) {
                logger.warn("cannot unblock null host");
                return false;
            } else {
                logger.warn("temporarily unblocking site: " + site);
                InetAddress addr = bd.getClientAddress();

                bypassMonitor.addUnblockedSite(addr, site);
                getDecisionEngine().addUnblockedSite(addr, site);

                UnblockEvent ue = new UnblockEvent(addr, false,
                                                   bd.getFormattedUrl(),
                                                   getVendor(), getNodeId().getPolicy(), bd.getUid());
                unblockEventLogger.log(ue);
                return true;
            }
        }
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
        //XXX save & reconfigure
    }

    public List<GenericRule> getBlockedExtensions()
    {
        return settings.getBlockedExtensions();
    }

    public void setBlockedExtensions( List<GenericRule> blockedExtensions )
    {
        this.settings.setBlockedExtensions(blockedExtensions);
        //XXX save & reconfigure
    }

    public List<GenericRule> getBlockedMimeTypes()
    {
        return settings.getBlockedMimeTypes();
    }

    public void setBlockedMimeTypes( List<GenericRule> blockedMimeTypes )
    {
        this.settings.setBlockedMimeTypes(blockedMimeTypes);
        //XXX save & reconfigure
    }

    public List<GenericRule> getBlockedUrls()
    {
        return settings.getBlockedUrls();
    }

    public void setBlockedUrls( List<GenericRule> blockedUrls )
    {
        this.settings.setBlockedUrls(blockedUrls);
        //XXX save & reconfigure
    }

    public List<GenericRule> getPassedClients() 
    {
        return settings.getPassedClients();
    }

    public void setPassedClients( List<GenericRule> passedClients )
    {
        this.settings.setPassedClients(passedClients);
        //XXX save & reconfigure
    }

    public List<GenericRule> getPassedUrls()
    {
        return settings.getPassedUrls();
    }

    public void setPassedUrls( List<GenericRule> passedUrls )
    {
        this.settings.setPassedUrls(passedUrls);
        //XXX save & reconfigure
    }

    
    public Validator getValidator()
    {
        return new IPMaskedAddressValidator();
    }

    public abstract DecisionEngine getDecisionEngine();

    public abstract String getVendor();

    public abstract String getNodeTitle();

    public abstract String getName();

    public Token[] generateResponse( String nonce, TCPSession session, String uri, Header header, boolean persistent )
    {
        return replacementGenerator.generateResponse(nonce, session, uri,header, persistent);
    }

    public abstract void initializeSettings( WebFilterSettings settings );

    public void initializeCommonSettings( WebFilterSettings settings )
    {
        if (logger.isDebugEnabled()) {
            logger.debug(getNodeId() + " init settings");
        }


        List<GenericRule> s = new LinkedList<GenericRule>();

        // this third column is more of a description than a category, the way the client is using it
        // the second column is being used as the "category"
        s.add(new GenericRule("exe", "executable", "an executable file format" , null, false));
        s.add(new GenericRule("ocx", "executable", "an executable file format", null, false));
        s.add(new GenericRule("dll", "executable", "an executable file format", null, false));
        s.add(new GenericRule("cab", "executable", "an ActiveX executable file format", null, false));
        s.add(new GenericRule("bin", "executable", "an executable file format", null, false));
        s.add(new GenericRule("com", "executable", "an executable file format", null, false));
        s.add(new GenericRule("jpg", "image", "an image file format", null, false));
        s.add(new GenericRule("png", "image", "an image file format", null, false));
        s.add(new GenericRule("gif", "image", "an image file format", null, false));
        s.add(new GenericRule("jar", "java", "a Java file format", null, false));
        s.add(new GenericRule("class", "java", "a Java file format", null, false));
        s.add(new GenericRule("swf", "flash", "the flash file format", null, false));
        s.add(new GenericRule("mp3", "audio", "an audio file format", null, false));
        s.add(new GenericRule("wav", "audio", "an audio file format", null, false));
        s.add(new GenericRule("wmf", "audio", "an audio file format", null, false));
        s.add(new GenericRule("mpg", "video", "a video file format", null, false));
        s.add(new GenericRule("mov", "video", "a video file format", null, false));
        s.add(new GenericRule("avi", "video", "a video file format", null, false));
        s.add(new GenericRule("hqx", "archive", "an archived file format", null, false));
        s.add(new GenericRule("cpt", "compression", "a compressed file format", null, false));

        settings.setBlockedExtensions(s);

        List<GenericRule> m = new LinkedList<GenericRule>();
        m.add(new GenericRule("application/octet-stream", "unspecified data", "byte stream", null, false));

        m.add(new GenericRule("application/x-msdownload", "Microsoft download", "executable", null, false));
        m.add(new GenericRule("application/exe", "executable", "executable", null, false));
        m.add(new GenericRule("application/x-exe", "executable", "executable", null, false));
        m.add(new GenericRule("application/dos-exe", "DOS executable", "executable", null, false));
        m.add(new GenericRule("application/x-winexe", "Windows executable", "executable", null, false));
        m.add(new GenericRule("application/msdos-windows", "MS-DOS executable", "executable", null, false));
        m.add(new GenericRule("application/x-msdos-program", "MS-DOS program", "executable", null, false));
        m.add(new GenericRule("application/x-oleobject", "Microsoft OLE Object", "executable", null, false));

        m.add(new GenericRule("application/x-java-applet", "Java Applet", "executable", null, false));

        m.add(new GenericRule("audio/mpegurl", "MPEG audio URLs", "audio", null, false));
        m.add(new GenericRule("audio/x-mpegurl", "MPEG audio URLs", "audio", null, false));
        m.add(new GenericRule("audio/mp3", "MP3 audio", "audio", null, false));
        m.add(new GenericRule("audio/x-mp3", "MP3 audio", "audio", null, false));
        m.add(new GenericRule("audio/mpeg", "MPEG audio", "audio", null, false));
        m.add(new GenericRule("audio/mpg", "MPEG audio", "audio", null, false));
        m.add(new GenericRule("audio/x-mpeg", "MPEG audio", "audio", null, false));
        m.add(new GenericRule("audio/x-mpg", "MPEG audio", "audio", null, false));
        m.add(new GenericRule("application/x-ogg", "Ogg Vorbis", "audio", null, false));
        m.add(new GenericRule("audio/m4a", "MPEG 4 audio", "audio", null, false));
        m.add(new GenericRule("audio/mp2", "MP2 audio", "audio", null, false));
        m.add(new GenericRule("audio/mp1", "MP1 audio", "audio", null, false));
        m.add(new GenericRule("application/ogg", "Ogg Vorbis", "audio", null, false));
        m.add(new GenericRule("audio/wav", "Microsoft WAV", "audio", null, false));
        m.add(new GenericRule("audio/x-wav", "Microsoft WAV", "audio", null, false));
        m.add(new GenericRule("audio/x-pn-wav", "Microsoft WAV", "audio", null, false));
        m.add(new GenericRule("audio/aac", "Advanced Audio Coding", "audio", null, false));
        m.add(new GenericRule("audio/midi", "MIDI audio", "audio", null, false));
        m.add(new GenericRule("audio/mpeg", "MPEG audio", "audio", null, false));
        m.add(new GenericRule("audio/aiff", "AIFF audio", "audio", null, false));
        m.add(new GenericRule("audio/x-aiff", "AIFF audio", "audio", null, false));
        m.add(new GenericRule("audio/x-pn-aiff", "AIFF audio", "audio", null, false));
        m.add(new GenericRule("audio/x-pn-windows-acm", "Windows ACM", "audio", null, false));
        m.add(new GenericRule("audio/x-pn-windows-pcm", "Windows PCM", "audio", null, false));
        m.add(new GenericRule("audio/basic", "8-bit u-law PCM", "audio", null, false));
        m.add(new GenericRule("audio/x-pn-au", "Sun audio", "audio", null, false));
        m.add(new GenericRule("audio/3gpp", "3GPP", "audio", null, false));
        m.add(new GenericRule("audio/3gpp-encrypted", "encrypted 3GPP", "audio", null, false));
        m.add(new GenericRule("audio/scpls", "streaming mp3 playlists", "audio", null, false));
        m.add(new GenericRule("audio/x-scpls", "streaming mp3 playlists", "audio", null, false));
        m.add(new GenericRule("application/smil", "SMIL", "audio", null, false));
        m.add(new GenericRule("application/sdp", "Streaming Download Project", "audio", null, false));
        m.add(new GenericRule("application/x-sdp", "Streaming Download Project", "audio", null, false));
        m.add(new GenericRule("audio/amr", "AMR codec", "audio", null, false));
        m.add(new GenericRule("audio/amr-encrypted", "AMR encrypted codec", "audio", null, false));
        m.add(new GenericRule("audio/amr-wb", "AMR-WB codec", "audio", null, false));
        m.add(new GenericRule("audio/amr-wb-encrypted", "AMR-WB encrypted codec", "audio", null, false));
        m.add(new GenericRule("audio/x-rn-3gpp-amr", "3GPP codec", "audio", null, false));
        m.add(new GenericRule("audio/x-rn-3gpp-amr-encrypted", "3GPP-AMR encrypted codec", "audio", null, false));
        m.add(new GenericRule("audio/x-rn-3gpp-amr-wb", "3gpp-AMR-WB codec", "audio", null, false));
        m.add(new GenericRule("audio/x-rn-3gpp-amr-wb-encrypted", "3gpp-AMR_WB encrypted codec", "audio", null, false));
        m.add(new GenericRule("application/streamingmedia", "Streaming Media", "audio", null, false));

        m.add(new GenericRule("video/mpeg", "MPEG video", "video", null, false));
        m.add(new GenericRule("audio/x-ms-wma", "Windows Media", "video", null, false));
        m.add(new GenericRule("video/quicktime", "QuickTime", "video", null, false));
        m.add(new GenericRule("video/x-ms-asf", "Microsoft ASF", "video", null, false));
        m.add(new GenericRule("video/x-msvideo", "Microsoft AVI", "video", null, false));
        m.add(new GenericRule("video/x-sgi-mov", "SGI movie", "video", null, false));
        m.add(new GenericRule("video/3gpp", "3GPP video", "video", null, false));
        m.add(new GenericRule("video/3gpp-encrypted", "3GPP encrypted video", "video", null, false));
        m.add(new GenericRule("video/3gpp2", "3GPP2 video", "video", null, false));

        m.add(new GenericRule("audio/x-realaudio", "RealAudio", "audio", null, false));
        m.add(new GenericRule("text/vnd.rn-realtext", "RealText", "text", null, false));
        m.add(new GenericRule("audio/vnd.rn-realaudio", "RealAudio", "audio", null, false));
        m.add(new GenericRule("audio/x-pn-realaudio", "RealAudio plug-in", "audio", null, false));
        m.add(new GenericRule("image/vnd.rn-realpix", "RealPix", "image", null, false));
        m.add(new GenericRule("application/vnd.rn-realmedia", "RealMedia", "video", null, false));
        m.add(new GenericRule("application/vnd.rn-realmedia-vbr", "RealMedia VBR", "video", null, false));
        m.add(new GenericRule("application/vnd.rn-realmedia-secure", "secure RealMedia", "video", null, false));
        m.add(new GenericRule("application/vnd.rn-realaudio-secure", "secure RealAudio", "audio", null, false));
        m.add(new GenericRule("audio/x-realaudio-secure", "secure RealAudio", "audio", null, false));
        m.add(new GenericRule("video/vnd.rn-realvideo-secure", "secure RealVideo", "video", null, false));
        m.add(new GenericRule("video/vnd.rn-realvideo", "RealVideo", "video", null, false));
        m.add(new GenericRule("application/vnd.rn-realsystem-rmj", "RealSystem media", "video", null, false));
        m.add(new GenericRule("application/vnd.rn-realsystem-rmx", "RealSystem secure media", "video", null, false));
        m.add(new GenericRule("audio/rn-mpeg", "MPEG audio", "audio", null, false));
        m.add(new GenericRule("application/x-shockwave-flash", "Macromedia Shockwave", "multimedia", null, false));
        m.add(new GenericRule("application/x-director", "Macromedia Shockwave", "multimedia", null, false));
        m.add(new GenericRule("application/x-authorware-bin", "Macromedia Authorware binary", "multimedia", null, false));
        m.add(new GenericRule("application/x-authorware-map", "Macromedia Authorware shocked file", "multimedia", null, false));
        m.add(new GenericRule("application/x-authorware-seg", "Macromedia Authorware shocked packet", "multimedia", null, false));
        m.add(new GenericRule("application/futuresplash", "Macromedia FutureSplash", "multimedia", null, false));

        m.add(new GenericRule("application/zip", "ZIP", "archive", null, false));
        m.add(new GenericRule("application/x-lzh", "LZH archive", "archive", null, false));

        m.add(new GenericRule("image/gif", "Graphics Interchange Format", "image", null, false));
        m.add(new GenericRule("image/png", "Portable Network Graphics", "image", null, false));
        m.add(new GenericRule("image/jpeg", "JPEG", "image", null, false));
        m.add(new GenericRule("image/bmp", "Microsoft BMP", "image", null, false));
        m.add(new GenericRule("image/tiff", "Tagged Image File Format", "image", null, false));
        m.add(new GenericRule("image/x-freehand", "Macromedia Freehand", "image", null, false));
        m.add(new GenericRule("image/x-cmu-raster", "CMU Raster", "image", null, false));
        m.add(new GenericRule("image/x-rgb", "RGB image", "image", null, false));

        m.add(new GenericRule("text/css", "cascading style sheet", "text", null, false));
        m.add(new GenericRule("text/html", "HTML", "text", null, false));
        m.add(new GenericRule("text/plain", "plain text", "text", null, false));
        m.add(new GenericRule("text/richtext", "rich text", "text", null, false));
        m.add(new GenericRule("text/tab-separated-values", "tab separated values", "text", null, false));
        m.add(new GenericRule("text/xml", "XML", "text", null, false));
        m.add(new GenericRule("text/xsl", "XSL", "text", null, false));
        m.add(new GenericRule("text/x-sgml", "SGML", "text", null, false));
        m.add(new GenericRule("text/x-vcard", "vCard", "text", null, false));

        m.add(new GenericRule("application/mac-binhex40", "Macintosh BinHex", "archive", null, false));
        m.add(new GenericRule("application/x-stuffit", "Macintosh Stuffit archive", "archive", null, false));
        m.add(new GenericRule("application/macwriteii", "MacWrite Document", "document", null, false));
        m.add(new GenericRule("application/applefile", "Macintosh File", "archive", null, false));
        m.add(new GenericRule("application/mac-compactpro", "Macintosh Compact Pro", "archive", null, false));

        m.add(new GenericRule("application/x-bzip2", "block compressed", "compressed", null, false));
        m.add(new GenericRule("application/x-shar", "shell archive", "archive", null, false));
        m.add(new GenericRule("application/x-gtar", "gzipped tar archive", "archive", null, false));
        m.add(new GenericRule("application/x-gzip", "gzip compressed", "compressed", null, false));
        m.add(new GenericRule("application/x-tar", "4.3BSD tar archive", "archive", null, false));
        m.add(new GenericRule("application/x-ustar", "POSIX tar archive", "archive", null, false));
        m.add(new GenericRule("application/x-cpio", "old cpio archive", "archive", null, false));
        m.add(new GenericRule("application/x-bcpio", "POSIX cpio archive", "archive", null, false));
        m.add(new GenericRule("application/x-sv4crc", "System V cpio with CRC", "archive", null, false));
        m.add(new GenericRule("application/x-compress", "UNIX compressed", "compressed", null, false));
        m.add(new GenericRule("application/x-sv4cpio", "System V cpio", "archive", null, false));
        m.add(new GenericRule("application/x-sh", "UNIX shell script", "executable", null, false));
        m.add(new GenericRule("application/x-csh", "UNIX csh script", "executable", null, false));
        m.add(new GenericRule("application/x-tcl", "Tcl script", "executable", null, false));
        m.add(new GenericRule("application/x-javascript", "JavaScript", "executable", null, false));

        m.add(new GenericRule("application/x-excel", "Microsoft Excel", "document", null, false));
        m.add(new GenericRule("application/mspowerpoint", "Microsoft Powerpoint", "document", null, false));
        m.add(new GenericRule("application/msword", "Microsoft Word", "document", null, false));
        m.add(new GenericRule("application/wordperfect5.1", "Word Perfect", "document", null, false));
        m.add(new GenericRule("application/rtf", "Rich Text Format", "document", null, false));
        m.add(new GenericRule("application/pdf", "Adobe Acrobat", "document", null, false));
        m.add(new GenericRule("application/postscript", "Postscript", "document", null, false));

        settings.setBlockedMimeTypes(m);
    }

    public void incrementScanCount()
    {
        scanBlinger.increment();
    }

    public void incrementBlockCount()
    {
        blockBlinger.increment();
    }

    public void incrementPassCount()
    {
        passBlinger.increment();
    }

    public void incrementPassLogCount()
    {
        passLogBlinger.increment();
    }

    protected WebFilterReplacementGenerator buildReplacementGenerator()
    {
        return new WebFilterReplacementGenerator(getNodeId());
    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    @Override
    protected void postInit(String[] args)
    {
        SettingsManager settingsManager = LocalUvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeId().getId().toString();
        WebFilterSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-" + this.getName() + "/" + "settings_" + nodeID;
        
        try {
            readSettings = settingsManager.load( WebFilterSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        /**
         * If there are no settings, run the conversion script to see if there are any in the database
         * Then check again for the file
         */
        if (readSettings == null) {
            logger.warn("No settings found - Running conversion script to check DB");
            try {
                SimpleExec.SimpleExecResult result = null;
                logger.warn("Running: " + SETTINGS_CONVERSION_SCRIPT + " " + nodeID.toString() + " " + this.getName() + " " + settingsFileName + ".js");
                result = SimpleExec.exec( SETTINGS_CONVERSION_SCRIPT, new String[] { nodeID.toString(), this.getName(), settingsFileName + ".js"} , null, null, true, true, 1000*60, logger, true);
            } catch ( Exception e ) {
                logger.warn( "Conversion script failed.", e );
            } 

            try {
                readSettings = settingsManager.load( WebFilterSettings.class, settingsFileName );
                if (readSettings != null) {
                    logger.warn("Found settings imported from database");
                }
            } catch (SettingsManager.SettingsException e) {
                logger.warn("Failed to load settings:",e);
            }
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
            logger.info("Settings: " + this.settings.toJSONString());
        }

        deployWebAppIfRequired(logger);
    }

    @Override
    protected void postDestroy()
    {
        unDeployWebAppIfRequired(logger);
    }

    @Override
    protected void preStart() throws Exception
    {
        getDecisionEngine().removeAllUnblockedSites();
        bypassMonitor.start();
    }

    @Override
    protected void postStop()
    {
        bypassMonitor.stop();
        getDecisionEngine().removeAllUnblockedSites();
    }

    protected void log( WebFilterEvent se )
    {
        eventLogger.log(se);
    }

    protected void log( WebFilterEvent se, String host, int port, TCPNewSessionRequestEvent event )
    {
        /* only pass in the event if you don't want to log immediately. */
        if (event == null) {
            eventLogger.log(se);
        } else {
            event.sessionRequest().attach(se);
        }
    }

    protected String generateNonce( WebFilterBlockDetails details )
    {
        return replacementGenerator.generateNonce(details);
    }

    protected Token[] generateResponse( String nonce, TCPSession session, boolean persistent )
    {
        return replacementGenerator.generateResponse(nonce, session, persistent);
    }

    protected static synchronized void deployWebAppIfRequired( Logger logger )
    {
        if (0 != deployCount++) {
            return;
        }

        LocalUvmContext mctx = LocalUvmContextFactory.context();
        LocalAppServerManager asm = mctx.localAppServerManager();

        org.apache.catalina.Valve v = new com.untangle.uvm.util.OutsideValve()
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

        if (null != asm.loadInsecureApp("/webfilter", "webfilter", v)) {
            logger.debug("Deployed WebFilter WebApp");
        } else {
            logger.error("Unable to deploy WebFilter WebApp");
        }
    }

    protected static synchronized void unDeployWebAppIfRequired( Logger logger )
    {
        if (0 != --deployCount) {
            return;
        }

        LocalUvmContext mctx = LocalUvmContextFactory.context();
        LocalAppServerManager asm = mctx.localAppServerManager();

        if (asm.unloadWebApp("/webfilter")) {
            logger.debug("Unloaded WebFilter WebApp");
        } else {
            logger.warn("Unable to unload WebFilter WebApp");
        }
    }

    /**
     * Set the current settings to new Settings
     * And save the settings to disk
     */
    private void _setSettings( WebFilterSettings newSettings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = LocalUvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeId().getId().toString();
        try {
            settingsManager.save(WebFilterSettings.class, System.getProperty("uvm.settings.dir") + "/" + "untangle-node-" + this.getName() + "/" + "settings_" + nodeID, newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.info("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}
    }

}
