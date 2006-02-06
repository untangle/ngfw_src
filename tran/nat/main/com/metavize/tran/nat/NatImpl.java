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

import org.apache.log4j.Logger;

import org.hibernate.Query;
import org.hibernate.Session;

import com.metavize.mvvm.MvvmContextFactory;

import com.metavize.mvvm.argon.SessionMatcher;
import com.metavize.mvvm.argon.SessionMatcherFactory;
import com.metavize.mvvm.networking.NetworkSettings;
import com.metavize.mvvm.networking.NetworkSpace;
import com.metavize.mvvm.networking.Interface;
import com.metavize.mvvm.networking.NetworkException;
import com.metavize.mvvm.networking.IPNetwork;
import com.metavize.mvvm.networking.IPNetworkRule;

import com.metavize.mvvm.logging.SimpleEventFilter;
import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.logging.LogEvent;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;

import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.tran.TransformState;
import com.metavize.mvvm.tran.TransformStopException;
import com.metavize.mvvm.tran.ValidateException;

import com.metavize.mvvm.util.TransactionWork;
import com.metavize.tran.token.TokenAdaptor;

public class NatImpl extends AbstractTransform implements Nat
{
    private final NatEventHandler handler;
    private final NatSessionManager sessionManager;
    private final SettingsManager settingsManager = new SettingsManager();
    final NatStatisticManager statisticManager;

    private final SoloPipeSpec natPipeSpec;
    private final SoloPipeSpec natFtpPipeSpec;

    private final PipeSpec[] pipeSpecs;

    private final DhcpManager dhcpManager;

    private final EventLogger<LogEvent> eventLogger;

    private final Logger logger = Logger.getLogger( NatImpl.class );

    private NetworkSpacesSettingsPriv settingsPriv = null;

    public NatImpl()
    {
        this.handler = new NatEventHandler(this);
        sessionManager = new NatSessionManager(this);
        dhcpManager = new DhcpManager( this );
        statisticManager = new NatStatisticManager(getTransformContext());

        /* Have to figure out pipeline ordering, this should always next
         * to towards the outside */
        natPipeSpec = new SoloPipeSpec
            ("nat", this, this.handler, Fitting.OCTET_STREAM, Affinity.OUTSIDE,
             SoloPipeSpec.MAX_STRENGTH - 1);

        /* This subscription has to evaluate after NAT */
        natFtpPipeSpec = new SoloPipeSpec
            ("nat-ftp", this, new TokenAdaptor(this, new NatFtpFactory(this)),
             Fitting.FTP_TOKENS, Affinity.SERVER, 0);

        pipeSpecs = new SoloPipeSpec[] { natPipeSpec, natFtpPipeSpec };

        TransformContext tctx = getTransformContext();
        eventLogger = new EventLogger<LogEvent>(tctx);

        SimpleEventFilter ef = new NatRedirectFilter();
        eventLogger.addSimpleEventFilter(ef);
    }

    public NatSettingsWrapper getNatSettings()
    {
        if ( this.settingsPriv.getDhcpEnabled()) {
            /* Insert all of the leases from DHCP */
            dhcpManager.loadLeases( settingsPriv );
        } else {
            /* remove any leftover leases from DHCP */
            dhcpManager.fleeceLeases( settingsPriv  );
        }

        return settingsManager.buildWrapper( this.settingsPriv );
    }

    public void setNatSettings( final NatSettingsWrapper wrapper ) throws Exception
    {
        NetworkSpacesSettingsPriv settingsPriv;

        /* Validate the settings, and wrap the wrapper into private settings.
         * this precomputes all of dependencies (such as which netspace the DNS/DHCP
         * server run on.) */
        try {
            /* convert the setting state, this allows the GUI to run in either basic or advanced mode */
            settingsManager.convertSetupState( wrapper );

            wrapper.validate();

            /* Build the internal representation of the settings */
            settingsPriv = settingsManager.makePrivSettings( wrapper );
        } catch ( ValidateException e ) {
            logger.error( "Invalid NAT settings", e );
            throw e;
        }

        /* Remove all of the non-static addresses before saving */
        this.dhcpManager.fleeceLeases( settingsPriv );
        
        try {
            updateSettings( settingsPriv );
        } catch ( Exception exn ) {
            /* !!! ???? Should this be caught or thrown up.  Doing both to
             * guarantee it gets into the logs */
            logger.error( "Could not save Nat settings", exn );
            throw( exn );
        }
        
        /* Save the settings to the database */
        saveSettings( settingsPriv.getNatSettings());
    }

    public EventManager<LogEvent> getEventManager()
    {
        return eventLogger;
    }

    // package protected methods ----------------------------------------------

    NatEventHandler getHandler()
    {
        return this.handler;
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

        NatSettingsWrapper wrapper = settingsManager.getDefaultSettings( this.getTid());

        /* Deconfigure the dhcp and event manager */

        /* deconfigure the event handle and the dhcp manager,
         * this is just to stop DNS masq */
        dhcpManager.deconfigure();

        try {
            setNatSettings( wrapper );
        } catch( Exception e ) {
            logger.error( "Unable to set Nat Settings", e );
        }

        /* Stop the statistics manager */
        statisticManager.stop();
    }

    protected void postInit(String[] args)
    {
        final NatSettings natSettings[] = new NatSettings[1];

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from NatSettings hbs where hbs.tid = :tid");
                    q.setParameter("tid", getTid());
                    natSettings[0] = (NatSettings)q.uniqueResult();
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        NetworkSettings networkSettings = 
            MvvmContextFactory.context().networkManager().getNetworkSettings();

        if ( natSettings[0] == null ) {
            /* !!!! This should do something a little bit more reasonable */
            throw new NullPointerException( "unable to load settings" );
        }

        try {
            this.settingsPriv = settingsManager.makePrivSettings( natSettings[0], networkSettings );
        } catch ( TransformException e ) {
            logger.error( "uh ohh, tell rbs something is up", e );
            /* XXXXXXXXXXXXX !!!!!!!!!!!!! handle this before release */
            throw new NullPointerException( "ohh dang" );
        }
    }

    protected void preStart() throws TransformStartException
    {
        eventLogger.start();

        try {
            this.dhcpManager.configure( this.settingsPriv );
            this.handler.configure( this.settingsPriv );
        } catch ( TransformException e ) {
            throw new TransformStartException(e);
        }

        this.dhcpManager.startDnsMasq();
        this.statisticManager.start();
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

        statisticManager.stop();

        /* deconfigure the event handle */
        try {
            this.handler.deconfigure();
        } catch ( TransformException e ) {
            /* XXX Why ???,
             * RBS: it seems like the eventLogger should stop first */
            throw new TransformStopException( e );
        }

        eventLogger.stop();
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
        setNatSettings((NatSettingsWrapper)settings );
    }

    /** Reconfigure the box, use whenever the settings change, probably should be named reconfigure. */
    private void updateSettings( NetworkSpacesSettingsPriv settingsPriv ) 
        throws TransformException, NetworkException, ValidateException
    {
        /* Always save the network settings */
        MvvmContextFactory.context().networkManager().setNetworkSettings( settingsPriv.getNetworkSettings());
        
        if (getRunState() == TransformState.RUNNING) {
            this.dhcpManager.configure( settingsPriv );
            this.handler.configure( settingsPriv );
            this.dhcpManager.startDnsMasq();
        }

        this.settingsPriv = settingsPriv;
    }
    
    private void saveSettings( final NatSettings settings )
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate( settings );
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction( tw );
    }

    NatSessionManager getSessionManager()
    {
        return sessionManager;
    }
}
