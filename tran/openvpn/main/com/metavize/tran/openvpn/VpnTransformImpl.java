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
                /* VPN configuratoins needs information from the networking settings. */
                NetworkingConfiguration netConfig = MvvmContextFactory.context().networkingManager().get();

                /* This stops then starts openvpn */
                this.openVpnManager.configure( settings, netConfig );
                this.openVpnManager.restart();
            }
        } catch ( TransformException exn ) {
            logger.error( "Could not save VPN settings", exn );
        }
    }

    public VpnSettings getVpnSettings()
    {
        return this.settings;
    }

    public VpnSettings generateBaseParameters( VpnSettings settings )
    {
        try {
            certificateManager.createBase( settings );
            this.settings = settings;
        } catch ( TransformException e ) {
            logger.warn( "Unable to generate base parameters", e );
        }
            
        return this.settings;
    }

    public void generateClientKey( VpnClient client )
    {
    }

    public void distributeClientKey( VpnClient client, boolean usbKey, String email )
    {
        return;
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
        if ( this.settings == null ) {
            String[] args = {""};
            postInit( args );

            if ( this.settings == null ) initializeSettings();
        }

        reconfigure();
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
