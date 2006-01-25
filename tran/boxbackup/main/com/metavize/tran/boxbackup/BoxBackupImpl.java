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
package com.metavize.tran.boxbackup;


import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class BoxBackupImpl extends AbstractTransform implements BoxBackup
{
    private final EventHandler handler = new EventHandler(this);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { };
    private final Logger logger = Logger.getLogger(BoxBackupImpl.class);

    private BoxBackupSettings settings = null;

    public BoxBackupSettings getBoxBackupSettings()
    {
        return this.settings;
    }

    public void setBoxBackupSettings(final BoxBackupSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(settings);
                    BoxBackupImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        try {
            reconfigure();
        }
        catch (TransformException exn) {
            logger.error("Could not save BoxBackup settings", exn);
        }
    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    protected void initializeSettings()
    {
        BoxBackupSettings settings = new BoxBackupSettings(this.getTid());
        logger.info("Initializing Settings...");

        setBoxBackupSettings(settings);
    }

    protected void postInit(String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from BoxBackupSettings hbs where hbs.tid = :tid");
                    q.setParameter("tid", getTid());
                    BoxBackupImpl.this.settings = (BoxBackupSettings)q.uniqueResult();
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);
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
        BoxBackupSettings settings = getBoxBackupSettings();
        logger.info("Reconfigure()");

        if (settings == null) {
            throw new TransformException("Failed to get BoxBackup settings: " + settings);
        }

        handler.setSettings(settings);
    }

    public Object getSettings()
    {
        return getBoxBackupSettings();
    }

    public void setSettings(Object settings)
    {
        setBoxBackupSettings((BoxBackupSettings)settings);
    }
}
