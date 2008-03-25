/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.webfilter;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.catalina.Valve;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.node.http.UserWhitelistMode;
import com.untangle.node.token.Header;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenAdaptor;
import com.untangle.uvm.LocalAppServerManager;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.ListEventFilter;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.node.IPMaddrRule;
import com.untangle.uvm.node.MimeType;
import com.untangle.uvm.node.MimeTypeRule;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.Rule;
import com.untangle.uvm.node.StringRule;
import com.untangle.uvm.util.ListUtil;
import com.untangle.uvm.util.OutsideValve;
import com.untangle.uvm.util.QueryUtil;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.vnet.TCPSession;

/**
 * Implementation of the Web Filter.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class WebFilterImpl extends AbstractNode implements WebFilter
{
    private static int deployCount = 0;

    private final Logger logger = Logger.getLogger(getClass());

    private final WebFilterFactory factory = new WebFilterFactory(this);

    private final PipeSpec pipeSpec = new SoloPipeSpec
        ("http-blocker", this, new TokenAdaptor(this, factory), Fitting.HTTP_TOKENS,
         Affinity.CLIENT, 0);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { pipeSpec };

    private final Blacklist blacklist = new Blacklist(this);
    private final WebFilterReplacementGenerator replacementGenerator;

    private final EventLogger<WebFilterEvent> eventLogger;

    private volatile WebFilterSettings settings;

    // constructors -----------------------------------------------------------

    public WebFilterImpl()
    {
        replacementGenerator = new WebFilterReplacementGenerator(getTid());
        NodeContext tctx = getNodeContext();
        eventLogger = EventLoggerFactory.factory().getEventLogger(tctx);
        SimpleEventFilter sef = new WebFilterBlockedFilter();
        eventLogger.addSimpleEventFilter(sef);
        ListEventFilter lef = new WebFilterAllFilter();
        eventLogger.addListEventFilter(lef);
        sef = new WebFilterWhitelistFilter();
        eventLogger.addSimpleEventFilter(sef);
        lef = new WebFilterPassedFilter();
        eventLogger.addListEventFilter(lef);
    }

    // WebFilter methods ----------------------------------------------------

    public WebFilterSettings getWebFilterSettings()
    {
        if( settings == null )
            logger.error("Settings not yet initialized. State: " + getNodeContext().getRunState() );
        return settings;
    }

    public void setWebFilterSettings(final WebFilterSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(settings);
                    WebFilterImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        blacklist.configure(settings);
        reconfigure();
    }

    public EventManager<WebFilterEvent> getEventManager()
    {
        return eventLogger;
    }

    public UserWhitelistMode getUserWhitelistMode()
    {
        return settings.getBaseSettings().getUserWhitelistMode();
    }

    public WebFilterBlockDetails getDetails(String nonce)
    {
        return replacementGenerator.getNonceData(nonce);
    }

    // XXX factor with SpywareImpl.unblockSite
    public boolean unblockSite(String nonce, boolean global)
    {
        WebFilterBlockDetails bd = replacementGenerator.removeNonce(nonce);

        switch (settings.getBaseSettings().getUserWhitelistMode()) {
        case NONE:
            logger.debug("attempting to unblock in UserWhitelistMode.NONE");
            return false;
        case USER_ONLY:
            if (global) {
                logger.debug("attempting to unblock global in UserWhitelistMode.USER_ONLY");
                return false;
            }
        case USER_AND_GLOBAL:
            // its all good
            break;
        default:
            logger.error("missing case: " + settings.getBaseSettings().getUserWhitelistMode());
            break;
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
                StringRule sr = new StringRule(site, site, "user whitelisted",
                                               "whitelisted by user", true);
                settings.getPassedUrls().add(sr);
                setWebFilterSettings(settings);

                return true;
            }
        } else {
            String site = bd.getWhitelistHost();
            if (null == site) {
                logger.warn("cannot unblock null host");
                return false;
            } else {
                String url = "http://" + site;
                logger.warn("temporarily unblocking site: " + site);
                InetAddress addr = bd.getClientAddress();

                blacklist.addWhitelistHost(addr, site);

                return true;
            }
        }
    }
    
    /**
     * Causes the blacklist to populate its arrays.
     */
    public void reconfigure()
    {
        LocalUvmContextFactory.context().newThread(new Runnable() {
                public void run() {
                    blacklist.reconfigure();
                }
            }).start();
    }    
    
	public WebFilterBaseSettings getBaseSettings() {
		return settings.getBaseSettings();
	}

	public void setBaseSettings(final WebFilterBaseSettings baseSettings) {
        TransactionWork tw = new TransactionWork() {
			public boolean doWork(Session s) {
		        settings.setBaseSettings(baseSettings);
				s.merge(settings);
				return true;
			}

			public Object getResult() {
				return null;
			}
		};
		getNodeContext().runTransaction(tw);
	}

	public List<BlacklistCategory> getBlacklistCategories(int start, int limit,
			String... sortColumns) {
        return getRules(
				"select hbs.blacklistCategories from WebFilterSettings hbs where hbs.tid = :tid ",
				start, limit, sortColumns);
	}

	public List<StringRule> getBlockedExtensions(int start, int limit,
			String... sortColumns) {
        return getRules(
				"select hbs.blockedExtensions from WebFilterSettings hbs where hbs.tid = :tid ",
				start, limit, sortColumns);
	}

	public List<MimeTypeRule> getBlockedMimeTypes(int start, int limit,
			String... sortColumns) {
        return getRules(
				"select hbs.blockedMimeTypes from WebFilterSettings hbs where hbs.tid = :tid ",
				start, limit, sortColumns);
	}

	public List<StringRule> getBlockedUrls(int start, int limit,
			String... sortColumns) {
        return getRules(
				"select hbs.blockedUrls from WebFilterSettings hbs where hbs.tid = :tid ",
				start, limit, sortColumns);
	}

	public List<IPMaddrRule> getPassedClients(int start, int limit,
			String... sortColumns) {
        return getRules(
				"select hbs.passedClients from WebFilterSettings hbs where hbs.tid = :tid ",
				start, limit, sortColumns);
	}

	public List<StringRule> getPassedUrls(int start, int limit,
			String... sortColumns) {
        return getRules(
				"select hbs.passedUrls from WebFilterSettings hbs where hbs.tid = :tid ",
				start, limit, sortColumns);
	}

	public void updateBlacklistCategories(List<BlacklistCategory> added,
			List<Long> deleted, List<BlacklistCategory> modified) {
        updateCategories(getWebFilterSettings().getBlacklistCategories(), added, deleted, modified);
	}

	public void updateBlockedExtensions(List<StringRule> added,
			List<Long> deleted, List<StringRule> modified) {
		updateRules(getWebFilterSettings().getBlockedExtensions(), added,
				deleted, modified);
	}

	public void updateBlockedMimeTypes(List<MimeTypeRule> added,
			List<Long> deleted, List<MimeTypeRule> modified) {
        updateRules(getWebFilterSettings().getBlockedMimeTypes(), added, deleted, modified);
	}

	public void updateBlockedUrls(List<StringRule> added, List<Long> deleted,
			List<StringRule> modified) {
        updateRules(getWebFilterSettings().getBlockedUrls(), added, deleted, modified);
	}

	public void updatePassedClients(List<IPMaddrRule> added,
			List<Long> deleted, List<IPMaddrRule> modified) {
        updateRules(getWebFilterSettings().getPassedClients(), added, deleted, modified);
	}

	public void updatePassedUrls(List<StringRule> added, List<Long> deleted,
			List<StringRule> modified) {
        updateRules(getWebFilterSettings().getPassedUrls(), added, deleted, modified);
	}
    
	public void updateAll(final WebFilterBaseSettings baseSettings,
			final List[] passedClients, final List[] passedUrls, final List[] blockedUrls,
			final List[] blockedMimeTypes, final List[] blockedExtensions,
			final List[] blacklistCategories) {
		
        TransactionWork tw = new TransactionWork() {
			public boolean doWork(Session s) {
		    	if (baseSettings != null) {
			        settings.setBaseSettings(baseSettings);
		    	}
		    	if (passedClients != null && passedClients.length >= 3) {
		    		updateCachedRules(getWebFilterSettings().getPassedClients(), passedClients[0], passedClients[1], passedClients[2]);
		    	}
		    	if (passedUrls != null && passedUrls.length >= 3) {
		    		updateCachedRules(getWebFilterSettings().getPassedUrls(), passedUrls[0], passedUrls[1], passedUrls[2]);
		    	}
		    	if (blockedUrls != null && blockedUrls.length >= 3) {
		    		updateCachedRules(getWebFilterSettings().getBlockedUrls(), blockedUrls[0], blockedUrls[1], blockedUrls[2]);
		    	}
		    	if (blockedMimeTypes != null && blockedMimeTypes.length >= 3) {
		    		updateCachedRules(getWebFilterSettings().getBlockedMimeTypes(), blockedMimeTypes[0], blockedMimeTypes[1], blockedMimeTypes[2]);
		    	}
		    	if (blockedExtensions != null && blockedExtensions.length >= 3) {
		    		updateCachedRules(getWebFilterSettings().getBlockedExtensions(), blockedExtensions[0], blockedExtensions[1], blockedExtensions[2]);
		    	}		    	
		    	if (blacklistCategories != null && blacklistCategories.length >= 3) {
		    		updateCachedCategories(getWebFilterSettings().getBlacklistCategories(), blacklistCategories[0], blacklistCategories[1], blacklistCategories[2]);
		    	}

				s.merge(settings);

				return true;
			}

			public Object getResult() {
				return null;
			}
		};
		getNodeContext().runTransaction(tw);
    	
    	
		
		reconfigure();
	}

    // Node methods ------------------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    public void initializeSettings()
    {
        if (logger.isDebugEnabled()) {
            logger.debug(getTid() + " init settings");
        }

        WebFilterSettings settings = new WebFilterSettings(getTid());

        Set s = new HashSet<StringRule>();

        // this third column is more of a description than a category, the way the client is using it
        // the second column is being used as the "category"
        s.add(new StringRule("exe", "executable", "an executable file format" , false));
        s.add(new StringRule("ocx", "executable", "an executable file format", false));
        s.add(new StringRule("dll", "executable", "an executable file format", false));
        s.add(new StringRule("cab", "executable", "an ActiveX executable file format", false));
        s.add(new StringRule("bin", "executable", "an executable file format", false));
        s.add(new StringRule("com", "executable", "an executable file format", false));
        s.add(new StringRule("jpg", "image", "an image file format", false));
        s.add(new StringRule("png", "image", "an image file format", false ));
        s.add(new StringRule("gif", "image", "an image file format", false));
        s.add(new StringRule("jar", "java", "a Java file format", false));
        s.add(new StringRule("class", "java", "a Java file format", false));
        s.add(new StringRule("swf", "flash", "the flash file format", false));
        s.add(new StringRule("mp3", "audio", "an audio file format", false));
        s.add(new StringRule("wav", "audio", "an audio file format", false));
        s.add(new StringRule("wmf", "audio", "an audio file format", false));
        s.add(new StringRule("mpg", "video", "a video file format", false));
        s.add(new StringRule("mov", "video", "a video file format", false));
        s.add(new StringRule("avi", "video", "a video file format", false));
        s.add(new StringRule("hqx", "archive", "an archived file format", false));
        s.add(new StringRule("cpt", "compression", "a compressed file format", false));

        settings.setBlockedExtensions(s);

        s = new HashSet<MimeTypeRule>();
        s.add(new MimeTypeRule(new MimeType("application/octet-stream"), "unspecified data", "byte stream", false));

        s.add(new MimeTypeRule(new MimeType("application/x-msdownload"), "Microsoft download", "executable", false));
        s.add(new MimeTypeRule(new MimeType("application/exe"), "executable", "executable", false));
        s.add(new MimeTypeRule(new MimeType("application/x-exe"), "executable", "executable", false));
        s.add(new MimeTypeRule(new MimeType("application/dos-exe"), "DOS executable", "executable", false));
        s.add(new MimeTypeRule(new MimeType("application/x-winexe"), "Windows executable", "executable", false));
        s.add(new MimeTypeRule(new MimeType("application/msdos-windows"), "MS-DOS executable", "executable", false));
        s.add(new MimeTypeRule(new MimeType("application/x-msdos-program"), "MS-DOS program", "executable", false));
        s.add(new MimeTypeRule(new MimeType("application/x-oleobject"), "Microsoft OLE Object", "executable", false));

        s.add(new MimeTypeRule(new MimeType("application/x-java-applet"), "Java Applet", "executable", false));

        s.add(new MimeTypeRule(new MimeType("audio/mpegurl"), "MPEG audio URLs", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-mpegurl"), "MPEG audio URLs", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/mp3"), "MP3 audio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-mp3"), "MP3 audio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/mpeg"), "MPEG audio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/mpg"), "MPEG audio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-mpeg"), "MPEG audio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-mpg"), "MPEG audio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("application/x-ogg"), "Ogg Vorbis", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/m4a"), "MPEG 4 audio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/mp2"), "MP2 audio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/mp1"), "MP1 audio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("application/ogg"), "Ogg Vorbis", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/wav"), "Microsoft WAV", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-wav"), "Microsoft WAV", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-pn-wav"), "Microsoft WAV", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/aac"), "Advanced Audio Coding", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/midi"), "MIDI audio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/mpeg"), "MPEG audio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/aiff"), "AIFF audio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-aiff"), "AIFF audio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-pn-aiff"), "AIFF audio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-pn-windows-acm"), "Windows ACM", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-pn-windows-pcm"), "Windows PCM", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/basic"), "8-bit u-law PCM", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-pn-au"), "Sun audio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/3gpp"), "3GPP", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/3gpp-encrypted"), "encrypted 3GPP", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/scpls"), "streaming mp3 playlists", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-scpls"), "streaming mp3 playlists", "audio", false));
        s.add(new MimeTypeRule(new MimeType("application/smil"), "SMIL", "audio", false));
        s.add(new MimeTypeRule(new MimeType("application/sdp"), "Streaming Download Project", "audio", false));
        s.add(new MimeTypeRule(new MimeType("application/x-sdp"), "Streaming Download Project", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/amr"), "AMR codec", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/amr-encrypted"), "AMR encrypted codec", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/amr-wb"), "AMR-WB codec", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/amr-wb-encrypted"), "AMR-WB encrypted codec", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-rn-3gpp-amr"), "3GPP codec", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-rn-3gpp-amr-encrypted"), "3GPP-AMR encrypted codec", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-rn-3gpp-amr-wb"), "3gpp-AMR-WB codec", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-rn-3gpp-amr-wb-encrypted"), "3gpp-AMR_WB encrypted codec", "audio", false));
        s.add(new MimeTypeRule(new MimeType("application/streamingmedia"), "Streaming Media", "audio", false));

        s.add(new MimeTypeRule(new MimeType("video/mpeg"), "MPEG video", "video", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-ms-wma"), "Windows Media", "video", false));
        s.add(new MimeTypeRule(new MimeType("video/quicktime"), "QuickTime", "video", false));
        s.add(new MimeTypeRule(new MimeType("video/x-ms-asf"), "Microsoft ASF", "video", false));
        s.add(new MimeTypeRule(new MimeType("video/x-msvideo"), "Microsoft AVI", "video", false));
        s.add(new MimeTypeRule(new MimeType("video/x-sgi-mov"), "SGI movie", "video", false));
        s.add(new MimeTypeRule(new MimeType("video/3gpp"), "3GPP video", "video", false));
        s.add(new MimeTypeRule(new MimeType("video/3gpp-encrypted"), "3GPP encrypted video", "video", false));
        s.add(new MimeTypeRule(new MimeType("video/3gpp2"), "3GPP2 video", "video", false));

        s.add(new MimeTypeRule(new MimeType("audio/x-realaudio"), "RealAudio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("text/vnd.rn-realtext"), "RealText", "text", false));
        s.add(new MimeTypeRule(new MimeType("audio/vnd.rn-realaudio"), "RealAudio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-pn-realaudio"), "RealAudio plug-in", "audio", false));
        s.add(new MimeTypeRule(new MimeType("image/vnd.rn-realpix"), "RealPix", "image", false));
        s.add(new MimeTypeRule(new MimeType("application/vnd.rn-realmedia"), "RealMedia", "video", false));
        s.add(new MimeTypeRule(new MimeType("application/vnd.rn-realmedia-vbr"), "RealMedia VBR", "video", false));
        s.add(new MimeTypeRule(new MimeType("application/vnd.rn-realmedia-secure"), "secure RealMedia", "video", false));
        s.add(new MimeTypeRule(new MimeType("application/vnd.rn-realaudio-secure"), "secure RealAudio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("audio/x-realaudio-secure"), "secure RealAudio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("video/vnd.rn-realvideo-secure"), "secure RealVideo", "video", false));
        s.add(new MimeTypeRule(new MimeType("video/vnd.rn-realvideo"), "RealVideo", "video", false));
        s.add(new MimeTypeRule(new MimeType("application/vnd.rn-realsystem-rmj"), "RealSystem media", "video", false));
        s.add(new MimeTypeRule(new MimeType("application/vnd.rn-realsystem-rmx"), "RealSystem secure media", "video", false));
        s.add(new MimeTypeRule(new MimeType("audio/rn-mpeg"), "MPEG audio", "audio", false));
        s.add(new MimeTypeRule(new MimeType("application/x-shockwave-flash"), "Macromedia Shockwave", "multimedia", false));
        s.add(new MimeTypeRule(new MimeType("application/x-director"), "Macromedia Shockwave", "multimedia", false));
        s.add(new MimeTypeRule(new MimeType("application/x-authorware-bin"), "Macromedia Authorware binary", "multimedia", false));
        s.add(new MimeTypeRule(new MimeType("application/x-authorware-map"), "Macromedia Authorware shocked file", "multimedia", false));
        s.add(new MimeTypeRule(new MimeType("application/x-authorware-seg"), "Macromedia Authorware shocked packet", "multimedia", false));
        s.add(new MimeTypeRule(new MimeType("application/futuresplash"), "Macromedia FutureSplash", "multimedia", false));

        s.add(new MimeTypeRule(new MimeType("application/zip"), "ZIP", "archive", false));
        s.add(new MimeTypeRule(new MimeType("application/x-lzh"), "LZH archive", "archive", false));

        s.add(new MimeTypeRule(new MimeType("image/gif"), "Graphics Interchange Format", "image", false));
        s.add(new MimeTypeRule(new MimeType("image/png"), "Portable Network Graphics", "image", false));
        s.add(new MimeTypeRule(new MimeType("image/jpeg"), "JPEG", "image", false));
        s.add(new MimeTypeRule(new MimeType("image/bmp"), "Microsoft BMP", "image", false));
        s.add(new MimeTypeRule(new MimeType("image/tiff"), "Tagged Image File Format", "image", false));
        s.add(new MimeTypeRule(new MimeType("image/x-freehand"), "Macromedia Freehand", "image", false));
        s.add(new MimeTypeRule(new MimeType("image/x-cmu-raster"), "CMU Raster", "image", false));
        s.add(new MimeTypeRule(new MimeType("image/x-rgb"), "RGB image", "image", false));

        s.add(new MimeTypeRule(new MimeType("text/css"), "cascading style sheet", "text", false));
        s.add(new MimeTypeRule(new MimeType("text/html"), "HTML", "text", false));
        s.add(new MimeTypeRule(new MimeType("text/plain"), "plain text", "text", false));
        s.add(new MimeTypeRule(new MimeType("text/richtext"), "rich text", "text", false));
        s.add(new MimeTypeRule(new MimeType("text/tab-separated-values"), "tab separated values", "text", false));
        s.add(new MimeTypeRule(new MimeType("text/xml"), "XML", "text", false));
        s.add(new MimeTypeRule(new MimeType("text/xsl"), "XSL", "text", false));
        s.add(new MimeTypeRule(new MimeType("text/x-sgml"), "SGML", "text", false));
        s.add(new MimeTypeRule(new MimeType("text/x-vcard"), "vCard", "text", false));

        s.add(new MimeTypeRule(new MimeType("application/mac-binhex40"), "Macintosh BinHex", "archive", false));
        s.add(new MimeTypeRule(new MimeType("application/x-stuffit"), "Macintosh Stuffit archive", "archive", false));
        s.add(new MimeTypeRule(new MimeType("application/macwriteii"), "MacWrite Document", "document", false));
        s.add(new MimeTypeRule(new MimeType("application/applefile"), "Macintosh File", "archive", false));
        s.add(new MimeTypeRule(new MimeType("application/mac-compactpro"), "Macintosh Compact Pro", "archive", false));

        s.add(new MimeTypeRule(new MimeType("application/x-bzip2"), "block compressed", "compressed", false));
        s.add(new MimeTypeRule(new MimeType("application/x-shar"), "shell archive", "archive", false));
        s.add(new MimeTypeRule(new MimeType("application/x-gtar"), "gzipped tar archive", "archive", false));
        s.add(new MimeTypeRule(new MimeType("application/x-gzip"), "gzip compressed", "compressed", false));
        s.add(new MimeTypeRule(new MimeType("application/x-tar"), "4.3BSD tar archive", "archive", false));
        s.add(new MimeTypeRule(new MimeType("application/x-ustar"), "POSIX tar archive", "archive", false));
        s.add(new MimeTypeRule(new MimeType("application/x-cpio"), "old cpio archive", "archive", false));
        s.add(new MimeTypeRule(new MimeType("application/x-bcpio"), "POSIX cpio archive", "archive", false));
        s.add(new MimeTypeRule(new MimeType("application/x-sv4crc"), "System V cpio with CRC", "archive", false));
        s.add(new MimeTypeRule(new MimeType("application/x-compress"), "UNIX compressed", "compressed", false));
        s.add(new MimeTypeRule(new MimeType("application/x-sv4cpio"), "System V cpio", "archive", false));
        s.add(new MimeTypeRule(new MimeType("application/x-sh"), "UNIX shell script", "executable", false));
        s.add(new MimeTypeRule(new MimeType("application/x-csh"), "UNIX csh script", "executable", false));
        s.add(new MimeTypeRule(new MimeType("application/x-tcl"), "Tcl script", "executable", false));
        s.add(new MimeTypeRule(new MimeType("application/x-javascript"), "JavaScript", "executable", false));

        s.add(new MimeTypeRule(new MimeType("application/x-excel"), "Microsoft Excel", "document", false));
        s.add(new MimeTypeRule(new MimeType("application/mspowerpoint"), "Microsoft Powerpoint", "document", false));
        s.add(new MimeTypeRule(new MimeType("application/msword"), "Microsoft Word", "document", false));
        s.add(new MimeTypeRule(new MimeType("application/wordperfect5.1"), "Word Perfect", "document", false));
        s.add(new MimeTypeRule(new MimeType("application/rtf"), "Rich Text Format", "document", false));
        s.add(new MimeTypeRule(new MimeType("application/pdf"), "Adobe Acrobat", "document", false));
        s.add(new MimeTypeRule(new MimeType("application/postscript"), "Postscript", "document", false));

        settings.setBlockedMimeTypes(s);

        updateToCurrentCategories(settings);

        setWebFilterSettings(settings);
    }
    @Override
    protected void postInit(String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from WebFilterSettings hbs where hbs.tid = :tid");
                    q.setParameter("tid", getTid());
                    settings = (WebFilterSettings)q.uniqueResult();

                    updateToCurrentCategories(settings);

                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        if (logger.isDebugEnabled()) {
            logger.debug("IN POSTINIT SET BLACKLIST " + settings);
        }
        blacklist.configure(settings);
        blacklist.startUpdateTimer();
        reconfigure();

        deployWebAppIfRequired(logger);
    }

    @Override
    protected void postDestroy()
    {
        unDeployWebAppIfRequired(logger);
        blacklist.stopUpdateTimer();
    }

    // package protected methods ----------------------------------------------

    Blacklist getBlacklist()
    {
        return blacklist;
    }

    void log(WebFilterEvent se)
    {
        eventLogger.log(se);
    }

    String generateNonce(WebFilterBlockDetails details)
    {
        return replacementGenerator.generateNonce(details);
    }

    Token[] generateResponse(String nonce, TCPSession session, String uri,
                             Header header, boolean persistent)
    {
        return replacementGenerator.generateResponse(nonce, session, uri,
                                                     header, persistent);
    }

    Token[] generateResponse(String nonce, TCPSession session,
                             boolean persistent)
    {
        return replacementGenerator.generateResponse(nonce, session,
                                                     persistent);
    }

    // private methods --------------------------------------------------------

    // This is broken out since we added categories in 3.1, and since
    // the list can't be modified by the user it's quite safe to do
    // this here.
    private void updateToCurrentCategories(WebFilterSettings settings)
    {
        Set curCategories = settings.getBlacklistCategories();

        if (curCategories.size() == 0) {
            /*
             * First time initialization
             */
            BlacklistCategory bc = new BlacklistCategory
                ("porn", "Pornography", "Adult and Sexually Explicit");
            bc.setBlockDomains(true);
            bc.setBlockUrls(true);
            bc.setBlockExpressions(true);
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("mail", "Web Mail", "Web Mail");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("drugs", "Illegal Drugs", "Illegal Drugs");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("gambling", "Gambling", "Gambling");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("hacking", "Hacking", "Security Cracking");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("aggressive", "Hate and Aggression",
                                       "Hate and Aggression");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("violence", "Violence", "Violence");
            settings.addBlacklistCategory(bc);
        }
        if (curCategories.size() < 8) {
            /*
             * First time or upgrade from 3.0 to 3.1
             */
            BlacklistCategory bc = new BlacklistCategory("sports", "Sports", "Sports");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("jobsearch", "Job Search", "Job Search");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("vacation", "Vacation", "Vacation");
            settings.addBlacklistCategory(bc);
        }
        if (curCategories.size() < 11) {
            /*
             * First time or upgrade from 3.2 to 4.0
             */
            BlacklistCategory bc = new BlacklistCategory("ecommerce", "Shopping", "Online Shopping");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("socialnetworking", "Social Networking", "Social Networking");
            settings.addBlacklistCategory(bc);
            bc = new BlacklistCategory("proxy", "Anonymous Surfing", "Anonymous Web Surfing");
            settings.addBlacklistCategory(bc);
        }
        if (curCategories.size() < 14) {
            /*
             * First time or upgrade from 4.0 to 4.1
             */
            BlacklistCategory bc = new BlacklistCategory("dating", "Dating", "Online Dating");
            settings.addBlacklistCategory(bc);
        }
    }

    private static synchronized void deployWebAppIfRequired(Logger logger)
    {
        if (0 != deployCount++) {
            return;
        }

        LocalUvmContext mctx = LocalUvmContextFactory.context();
        LocalAppServerManager asm = mctx.appServerManager();

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

                /* Unified way to determine which parameter to check */
                protected String outsideErrorMessage()
                {
                    return "off-site access";
                }

                /* Unified way to determine which parameter to check */
                protected String httpErrorMessage()
                {
                    return "standard access";
                }
            };

        if (asm.loadInsecureApp("/webfilter", "webfilter", v)) {
            logger.debug("Deployed WebFilter WebApp");
        } else {
            logger.error("Unable to deploy WebFilter WebApp");
        }
    }

    private static synchronized void unDeployWebAppIfRequired(Logger logger) {
        if (0 != --deployCount) {
            return;
        }

        LocalUvmContext mctx = LocalUvmContextFactory.context();
        LocalAppServerManager asm = mctx.appServerManager();

        if (asm.unloadWebApp("/webfilter")) {
            logger.debug("Unloaded WebFilter WebApp");
        } else {
            logger.warn("Unable to unload WebFilter WebApp");
        }
    }
    
    private List getRules(final String queryString, final int start,
			final int limit, final String... sortColumns) {
		TransactionWork<List> tw = new TransactionWork<List>() {
			private List result;

			public boolean doWork(Session s) {
				Query q = s.createQuery(queryString
						+ QueryUtil.toOrderByClause(sortColumns));
				q.setParameter("tid", getTid());
				q.setFirstResult(start);
				q.setMaxResults(limit);
				result = q.list();

				return true;
			}

			public List getResult() {
				return result;
			}
		};
		getNodeContext().runTransaction(tw);

		return tw.getResult();
	}
    
    private void updateRules(final Set rules, final List added,
			final List<Long> deleted, final List modified) {
		TransactionWork tw = new TransactionWork() {
			public boolean doWork(Session s) {
				updateCachedRules(rules, added, deleted, modified);

				s.merge(settings);

				return true;
			}

			public Object getResult() {
				return null;
			}
		};
		getNodeContext().runTransaction(tw);
	}

	private void updateCachedRules(final Set rules, final List added,
			final List<Long> deleted, final List modified) {
		for (Iterator i = rules.iterator(); i.hasNext();) {
			Rule rule = (Rule) i.next();
			Rule mRule = null;
			if (deleted != null && ListUtil.contains(deleted, rule.getId())) {
				i.remove();
			} else if (modified != null
					&& (mRule = modifiedRule(rule, modified)) != null) {
				rule.update(mRule);
			}
		}

		if (added != null) {
			rules.addAll(added);
		}
	}

	private Rule modifiedRule(Rule rule, List modified) {
		for (Iterator iterator = modified.iterator(); iterator.hasNext();) {
			Rule currentRule = (Rule) iterator.next();
			if (currentRule.getId().equals(rule.getId())) {
				return currentRule;
			}
		}
		return null;
	}
    
    private void updateCategories(final Set categories, final List added,
			final List<Long> deleted, final List modified) {
		TransactionWork tw = new TransactionWork() {
			public boolean doWork(Session s) {
				updateCachedCategories(categories, added, deleted, modified);

				s.merge(settings);

				return true;
			}

			public Object getResult() {
				return null;
			}
		};
		getNodeContext().runTransaction(tw);
	}
    
	private void updateCachedCategories(final Set categories, final List added,
			final List<Long> deleted, final List modified) {
		for (Iterator i = categories.iterator(); i.hasNext();) {
			BlacklistCategory category = (BlacklistCategory) i.next();
			BlacklistCategory mCategory = null;
			if (deleted != null && ListUtil.contains(deleted, category.getId())) {
				i.remove();
			} else if (modified != null
					&& (mCategory = modifiedCategory(category, modified)) != null) {
				category.update(mCategory);
			}
		}

		if (added != null) {
			categories.addAll(added);
		}
	}

	private BlacklistCategory modifiedCategory(BlacklistCategory category, List modified) {
		for (Iterator iterator = modified.iterator(); iterator.hasNext();) {
			BlacklistCategory currentCategory = (BlacklistCategory) iterator.next();
			if (currentCategory.getId().equals(category.getId())) {
				return currentCategory;
			}
		}
		return null;
	}

}
