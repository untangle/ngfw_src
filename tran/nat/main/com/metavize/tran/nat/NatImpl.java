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


import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;

import com.metavize.mvvm.NetworkingManager;
import com.metavize.mvvm.NetworkingConfiguration;

import com.metavize.mvvm.tapi.IPSessionDesc;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.MPipeManager;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.SoloTransform;
import com.metavize.mvvm.tapi.Subscription;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tapi.event.SessionEventListener;
import com.metavize.mvvm.tapi.PipelineFoundry;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.tran.token.TokenAdaptor;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.firewall.PortMatcher;
import com.metavize.mvvm.tran.firewall.IntfMatcher;

import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformState;

import com.metavize.mvvm.argon.SessionMatcher;
import com.metavize.mvvm.argon.SessionMatcherFactory;

public class NatImpl extends AbstractTransform implements Nat
{
    private static final PipelineFoundry FOUNDRY = MvvmContextFactory.context().pipelineFoundry();

    private final Logger logger = Logger.getLogger( NatImpl.class );
    private NatSettings settings = null;
    final NatEventHandler handler;
    private NatSessionManager sessionManager;

    private final PipeSpec[] pipeSpecs = new PipeSpec[2];

    final MPipe[] mPipes = new MPipe[2];
    private final SessionEventListener[] listeners = new SessionEventListener[2];

    public NatImpl()
    {
        Set subscriptions = new HashSet();
        subscriptions.add(new Subscription(Protocol.TCP));
        subscriptions.add(new Subscription(Protocol.UDP));
        
        /* Have to figure out pipeline ordering, this should always next to towards the outside */
        pipeSpecs[0]  = new SoloPipeSpec( "nat", subscriptions, Fitting.OCTET_STREAM, 
                                          Affinity.OUTSIDE, SoloPipeSpec.MAX_STRENGTH - 1 );
        
        subscriptions = new HashSet();
        subscriptions.add(new Subscription(Protocol.TCP));
        
        /* This subscription has to evaluate after NAT */
        pipeSpecs[1] = new SoloPipeSpec( "nat-ftp", subscriptions, Fitting.FTP_TOKENS, 
                                         Affinity.SERVER, 0 );
        
        sessionManager = new NatSessionManager( this );

        listeners[0] = handler = new NatEventHandler( this );
        listeners[1] = new TokenAdaptor( new NatFtpFactory( this ));
    }

    public NatSettings getNatSettings()
    {
        if ( settings.getDhcpEnabled()) {
            /* Insert all of the leases from DHCP */
            DhcpManager.getInstance().loadLeases( settings );
        } else {
            /* remove any leftover leases from DHCP */
            DhcpManager.getInstance().fleeceLeases( settings );
        }
        
        return this.settings;
    }

    public void setNatSettings( NatSettings settings ) throws Exception
    {
        /* Remove all of the non-static addresses before saving */
        DhcpManager.getInstance().fleeceLeases( settings );

        /* Validate the settings */
	try{
	    settings.validate();
	}
	catch(Exception e){
	    logger.error( "Invalid NAT settings", e );
	    throw e;
	}

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
                logger.warn( "Could not close hibernate session", exn );
            }
        }

        try {
            reconfigure();

            if ( getRunState() == TransformState.RUNNING ) {
                /* NAT configuration needs information from the networking settings. */
                NetworkingConfiguration netConfig = MvvmContextFactory.context().networkingManager().get();

                this.handler.configure( settings, netConfig );
                DhcpManager.getInstance().configure( settings, netConfig );
            }
        } catch (TransformException exn) {
            logger.error( "Could not save Nat settings", exn );
        }
    }

    private void registerEventListeners()
    {
        for (int i = 0; i < pipeSpecs.length; i++) {
            mPipes[i].setSessionEventListener(listeners[i]);
            logger.debug( "Connecting mPipe[" + i + "] as " + mPipes[i] + 
                          " with listener: " + listeners[i] );            
        }
    }

    // AbstractTransform methods ----------------------------------------------
    protected void connectMPipe()
    {
        for (int i = 0; i < pipeSpecs.length; i++) {
            mPipes[i] = MPipeManager.manager().plumbLocal(this, pipeSpecs[i]);
            FOUNDRY.registerMPipe(mPipes[i]);
        }
    }

    protected void disconnectMPipe()
    {
        for (int i = 0; i < mPipes.length; i++) {
            logger.debug( "Disconnecting mPipe[" + i + "] as " + mPipes[i] );
            if ( mPipes[i] != null ) {
                FOUNDRY.deregisterMPipe(mPipes[i]);
            } else {
                logger.warn("Disconnecting null mPipe[" + i + "]");
            }
            mPipes[i] = null;
        }
    }

    protected void initializeSettings()
    {
        logger.info("Initializing Settings...");
        
        NatSettings settings = getDefaultSettings();

	try{
	    setNatSettings(settings);
	}
	catch(Exception e){
	    logger.error( "Unable to set Nat Settings", e );
	    return;
	}

        /* Disable everything */
        /* deconfigure the event handle and the dhcp manager */
        handler.deconfigure();
        DhcpManager.getInstance().deconfigure();
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
            logger.warn("Could not get NatSettings", exn);
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
        
        /* NAT configuration needs information from the networking settings. */
        NetworkingConfiguration netConfig = MvvmContextFactory.context().networkingManager().get();
        
        this.handler.configure( settings, netConfig );
        DhcpManager.getInstance().configure( settings, netConfig );
    }

    protected void postStart()
    {
        registerEventListeners();

        /* Kill all active sessions */
        shutdownMatchingSessions();
    }

    protected void postStop()
    {
        /* Kill all active sessions */
        shutdownMatchingSessions();

        /* deconfigure the event handle */
        handler.deconfigure();
        DhcpManager.getInstance().deconfigure();
    }

    public void reconfigure() throws TransformException
    {
        NatSettings settings = getNatSettings();

        logger.info( "Reconfigure()" );

        /* XXXX, what goes here. Configure the handler */
        
        /* Settings will always be null right now */
        if ( settings == null ) {
            throw new TransformException( "Failed to get Nat settings: " + settings );
        }
    }

    private void updateToCurrent(NatSettings settings)
    {
        if ( settings == null ) {
            logger.error( "NULL Nat Settings" );
            return;
        }
        
        logger.info( "Update Settings Complete" );
    }

    public void dumpSessions()
    {
        for (MPipe pipe : mPipes) {
            if (pipe != null) {
                pipe.dumpSessions();
            }
        }
    }

    public IPSessionDesc[] liveSessionDescs()
    {
        IPSessionDesc[] s1 = null;
        if (mPipes[0] != null) {
            s1 = mPipes[0].liveSessionDescs();
        } else {
            s1 = new IPSessionDesc[0];
        }

        IPSessionDesc[] s2 = null;
        if (mPipes[1] != null) {
            s2 = mPipes[1].liveSessionDescs();
        } else {
            s2 = new IPSessionDesc[0];
        }

        IPSessionDesc[] retDescs = new IPSessionDesc[s1.length + s2.length];
        System.arraycopy(s1, 0, retDescs, 0, s1.length);
        System.arraycopy(s2, 0, retDescs, s1.length, s2.length);
        return retDescs;
    }

    /* Kill all sessions when starting or stopping this transform */
    protected SessionMatcher sessionMatcher()
    {
        return SessionMatcherFactory.getAllInstance();
    }

    // XXX soon to be deprecated ----------------------------------------------
    public Object getSettings()
    {
        return getNatSettings();
    }

    public void setSettings(Object settings) throws Exception
    {
        setNatSettings((NatSettings)settings);
    }

    private NatSettings getDefaultSettings()
    {
        logger.info( "Using default settings" );
        
        NatSettings settings = new NatSettings( this.getTid());

        List<RedirectRule> redirectList = new LinkedList<RedirectRule>();
        
        try {
            settings.setNatEnabled( true );
            settings.setNatInternalAddress( IPaddr.parse( "192.168.1.1" ));
            settings.setNatInternalSubnet( IPaddr.parse( "255.255.255.0" ));
            
            settings.setDmzLoggingEnabled( false );
            
            /* DMZ Settings */
            settings.setDmzEnabled( false );
            /* A sample DMZ */
            settings.setDmzAddress( IPaddr.parse( "192.168.1.2" ));
                    
            RedirectRule tmp = new RedirectRule( false, ProtocolMatcher.MATCHER_ALL,
                                                 IntfMatcher.MATCHER_OUT, IntfMatcher.MATCHER_ALL,
                                                 IPMatcher.MATCHER_ALL, IPMatcher.MATCHER_LOCAL,
                                                 PortMatcher.MATCHER_ALL, new PortMatcher( 8080 ),
                                                 true, IPaddr.parse( "192.168.1.16" ), 80 );
            tmp.setDescription( "Redirect incoming traffic to EdgeGuard port 8080 to port 80 on 192.168.1.16" );
            redirectList.add( tmp );
            
            tmp = new RedirectRule( false, ProtocolMatcher.MATCHER_ALL,
                                    IntfMatcher.MATCHER_OUT, IntfMatcher.MATCHER_ALL,
                                    IPMatcher.MATCHER_ALL, IPMatcher.MATCHER_ALL,
                                    PortMatcher.MATCHER_ALL, new PortMatcher( 6000, 10000 ),
                                    true, (IPaddr)null, 6000 );
            tmp.setDescription( "Redirect incoming traffic from ports 6000-10000 to port 6000" );
            redirectList.add( tmp );
            
            tmp = new RedirectRule( false, ProtocolMatcher.MATCHER_ALL,
                                    IntfMatcher.MATCHER_IN, IntfMatcher.MATCHER_ALL,
                                    IPMatcher.MATCHER_ALL, IPMatcher.parse( "1.2.3.4" ),
                                    PortMatcher.MATCHER_ALL, PortMatcher.MATCHER_ALL,
                                    true, IPaddr.parse( "4.3.2.1" ), 0 );
            tmp.setDescription( "Redirect outgoing traffic going to 1.2.3.4 to 4.2.3.1, (port is unchanged)" );
            redirectList.add( tmp );
            
            
            for ( Iterator<RedirectRule> iter = redirectList.iterator() ; iter.hasNext() ; ) {
                iter.next().setCategory( "[Sample]" );
            }

            /* Enable DNS and DHCP */
            settings.setDnsEnabled( true );
            settings.setDhcpEnabled( true );
                        
            settings.setDhcpStartAndEndAddress( IPaddr.parse( "192.168.1.100" ), 
                                                IPaddr.parse( "192.168.1.200" ));
            

        } catch ( Exception e ) {
            logger.error( "This should never happen", e );
        }
            
        settings.setRedirectList( redirectList );
        
        return settings;
    }

    private NatSettings getTestSettings()
    {
        logger.info( "Using the test settings" );
        
        NatSettings settings = new NatSettings( this.getTid());

        /* Need this to lookup the local IP address */
        NetworkingConfiguration netConfig = MvvmContextFactory.context().networkingManager().get();

        try {            
            /* Nat settings */
            settings.setNatEnabled( true );
            settings.setNatInternalAddress( IPaddr.parse( "192.168.1.1" ));
            settings.setNatInternalSubnet( IPaddr.parse( "255.255.255.0" ));

            /* DMZ Settings */
            settings.setDmzEnabled( true );
            settings.setDmzAddress( IPaddr.parse( "192.168.1.4" ));

            /* Redirect settings */
            IPaddr redirectHost        = IPaddr.parse( "192.168.1.6" );
            IPMatcher localHostMatcher = new IPMatcher( netConfig.host());

            List redirectList = new LinkedList();
            
            /* This rule is enabled, redirect port 7000 to the redirect host port 7 */
            redirectList.add( new RedirectRule( true, ProtocolMatcher.MATCHER_ALL,
                                                IntfMatcher.MATCHER_OUT, IntfMatcher.MATCHER_ALL,
                                                IPMatcher.MATCHER_ALL, localHostMatcher,
                                                PortMatcher.MATCHER_ALL, new PortMatcher( 7000 ),
                                                true, redirectHost, 7 ));
            
            /* This rule is disabled, to verify the on off switch works */
            redirectList.add( new RedirectRule( false, ProtocolMatcher.MATCHER_ALL,
                                                IntfMatcher.MATCHER_OUT, IntfMatcher.MATCHER_IN,
                                                IPMatcher.MATCHER_ALL, localHostMatcher, 
                                                PortMatcher.MATCHER_ALL, new PortMatcher( 5901 ),
                                                true, redirectHost, 5900 ));
            
            settings.setRedirectList( redirectList );

            /* Enable DNS and DHCP */
            settings.setDnsEnabled( true );
            settings.setDhcpEnabled( true );
            
            /* Quick sanity test, remove in a second */
            IPaddr tmp  = IPaddr.parse( "192.168.1.1" );
            IPaddr full = IPaddr.parse( "0.0.0.0" );
            
            if ( !tmp.isInNetwork( tmp, full ))
                throw new IllegalStateException( "isInNetwork is totally busted" );
            
            /* Set in reverse to test if it works */
            settings.setDhcpStartAndEndAddress( IPaddr.parse( "192.168.1.155" ), 
                                                IPaddr.parse( "192.168.1.150" ));
            
        } catch (Exception e ) {
            logger.error( "This should never happen", e );
        }
                
        return settings;
    }

    NatSessionManager getSessionManager()
    {
        return sessionManager;
    }
}
