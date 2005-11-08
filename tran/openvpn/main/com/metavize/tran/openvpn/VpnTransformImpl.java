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

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.NetworkingConfiguration;
import com.metavize.mvvm.tran.TransformState;
import com.metavize.mvvm.tran.TransformException;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.util.TransactionWork;

public class VpnTransformImpl extends AbstractTransform
    implements VpnTransform
{
    private final Logger logger = Logger.getLogger( VpnTransformImpl.class );

    private final PipeSpec[] pipeSpec = new PipeSpec[0];
    private final OpenVpnManager openVpnManager = new OpenVpnManager();
    private final CertificateManager certificateManager = new CertificateManager();
    private final AddressMapper addressMapper = new AddressMapper();

    private VpnSettings settings;

    // constructor ------------------------------------------------------------

    public VpnTransformImpl()
    {
    }

    protected void initializeSettings()
    {
        VpnSettings settings = new VpnSettings( this.getTid());
        logger.info("Initializing Settings...");

        setVpnSettings( settings );
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

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork( Session s )
                {
                    s.saveOrUpdate( settings );
                    VpnTransformImpl.this.settings = settings;
                    return true;
                }

                public Object getResult()
                { 
                    return null;
                }
            };

        getTransformContext().runTransaction( tw );

        try {
            reconfigure();
            
            if ( getRunState() == TransformState.RUNNING ) {
                /* This stops then starts openvpn */
                this.openVpnManager.configure( settings );
                this.openVpnManager.restart();
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

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpec;
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

                public Object getResult()
                { 
                    return null; 
                }
            };
        getTransformContext().runTransaction( tw );

        reconfigure();
    }

    protected void preStart()
    {
        /* XXXXX Need a way of not starting if the transform is not configured */
        if ( this.settings == null ) {
            String[] args = {""};
            postInit( args );

            if ( this.settings == null ) initializeSettings();
        }

        reconfigure();
    }

    protected void postStop()
    {
        try {
            this.openVpnManager.stop();
        } catch ( TransformException e ){
            logger.error( "Unable to stop open vpn", e );
        }
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
