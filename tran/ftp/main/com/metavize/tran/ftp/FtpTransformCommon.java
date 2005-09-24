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

package com.metavize.tran.ftp;

import java.util.HashSet;
import java.util.Set;

import com.metavize.mvvm.tran.Transform;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class FtpTransformCommon
{
    private static final Object LOCK = new Object();

    private static FtpTransformCommon COMMON;

    private final Set<FtpTransformImpl> listeners = new HashSet<FtpTransformImpl>();

    private final Logger logger = Logger.getLogger(FtpTransformCommon.class);

    private FtpSettings settings;

    // constructors -----------------------------------------------------------

    private FtpTransformCommon(Transform tran)
    {
        Session s = tran.getTransformContext().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from FtpSettings fs");
            settings = (FtpSettings)q.uniqueResult();

            if (null == settings) {
                settings = new FtpSettings();
                s.save(settings);
            }

            reconfigure();

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get FtpSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }
    }

    // static factories -------------------------------------------------------

    static FtpTransformCommon common(Transform tran)
    {
        synchronized (LOCK) {
            if (null == COMMON) {
                COMMON = new FtpTransformCommon(tran);
            }
        }

        return COMMON;
    }

    // package protected methods ----------------------------------------------

    void registerListener(FtpTransformImpl tran)
    {
        synchronized (listeners) {
            listeners.add(tran);
        }
    }

    void deregisterListener(FtpTransformImpl tran)
    {
        synchronized (listeners) {
            listeners.remove(tran);
        }
    }

    void reconfigure()
    {
        synchronized (listeners) {
            for (FtpTransformImpl t : listeners) {
                t.doReconfigure(settings);
            }
        }
    }

    FtpSettings getFtpSettings()
    {
        return settings;
    }

    void setFtpSettings(Transform tran, FtpSettings settings)
    {
        Session s = tran.getTransformContext().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdate(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get FtpSettings", exn);
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
