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

package com.metavize.tran.mail;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.CasingPipeSpec;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.util.TransactionWork;
import com.metavize.tran.mail.impl.imap.ImapCasingFactory;
import com.metavize.tran.mail.impl.smtp.SmtpCasingFactory;
import com.metavize.tran.mail.papi.*;
import com.metavize.tran.mail.impl.quarantine.Quarantine;
import com.metavize.tran.mail.papi.quarantine.QuarantineTransformView;
import com.metavize.tran.mail.papi.quarantine.QuarantineMaintenenceView;
import com.metavize.tran.mail.papi.quarantine.QuarantineUserView;
import com.metavize.tran.mail.papi.quarantine.QuarantineSettings;
import com.metavize.tran.mail.papi.safelist.SafelistSettings;
import com.metavize.tran.mail.papi.safelist.SafelistAdminView;
import com.metavize.tran.mail.papi.safelist.SafelistEndUserView;
import com.metavize.tran.mail.papi.safelist.SafelistTransformView;
import com.metavize.tran.mail.impl.safelist.SafelistManager;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.tran.TransformStopException;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import com.metavize.mvvm.MvvmContextFactory;


public class MailTransformImpl extends AbstractTransform
    implements MailTransform, MailExport
{
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

    private MailTransformSettings settings;
    private static Quarantine s_quarantine;//This will never be null for *instances* of
                                           //MailTransformImpl
    private static SafelistManager s_safelistMngr;
    private static boolean s_deployedWebApp = false;
    private static boolean s_unDeployedWebApp = false;

    // constructors -----------------------------------------------------------

    public MailTransformImpl()
    {
        logger.debug("MailTransformImpl");

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
    

   
    @Override
    protected void postStart() throws TransformStartException {
      super.postStart();
      deployWebAppIfRequired(logger);
      s_quarantine.open();
    }

    @Override
    protected void preStop() throws TransformStopException {
      super.preStop();
      unDeployWebAppIfRequired(logger);
    }

    @Override
    protected void preDestroy() throws TransformException {
      super.preDestroy();
      s_quarantine.close();
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
        s_safelistMngr.setSettings(settings.getSafelistSettings());
    }

    public QuarantineUserView getQuarantineUserView() {
      return s_quarantine;
    }

    public QuarantineMaintenenceView getQuarantineMaintenenceView() {
      return s_quarantine;
    }
    
    public SafelistEndUserView getSafelistEndUserView() {
      return s_safelistMngr;
    }

    public SafelistAdminView getSafelistAdminView() {
      return s_safelistMngr;
    }

    // MailExport methods -----------------------------------------------------

    public MailTransformSettings getExportSettings()
    {
        return getMailTransformSettings();
    }
    
    public QuarantineTransformView getQuarantineTransformView() {
      return s_quarantine;
    }

    public SafelistTransformView getSafelistTransformView() {
      return s_safelistMngr;
    }    

    // Transform methods ------------------------------------------------------

    public void reconfigure()
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

    protected void initializeSettings() {
      
    }

    protected void postInit(String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from MailTransformSettings ms");
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
                    if(settings.getQuarantineSettings() == null ||
                      settings.getQuarantineSettings().getSecretKey() == null) {

                      QuarantineSettings qs = new QuarantineSettings();
                      
                      qs.setMaxQuarantineTotalSz(10 * 1000000000);//10Gig - I hope
                      qs.setDigestHourOfDay(6);//6 am
                      qs.setDigestFrom("quarantine@local.host");
                      byte[] secretKey = new byte[4];
                      new java.util.Random().nextBytes(secretKey);
                      qs.setSecretKey(secretKey);
                      qs.setMaxMailIntern(QuarantineSettings.WEEK * 2);
                      qs.setMaxIdleInbox(QuarantineSettings.WEEK * 4);
                      
                      settings.setQuarantineSettings(qs);
                      
                      shouldSave = true;
                    }
                    if(settings.getSafelistSettings() == null) {
                    
                      SafelistSettings ss = new SafelistSettings();

                      //TODO Set defaults here
                      
                      settings.setSafelistSettings(ss);
                      
//                      shouldSave = true;
                    }

                    if(shouldSave) {
                      s.save(settings);
                    }

                    reconfigure();
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        s_quarantine.setSettings(settings.getQuarantineSettings());
        s_safelistMngr.setSettings(settings.getSafelistSettings());
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
}
