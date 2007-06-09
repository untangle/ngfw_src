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

package com.untangle.tran.mail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.portal.Application;
import com.untangle.mvvm.portal.LocalApplicationManager;
import com.untangle.mvvm.portal.LocalPortalManager;
import com.untangle.mvvm.tapi.AbstractTransform;
import com.untangle.mvvm.tapi.CasingPipeSpec;
import com.untangle.mvvm.tapi.Fitting;
import com.untangle.mvvm.tapi.PipeSpec;
import com.untangle.mvvm.tran.TransformException;
import com.untangle.mvvm.util.TransactionWork;
import com.untangle.tran.mail.impl.imap.ImapCasingFactory;
import com.untangle.tran.mail.impl.quarantine.Quarantine;
import com.untangle.tran.mail.impl.safelist.SafelistManager;
import com.untangle.tran.mail.impl.smtp.SmtpCasingFactory;
import com.untangle.tran.mail.papi.*;
import com.untangle.tran.mail.papi.quarantine.BadTokenException;
import com.untangle.tran.mail.papi.quarantine.Inbox;
import com.untangle.tran.mail.papi.quarantine.InboxAlreadyRemappedException;
import com.untangle.tran.mail.papi.quarantine.InboxIndex;
import com.untangle.tran.mail.papi.quarantine.MailSummary;
import com.untangle.tran.mail.papi.quarantine.NoSuchInboxException;
import com.untangle.tran.mail.papi.quarantine.QuarantineMaintenenceView;
import com.untangle.tran.mail.papi.quarantine.QuarantineSettings;
import com.untangle.tran.mail.papi.quarantine.QuarantineTransformView;
import com.untangle.tran.mail.papi.quarantine.QuarantineUserActionFailedException;
import com.untangle.tran.mail.papi.quarantine.QuarantineUserView;
import com.untangle.tran.mail.papi.safelist.NoSuchSafelistException;
import com.untangle.tran.mail.papi.safelist.SafelistActionFailedException;
import com.untangle.tran.mail.papi.safelist.SafelistAdminView;
import com.untangle.tran.mail.papi.safelist.SafelistEndUserView;
import com.untangle.tran.mail.papi.safelist.SafelistManipulation;
import com.untangle.tran.mail.papi.safelist.SafelistSettings;
import com.untangle.tran.mail.papi.safelist.SafelistTransformView;
import com.untangle.tran.mime.EmailAddress;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class MailTransformImpl extends AbstractTransform
    implements MailTransform, MailExport
{
    private static final String QUARANTINE_JS_URL = "/quarantine/app.js";

    private final Logger logger = Logger.getLogger(MailTransformImpl.class);

    private final CasingPipeSpec SMTP_PIPE_SPEC = new CasingPipeSpec
        ("smtp", this, SmtpCasingFactory.factory(),
         Fitting.SMTP_STREAM, Fitting.SMTP_TOKENS);
    private final CasingPipeSpec POP_PIPE_SPEC = new CasingPipeSpec
        ("pop", this, PopCasingFactory.factory(),
         Fitting.POP_STREAM, Fitting.POP_TOKENS);
    private final CasingPipeSpec IMAP_PIPE_SPEC = new CasingPipeSpec
        ("imap", this, ImapCasingFactory.factory(),
         Fitting.IMAP_STREAM, Fitting.IMAP_TOKENS);

    private final PipeSpec[] pipeSpecs = new PipeSpec[]
        { SMTP_PIPE_SPEC, POP_PIPE_SPEC, IMAP_PIPE_SPEC };

    private Application quarantineApp;

    private MailTransformSettings settings;
    private static Quarantine s_quarantine;//This will never be null for *instances* of
                                           //MailTransformImpl
    private static final long ONE_GB = (1024L * 1024L * 1024L);

    //HAck instances for RMI issues
    private QuarantineUserViewWrapper m_quv = new QuarantineUserViewWrapper();
    private QuarantineMaintenenceViewWrapper m_qmv = new QuarantineMaintenenceViewWrapper();
    private QuarantineTransformViewWrapper m_qtv = new QuarantineTransformViewWrapper();
    private static SafelistManager s_safelistMngr;
    private SafelistTransformViewWrapper m_stv = new SafelistTransformViewWrapper();
    private SafelistEndUserViewWrapper m_suv = new SafelistEndUserViewWrapper();
    private SafelistAdminViewWrapper m_sav = new SafelistAdminViewWrapper();
    private static boolean s_deployedWebApp = false;
    private static boolean s_unDeployedWebApp = false;

    // constructors -----------------------------------------------------------

    public MailTransformImpl()
    {
        logger.debug("<init>");

        //TODO bscott I assume this class will only ever be instantiated
        // on the server?!?
        createSingletonsIfRequired();

        MailExportFactory.factory().registerExport(this);
    }

    private static synchronized void createSingletonsIfRequired() {
        if(s_quarantine == null) {
            s_quarantine = new Quarantine();
        }
        if(s_safelistMngr == null) {
            s_safelistMngr = new SafelistManager();
        }
    }

    private static synchronized void deployWebAppIfRequired(Logger logger) {
        if(!s_deployedWebApp) {
            if(MvvmContextFactory.context().appServerManager().loadQuarantineApp("/quarantine", "quarantine")) {
                logger.debug("Deployed Quarantine web app");
            }
            else {
                logger.error("Unable to deploy Quarantine web app");
            }
            s_deployedWebApp = true;
        }
    }

    private static synchronized void unDeployWebAppIfRequired(Logger logger) {
        if(!s_unDeployedWebApp) {
            if(MvvmContextFactory.context().appServerManager().unloadWebApp("/quarantine")) {
                logger.debug("Unloaded Quarantine web app");
            }
            else {
                logger.error("Unable to unload Quarantine web app");
            }
            s_unDeployedWebApp = true;
        }
    }

    private void registerApps() {
        MvvmLocalContext mctx = MvvmContextFactory.context();
        LocalPortalManager lpm = mctx.portalManager();
        LocalApplicationManager lam = lpm.applicationManager();
        quarantineApp = lam.registerApplication("Quarantine",
                                                "Email Quarantine",
                                                "Allows you to access to email quarantine.",
                                                null, null, 0,
                                                QUARANTINE_JS_URL);
    }

    private void deregisterApps() {
        MvvmLocalContext mctx = MvvmContextFactory.context();
        LocalPortalManager lpm = mctx.portalManager();
        LocalApplicationManager lam = lpm.applicationManager();
        lam.deregisterApplication(quarantineApp);
    }


    // MailTransform methods --------------------------------------------------

    public MailTransformSettings getMailTransformSettings()
    {
        return settings;
    }

    public void setMailTransformSettings(final MailTransformSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    MailTransformImpl.this.settings = (MailTransformSettings)s.merge(settings);
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        reconfigure();

        s_quarantine.setSettings(this, settings.getQuarantineSettings());
        s_safelistMngr.setSettings(this, settings);
    }

    public QuarantineUserView getQuarantineUserView() {
        return m_quv;
    }

    public QuarantineMaintenenceView getQuarantineMaintenenceView() {
        return m_qmv;
    }

    public SafelistEndUserView getSafelistEndUserView() {
        return m_suv;
    }

    public SafelistAdminView getSafelistAdminView() {
        return m_sav;
    }

    // TODO - need to hook into HDD manager
    public long getMinAllocatedStoreSize(boolean inGB) { // john wanted a XXX on this
        if (false == inGB) {
            return ONE_GB;
        }
        return ONE_GB / ONE_GB;
    }

    // TODO - need to hook into HDD manager
    // max is arbitrarily set to 30 GB
    public long getMaxAllocatedStoreSize(boolean inGB) { // john wanted a XXX on this
        if (false == inGB) {
            return (30 * ONE_GB);
        }
        return 30 * (ONE_GB / ONE_GB);
    }

    // MailExport methods -----------------------------------------------------

    public MailTransformSettings getExportSettings()
    {
        return getMailTransformSettings();
    }

    public QuarantineTransformView getQuarantineTransformView() {
        return m_qtv;
    }

    public SafelistTransformView getSafelistTransformView() {
        return m_stv;
    }

    private void reconfigure()
    {
        SMTP_PIPE_SPEC.setEnabled(settings.isSmtpEnabled());
        POP_PIPE_SPEC.setEnabled(settings.isPopEnabled());
        IMAP_PIPE_SPEC.setEnabled(settings.isImapEnabled());

        /* release session if parser doesn't catch or
         * explicitly throws its own parse exception
         * (parser will catch certain parse exceptions)
         */
        POP_PIPE_SPEC.setReleaseParseExceptions(true);
    }

    // Transform methods ------------------------------------------------------

    @Override
    protected void preDestroy() throws TransformException {
        super.preDestroy();
        deregisterApps();
        logger.debug("preDestroy()");
        unDeployWebAppIfRequired(logger);
        s_quarantine.close();
    }

    protected void postInit(String[] args)
    {
        logger.debug("postInit()");
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from MailTransformSettings ms");
                    settings = (MailTransformSettings)q.uniqueResult();

                    boolean shouldSave = false;

                    if (null == settings) {
                        settings = new MailTransformSettings();
                        //Set the defaults
                        settings.setSmtpEnabled(true);
                        settings.setPopEnabled(true);
                        settings.setImapEnabled(true);
                        settings.setSmtpInboundTimeout(1000*30);
                        settings.setSmtpOutboundTimeout(1000*30);
                        settings.setPopInboundTimeout(1000*30);
                        settings.setPopOutboundTimeout(1000*30);
                        settings.setImapInboundTimeout(1000*30);
                        settings.setImapOutboundTimeout(1000*30);
                        shouldSave = true;
                    }

                    if(settings.getQuarantineSettings() == null) {
                        QuarantineSettings qs = new QuarantineSettings();

                        qs.setMaxQuarantineTotalSz(10 * ONE_GB);//10GB
                        qs.setDigestHourOfDay(6);//6 am
                        qs.setDigestMinuteOfDay(0);//6 am
                        byte[] secretKey = new byte[4];
                        new java.util.Random().nextBytes(secretKey);
                        qs.setSecretKey(secretKey);
                        qs.setMaxMailIntern(QuarantineSettings.WEEK * 2);
                        qs.setMaxIdleInbox(QuarantineSettings.WEEK * 4);

                        settings.setQuarantineSettings(qs);
                        shouldSave = true;
                    }

                    if(settings.getSafelistSettings() == null) {
                        ArrayList<SafelistSettings> ss = new ArrayList();

                        //TODO Set defaults here - DEFAULT TO WHAT?????

                        settings.setSafelistSettings(ss);
                        shouldSave = true;
                    }

                    if(true == shouldSave) {
                        s.save(settings);
                    }

                    reconfigure();
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        logger.debug("Initialize SafeList/Quarantine...");
        s_quarantine.setSettings(this, settings.getQuarantineSettings());
        s_safelistMngr.setSettings(this, settings);

        try {
            // create GLOBAL safelist for admin to manage POP/IMAP accounts
            // (GLOBAL safelist is created only if it doesn't exist yet)
            s_safelistMngr.createSafelist("GLOBAL");
        } catch (Exception ignore) {} //nothing can be done

        deployWebAppIfRequired(logger);
        s_quarantine.open();
        registerApps();
    }

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getMailTransformSettings();
    }

    public void setSettings(Object settings)
    {
        setMailTransformSettings((MailTransformSettings)settings);
    }


    //================================================================
    //Hacks to work around issues w/ the implicit RMI proxy stuff

    abstract class QuarantineManipulationWrapper {

        public InboxIndex purge(String account,
                                String...doomedMails)
            throws NoSuchInboxException, QuarantineUserActionFailedException {
            return s_quarantine.purge(account, doomedMails);
        }

        public InboxIndex rescue(String account,
                                 String...rescuedMails)
            throws NoSuchInboxException, QuarantineUserActionFailedException {
            return s_quarantine.rescue(account, rescuedMails);
        }

        public InboxIndex getInboxIndex(String account)
            throws NoSuchInboxException, QuarantineUserActionFailedException {
            return s_quarantine.getInboxIndex(account);
        }

        public void test() {
            //Nothing to do
        }
    }

    class QuarantineUserViewWrapper
        extends QuarantineManipulationWrapper
        implements QuarantineUserView {

        public String getAccountFromToken(String token)
            throws BadTokenException {
            return s_quarantine.getAccountFromToken(token);
        }

        public boolean requestDigestEmail(String account)
            throws NoSuchInboxException, QuarantineUserActionFailedException {
            return s_quarantine.requestDigestEmail(account);
        }

        public void remapSelfService(String from, String to)
            throws QuarantineUserActionFailedException, InboxAlreadyRemappedException {
            s_quarantine.remapSelfService(from, to);
        }

        public boolean unmapSelfService(String inboxName, String aliasToRemove)
            throws QuarantineUserActionFailedException {
            return s_quarantine.unmapSelfService(inboxName, aliasToRemove);
        }

        public String getMappedTo(String account)
            throws QuarantineUserActionFailedException {
            return s_quarantine.getMappedTo(account);
        }

        public String[] getMappedFrom(String account)
            throws QuarantineUserActionFailedException {
            return s_quarantine.getMappedFrom(account);
        }
    }

    class QuarantineMaintenenceViewWrapper
        extends QuarantineManipulationWrapper
        implements QuarantineMaintenenceView {

        public long getInboxesTotalSize()
            throws QuarantineUserActionFailedException {
            return s_quarantine.getInboxesTotalSize();
        }

        public String getFormattedInboxesTotalSize(boolean inMB) {
            return s_quarantine.getFormattedInboxesTotalSize(inMB);
        }

        public List<Inbox> listInboxes()
            throws QuarantineUserActionFailedException {
            return s_quarantine.listInboxes();
        }

        public void deleteInbox(String account)
            throws NoSuchInboxException, QuarantineUserActionFailedException {
            s_quarantine.deleteInbox(account);
        }

        public void rescueInbox(String account)
            throws NoSuchInboxException, QuarantineUserActionFailedException {
            s_quarantine.rescueInbox(account);
        }
    }

    class QuarantineTransformViewWrapper
        implements QuarantineTransformView {
        public boolean quarantineMail(File file,
                                      MailSummary summary,
                                      EmailAddress...recipients) {
            return s_quarantine.quarantineMail(file, summary, recipients);
        }
    }

    class SafelistTransformViewWrapper
        implements SafelistTransformView {
        public boolean isSafelisted(EmailAddress envelopeSender,
                                    EmailAddress mimeFrom,
                                    List<EmailAddress> recipients) {
            return s_safelistMngr.isSafelisted(envelopeSender, mimeFrom, recipients);
        }
    }

    abstract class SafelistManipulationWrapper
        implements SafelistManipulation {

        public String[] addToSafelist(String safelistOwnerAddress,
                                      String toAdd)
            throws NoSuchSafelistException, SafelistActionFailedException {
            return s_safelistMngr.addToSafelist(safelistOwnerAddress, toAdd);
        }

        public String[] removeFromSafelist(String safelistOwnerAddress,
                                           String toRemove)
            throws NoSuchSafelistException, SafelistActionFailedException {
            return s_safelistMngr.removeFromSafelist(safelistOwnerAddress, toRemove);
        }

        public String[] replaceSafelist(String safelistOwnerAddress,
                                        String...listContents)
            throws NoSuchSafelistException, SafelistActionFailedException {
            return s_safelistMngr.replaceSafelist(safelistOwnerAddress, listContents);
        }

        public String[] getSafelistContents(String safelistOwnerAddress)
            throws NoSuchSafelistException, SafelistActionFailedException {
            return s_safelistMngr.getSafelistContents(safelistOwnerAddress);
        }

        public int getSafelistCnt(String safelistOwnerAddress)
            throws NoSuchSafelistException, SafelistActionFailedException {
            return s_safelistMngr.getSafelistCnt(safelistOwnerAddress);
        }

        public boolean hasOrCanHaveSafelist(String address) {
            return s_safelistMngr.hasOrCanHaveSafelist(address);
        }

        public void test() {
        }
    }

    class SafelistEndUserViewWrapper
        extends SafelistManipulationWrapper
        implements SafelistEndUserView {
    }

    class SafelistAdminViewWrapper
        extends SafelistManipulationWrapper
        implements SafelistAdminView {

        public List<String> listSafelists()
            throws SafelistActionFailedException {
            return s_safelistMngr.listSafelists();
        }

        public void deleteSafelist(String safelistOwnerAddress)
            throws SafelistActionFailedException {
            s_safelistMngr.deleteSafelist(safelistOwnerAddress);
        }

        public void createSafelist(String newListOwnerAddress)
            throws SafelistActionFailedException {
            s_safelistMngr.createSafelist(newListOwnerAddress);
        }

        public boolean safelistExists(String safelistOwnerAddress)
            throws SafelistActionFailedException {
            return s_safelistMngr.safelistExists(safelistOwnerAddress);
        }
    }
}
