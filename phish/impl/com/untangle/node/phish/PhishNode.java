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

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sleepycat.je.DatabaseException;
import com.untangle.node.http.UserWhitelistMode;
import com.untangle.node.spam.SpamBaseSettings;
import com.untangle.node.spam.SpamNodeImpl;
import com.untangle.node.spam.SpamSettings;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenAdaptor;
import com.untangle.node.util.EncryptedUrlList;
import com.untangle.node.util.UrlDatabase;
import com.untangle.node.util.UrlList;
import com.untangle.uvm.LocalAppServerManager;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.ListEventFilter;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.util.OutsideValve;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.vnet.TCPSession;
import org.apache.catalina.Valve;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import static com.untangle.node.util.Ascii.CRLF;

public class PhishNode extends SpamNodeImpl implements Phish
{
    private final Logger logger = Logger.getLogger(getClass());

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
                urlDatabase.setUpdatePeriod(30 * 60 * 1000); // every 30 minutes
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

    public void setPhishBaseSettings(PhishBaseSettings phishBaseSettings)
    {
        PhishSettings phishSettings = getPhishSettings();
        phishBaseSettings.copy(phishSettings.getBaseSettings());
        phishSettings.setEnableGooglePhishList(phishBaseSettings.getEnableGooglePhishList());
        setSpamSettings(phishSettings);
    }

    public PhishBaseSettings getPhishBaseSettings(boolean updateInfo)
    {
        PhishBaseSettings phishBaseSettings = new PhishBaseSettings();
        PhishSettings phishSettings = getPhishSettings();
        SpamBaseSettings spamBaseSettings = getBaseSettings(updateInfo);
        phishSettings.getBaseSettings().copy(phishBaseSettings);
        phishBaseSettings.setLastUpdate(spamBaseSettings.getLastUpdate());
        phishBaseSettings.setSignatureVersion(spamBaseSettings.getSignatureVersion());
        phishBaseSettings.setEnableGooglePhishList(phishSettings.getEnableGooglePhishList());
        return phishBaseSettings;
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
        tmpSpamSettings.getBaseSettings().getSmtpConfig().setBlockSuperSpam(false);

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

        SpamSettings ps = getSpamSettings();
        ps.getBaseSettings().getSmtpConfig().setBlockSuperSpam(false);
        setSpamSettings(ps);
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

    // private methods -------------------------------------------------------

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
            };

        if (null != asm.loadInsecureApp("/phish", "phish", v)) {
            logger.debug("Deployed phish WebApp");
        } else {
            logger.error("Unable to deploy phish WebApp");
        }
    }

    // XXX factor out this shit
    private static synchronized void unDeployWebAppIfRequired(Logger logger) {
        if (0 != --deployCount) {
            return;
        }

        LocalUvmContext mctx = LocalUvmContextFactory.context();
        LocalAppServerManager asm = mctx.appServerManager();

        if (asm.unloadWebApp("/phish")) {
            logger.debug("Unloaded phish WebApp");
        } else {
            logger.warn("Unable to unload phish WebApp");
        }
    }

    // XXX duplicated code
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

        Map<String, String> m = new HashMap<String, String>(1);
        m.put("apikey", "ABQIAAAAcF3DrVo7y87-tH8HDXqeYBTJqIcXJiJ1Klr7Vk1tUUBxWLpa4w");
        m.put("client", "api");

        try {
            UrlList ul = new EncryptedUrlList(URL_BASE, "phish", "goog-black-hash", m, null);
            urlDatabase.addBlacklist("goog-black-hash", ul);
        } catch (DatabaseException exn) {
            logger.warn("could not open database", exn);
        } catch (IOException exn) {
            logger.warn("could not open database", exn);
        }

        try {
            UrlList ul = new EncryptedUrlList(URL_BASE, "phish", "goog-malware-hash", m, null);
            urlDatabase.addBlacklist("goog-malware-hash", ul);
        } catch (DatabaseException exn) {
            logger.warn("could not open database", exn);
        } catch (IOException exn) {
            logger.warn("could not open database", exn);
        }

        urlDatabase.updateAll(true);

        return urlDatabase;
    }
}
