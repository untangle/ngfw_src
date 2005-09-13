/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.sigma;


import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class SigmaImpl extends AbstractTransform implements Sigma
{
    private final EventHandler handler = new EventHandler(this);
    private final SoloPipeSpec pipeSpec =
        new SoloPipeSpec("sigma", this, handler, Fitting.OCTET_STREAM,Affinity.CLIENT, 0);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { pipeSpec };
    private final Logger logger = Logger.getLogger(SigmaImpl.class);

    private SigmaSettings settings = null;

    public SigmaSettings getSigmaSettings()
    {
        return this.settings;
    }

    public void setSigmaSettings(SigmaSettings settings)
    {
        Session s = getTransformContext().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get SigmaSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close hibernate sessino", exn);
            }
        }

        try {
            reconfigure();
        }
        catch (TransformException exn) {
            logger.error("Could not save Sigma settings", exn);
        }
    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    protected void initializeSettings()
    {
        SigmaSettings settings = new SigmaSettings(this.getTid());
        logger.info("Initializing Settings...");

        setSigmaSettings(settings);
    }

    protected void postInit(String[] args)
    {
        Session s = getTransformContext().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from SigmaSettings hbs where hbs.tid = :tid");
            q.setParameter("tid", getTid());
            this.settings = (SigmaSettings)q.uniqueResult();

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get SigmaSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }
    }

    protected void preStart() throws TransformStartException
    {
        try {
            reconfigure();
        } catch (Exception e) {
            throw new TransformStartException(e);
        }
    }

    public    void reconfigure() throws TransformException
    {
        SigmaSettings settings = getSigmaSettings();
        logger.info("Reconfigure()");

        if (settings == null) {
            throw new TransformException("Failed to get Sigma settings: " + settings);
        }

        handler.setSettings(settings);
    }

    public Object getSettings()
    {
        return getSigmaSettings();
    }

    public void setSettings(Object settings)
    {
        setSigmaSettings((SigmaSettings)settings);
    }
}
