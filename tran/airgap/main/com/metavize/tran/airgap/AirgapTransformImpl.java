/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.airgap;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tran.TransformStats;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class AirgapTransformImpl extends AbstractTransform
    implements AirgapTransform
{
    private final Logger logger = Logger.getLogger(AirgapTransformImpl.class);

    private AirgapSettings settings;

    // We keep a stats around so we don't have to create one each time.
    private FakeTransformStats fakeStats;

    public AirgapTransformImpl() {}

    public void setAirgapSettings(AirgapSettings settings)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn(exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn(exn); // XXX TransExn
            }
        }
    }

    public AirgapSettings getAirgapSettings()
    {
        return settings;
    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return new PipeSpec[0];
    }

    protected void postInit(String[] args)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery
                ("from AirgapSettings ts where ts.tid = :tid");
            q.setParameter("tid", getTid());
            settings = (AirgapSettings)q.uniqueResult();

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn(exn);
        } finally {
            try {
                if (null != s) {
                    s.close();
                }
            } catch (HibernateException exn) {
                logger.warn(exn);
            }
        }
    }

    public TransformStats getStats() throws IllegalStateException
    {
        fakeStats.update();
        return fakeStats;
    }

    protected void preStart()
    {
        if (this.settings == null) {
            String[] args = {""};
            postInit(args);
        }
        fakeStats = new FakeTransformStats();
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getAirgapSettings();
    }

    public void setSettings(Object settings)
    {
        setAirgapSettings((AirgapSettings)settings);
    }
}
