/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ProtoFilterImpl.java,v 1.12 2005/02/11 22:47:01 jdi Exp $
 */
package com.metavize.tran.protofilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.SoloTransform;
import com.metavize.mvvm.tapi.Subscription;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tran.*;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class NatImpl extends SoloTransform implements Firewall
{
    private static final Logger logger = Logger.getLogger( NatImpl.class );
    private final PipeSpec pipeSpec;
    private NatSettings settings = null;
    private EventHandler handler = null;

    public NatImpl()
    {
        Set subscriptions = new HashSet();
        subscriptions.add(new Subscription(Protocol.TCP));
        subscriptions.add(new Subscription(Protocol.UDP));
        
        /* Have to figure out pipeline ordering, this should always next to towards the outside */
        this.pipeSpec = new PipeSpec( "nat", Fitting.OCTET_STREAM, subscriptions, Affinity.BEGIN );
    }

    public NatSettings getNatSettings()
    {
        return this.settings;
    }

    public void setNatSettings( NatSettings settings)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn( "Could not get NatSettings", exn );
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn( "Could not close hibernate sessino", exn );
            }
        }

        try {
            reconfigure();
        }
        catch (TransformException exn) {
            logger.error( "Could not save Nat settings", exn );
        }
    }

    public PipeSpec getPipeSpec()
    {
        return this.pipeSpec;
    }


    protected void initializeSettings()
    {
        NatSettings settings = new NatSettings(this.getTid());
        logger.info("Initializing Settings...");

        updateToCurrent(settings);

        setNatSettings(settings);
    }

    protected void postInit(String[] args)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from NatSettings hbs where hbs.tid = :tid");
            q.setParameter("tid", getTid());
            this.settings = (NatSettings)q.uniqueResult();

            updateToCurrent(this.settings);

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get HttpBlockerSettings", exn);
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

        getMPipe().setSessionEventListener(this.handler);
    }

    public void reconfigure() throws TransformException
    {
        NatSettings settings = getNatSettings();

        logger.info( "Reconfigure()" );

        if ( settings == null ) {
            throw new TransformException( "Failed to get Nat settings: " + settings );
        }
        
        /* Update the settings */
        
        /* Start/stop DNS Masquerading */

        
    }


    private void updateToCurrent(NatSettings settings)
    {
        if ( settings == null ) {
            logger.error( "NULL Nat Settings" );
            return;
        }
        
        logger.info( "Update Settings Complete" );
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getNatSettings();
    }

    public void setSettings(Object settings)
    {
        setNatSettings((NatSettings)settings);
    }
}
