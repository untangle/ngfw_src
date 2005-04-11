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
package com.metavize.tran.nat;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;

import java.net.InetAddress;
import java.net.Inet4Address;

import org.apache.log4j.Logger;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;

import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.SoloTransform;
import com.metavize.mvvm.tapi.Subscription;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.NetworkingManager;
import com.metavize.mvvm.NetworkingConfiguration;

import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.tran.TransformException;

import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.firewall.PortMatcher;
import com.metavize.mvvm.tran.firewall.IntfMatcher;

public class NatImpl extends SoloTransform implements Nat
{
    private static final Logger logger = Logger.getLogger( NatImpl.class );
    private final PipeSpec pipeSpec;
    private NatSettings settings = null;
    private NatEventHandler handler = null;

    public NatImpl()
    {
        Set subscriptions = new HashSet();
        subscriptions.add(new Subscription(Protocol.TCP));
        subscriptions.add(new Subscription(Protocol.UDP));
        
        /* Have to figure out pipeline ordering, this should always next to towards the outside */
        this.pipeSpec = new PipeSpec( "nat", Fitting.OCTET_STREAM, subscriptions, Affinity.INSIDE, 
                                      PipeSpec.STRENGTH_MAX - 1 );
    }

    public NatSettings getNatSettings()
    {
        return this.settings;
    }

    public void setNatSettings( NatSettings settings)
    {
        /* XXXX Not using hibernate to save settings yet, everything
         * is hardcoded. */
        /*
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
        */

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
        /* XXXX Not using hibernate to save settings yet, everything
         * is hardcoded. */

        /*
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from NatSettings hbs where hbs.tid = :tid");
            q.setParameter("tid", getTid());
            this.settings = (NatSettings)q.uniqueResult();

            updateToCurrent(this.settings);

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get NatSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }
        */
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

        /* XXX Settings will always be null right now */
        /*
        if ( settings == null ) {
        throw new TransformException( "Failed to get Nat settings: " + settings );
        } */
        
                
        /* Configure the handler */
        if ( handler == null ) handler = new NatEventHandler();
        
        /* Configure the handler */
        /* XXX This is hardcoded, no good */
        useTestSettings();
        /* XXX This is hardcoded, no good */        
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

    private void useTestSettings()
    {
        IPMatcher natInternalMatcher;

        /* Matcher for the local host */
        IPMatcher hostMatcher;

        NetworkingConfiguration netConfig = MvvmContextFactory.context().networkingManager().get();
        InetAddress dmzHost;
        
        InetAddress redirectHost;

        try {
            natInternalMatcher = IPMatcher.parse( "192.168.1.0/255.255.255.0" );
            
            hostMatcher = new IPMatcher((Inet4Address)netConfig.host().getAddr());

            /* Designate 192.168.1.4 as the DMZ */
            dmzHost = InetAddress.getByName( "192.168.1.4" );
            
            /* Redirect a few ports to 192.168.1.6 */
            redirectHost = InetAddress.getByName( "192.168.1.6" );
       } catch ( Exception ex ) {
            logger.error( "This should never happen: ", ex );
            return;
        }
        
        /* Redirect all natted traffic to the local host */
        RedirectMatcher nat = new RedirectMatcher( true, ProtocolMatcher.MATCHER_ALL,
                                                   IntfMatcher.MATCHER_IN, IntfMatcher.MATCHER_ALL,
                                                   natInternalMatcher, IPMatcher.MATCHER_ALL, 
                                                   PortMatcher.MATCHER_ALL, PortMatcher.MATCHER_ALL,
                                                   false, netConfig.host().getAddr(), 0 );

        RedirectMatcher dmz = new RedirectMatcher( true, ProtocolMatcher.MATCHER_ALL,
                                                   IntfMatcher.MATCHER_OUT, IntfMatcher.MATCHER_ALL,
                                                   IPMatcher.MATCHER_ALL, hostMatcher,
                                                   PortMatcher.MATCHER_ALL, PortMatcher.MATCHER_ALL,
                                                   true, dmzHost, -1 );
                
        List<RedirectMatcher> redirectList = new LinkedList();

        /* This rule is enabled, redirect port 7000 to the redirect host port 7 */
        redirectList.add( new RedirectMatcher( true, ProtocolMatcher.MATCHER_ALL,
                                               IntfMatcher.MATCHER_OUT, IntfMatcher.MATCHER_ALL,
                                               IPMatcher.MATCHER_ALL, hostMatcher,
                                               PortMatcher.MATCHER_ALL, new PortMatcher( 7000 ),
                                               true, redirectHost, 7 ));

        /* This rule is disabled, to verify the on off switch works */
        redirectList.add( new RedirectMatcher( false, ProtocolMatcher.MATCHER_ALL,
                                               IntfMatcher.MATCHER_OUT, IntfMatcher.MATCHER_ALL,
                                               IPMatcher.MATCHER_ALL, hostMatcher, 
                                               PortMatcher.MATCHER_ALL, new PortMatcher( 5901 ),
                                               true, redirectHost, 5900 ));

        handler.setNat( nat );
        handler.setDmz( dmz );
        handler.setRedirectList( redirectList );
    }
}
