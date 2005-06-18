/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.httpblocker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.Interface;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tapi.SoloTransform;
import com.metavize.mvvm.tapi.Subscription;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.mvvm.tran.MimeType;
import com.metavize.mvvm.tran.MimeTypeRule;
import com.metavize.mvvm.tran.PipelineInfo;
import com.metavize.mvvm.tran.PortRange;
import com.metavize.mvvm.tran.StringRule;
import com.metavize.tran.http.HttpRequestEvent;
import com.metavize.tran.http.HttpResponseEvent;
import com.metavize.tran.token.TokenAdaptor;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class HttpBlockerImpl extends SoloTransform implements HttpBlocker
{
    private static final Logger logger = Logger
        .getLogger(HttpBlockerImpl.class);


    private final HttpBlockerFactory factory = new HttpBlockerFactory(this);
    private final PipeSpec pipeSpec;

    private volatile HttpBlockerSettings settings;

    // constructors -----------------------------------------------------------

    public HttpBlockerImpl()
    {
        Subscription s = new Subscription(Protocol.TCP,
                                          Interface.ANY, Interface.ANY,
                                          IPMaddr.anyAddr, PortRange.ANY,
                                          IPMaddr.anyAddr, PortRange.ANY);

        pipeSpec = new SoloPipeSpec("http-blocker", s, Fitting.HTTP_TOKENS,
                                    Affinity.CLIENT, 0);
    }

    // HttpBlocker methods ----------------------------------------------------

    public HttpBlockerSettings getHttpBlockerSettings()
    {
        return settings;
    }

    public void setHttpBlockerSettings(HttpBlockerSettings settings)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get HttpBlockerSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close hibernate session", exn);
            }
        }

        Blacklist.BLACKLIST.configure(settings);
    }

    public List<RequestLog> getAllEvents()
    {
        List<RequestLog> l = new ArrayList<RequestLog>(100);

        Session s = TransformContextFactory.context().openSession();
        try {
            Query q = s.createQuery
                ("FROM HttpRequestEvent req, HttpResponseEvent resp, "
                 +    "PipelineInfo pio "
                 + "WHERE req.requestLine = resp.requestLine "
                 +       "AND req.sessionId = pio.sessionId "
                 + "ORDER BY timeStamp DESC");

            int c = 0;
            for (Iterator i = q.iterate(); i.hasNext() && 100 > c++; ) {
                Object[] o = (Object[])i.next();
                HttpRequestEvent req = (HttpRequestEvent)o[0];
                HttpResponseEvent resp = (HttpResponseEvent)o[1];
                PipelineInfo pio = (PipelineInfo)o[2];
                RequestLog requestLog = new RequestLog(req, resp, pio);
                l.add(requestLog);
            }
        } catch (HibernateException exn) {
            logger.warn("query failed for getAllEvents", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }

        return l;
    }

    // SoloTransform methods --------------------------------------------------

    /**
     * Causes the blacklist to populate its arrays.
     */
    public void reconfigure()
    {
        Blacklist.BLACKLIST.reconfigure();
    }

    protected void initializeSettings()
    {
        logger.debug(getTid() + " init settings");
        HttpBlockerSettings settings = new HttpBlockerSettings(getTid());

        List s = new ArrayList();

        s.add(new StringRule("exe", "executable", "download" , false));
        s.add(new StringRule("ocx", "executable", "ActiveX", false));
        s.add(new StringRule("dll", "executable", "ActiveX", false));
        s.add(new StringRule("cab", "executable", "ActiveX", false));
        s.add(new StringRule("bin", "executable", "download", false));
        s.add(new StringRule("com", "executable", "download", false));
        s.add(new StringRule("jpg", "image", "download", false));
        s.add(new StringRule("png", "image", "download", false ));
        s.add(new StringRule("gif", "image", "download", false));
        s.add(new StringRule("jar", "java", "download", false));
        s.add(new StringRule("class", "java", "download", false));
        s.add(new StringRule("mp3", "audio", "download", false));
        s.add(new StringRule("wav", "audio", "download", false));
        s.add(new StringRule("wmf", "audio", "download", false));
        s.add(new StringRule("mov", "video", "download", false));
        s.add(new StringRule("mpg", "video", "download", false));
        s.add(new StringRule("avi", "video", "download", false));
        s.add(new StringRule("swf", "flash", "download", false));
        s.add(new StringRule("mp3", "audio", "stream", false));
        s.add(new StringRule("wav", "audio", "stream", false));
        s.add(new StringRule("wmf", "audio", "stream", false));
        s.add(new StringRule("mov", "video", "stream", false));
        s.add(new StringRule("mpg", "video", "stream", false));
        s.add(new StringRule("avi", "video", "stream", false));
        s.add(new StringRule("hqx", "archive", "download", false));
        s.add(new StringRule("cpt", "compression", "download", false));

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
                                   "Hate and Agression");
        settings.addBlacklistCategory(bc);
        bc = new BlacklistCategory("violence", "Violence", "Violence");
        settings.addBlacklistCategory(bc);

        setHttpBlockerSettings(settings);
    }

    protected PipeSpec getPipeSpec()
    {
        return pipeSpec;
    }

    protected void postInit(String[] args)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery
                ("from HttpBlockerSettings hbs where hbs.tid = :tid");
            q.setParameter("tid", getTid());
            settings = (HttpBlockerSettings)q.uniqueResult();

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get HttpBlockerSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }

        logger.debug("IN POSTINIT SET BLACKLIST " + settings);
        Blacklist.BLACKLIST.configure(settings);
        Blacklist.BLACKLIST.reconfigure();
    }

    protected void preStart()
    {
        getMPipe().setSessionEventListener(new TokenAdaptor(factory));
    }

    protected void postDestroy()
    {
        Blacklist.BLACKLIST.destroy();
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
