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

package com.metavize.tran.http;

import java.util.HashSet;
import java.util.Set;

import com.metavize.mvvm.tran.Transform;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class HttpTransformCommon
{
    private static final Object LOCK = new Object();

    private static HttpTransformCommon COMMON;

    private final Set<HttpTransformImpl> listeners = new HashSet<HttpTransformImpl>();

    private final Logger logger = Logger.getLogger(HttpTransformCommon.class);

    private HttpSettings settings;

    // constructors -----------------------------------------------------------

    private HttpTransformCommon(Transform tran)
    {
        Session s = tran.getTransformContext().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from HttpSettings hbs");
            settings = (HttpSettings)q.uniqueResult();

            if (null == settings) {
                settings = new HttpSettings();
                s.save(settings);
            }

            reconfigure();

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get HttpSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }
    }

    // static factories -------------------------------------------------------

    static HttpTransformCommon common(Transform tran)
    {
        synchronized (LOCK) {
            if (null == COMMON) {
                COMMON = new HttpTransformCommon(tran);
            }
        }

        return COMMON;
    }

    // package protected methods ----------------------------------------------

    void registerListener(HttpTransformImpl tran)
    {
        synchronized (listeners) {
            listeners.add(tran);
        }
    }

    void deregisterListener(HttpTransformImpl tran)
    {
        synchronized (listeners) {
            listeners.remove(tran);
        }
    }

    void reconfigure()
    {
        synchronized (listeners) {
            for (HttpTransformImpl t : listeners) {
                t.doReconfigure(settings);
            }
        }
    }

    HttpSettings getHttpSettings()
    {
        return settings;
    }

    void setHttpSettings(Transform tran, HttpSettings settings)
    {
        Session s = tran.getTransformContext().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.merge(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get HttpSettings", exn);
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
