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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.NetworkingConfiguration;
import com.metavize.mvvm.argon.SessionMatcher;
import com.metavize.mvvm.argon.SessionMatcherFactory;
import com.metavize.mvvm.logging.EventHandler;
import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.tran.TransformState;
import com.metavize.mvvm.tran.TransformStopException;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.firewall.IntfMatcher;
import com.metavize.mvvm.tran.firewall.PortMatcher;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import com.metavize.mvvm.util.TransactionWork;
import com.metavize.tran.token.TokenAdaptor;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class NatImpl extends AbstractTransform implements Nat
{
    private static final int CREATE_DATE_IDX   =  1;
    private static final int PROTO_IDX         =  2;
    private static final int O_CLIENT_ADDR_IDX =  3;
    private static final int O_CLIENT_PORT_IDX =  4;
    private static final int R_CLIENT_ADDR_IDX =  5;
    private static final int O_SERVER_ADDR_IDX =  6;
    private static final int O_SERVER_PORT_IDX =  7;
    private static final int R_SERVER_ADDR_IDX =  8;
    private static final int R_SERVER_PORT_IDX =  9;
    private static final int CLIENT_INTF_IDX   = 10;
    private static final int SERVER_INTF_IDX   = 11;
    private static final int RULE_INDEX_IDX    = 12;
    private static final int IS_DMZ_IDX        = 13;

    private final NatEventHandler handler;
    private final NatSessionManager sessionManager;
    final NatStatisticManager statisticManager;

    private final SoloPipeSpec natPipeSpec;
    private final SoloPipeSpec natFtpPipeSpec;

    private final PipeSpec[] pipeSpecs;

    private final DhcpManager dhcpManager;

    private final EventLogger<LogEvent> eventLogger;

    private final Logger logger = Logger.getLogger( NatImpl.class );

    private NatSettings settings = null;

    public NatImpl()
    {
        handler = new NatEventHandler(this);
        sessionManager = new NatSessionManager(this);
        dhcpManager = new DhcpManager( this );
        statisticManager = new NatStatisticManager();

        /* Have to figure out pipeline ordering, this should always next
         * to towards the outside */
        natPipeSpec = new SoloPipeSpec
            ("nat", this, handler, Fitting.OCTET_STREAM, Affinity.OUTSIDE,
             SoloPipeSpec.MAX_STRENGTH - 1);

        /* This subscription has to evaluate after NAT */
        natFtpPipeSpec = new SoloPipeSpec
            ("nat-ftp", this, new TokenAdaptor(this, new NatFtpFactory(this)),
             Fitting.FTP_TOKENS, Affinity.SERVER, 0);

        pipeSpecs = new SoloPipeSpec[] { natPipeSpec, natFtpPipeSpec };

        TransformContext tctx = getTransformContext();
        eventLogger = new EventLogger<LogEvent>(tctx);

        EventHandler eh = new NatRedirectEventHandler(tctx);
        eventLogger.addEventHandler(eh);
    }

    public NatSettings getNatSettings()
    {
        if (settings.getDhcpEnabled()) {
            /* Insert all of the leases from DHCP */
            dhcpManager.loadLeases(settings);
        } else {
            /* remove any leftover leases from DHCP */
            dhcpManager.fleeceLeases(settings);
        }

        return this.settings;
    }

    public void setNatSettings(final NatSettings settings) throws Exception
    {
        /* Remove all of the non-static addresses before saving */
        dhcpManager.fleeceLeases( settings );

        /* Validate the settings */
        try {
            settings.validate();
        }
        catch (Exception e) {
            logger.error("Invalid NAT settings", e);
            throw e;
        }

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(settings);
                    NatImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        try {
            reconfigure();

            if (getRunState() == TransformState.RUNNING) {
                /* NAT configuration needs information from the
                 * networking settings. */
                NetworkingConfiguration netConfig = MvvmContextFactory.context().networkingManager().get();

                /* Have to configure DHCP before the handler */
                dhcpManager.configure(settings, netConfig);
                this.handler.configure(settings, netConfig);
                dhcpManager.startDnsMasq();
            }
        } catch (TransformException exn) {
            logger.error( "Could not save Nat settings", exn );
        }
    }

    public EventManager<LogEvent> getEventManager()
    {
        return eventLogger;
    }

    // package protected methods ----------------------------------------------

    NatEventHandler getHandler()
    {
        return handler;
    }

    MPipe getNatMPipe()
    {
        return natPipeSpec.getMPipe();
    }

    MPipe getNatFtpPipeSpec()
    {
        return natFtpPipeSpec.getMPipe();
    }

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    protected void initializeSettings()
    {
        logger.info("Initializing Settings...");

        NatSettings settings = getDefaultSettings();

        /* Disable everything */

        /* deconfigure the event handle and the dhcp manager */
        dhcpManager.deconfigure();

        try {
            setNatSettings(settings);
            // Handler doesn't need to be deconfigured at initialization.
            // handler.deconfigure();
        } catch( Exception e ) {
            logger.error( "Unable to set Nat Settings", e );
        }

        /* Stop the statistics manager */
        statisticManager.stop();
    }

    protected void postInit(String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from NatSettings hbs where hbs.tid = :tid");
                    q.setParameter("tid", getTid());
                    NatImpl.this.settings = (NatSettings)q.uniqueResult();

                    updateToCurrent(NatImpl.this.settings);

                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);
    }

    protected void preStart() throws TransformStartException
    {
        eventLogger.start();

        try {
            reconfigure();
        } catch (Exception e) {
            throw new TransformStartException(e);
        }

        /* NAT configuration needs information from the networking settings. */
        NetworkingConfiguration netConfig = MvvmContextFactory.context()
            .networkingManager().get();


        dhcpManager.configure( settings, netConfig );
        try {
            handler.configure( settings, netConfig );
        } catch ( TransformException e ) {
            throw new TransformStartException(e);
        }
        dhcpManager.startDnsMasq();

        statisticManager.start();
    }

    protected void postStart()
    {
        /* Kill all active sessions */
        shutdownMatchingSessions();
    }

    protected void postStop() throws TransformStopException
    {
        /* Kill all active sessions */
        shutdownMatchingSessions();

        dhcpManager.deconfigure();

        /* deconfigure the event handle */
        try {
            handler.deconfigure();
        } catch ( TransformException e ) {
            throw new TransformStopException( e );
        }

        statisticManager.stop();

        eventLogger.stop();
    }

    public void reconfigure() throws TransformException
    {
        NatSettings settings = getNatSettings();

        logger.info("Reconfigure()");

        /* ????, what goes here. Configure the handler */

        /* Settings will always be null right now */
        if (settings == null) throw new TransformException("Failed to get Nat settings: " + settings);
    }

    private void updateToCurrent(NatSettings settings)
    {
        if (settings == null) {
            logger.error("NULL Nat Settings");
        } else {
            logger.info("Update Settings Complete");
        }
    }

    /* Kill all sessions when starting or stopping this transform */
    protected SessionMatcher sessionMatcher()
    {
        return SessionMatcherFactory.getAllInstance();
    }

    void log(LogEvent le)
    {
        eventLogger.log(le);
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
                                                 IntfMatcher.getOutside(), IntfMatcher.getAll(),
                                                 IPMatcher.MATCHER_ALL, IPMatcher.MATCHER_LOCAL,
                                                 PortMatcher.MATCHER_ALL, new PortMatcher( 8080 ),
                                                 true, IPaddr.parse( "192.168.1.16" ), 80 );
            tmp.setDescription( "Redirect incoming traffic to EdgeGuard port 8080 to port 80 on 192.168.1.16" );
            tmp.setLog( true );

            redirectList.add( tmp );

            tmp = new RedirectRule( false, ProtocolMatcher.MATCHER_ALL,
                                    IntfMatcher.getOutside(), IntfMatcher.getAll(),
                                    IPMatcher.MATCHER_ALL, IPMatcher.MATCHER_ALL,
                                    PortMatcher.MATCHER_ALL, new PortMatcher( 6000, 10000 ),
                                    true, (IPaddr)null, 6000 );
            tmp.setDescription( "Redirect incoming traffic from ports 6000-10000 to port 6000" );
            redirectList.add( tmp );

            tmp = new RedirectRule( false, ProtocolMatcher.MATCHER_ALL,
                                    IntfMatcher.getInside(), IntfMatcher.getAll(),
                                    IPMatcher.MATCHER_ALL, IPMatcher.parse( "1.2.3.4" ),
                                    PortMatcher.MATCHER_ALL, PortMatcher.MATCHER_ALL,
                                    true, IPaddr.parse( "4.3.2.1" ), 0 );
            tmp.setDescription( "Redirect outgoing traffic going to 1.2.3.4 to 4.2.3.1, (port is unchanged)" );
            tmp.setLog( true );

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
                                                IntfMatcher.getOutside(), IntfMatcher.getAll(),
                                                IPMatcher.MATCHER_ALL, localHostMatcher,
                                                PortMatcher.MATCHER_ALL, new PortMatcher( 7000 ),
                                                true, redirectHost, 7 ));

            /* This rule is disabled, to verify the on off switch works */
            redirectList.add( new RedirectRule( false, ProtocolMatcher.MATCHER_ALL,
                                                IntfMatcher.getInside(), IntfMatcher.getInside(),
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
