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
package com.metavize.tran.firewall;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.NetworkingManager;
import com.metavize.mvvm.NetworkingConfiguration;

import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.SoloTransform;
import com.metavize.mvvm.tapi.Subscription;
import com.metavize.mvvm.tapi.TransformContextFactory;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.firewall.PortMatcher;
import com.metavize.mvvm.tran.firewall.IntfMatcher;
import com.metavize.mvvm.tran.firewall.FirewallRule;

import com.metavize.mvvm.argon.SessionMatcher;
import com.metavize.mvvm.argon.SessionMatcherFactory;

import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.tran.TransformException;

public class FirewallImpl extends SoloTransform implements Firewall
{
    private static final Logger logger = Logger.getLogger(FirewallImpl.class);
    private final PipeSpec pipeSpec;
    private FirewallSettings settings = null;
    private EventHandler handler = null;

    public FirewallImpl()
    {
        Set subscriptions = new HashSet();
        subscriptions.add(new Subscription(Protocol.TCP));
        subscriptions.add(new Subscription(Protocol.UDP));
        
        /* Have to figure out pipeline ordering, this should always next to towards the outside */
        this.pipeSpec = new SoloPipeSpec( "firewall", subscriptions, Fitting.OCTET_STREAM, 
                                          Affinity.INSIDE, SoloPipeSpec.MAX_STRENGTH );
    }

    public FirewallSettings getFirewallSettings()
    {
        return this.settings;
    }

    public void setFirewallSettings(FirewallSettings settings)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get FirewallSettings", exn);
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
            logger.error("Could not save Firewall settings", exn);
        }
    }

    public PipeSpec getPipeSpec()
    {
        return this.pipeSpec;
    }


    protected void initializeSettings()
    {
        logger.info("Initializing Settings...");

        /* XXX Should be default settings */
        FirewallSettings settings = getTestSettings();

        setFirewallSettings(settings);
    }

    protected void postInit(String[] args)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from FirewallSettings hbs where hbs.tid = :tid");
            q.setParameter("tid", getTid());
            this.settings = (FirewallSettings)q.uniqueResult();

            updateToCurrent(this.settings);

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get FirewallSettings", exn);
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

    protected void postStart()
    {
        /* Kill all active sessions */
        shutdownMatchingSessions();
    }

    protected void postStop()
    {
        /* Kill all active sessions */
        shutdownMatchingSessions();
    }

    public    void reconfigure() throws TransformException
    {
        FirewallSettings settings = getFirewallSettings();
        ArrayList enabledPatternsList = new ArrayList();

        logger.info("Reconfigure()");

        if (settings == null) {
            throw new TransformException("Failed to get Firewall settings: " + settings);
        }

        if ( handler == null ) handler = new EventHandler();
        
        handler.configure( settings );
    }

    private   void updateToCurrent(FirewallSettings settings)
    {
        if (settings == null) {
            logger.error( "NULL Firewall Settings" );
            return;
        }

        logger.info( "Update Settings Complete" );
    }

    FirewallSettings getTestSettings()
    {
        logger.info( "Using the test settings" );
        FirewallSettings settings = new FirewallSettings( this.getTid());
        
        /* Need this to lookup the local IP address */
        NetworkingConfiguration netConfig = MvvmContextFactory.context().networkingManager().get();
        
        try {
            /* Redirect settings */
            settings.setQuickExit( true );
            settings.setRejectSilently( true );
            settings.setDefaultAccept( false );

            List firewallList = new LinkedList();

            IPMatcher localHostMatcher = new IPMatcher( netConfig.host());
            
            /* This rule blocks all traffic from 5900-6000 to 7000 */
            firewallList.add( new FirewallRule( true, ProtocolMatcher.MATCHER_ALL,
                                                IntfMatcher.MATCHER_IN, IntfMatcher.MATCHER_ALL,
                                                IPMatcher.MATCHER_ALL, IPMatcher.MATCHER_ALL,
                                                new PortMatcher( 5900,6000 ), new PortMatcher( 7000, 7001 ),
                                                true ));
            
            /* This rule is disabled, to verify the on off switch works */
            firewallList.add( new FirewallRule( false, ProtocolMatcher.MATCHER_TCP,
                                                IntfMatcher.MATCHER_ALL, IntfMatcher.MATCHER_ALL,
                                                IPMatcher.MATCHER_ALL, 
                                                new IPMatcher( IPaddr.parse( "10.0.0.44" )), 
                                                PortMatcher.MATCHER_ALL, new PortMatcher( 5901 ),
                                                true ));
            
            /* Rule to catch all outgoing traffic */
            firewallList.add( new FirewallRule( true, ProtocolMatcher.MATCHER_ALL,
                                                IntfMatcher.MATCHER_IN, IntfMatcher.MATCHER_ALL,
                                                IPMatcher.MATCHER_ALL, IPMatcher.MATCHER_ALL,
                                                PortMatcher.MATCHER_ALL, PortMatcher.MATCHER_ALL,
                                                false ));
            
            settings.setFirewallRuleList( firewallList );
            
        } catch (Exception e ) {
            logger.error( "This should never happen", e );
        }
                
        return settings;

    }

    /* Kill all sessions when starting or stopping this transform */
    protected SessionMatcher sessionMatcher()
    {
        return SessionMatcherFactory.getAllInstance();
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getFirewallSettings();
    }

    public void setSettings(Object settings)
    {
        setFirewallSettings((FirewallSettings)settings);
    }
}
