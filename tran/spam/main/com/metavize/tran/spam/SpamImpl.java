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

package com.metavize.tran.spam;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.tran.mail.SMTPNotifyAction;
import com.metavize.tran.mail.SMTPSpamMessageAction;
import com.metavize.tran.mail.SpamMessageAction;
import com.metavize.tran.token.TokenAdaptor;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class SpamImpl extends AbstractTransform implements SpamTransform
{
    private static final Logger logger = Logger.getLogger(SpamImpl.class);

    private final PipeSpec[] pipeSpecs = new PipeSpec[] {
        new SoloPipeSpec("spam-smtp", this, new TokenAdaptor(new SpamSmtpFactory(this)), Fitting.SMTP_TOKENS, Affinity.SERVER, 0),
        new SoloPipeSpec("pop-smtp", this, new TokenAdaptor(new SpamPopFactory(this)), Fitting.POP_TOKENS, Affinity.SERVER, 0),
        new SoloPipeSpec("imap-smtp", this, new TokenAdaptor(new SpamImapFactory(this)), Fitting.IMAP_TOKENS, Affinity.SERVER, 0)
    };

    private final SpamScanner scanner;

    private SpamSettings zSpamSettings;

    // constructors -----------------------------------------------------------

    public SpamImpl(SpamScanner scanner)
    {
        this.scanner = scanner;
    }

    // Transform methods ------------------------------------------------------

    public SpamSettings getSpamSettings()
    {
        return this.zSpamSettings;
    }

    public void setSpamSettings(SpamSettings zSpamSettings)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(zSpamSettings);
            this.zSpamSettings = zSpamSettings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get SpamSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close hibernate session", exn);
            }
        }

        reconfigure();
        return;
    }

    public void reconfigure() { return; }

    protected void initializeSettings()
    {
        SpamSettings zTmpSpamSettings = new SpamSettings(getTid());

        zTmpSpamSettings.setSMTPInbound(new SpamSMTPConfig(true, SMTPSpamMessageAction.MARK, SMTPNotifyAction.NEITHER, "Scan incoming SMTP e-mail" ));
        zTmpSpamSettings.setSMTPOutbound(new SpamSMTPConfig(false, SMTPSpamMessageAction.PASS, SMTPNotifyAction.NEITHER, "Scan outgoing SMTP e-mail" ));

        zTmpSpamSettings.setPOPInbound(new SpamPOPConfig(true, SpamMessageAction.MARK, "Scan incoming POP e-mail" ));
        zTmpSpamSettings.setPOPOutbound(new SpamPOPConfig(false, SpamMessageAction.PASS, "Scan outgoing POP e-mail" ));

        zTmpSpamSettings.setIMAPInbound(new SpamIMAPConfig(true, SpamMessageAction.MARK, "Scan incoming IMAP e-mail" ));
        zTmpSpamSettings.setIMAPOutbound(new SpamIMAPConfig(false, SpamMessageAction.PASS, "Scan outgoing IMAP e-mail" ));

        setSpamSettings(zTmpSpamSettings);
        return;
    }

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    protected void preInit(String args[])
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery
                ("from SpamSettings ss where ss.tid = :tid");
            q.setParameter("tid", getTid());
            zSpamSettings = (SpamSettings)q.uniqueResult();

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get SpamSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close hibernate session", exn);
            }
        }

        return;
    }

    SpamScanner getScanner()
    {
        return scanner;
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return null;
    }

    public void setSettings(Object settings)
    {
        setSpamSettings((SpamSettings)settings);
        return;
    }
}
