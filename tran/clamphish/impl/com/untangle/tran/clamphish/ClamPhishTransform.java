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

package com.untangle.tran.clamphish;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import com.sleepycat.je.DatabaseException;
import com.untangle.mvvm.LocalAppServerManager;
import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.tapi.Affinity;
import com.untangle.mvvm.tapi.Fitting;
import com.untangle.mvvm.tapi.PipeSpec;
import com.untangle.mvvm.tapi.SoloPipeSpec;
import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.mvvm.util.OutsideValve;
import com.untangle.tran.http.UserWhitelistMode;
import com.untangle.tran.spam.SpamImpl;
import com.untangle.tran.spam.SpamSettings;
import com.untangle.tran.token.Token;
import com.untangle.tran.token.TokenAdaptor;
import com.untangle.tran.util.EncryptedUrlList;
import com.untangle.tran.util.PrefixUrlList;
import com.untangle.tran.util.UrlDatabase;
import com.untangle.tran.util.UrlList;
import org.apache.catalina.Valve;
import org.apache.log4j.Logger;

import static com.untangle.tran.util.Ascii.CRLF;

public class ClamPhishTransform extends SpamImpl
    implements ClamPhish
{
    private static final String OUT_MOD_SUB_TEMPLATE =
      "[PHISH] $MIMEMessage:SUBJECT$";
    private static final String OUT_MOD_BODY_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$\r\n" +
        "was determined by Untangle Identity Theft Blocker to be PHISH (a\r\n" +
        "fraudulent email intended to steal information).  The kind of PHISH that was\r\n" +
        "found was $SPAMReport:FULL$";

    private static final String IN_MOD_SUB_TEMPLATE = OUT_MOD_SUB_TEMPLATE;
    private static final String IN_MOD_BODY_TEMPLATE = OUT_MOD_BODY_TEMPLATE;

    private static final String OUT_MOD_BODY_SMTP_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)\r\n" +
        "was determined by Untangle Identity Theft Blocker to be PHISH (a\r\n" +
        "fraudulent email intended to steal information).  The kind of PHISH that was\r\n" +
        "found was $SPAMReport:FULL$";

    private static final String IN_MOD_BODY_SMTP_TEMPLATE = OUT_MOD_BODY_SMTP_TEMPLATE;

    private static final String PHISH_HEADER_NAME = "X-Phish-Flag";

    private static final String OUT_NOTIFY_SUB_TEMPLATE =
      "[PHISH NOTIFICATION] re: $MIMEMessage:SUBJECT$";

    private static final String OUT_NOTIFY_BODY_TEMPLATE =
        "On $MIMEHeader:DATE$ a message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)\r\n" +
        "was received by $SMTPTransaction:TO$.  The message was determined\r\n" +
        "by Untangle Identity Theft Blocker to be PHISH (a fraudulent\r\n" +
        "email intended to steal information).  The kind of PHISH that was found was\r\n" +
        "$SPAMReport:FULL$";

    private static final String IN_NOTIFY_SUB_TEMPLATE = OUT_NOTIFY_SUB_TEMPLATE;
    private static final String IN_NOTIFY_BODY_TEMPLATE = OUT_NOTIFY_BODY_TEMPLATE;

    private static boolean webappDeployed = false;

    // We want to make sure that phish is before spam,
    // before virus in the pipeline (towards the client for smtp,
    // server for pop/imap).
    private final PipeSpec[] pipeSpecs = new PipeSpec[] {
        new SoloPipeSpec("phish-smtp", this, new TokenAdaptor(this, new PhishSmtpFactory(this)), Fitting.SMTP_TOKENS, Affinity.CLIENT, 12),
        new SoloPipeSpec("phish-pop", this, new TokenAdaptor(this, new PhishPopFactory(this)), Fitting.POP_TOKENS, Affinity.SERVER, 12),
        new SoloPipeSpec("phish-imap", this, new TokenAdaptor(this, new PhishImapFactory(this)), Fitting.IMAP_TOKENS, Affinity.SERVER, 12),
        new SoloPipeSpec("phish-http", this, new TokenAdaptor(this, new PhishHttpFactory(this)), Fitting.HTTP_TOKENS, Affinity.CLIENT, 12)
    };

    private final UrlDatabase urlDatabase;
    private final PhishReplacementGenerator replacementGenerator;

    private final Logger logger = Logger.getLogger(getClass());

    // constructors -----------------------------------------------------------

    public ClamPhishTransform()
    {
        super(new ClamPhishScanner());

        replacementGenerator = new PhishReplacementGenerator(getTid());

        urlDatabase = new UrlDatabase();

        // XXX post/pre init!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        File dbHome = new File(System.getProperty("bunnicula.db.dir"), "clamphish");
        try {
            URL url = new URL("http://sb.google.com/safebrowsing/update?version=goog-black-url:1:1");
            UrlList ul = new PrefixUrlList(dbHome, "goog-black-url", url);
            urlDatabase.addBlacklist("goog-black-url", ul);
        } catch (DatabaseException exn) {
            logger.warn("could not open database", exn);
        } catch (IOException exn) {
            logger.warn("could not open database", exn);
        }

        try {
            URL url = new URL("http://sb.google.com/safebrowsing/update?version=goog-black-enchash:1:1");
            UrlList ul = new EncryptedUrlList(dbHome, "goog-black-enchash", url);
            urlDatabase.addBlacklist("goog-black-enchash", ul);
        } catch (DatabaseException exn) {
            logger.warn("could not open database", exn);
        } catch (IOException exn) {
            logger.warn("could not open database", exn);
        }

        urlDatabase.updateAll();
        // XXX post/pre init!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    }

    // public methods ---------------------------------------------------------

    public ClamPhishBlockDetails getBlockDetails(String nonce)
    {
        return replacementGenerator.getNonceData(nonce);
    }

    public UserWhitelistMode getUserWhitelistMode()
    {
        return UserWhitelistMode.USER_AND_GLOBAL; // XXX
    }

    public boolean unblockSite(String nonce, boolean global)
    {
        // XXX do it!
        return true;
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
        deployWebAppIfRequired(logger);
    }

    @Override
    protected void postDestroy()
    {
        unDeployWebAppIfRequired(logger);
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

    Token[] generateResponse(ClamPhishBlockDetails bd, TCPSession session,
                             boolean persistent)
    {
        return replacementGenerator.generateResponse(bd, session, persistent);
    }

    UrlDatabase getUrlDatabase()
    {
        return urlDatabase;
    }

    // private methods --------------------------------------------------------

    // XXX factor out this shit
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

                protected String httpErrorMessage()
                {
                    return "Standard access prohibited";
                }
            };

        if (asm.loadInsecureApp("/idblocker", "idblocker", v)) {
            logger.debug("Deployed idblocker WebApp");
        } else {
            logger.error("Unable to deploy idblocker WebApp");
        }

        webappDeployed = true;
    }

    // XXX factor out this shit
    private static synchronized void unDeployWebAppIfRequired(Logger logger) {
        if (!webappDeployed) {
            return;
        }

        MvvmLocalContext mctx = MvvmContextFactory.context();
        LocalAppServerManager asm = mctx.appServerManager();

        if (asm.unloadWebApp("/idblocker")) {
            logger.debug("Unloaded idblocker WebApp");
        } else {
            logger.warn("Unable to unload idblocker WebApp");
        }

        webappDeployed = false;
    }
}
