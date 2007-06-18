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

package com.untangle.node.phish;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sleepycat.je.DatabaseException;
import com.untangle.uvm.LocalAppServerManager;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.ListEventFilter;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.vnet.TCPSession;
import com.untangle.uvm.util.OutsideValve;
import com.untangle.node.http.UserWhitelistMode;
import com.untangle.node.spam.SpamImpl;
import com.untangle.node.spam.SpamSettings;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenAdaptor;
import com.untangle.node.util.EncryptedUrlList;
import com.untangle.node.util.PrefixUrlList;
import com.untangle.node.util.UrlDatabase;
import com.untangle.node.util.UrlList;
import org.apache.catalina.Valve;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import static com.untangle.node.util.Ascii.CRLF;

public class PhishNode extends SpamImpl
    implements Phish
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String OUT_MOD_SUB_TEMPLATE =
        "[PHISH] $MIMEMessage:SUBJECT$";
    private static final String OUT_MOD_BODY_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$\r\n" +
        "was determined by Untangle Phish Blocker to be PHISH (a\r\n" +
        "fraudulent email intended to steal information).  The kind of PHISH that was\r\n" +
        "found was $SPAMReport:FULL$";

    private static final String IN_MOD_SUB_TEMPLATE = OUT_MOD_SUB_TEMPLATE;
    private static final String IN_MOD_BODY_TEMPLATE = OUT_MOD_BODY_TEMPLATE;

    private static final String OUT_MOD_BODY_SMTP_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)\r\n" +
        "was determined by Untangle Phish Blocker to be PHISH (a\r\n" +
        "fraudulent email intended to steal information).  The kind of PHISH that was\r\n" +
        "found was $SPAMReport:FULL$";

    private static final String IN_MOD_BODY_SMTP_TEMPLATE = OUT_MOD_BODY_SMTP_TEMPLATE;

    private static final String PHISH_HEADER_NAME = "X-Phish-Flag";

    private static final String OUT_NOTIFY_SUB_TEMPLATE =
        "[PHISH NOTIFICATION] re: $MIMEMessage:SUBJECT$";

    private static final String OUT_NOTIFY_BODY_TEMPLATE =
        "On $MIMEHeader:DATE$ a message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)\r\n" +
        "was received by $SMTPTransaction:TO$.  The message was determined\r\n" +
        "by Untangle Phish Blocker to be PHISH (a fraudulent\r\n" +
        "email intended to steal information).  The kind of PHISH that was found was\r\n" +
        "$SPAMReport:FULL$";

    private static final String IN_NOTIFY_SUB_TEMPLATE = OUT_NOTIFY_SUB_TEMPLATE;
    private static final String IN_NOTIFY_BODY_TEMPLATE = OUT_NOTIFY_BODY_TEMPLATE;

    private static final URL URL_BASE;

    private static UrlDatabase urlDatabase = null;
    private static int urlDatabaseCount = 0;
    private static int deployCount = 0;

    static {
        try {
            URL_BASE = new URL("http://sb.google.com/safebrowsing");
        } catch (MalformedURLException exn) {
            throw new RuntimeException(exn);
        }
    }

    // We want to make sure that phish is before spam,
    // before virus in the pipeline (towards the client for smtp,
    // server for pop/imap).
    private final PipeSpec[] pipeSpecs = new PipeSpec[] {
        new SoloPipeSpec("phish-smtp", this, new TokenAdaptor(this, new PhishSmtpFactory(this)), Fitting.SMTP_TOKENS, Affinity.CLIENT, 12),
        new SoloPipeSpec("phish-pop", this, new TokenAdaptor(this, new PhishPopFactory(this)), Fitting.POP_TOKENS, Affinity.SERVER, 12),
        new SoloPipeSpec("phish-imap", this, new TokenAdaptor(this, new PhishImapFactory(this)), Fitting.IMAP_TOKENS, Affinity.SERVER, 12),
        new SoloPipeSpec("phish-http", this, new TokenAdaptor(this, new PhishHttpFactory(this)), Fitting.HTTP_TOKENS, Affinity.CLIENT, 12)
    };

    private final Map<InetAddress, Set<String>> hostWhitelists
        = new HashMap<InetAddress, Set<String>>();
    private final PhishReplacementGenerator replacementGenerator;

    private final EventLogger<PhishHttpEvent> phishHttpEventLogger;

    // constructors -----------------------------------------------------------

    public PhishNode()
    {
        super(new PhishScanner());

        replacementGenerator = new PhishReplacementGenerator(getTid());

        synchronized (PhishNode.class) {
            if (null == urlDatabase) {
                urlDatabase = makeUrlDatabase();
            }
        }

        phishHttpEventLogger = EventLoggerFactory.factory().getEventLogger(getNodeContext());

        SimpleEventFilter sef = new PhishHttpBlockedFilter();
        phishHttpEventLogger.addSimpleEventFilter(sef);
        ListEventFilter lef = new PhishHttpAllFilter();
        phishHttpEventLogger.addListEventFilter(lef);
        lef = new PhishHttpPassedFilter();
        phishHttpEventLogger.addListEventFilter(lef);
    }

    // public methods ---------------------------------------------------------

    public EventManager<PhishHttpEvent> getPhishHttpEventManager()
    {
        return phishHttpEventLogger;
    }

    public void setPhishSettings(PhishSettings spamSettings)
    {
        setSpamSettings(spamSettings);
    }

    public PhishSettings getPhishSettings()
    {
        return (PhishSettings)getSpamSettings();
    }


    public PhishBlockDetails getBlockDetails(String nonce)
    {
        return replacementGenerator.getNonceData(nonce);
    }

    public UserWhitelistMode getUserWhitelistMode()
    {
        return UserWhitelistMode.USER_ONLY; // XXX
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
                Set<String> wl = hostWhitelists.get(addr);
                if (null == wl) {
                    wl = new HashSet<String>();
                    hostWhitelists.put(addr, wl);
                }
                wl.add(site);
            }

            return true;
        }
    }

    public void initializeSettings()
    {
        logger.debug("Initializing Settings");

        PhishSettings tmpSpamSettings = new PhishSettings(getTid());
        tmpSpamSettings.setEnableGooglePhishList(true);
        configureSpamSettings(tmpSpamSettings);
        setSpamSettings(tmpSpamSettings);
    }

    // protected methods ------------------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    @Override
    protected void postInit(String args[])
    {
        synchronized (PhishNode.class) {
            if (0 == urlDatabaseCount) {
                urlDatabase.startUpdateTimer();
            }
            urlDatabaseCount++;
        }
        deployWebAppIfRequired(logger);
    }

    @Override
    protected void postDestroy()
    {
        synchronized (PhishNode.class) {
            urlDatabaseCount--;
            if (0 == urlDatabaseCount) {
                urlDatabase.stopUpdateTimer();
            }
        }
        unDeployWebAppIfRequired(logger);
    }

    @Override
    protected Query getSettingsQuery(Session s)
    {
        Query q = s.createQuery("from PhishSettings ss where ss.tid = :tid");
        q.setParameter("tid", getTid());
        return q;
    }

    @Override
    protected void initSpamRBLList(SpamSettings tmpSpamSettings) {
        return; // does not apply to clamphish
    }

    @Override
    protected void initSpamAssassinDefList(SpamSettings tmpSpamSettings) {
        return; // does not apply to clamphish
    }

    @Override
    protected void initSpamAssassinLclList(SpamSettings tmpSpamSettings) {
        return; // does not apply to clamphish
    }

    @Override
    public String getDefaultSubjectWrapperTemplate(boolean inbound) {
        return inbound?IN_MOD_SUB_TEMPLATE:OUT_MOD_SUB_TEMPLATE;
    }

    @Override
    public String getDefaultBodyWrapperTemplate(boolean inbound) {
        return inbound?IN_MOD_BODY_TEMPLATE:OUT_MOD_BODY_TEMPLATE;
    }

    @Override
    public String getDefaultSMTPSubjectWrapperTemplate(boolean inbound) {
        return getDefaultSubjectWrapperTemplate(inbound);
    }

    @Override
    public String getDefaultSMTPBodyWrapperTemplate(boolean inbound) {
        return inbound?IN_MOD_BODY_SMTP_TEMPLATE:OUT_MOD_BODY_SMTP_TEMPLATE;
    }

    @Override
    public String getDefaultIndicatorHeaderName() {
        return PHISH_HEADER_NAME;
    }

    @Override
    public String getDefaultNotifySubjectTemplate(boolean inbound) {
        return inbound?IN_NOTIFY_SUB_TEMPLATE:OUT_NOTIFY_SUB_TEMPLATE;
    }

    @Override
    public String getDefaultNotifyBodyTemplate(boolean inbound) {
        return inbound?IN_NOTIFY_BODY_TEMPLATE:OUT_NOTIFY_BODY_TEMPLATE;
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

    @Override
    public List<String> getUnWhitelistFromList() {
        return null; // does not apply to clamphish
    }

    @Override
    public void setUnWhitelistFromList(List<String> unWhitelistFromList) {
        return; // does not apply to clamphish
    }

    @Override
    public List<String> getUnWhitelistFromRcvdList() {
        return null; // does not apply to clamphish
    }

    @Override
    public void setUnWhitelistFromRcvdList(List<String> unWhitelistFromRcvdList) {
        return; // does not apply to clamphish
    }

    // package private methods ------------------------------------------------

    void logHttp(PhishHttpEvent phishHttpEvent)
    {
        phishHttpEventLogger.log(phishHttpEvent);
    }

    Token[] generateResponse(PhishBlockDetails bd, TCPSession session,
                             boolean persistent)
    {
        return replacementGenerator.generateResponse(bd, session, persistent);
    }

    boolean isWhitelistedDomain(String host, InetAddress clientAddr)
    {
        Set<String> l = hostWhitelists.get(clientAddr);
        if (null != l) {
            for (String d = host; null != d; d = nextHost(d)) {
                if (l.contains(d)) {
                    return true;
                }
            }
        }

        return false;
    }

    UrlDatabase getUrlDatabase()
    {
        return urlDatabase;
    }

    // private methods --------------------------------------------------------

    // XXX factor out this shit
    private static synchronized void deployWebAppIfRequired(Logger logger) {
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

                protected String httpErrorMessage()
                {
                    return "standard access";
                }
            };

        if (asm.loadInsecureApp("/idblocker", "idblocker", v)) {
            logger.debug("Deployed idblocker WebApp");
        } else {
            logger.error("Unable to deploy idblocker WebApp");
        }
    }

    // XXX factor out this shit
    private static synchronized void unDeployWebAppIfRequired(Logger logger) {
        if (0 != --deployCount) {
            return;
        }

        LocalUvmContext mctx = LocalUvmContextFactory.context();
        LocalAppServerManager asm = mctx.appServerManager();

        if (asm.unloadWebApp("/idblocker")) {
            logger.debug("Unloaded idblocker WebApp");
        } else {
            logger.warn("Unable to unload idblocker WebApp");
        }
    }

    // XXX factor this shit out!
    private String nextHost(String host)
    {
        int i = host.indexOf('.');
        if (0 > i || i == host.lastIndexOf('.')) {
            return null;  /* skip TLD */
        }

        return host.substring(i + 1);
    }

    private UrlDatabase makeUrlDatabase()
    {
        urlDatabase = new UrlDatabase();

        File dbHome = new File(System.getProperty("bunnicula.db.dir"), "phish");
        try {
            UrlList ul = new PrefixUrlList(dbHome, URL_BASE, "goog-black-url");
            urlDatabase.addBlacklist("goog-black-url", ul);
        } catch (DatabaseException exn) {
            logger.warn("could not open database", exn);
        } catch (IOException exn) {
            logger.warn("could not open database", exn);
        }

        try {
            UrlList ul = new EncryptedUrlList(dbHome, URL_BASE, "goog-black-enchash");
            urlDatabase.addBlacklist("goog-black-enchash", ul);
        } catch (DatabaseException exn) {
            logger.warn("could not open database", exn);
        } catch (IOException exn) {
            logger.warn("could not open database", exn);
        }

        urlDatabase.updateAll(true);

        return urlDatabase;
    }
}
