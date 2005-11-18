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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.NetworkingConfiguration;
import com.metavize.mvvm.argon.SessionMatcher;
import com.metavize.mvvm.argon.SessionMatcherFactory;
import com.metavize.mvvm.logging.EventFilter;
import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.firewall.PortMatcher;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import com.metavize.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class FirewallImpl extends AbstractTransform implements Firewall
{
    private final EventHandler handler;
    private final SoloPipeSpec pipeSpec;
    private final SoloPipeSpec[] pipeSpecs;

    private final EventLogger<FirewallEvent> eventLogger;

    private final Logger logger = Logger.getLogger(FirewallImpl.class);

    private FirewallSettings settings = null;
    final FirewallStatisticManager statisticManager;

    public FirewallImpl()
    {
        this.handler = new EventHandler( this );
        this.statisticManager = new FirewallStatisticManager(getTransformContext());

        /* Have to figure out pipeline ordering, this should always
         * next to towards the outside, then there is OpenVpn and then Nat */
        this.pipeSpec = new SoloPipeSpec
            ("firewall", this, handler, Fitting.OCTET_STREAM, Affinity.OUTSIDE,
             SoloPipeSpec.MAX_STRENGTH - 3);
        this.pipeSpecs = new SoloPipeSpec[] { pipeSpec };

        TransformContext tctx = getTransformContext();
        eventLogger = new EventLogger<FirewallEvent>(tctx);

        EventFilter ef = new FirewallAllFilter();
        eventLogger.addEventFilter(ef);
        ef = new FirewallBlockedFilter();
        eventLogger.addEventFilter(ef);
    }

    // Firewall methods -------------------------------------------------------

    public FirewallSettings getFirewallSettings()
    {
        return settings;
    }

    public void setFirewallSettings(final FirewallSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(settings);
                    FirewallImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        try {
            reconfigure();
        }
        catch (TransformException exn) {
            logger.error("Could not save Firewall settings", exn);
        }
    }

    public EventManager<FirewallEvent> getEventManager()
    {
        return eventLogger;
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

        FirewallSettings settings = getDefaultSettings();

        setFirewallSettings(settings);

        statisticManager.stop();
    }

    protected void postInit(String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from FirewallSettings hbs where hbs.tid = :tid");
                    q.setParameter("tid", getTid());
                    FirewallImpl.this.settings = (FirewallSettings)q.uniqueResult();

                    updateToCurrent(FirewallImpl.this.settings);

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

        statisticManager.start();
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

        statisticManager.stop();

        eventLogger.stop();
    }

    public    void reconfigure() throws TransformException
    {
        FirewallSettings settings = getFirewallSettings();
        ArrayList enabledPatternsList = new ArrayList();

        logger.info("Reconfigure()");

        if (settings == null) {
            throw new TransformException("Failed to get Firewall settings: " + settings);
        }

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

    void log(FirewallEvent logEvent)
    {
        eventLogger.log(logEvent);
    }

    FirewallSettings getDefaultSettings()
    {
        logger.info( "Loading the default settings" );
        FirewallSettings settings = new FirewallSettings( this.getTid());

        /* Need this to lookup the local IP address */
        NetworkingConfiguration netConfig = MvvmContextFactory.context().networkingManager().get();

        try {
            /* Redirect settings */
            settings.setQuickExit( true );
            settings.setRejectSilently( true );
            settings.setDefaultAccept( true );

            List<FirewallRule> firewallList = new LinkedList<FirewallRule>();

            IPMatcher localHostMatcher = new IPMatcher( netConfig.host());



            FirewallRule tmp = new FirewallRule( false, ProtocolMatcher.MATCHER_ALL,
                                                 false, true,
                                                 IPMatcher.MATCHER_ALL, IPMatcher.MATCHER_ALL,
                                                 PortMatcher.MATCHER_ALL, new PortMatcher( 21 ),
                                                 true );
            tmp.setLog( true );
            tmp.setDescription( "Block and log all incoming traffic destined to port 21 (FTP)" );
            firewallList.add( tmp );

            /* Block all traffic TCP traffic from the network 1.2.3.4/255.255.255.0 */
            tmp = new FirewallRule( false, ProtocolMatcher.MATCHER_TCP,
                                    true, true,
                                    IPMatcher.parse( "1.2.3.0/255.255.255.0" ), IPMatcher.MATCHER_ALL,
                                    PortMatcher.MATCHER_ALL, PortMatcher.MATCHER_ALL,
                                    true );
            tmp.setDescription( "Block all TCP traffic from 1.2.3.0 netmask 255.255.255.0" );
            firewallList.add( tmp );

            tmp = new FirewallRule( false, ProtocolMatcher.MATCHER_ALL,
                                    true, true,
                                    IPMatcher.MATCHER_ALL, IPMatcher.parse( "1.2.3.1-1.2.3.10" ),
                                    new PortMatcher( 1000, 5000 ), PortMatcher.MATCHER_ALL,
                                    false );
            tmp.setLog( true );
            tmp.setDescription( "Accept and log all traffic to the range 1.2.3.1 - 1.2.3.10 from ports 1000-5000" );
            firewallList.add( tmp );

            tmp = new FirewallRule( false, ProtocolMatcher.MATCHER_PING,
                                    true, true,
                                    IPMatcher.MATCHER_ALL, IPMatcher.parse( "1.2.3.1" ),
                                    PortMatcher.MATCHER_PING, PortMatcher.MATCHER_PING,
                                    false );
            tmp.setDescription( "Accept PINGs to 1.2.3.1.  Note: the source and destination ports are ignored." );
            firewallList.add( tmp );


            for ( Iterator<FirewallRule> iter = firewallList.iterator() ; iter.hasNext() ; ) {
                iter.next().setCategory( "[Sample]" );
            }

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
