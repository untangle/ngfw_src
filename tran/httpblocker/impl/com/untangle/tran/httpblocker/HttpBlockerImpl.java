/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.httpblocker;

import java.util.ArrayList;
import java.util.List;

import com.untangle.mvvm.LocalAppServerManager;
import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.logging.EventLogger;
import com.untangle.mvvm.logging.EventLoggerFactory;
import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.logging.ListEventFilter;
import com.untangle.mvvm.logging.SimpleEventFilter;
import com.untangle.mvvm.tapi.AbstractTransform;
import com.untangle.mvvm.tapi.Affinity;
import com.untangle.mvvm.tapi.Fitting;
import com.untangle.mvvm.tapi.PipeSpec;
import com.untangle.mvvm.tapi.SoloPipeSpec;
import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.mvvm.tran.MimeType;
import com.untangle.mvvm.tran.MimeTypeRule;
import com.untangle.mvvm.tran.StringRule;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.util.OutsideValve;
import com.untangle.mvvm.util.TransactionWork;
import com.untangle.tran.token.Header;
import com.untangle.tran.token.Token;
import com.untangle.tran.token.TokenAdaptor;
import org.apache.catalina.Valve;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class HttpBlockerImpl extends AbstractTransform implements HttpBlocker
{
    private static boolean webappDeployed = false;

    private final Logger logger = Logger.getLogger(getClass());

    private final HttpBlockerFactory factory = new HttpBlockerFactory(this);

    private final PipeSpec pipeSpec = new SoloPipeSpec
        ("http-blocker", this, new TokenAdaptor(this, factory), Fitting.HTTP_TOKENS,
         Affinity.CLIENT, 0);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { pipeSpec };

    private final Blacklist blacklist = new Blacklist(this);
    private final HttpBlockerReplacementGenerator replacementGenerator;

    private final EventLogger<HttpBlockerEvent> eventLogger;

    private volatile HttpBlockerSettings settings;

    // constructors -----------------------------------------------------------

    public HttpBlockerImpl()
    {
        replacementGenerator = new HttpBlockerReplacementGenerator(getTid());
        TransformContext tctx = getTransformContext();
        eventLogger = EventLoggerFactory.factory().getEventLogger(tctx);
        SimpleEventFilter sef = new HttpBlockerBlockedFilter();
        eventLogger.addSimpleEventFilter(sef);
        ListEventFilter lef = new HttpBlockerAllFilter();
        eventLogger.addListEventFilter(lef);
        sef = new HttpBlockerWhitelistFilter();
        eventLogger.addSimpleEventFilter(sef);
        lef = new HttpBlockerPassedFilter();
        eventLogger.addListEventFilter(lef);
    }

    // HttpBlocker methods ----------------------------------------------------

    public HttpBlockerSettings getHttpBlockerSettings()
    {
        if( settings == null )
            logger.error("Settings not yet initialized. State: " + getTransformContext().getRunState() );
        return settings;
    }

    public void setHttpBlockerSettings(final HttpBlockerSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(settings);
                    HttpBlockerImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        blacklist.configure(settings);
        reconfigure();
    }

    public EventManager<HttpBlockerEvent> getEventManager()
    {
        return eventLogger;
    }

    public HttpBlockerBlockDetails getDetails(String nonce)
    {
        return replacementGenerator.getNonceData(nonce);
    }

    // Transform methods ------------------------------------------------------

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

        HttpBlockerSettings settings = new HttpBlockerSettings(getTid());

        List s = new ArrayList();

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

        s = new ArrayList();
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

        setHttpBlockerSettings(settings);
    }
    @Override
    protected void postInit(String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from HttpBlockerSettings hbs where hbs.tid = :tid");
                    q.setParameter("tid", getTid());
                    settings = (HttpBlockerSettings)q.uniqueResult();

                    updateToCurrentCategories(settings);

                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        if (logger.isDebugEnabled()) {
            logger.debug("IN POSTINIT SET BLACKLIST " + settings);
        }
        blacklist.configure(settings);
        reconfigure();

        deployWebAppIfRequired(logger);
    }

    @Override
    protected void postDestroy()
    {
        unDeployWebAppIfRequired(logger);
        blacklist.destroy();
    }

    // package protected methods ----------------------------------------------

    Blacklist getBlacklist()
    {
        return blacklist;
    }

    void log(HttpBlockerEvent se)
    {
        eventLogger.log(se);
    }

    String generateNonce(HttpBlockerBlockDetails details)
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

    /**
     * Causes the blacklist to populate its arrays.
     */
    private void reconfigure()
    {
        MvvmContextFactory.context().newThread(new Runnable() {
                public void run() {
                    blacklist.reconfigure();
                }
            }).start();
    }

    // This is broken out since we added categories in 3.1, and since
    // the list can't be modified by the user it's quite safe to do
    // this here.
    private void updateToCurrentCategories(HttpBlockerSettings settings)
    {
        List curCategories = settings.getBlacklistCategories();

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

    private static synchronized void deployWebAppIfRequired(Logger logger) {
        if (webappDeployed) {
            return;
        }

        MvvmLocalContext mctx = MvvmContextFactory.context();
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
                    return "Off-site access prohibited";
                }

                /* Unified way to determine which parameter to check */
                protected String httpErrorMessage()
                {
                    return "Standard access prohibited";
                }
            };

        if (asm.loadInsecureApp("/httpblocker", "httpblocker", v)) {
            logger.debug("Deployed HttpBlocker WebApp");
        } else {
            logger.error("Unable to deploy HttpBlocker WebApp");
        }

        webappDeployed = true;
    }

    private static synchronized void unDeployWebAppIfRequired(Logger logger) {
        if (!webappDeployed) {
            return;
        }

        MvvmLocalContext mctx = MvvmContextFactory.context();
        LocalAppServerManager asm = mctx.appServerManager();

        if (asm.unloadWebApp("/httpblocker")) {
            logger.debug("Unloaded HttpBlocker WebApp");
        } else {
            logger.warn("Unable to unload HttpBlocker WebApp");
        }

        webappDeployed = false;
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getHttpBlockerSettings();
    }

    public void setSettings(Object settings)
    {
        setHttpBlockerSettings((HttpBlockerSettings)settings);
    }
}
