/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.tran.firewall;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.untangle.mvvm.localapi.SessionMatcher;
import com.untangle.mvvm.localapi.SessionMatcherFactory;
import com.untangle.mvvm.logging.EventLogger;
import com.untangle.mvvm.logging.EventLoggerFactory;
import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.logging.SimpleEventFilter;
import com.untangle.mvvm.tapi.AbstractTransform;
import com.untangle.mvvm.tapi.Affinity;
import com.untangle.mvvm.tapi.Fitting;
import com.untangle.mvvm.tapi.PipeSpec;
import com.untangle.mvvm.tapi.SoloPipeSpec;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.tran.TransformException;
import com.untangle.mvvm.tran.TransformStartException;
import com.untangle.mvvm.tran.firewall.ip.IPMatcherFactory;
import com.untangle.mvvm.tran.firewall.port.PortMatcherFactory;
import com.untangle.mvvm.tran.firewall.protocol.ProtocolMatcherFactory;
import com.untangle.mvvm.util.TransactionWork;
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
        eventLogger = EventLoggerFactory.factory().getEventLogger(getTransformContext());

        SimpleEventFilter ef = new FirewallAllFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new FirewallBlockedFilter();
        eventLogger.addSimpleEventFilter(ef);
    }

    // Firewall methods -------------------------------------------------------

    public FirewallSettings getFirewallSettings()
    {
        if( settings == null )
            logger.error("Settings not yet initialized. State: " + getTransformContext().getRunState() );
        return settings;
    }

    public void setFirewallSettings(final FirewallSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(settings);
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

    public void initializeSettings()
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
    }

    private void reconfigure() throws TransformException
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

        try {
            IPMatcherFactory ipmf = IPMatcherFactory.getInstance();
            PortMatcherFactory pmf = PortMatcherFactory.getInstance();
            ProtocolMatcherFactory prmf = ProtocolMatcherFactory.getInstance();


            /* A few sample settings */
            settings.setQuickExit( true );
            settings.setRejectSilently( true );
            settings.setDefaultAccept( true );

            List<FirewallRule> firewallList = new LinkedList<FirewallRule>();

            FirewallRule tmp = new FirewallRule( false, prmf.getTCPAndUDPMatcher(),
                                                 false, true,
                                                 ipmf.getAllMatcher(), ipmf.getAllMatcher(),
                                                 pmf.getAllMatcher(), pmf.makeSingleMatcher( 21 ),
                                                 true );
            tmp.setLog( true );
            tmp.setDescription( "Block and log all incoming traffic destined to port 21 (FTP)" );
            firewallList.add( tmp );

            /* Block all traffic TCP traffic from the network 1.2.3.4/255.255.255.0 */
            tmp = new FirewallRule( false, prmf.getTCPMatcher(),
                                    true, true,
                                    ipmf.parse( "1.2.3.0/255.255.255.0" ), ipmf.getAllMatcher(),
                                    pmf.getAllMatcher(), pmf.getAllMatcher(),
                                    true );
            tmp.setDescription( "Block all TCP traffic from 1.2.3.0 netmask 255.255.255.0" );
            firewallList.add( tmp );

            tmp = new FirewallRule( false, prmf.getTCPAndUDPMatcher(),
                                    true, true,
                                    ipmf.getAllMatcher(), ipmf.parse( "1.2.3.1 - 1.2.3.10" ),
                                    pmf.makeRangeMatcher( 1000, 5000 ), pmf.getAllMatcher(),
                                    false );
            tmp.setLog( true );
            tmp.setDescription( "Accept and log all traffic to the range 1.2.3.1 - 1.2.3.10 from ports 1000-5000" );
            firewallList.add( tmp );

            tmp = new FirewallRule( false, prmf.getPingMatcher(),
                                    true, true,
                                    ipmf.getAllMatcher(), ipmf.parse( "1.2.3.1" ),
                                    pmf.getPingMatcher(), pmf.getPingMatcher(),
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
        return SessionMatcherFactory.makePolicyInstance( getPolicy());
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
