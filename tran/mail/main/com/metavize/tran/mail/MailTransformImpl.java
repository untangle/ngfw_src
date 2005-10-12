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
import com.metavize.tran.mail.papi.quarantine.QuarantineUserView;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;


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

    // constructors -----------------------------------------------------------

    public MailTransformImpl()
    {
        logger.debug("MailTransformImpl");

        //TODO bscott I assume this class will only ever be instantiated
        // on the server?!?
        createQuarantineIfRequired();

        MailExportFactory.factory().registerExport(this);
    }

    private static synchronized void createQuarantineIfRequired() {
      if(s_quarantine == null) {
        s_quarantine = new Quarantine();
      }
    }
     
    protected void preDestroy() {
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
    }

    public QuarantineUserView getQuarantineUserView() {
      return s_quarantine;
    }

    // MailExport methods -----------------------------------------------------

    public MailTransformSettings getExportSettings()
    {
        return getMailTransformSettings();
    }
    
    public QuarantineTransformView getQuarantineTransformView() {
      return s_quarantine;
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

    protected void initializeSettings() { }

    protected void postInit(String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from MailTransformSettings ms");
                    settings = (MailTransformSettings)q.uniqueResult();

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

                        s.save(settings);
                    }

                    reconfigure();
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);
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
