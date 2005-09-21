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

import java.util.HashSet;
import java.util.Set;

import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.mail.papi.MailTransformSettings;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class MailTransformCommon
{
    private static final Object LOCK = new Object();

    private static MailTransformCommon COMMON;

    private final Set<MailTransformImpl> listeners = new HashSet<MailTransformImpl>();

    private final Logger logger = Logger.getLogger(MailTransformCommon.class);

    private MailTransformSettings settings;

    // constructors -----------------------------------------------------------

    private MailTransformCommon(Transform tran)
    {
        Session s = tran.getTransformContext().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from MailTransformSettings ms");
            settings = (MailTransformSettings)q.uniqueResult();

            if (null == settings) {
                settings = new MailTransformSettings();
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

                s.save(settings);
            }

            reconfigure();

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
    }

    // static factories -------------------------------------------------------

    static MailTransformCommon common(Transform tran)
    {
        synchronized (LOCK) {
            if (null == COMMON) {
                COMMON = new MailTransformCommon(tran);
            }
        }

        return COMMON;
    }

    // package protected methods ----------------------------------------------

    void registerListener(MailTransformImpl tran)
    {
        synchronized (listeners) {
            listeners.add(tran);
        }
    }

    void deregisterListener(MailTransformImpl tran)
    {
        synchronized (listeners) {
            listeners.remove(tran);
        }
    }

    void reconfigure()
    {
        synchronized (listeners) {
            for (MailTransformImpl t : listeners) {
                t.doReconfigure(settings);
            }
        }
    }

    MailTransformSettings getMailTransformSettings()
    {
        return settings;
    }

    void setMailTransformSettings(Transform tran, MailTransformSettings settings)
    {
        Session s = tran.getTransformContext().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.merge(settings);
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
}
