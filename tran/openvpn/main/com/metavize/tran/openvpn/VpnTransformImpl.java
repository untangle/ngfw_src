/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.openvpn;

import java.util.List;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.tran.TransformState;
import com.metavize.mvvm.tran.TransformStopException;
import com.metavize.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class VpnTransformImpl extends AbstractTransform
    implements VpnTransform
{
    private static final String TRAN_NAME    = "openvpn";
    private static final String WEB_APP      = TRAN_NAME;
    private static final String WEB_APP_PATH = "/" + WEB_APP;

    private final Logger logger = Logger.getLogger( VpnTransformImpl.class );

    final VpnStatisticManager statisticManager;

    private boolean isWebAppDeployed = false;

    private final SoloPipeSpec pipeSpec;
    private final SoloPipeSpec[] pipeSpecs;

    private final OpenVpnManager openVpnManager = new OpenVpnManager();
    private final CertificateManager certificateManager = new CertificateManager();
    private final AddressMapper addressMapper = new AddressMapper();

    private final EventHandler handler;

    private VpnSettings settings;

    // constructor ------------------------------------------------------------

    public VpnTransformImpl()
    {
        this.handler          = new EventHandler( this );
        this.statisticManager = new VpnStatisticManager(getTransformContext());

        /* Have to figure out pipeline ordering, this should always
         * next to towards the outside, then there is OpenVpn and then Nat */
        this.pipeSpec = new SoloPipeSpec
            ( TRAN_NAME, this, handler, Fitting.OCTET_STREAM, Affinity.OUTSIDE,
             SoloPipeSpec.MAX_STRENGTH - 2);
        this.pipeSpecs = new SoloPipeSpec[] { pipeSpec };
    }

    protected void initializeSettings()
    {
        VpnSettings settings = new VpnSettings( this.getTid());
        logger.info("Initializing Settings...");

        setVpnSettings( settings );

        /* Stop the statistics manager */
        statisticManager.stop();
    }

    // VpnTransform methods --------------------------------------------------

    public void setVpnSettings( final VpnSettings settings )
    {
        /* Attempt to assign all of the clients addresses */
        try {
            addressMapper.assignAddresses( settings );
        } catch ( TransformException exn ) {
            logger.error( "Could not save VPN settings", exn );
        }

        /* Update the status/generate all of the certificates for clients */
        this.certificateManager.updateCertificateStatus( settings );

        /* XXXXXXXXXXXXXXXXX NO, just for testing */
        for ( VpnClient client : (List<VpnClient>)settings.getClientList()) {
            try {
                this.openVpnManager.writeClientConfigurationFiles( settings, client );
            } catch ( TransformException e ) {
                logger.error( "Error writing config file for " + client.getName());
            }
        }

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork( Session s )
                {
                    s.saveOrUpdate( settings );
                    VpnTransformImpl.this.settings = settings;
                    return true;
                }
            };

        getTransformContext().runTransaction( tw );

        try {
            reconfigure();

            if ( getRunState() == TransformState.RUNNING ) {
                /* This stops then starts openvpn */
                this.openVpnManager.configure( settings );
                this.openVpnManager.restart();
                this.handler.configure( settings );
            }
        } catch ( TransformException exn ) {
            logger.error( "Could not save VPN settings", exn );
        }
    }

    public VpnSettings getVpnSettings()
    {

        /* XXXXXXXXXXXXXXXXXXXX This is not really legit, done so the schema doesn't have to be
         * written for a little while */
        if ( this.settings == null ) this.settings = new VpnSettings( this.getTid());

        return this.settings;
    }

    public VpnSettings generateBaseParameters( VpnSettings settings )
    {
        try {
            certificateManager.createBase( settings );
            setVpnSettings( settings );
        } catch ( TransformException e ) {
            logger.warn( "Unable to generate base parameters", e );
        }

        return this.settings;
    }

    public VpnClient generateClientCertificate( VpnSettings settings, VpnClient client )
    {
        try {
            certificateManager.createClient( client );
        } catch ( TransformException e ) {
            logger.error( "Unable to create a client certificate", e );
        }

        return client;
    }

    public VpnClient revokeClientCertificate( VpnSettings settings, VpnClient client )
    {
        try {
            certificateManager.revokeClient( client );
        } catch ( TransformException e ) {
            logger.error( "Unable to revoke a client certificate", e );
        }

        return client;
    }


    public void distributeClientKey( VpnClient client, boolean usbKey, String email )
    {
    }

    /* Get the common name for the key, and clear it if it exists */
    public synchronized String lookupClientDistributionKey( String key, IPaddr clientAddress )
    {
        logger.debug( "Looking up client for key: " + key );

        /* Could use a hash map, but why bother ? */
        for ( final VpnClient client : ((List<VpnClient>)this.settings.getClientList())) {
            String clientKey = client.getDistributionKey();

            if ( clientKey != null ) clientKey = clientKey.trim();
            logger.debug( "Checking: " + clientKey );

            if ( clientKey == null || clientKey.length() == 0 ) continue;
            if ( clientKey.equalsIgnoreCase( key )) {

                client.setDistributionKey( null );

                TransactionWork tw = new TransactionWork()
                    {
                        public boolean doWork( Session s )
                        {
                            s.saveOrUpdate( client );
                            return true;
                        }
                    };

                getTransformContext().runTransaction( tw );

                /* Log the client distribution event.  Must be done with
                 * the statistic manager because the thread is not currently
                 * registered for the event logger. */
                this.statisticManager.
                    addClientDistributionEvent( new ClientDistributionEvent( clientAddress,
                                                                             client.getName()));
                return client.getInternalName();
            }
        }

        return null;
    }

    private synchronized void deployWebAppIfRequired()
    {
        if ( !isWebAppDeployed ) {
            if ( MvvmContextFactory.context().loadWebApp( WEB_APP_PATH, WEB_APP )) {
                logger.debug("Deployed openvpn web app");
            }
            else logger.error("Unable to deploy openvpn web app");
        }
        isWebAppDeployed = true;
    }

    private synchronized void unDeployWebAppIfRequired()
    {
        if ( isWebAppDeployed ) {
            if( MvvmContextFactory.context().unloadWebApp(WEB_APP_PATH )) {
                logger.debug("Unloaded openvpn web app");
            }
            logger.error("Unable to unload openvpn web app");
        }
        isWebAppDeployed = false;
    }

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // lifecycle --------------------------------------------------------------

    protected void postInit(final String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork( Session s )
                {
                    Query q = s.createQuery( "from VpnSettings ts where ts.tid = :tid" );

                    q.setParameter( "tid", getTid());

                    settings = (VpnSettings)q.uniqueResult();
                    return true;
                }
            };
        getTransformContext().runTransaction( tw );

        reconfigure();
    }

    @Override protected void preStart() throws TransformStartException
    {
        super.preStart();

        /* XXXXX Need a way of not starting if the transform is not configured */
        if ( this.settings == null ) {
            String[] args = {""};
            postInit( args );

            if ( this.settings == null ) initializeSettings();
        }

        reconfigure();

        deployWebAppIfRequired();

        statisticManager.start();
    }

    @Override protected void postStop() throws TransformStopException
    {
        super.postStop();
        statisticManager.stop();

        try {
            this.openVpnManager.stop();
        } catch ( TransformException e ) {
            throw new TransformStopException( e );
        }
    }

    @Override protected void preStop() throws TransformStopException {
        super.preStop();
        unDeployWebAppIfRequired();
    }

    public void reconfigure()
    {
        /* Nothing to do here */
    }

    // private methods -------------------------------------------------------

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getVpnSettings();
    }

    public void setSettings( Object settings )
    {
        setVpnSettings((VpnSettings)settings);
    }
}
