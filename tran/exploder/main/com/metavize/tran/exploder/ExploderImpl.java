/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.exploder;

import java.io.File;
import java.util.List;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.util.TransactionWork;
import com.metavize.tran.mail.impl.quarantine.Quarantine;
import com.metavize.tran.mail.impl.safelist.SafelistManager;
import com.metavize.tran.mail.papi.*;
import com.metavize.tran.mail.papi.quarantine.BadTokenException;
import com.metavize.tran.mail.papi.quarantine.Inbox;
import com.metavize.tran.mail.papi.quarantine.InboxIndex;
import com.metavize.tran.mail.papi.quarantine.MailSummary;
import com.metavize.tran.mail.papi.quarantine.NoSuchInboxException;
import com.metavize.tran.mail.papi.quarantine.QuarantineMaintenenceView;
import com.metavize.tran.mail.papi.quarantine.QuarantineTransformView;
import com.metavize.tran.mail.papi.quarantine.QuarantineUserActionFailedException;
import com.metavize.tran.mail.papi.quarantine.QuarantineUserView;
import com.metavize.tran.mail.papi.safelist.NoSuchSafelistException;
import com.metavize.tran.mail.papi.safelist.SafelistActionFailedException;
import com.metavize.tran.mail.papi.safelist.SafelistAdminView;
import com.metavize.tran.mail.papi.safelist.SafelistEndUserView;
import com.metavize.tran.mail.papi.safelist.SafelistManipulation;
import com.metavize.tran.mail.papi.safelist.SafelistTransformView;
import com.metavize.tran.mime.EmailAddress;
import org.apache.log4j.Logger;
import org.hibernate.Session;

public class ExploderImpl extends AbstractTransform
    implements MailTransform, MailExport
{
    private final Logger logger = Logger.getLogger(MailTransformImpl.class);

    private final PipeSpec[] pipeSpecs = new PipeSpec[0];

    private MailTransformSettings settings;
    private static Quarantine s_quarantine;//This will never be null for *instances* of
                                           //MailTransformImpl

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
            if(MvvmContextFactory.context().loadWebApp("/quarantine", "quarantine")) {
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
            if(MvvmContextFactory.context().unloadWebApp("/quarantine")) {
                logger.debug("Unloaded Quarantine web app");
            }
            else {
                logger.error("Unable to unload Quarantine web app");
            }
            s_unDeployedWebApp = true;
        }
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
                    s.saveOrUpdate(settings);
                    MailTransformImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        reconfigure();

        s_quarantine.setSettings(settings.getQuarantineSettings());
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

    // Transform methods ------------------------------------------------------

    public void reconfigure()
    {
        /* release session if parser doesn't catch or
         * explicitly throws its own parse exception
         * (parser will catch certain parse exceptions)
         */
        POP_PIPE_SPEC.setReleaseParseExceptions(true);
    }

    protected void initializeSettings() {
    }

    @Override
    protected void preDestroy() throws TransformException {
        super.preDestroy();
        logger.debug("preDestroy()");
        unDeployWebAppIfRequired(logger);
        s_quarantine.close();
    }

    protected void postInit(String[] args)
    {
        logger.debug("postInit()");

        logger.debug("Initialize SafeList/Quarantine...");
        s_quarantine.setSettings(settings.getQuarantineSettings());
        s_safelistMngr.setSettings(this, settings);
        deployWebAppIfRequired(logger);
        s_quarantine.open();
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

    }
    class QuarantineMaintenenceViewWrapper
        extends QuarantineManipulationWrapper
        implements QuarantineMaintenenceView {

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
