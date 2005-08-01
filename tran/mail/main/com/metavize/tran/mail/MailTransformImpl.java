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

import com.metavize.tran.mail.PopCasingFactory;
import com.metavize.tran.mail.papi.*;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.CasingPipeSpec;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.TransformContextFactory;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;
import com.metavize.tran.mail.impl.imap.ImapCasingFactory;
import com.metavize.tran.mail.impl.smtp.SmtpCasingFactory;

public class MailTransformImpl extends AbstractTransform
    implements MailTransform, MailExport
{
    private final Logger logger = Logger.getLogger(getClass());

    PipeSpec SMTP_PIPE_SPEC = new CasingPipeSpec
        ("smtp", this, SmtpCasingFactory.factory(),
         Fitting.SMTP_STREAM, Fitting.SMTP_TOKENS);
    PipeSpec POP_PIPE_SPEC = new CasingPipeSpec
        ("pop", this, PopCasingFactory.factory(),
         Fitting.POP_STREAM, Fitting.POP_TOKENS);
    PipeSpec IMAP_PIPE_SPEC = new CasingPipeSpec
        ("imap", this, ImapCasingFactory.factory(),
         Fitting.IMAP_STREAM, Fitting.IMAP_TOKENS);

    private final PipeSpec[] pipeSpecs = new PipeSpec[]
        { SMTP_PIPE_SPEC, POP_PIPE_SPEC, IMAP_PIPE_SPEC };

    private MailTransformSettings settings;

    // constructors -----------------------------------------------------------

    public MailTransformImpl()
    {
        logger.debug("MailTransformImpl");

        MailExportFactory.init(this);
    }

    // MailTransform methods --------------------------------------------------

    public MailTransformSettings getMailTransformSettings()
    {
        return settings;
    }

    public void setMailTransformSettings(MailTransformSettings settings)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get MailTransformSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close hibernate session", exn);
            }
        }

        reconfigure();
    }

    // MailExport methods -----------------------------------------------------

    public MailTransformSettings getExportSettings()
    {
        return getMailTransformSettings();
    }

    // Transform methods ------------------------------------------------------

    public void reconfigure()
    {
        if (null != settings) {
            SMTP_PIPE_SPEC.setEnabled(settings.isSmtpEnabled());
            POP_PIPE_SPEC.setEnabled(settings.isPopEnabled());
            IMAP_PIPE_SPEC.setEnabled(settings.isImapEnabled());
        }
    }
    protected void initializeSettings()
    {
        settings = new MailTransformSettings();
        settings.setTid(getTid());
        //Set the defaults
        settings.setSmtpEnabled(true);
        settings.setPopEnabled(true);
        settings.setImapEnabled(false);//TODO bscott this is currently neutered
        settings.setSmtpInboundTimeout(1000*30);
        settings.setSmtpOutboundTimeout(1000*30);
        settings.setPopInboundTimeout(1000*30);
        settings.setPopOutboundTimeout(1000*30);
        settings.setImapInboundTimeout(1000*30);
        settings.setImapOutboundTimeout(1000*30);
        setMailTransformSettings(settings);
    }

    protected void postInit(String[] args)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery
                ("from MailTransformSettings hbs where hbs.tid = :tid");
            q.setParameter("tid", getTid());
            settings = (MailTransformSettings)q.uniqueResult();

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get MailTransformSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }

        if (null == settings) {
            initializeSettings();
        }

        reconfigure();
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
