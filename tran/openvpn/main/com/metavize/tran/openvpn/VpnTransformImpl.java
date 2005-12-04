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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.List;
import java.util.LinkedList;
import java.util.Random;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.IntfConstants;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.tran.UnconfiguredException;
import com.metavize.mvvm.tran.TransformState;
import com.metavize.mvvm.tran.TransformStats;
import com.metavize.mvvm.tran.TransformStopException;
import com.metavize.mvvm.tran.ValidateException;

import com.metavize.mvvm.util.TransactionWork;
import com.metavize.mvvm.argon.ArgonException;

import com.metavize.mvvm.tran.script.ScriptRunner;

import com.metavize.mvvm.MailSender;

public class VpnTransformImpl extends AbstractTransform
    implements VpnTransform
{
    private static final String TRAN_NAME    = "openvpn";
    private static final String WEB_APP      = TRAN_NAME;
    private static final String WEB_APP_PATH = "/" + WEB_APP;
    private static final String MAIL_IMAGE_DIR_PREFIX = "images";
    private static final String MAIL_IMAGE_DIR = Constants.DATA_DIR + "/images";

    private static final String CLEANUP_SCRIPT = Constants.SCRIPT_DIR + "/cleanup";

    private final Logger logger = Logger.getLogger( VpnTransformImpl.class );
    
    private boolean isWebAppDeployed = false;

    private final SoloPipeSpec pipeSpec;
    private final SoloPipeSpec[] pipeSpecs;

    private final Random random = new Random();

    private final OpenVpnManager openVpnManager = new OpenVpnManager();
    private final CertificateManager certificateManager = new CertificateManager();
    private final AddressMapper addressMapper = new AddressMapper();
    private final OpenVpnMonitor openVpnMonitor;

    private final EventHandler handler;

    private VpnSettings settings;

    private Sandbox sandbox = null;

    // constructor ------------------------------------------------------------

    public VpnTransformImpl()
    {
        this.handler          = new EventHandler( this );
        this.openVpnMonitor   = new OpenVpnMonitor( this );

        /* Have to figure out pipeline ordering, this should always
         * next to towards the outside, then there is OpenVpn and then Nat */
        this.pipeSpec = new SoloPipeSpec
            ( TRAN_NAME, this, handler, Fitting.OCTET_STREAM, Affinity.OUTSIDE,
             SoloPipeSpec.MAX_STRENGTH - 2);
        this.pipeSpecs = new SoloPipeSpec[] { pipeSpec };
    }
    
    @Override protected void initializeSettings()
    {
        VpnSettings settings = new VpnSettings( this.getTid());
        logger.info( "Initializing Settings... to unconfigured" );

        setVpnSettings( settings );

        /* Stop the open vpn monitor(I don't think this actually does anything, rbs) */
        openVpnMonitor.stop();
    }

    // VpnTransform methods --------------------------------------------------
    public void setVpnSettings( final VpnSettings newSettings )
    {
        final VpnSettings oldSettings = this.settings;

        /* Attempt to assign all of the clients addresses only if in server mode */
        try {
            if ( !newSettings.getIsEdgeGuardClient()) addressMapper.assignAddresses( newSettings );
        } catch ( TransformException exn ) {
            logger.error( "Could not assign client addresses, continuing", exn );
        }

        /* Update the status/generate all of the certificates for clients */
        this.certificateManager.updateCertificateStatus( newSettings );

        TransactionWork deletePreviousTW = new TransactionWork()
            {
                public boolean doWork( Session s )
                {
                    s.delete( oldSettings );
                    return true;
                }
            };

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork( Session s )
                {
                    s.saveOrUpdate( newSettings );
                    VpnTransformImpl.this.settings = newSettings;
                    return true;
                }
            };

        /* If necessary, delete the old settings */
        if ( oldSettings != null && newSettings.getId() != oldSettings.getId()) {
            getTransformContext().runTransaction( deletePreviousTW );
        }
        
        getTransformContext().runTransaction( tw );

        try {
            reconfigure();

            if ( getRunState() == TransformState.RUNNING ) {
                /* This stops then starts openvpn */
                this.openVpnManager.configure( this.settings );
                this.openVpnManager.restart( this.settings );
                this.handler.configure( this.settings );
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

    private void distributeAllClientFiles( VpnSettings settings ) throws TransformException
    {
        for ( VpnClient client : (List<VpnClient>)settings.getCompleteClientList()) {
            if ( !client.getDistributeClient()) continue;
            distributeClientConfig( client );
        }
    }

    public void distributeClientConfig( final VpnClient client )
        throws TransformException
    {
        /* this client may already have a key, the key may have 
         * already been created. */
        this.certificateManager.createClient( client );

        if ( client.getDistributeUsb()) {
            // ??? Should this be here?
            // Uncommented in case there is an email lying around somewhere, and someone
            // uses it to retrieve the data after the admin distributes it over USB.
            // client.setDistributionKey( null );
        } else {
            /* Generate a random key for email distribution */
            String key = client.getDistributionKey();
            
            /* Only generate a new key if the key has been modified */
            if ( key == null || key.length() == 0 ) {
                if ( client.getIsEdgeGuard()) {
                    /* Use a shorter key for edge guard clients since they have to be 
                     * typed in manually */
                    key = String.format( "%08x", random.nextInt());
                } else {
                    key = String.format( "%08x%08x", random.nextInt(), random.nextInt());
                }
            }
            client.setDistributionKey( key );
        }
        
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork( Session s )
                {
                    s.saveOrUpdate( client );
                    return true;
                }
            };
        
        getTransformContext().runTransaction( tw );

        this.openVpnManager.writeClientConfigurationFiles( settings, client );
        
        if ( client.getDistributeUsb()) distributeClientConfigUsb( client );
        else distributeClientConfigEmail( client, client.getDistributionEmail());
    }

    private void distributeClientConfigUsb( VpnClient client )
        throws TransformException
    {
        /* XXX Nothing to do here, it is copied in writeConfigurationFiles */
    }

    private void distributeClientConfigEmail( VpnClient client, String email )
        throws TransformException
    {
        try {
            String subject = "OpenVPN Client";
            File imageDirectory = new File( MAIL_IMAGE_DIR );
            List<File> extraList = new LinkedList<File>();
            List<String> locationList = new LinkedList<String>();
            
            if ( imageDirectory.exists() && imageDirectory.isDirectory()) {
                for ( File image : imageDirectory.listFiles()) {
                    extraList.add( image );
                    locationList.add( MAIL_IMAGE_DIR_PREFIX + "/" + image.getName());
                }
            }

            MailSender mailSender = MvvmContextFactory.context().mailSender();
            
            /* Read in the contents of the file */
            FileReader fileReader = new FileReader( Constants.PACKAGES_DIR + "/mail-" + 
                                                    client.getInternalName() + ".eml" );
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1024];
            int rs;
            while (( rs = fileReader.read( buf )) > 0 ) sb.append( buf, 0, rs );
            
            try {
                fileReader.close();
            } catch ( IOException e ) {
                logger.warn( "Unable to close file", e );
            }
            
            String recipients[] = { email };
            
            mailSender.sendMessageWithAttachments( recipients, subject, sb.toString(), 
                                                   locationList, extraList );
        } catch ( Exception e ) {
            logger.warn( "Error distributing client key", e );
            throw new TransformException( e );
        }
    }

    /* Get the common name for the key, and clear it if it exists */
    public synchronized String lookupClientDistributionKey( String key, IPaddr clientAddress )
    {
        logger.debug( "Looking up client for key: " + key );

        /* Could use a hash map, but why bother ? */
        for ( final VpnClient client : ((List<VpnClient>)this.settings.getCompleteClientList())) {
            if ( lookupClientDistributionKey( key, clientAddress, client )) return client.getInternalName();
        }

        return null;
    }

    private boolean lookupClientDistributionKey( String key, IPaddr clientAddress, final VpnClient client )
    {
        String clientKey = client.getDistributionKey();

        /* XXX, possible check if it is live ??? */
        
        if ( clientKey != null ) clientKey = clientKey.trim();
        logger.debug( "Checking: " + clientKey );
        
        if ( clientKey == null || clientKey.length() == 0 ) return false;
        if ( !clientKey.equalsIgnoreCase( key )) return false;
            
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
         * the openvpn monitor because the thread is not currently 
         * registered for the event logger. */
        this.openVpnMonitor.
            addClientDistributionEvent( new ClientDistributionEvent( clientAddress, client.getName()));
        
        return true;
    }

    private synchronized void deployWebApp()
    {
        if ( !isWebAppDeployed ) {
            if ( MvvmContextFactory.context().loadWebApp( WEB_APP_PATH, WEB_APP )) {
                logger.debug( "Deployed openvpn web app" );
            }
            else logger.error( "Unable to deploy openvpn web app" );
        }
        isWebAppDeployed = true;
    }

    private synchronized void unDeployWebApp()
    {
        if ( isWebAppDeployed ) {
            if( MvvmContextFactory.context().unloadWebApp(WEB_APP_PATH )) {
                logger.debug( "Unloaded openvpn web app" );
            } else logger.error( "Unable to unload openvpn web app" );
        }
        isWebAppDeployed = false;
    }

    // AbstractTransform methods ----------------------------------------------

    @Override protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // lifecycle --------------------------------------------------------------
    @Override protected void preInit( final String[] args ) throws TransformException
    {
        super.preInit( args );
        
        /* Initially use tun0, even though it could eventually be configured to the tap interface  */
        try {
            MvvmContextFactory.context().argonManager().registerIntf( IntfConstants.VPN_INTF, "tun0" );
        } catch ( ArgonException e ) {
            throw new TransformException( "Unable to register VPN interface", e );
        }
    }
    
    @Override protected void postInit(final String[] args) throws TransformException
    {
        super.postInit( args );

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

        deployWebApp();

        reconfigure();
    }

    @Override protected void preStart() throws TransformStartException
    {
        super.preStart();

        if ( this.settings == null ) {
            String[] args = {""};
            try {
                postInit( args );
            } catch ( TransformException e ) {
                throw new TransformStartException( "post init", e );
            }

            if ( this.settings == null ) initializeSettings();
        }

        /* Don't start if openvpn cannot be configured */
        if ( !settings.isConfigured()) throw new UnconfiguredException( "Openvpn is not configured" );

        try {
            settings.validate();
                        
            this.openVpnManager.configure( settings );
            this.handler.configure( settings );
            this.openVpnManager.restart( settings );
        } catch( Exception e ) {
            try {
                this.openVpnManager.stop();
            } catch ( Exception stopException ) {
                logger.error( "Unable to stop the openvpn process", stopException );
            }
            throw new UnconfiguredException( e );
        }

        reconfigure();

        deployWebApp();

        openVpnMonitor.start();
    }

    @Override protected void postStop() throws TransformStopException
    {
        super.postStop();
    }

    @Override protected void preStop() throws TransformStopException {
        super.preStop();

        try {
            openVpnMonitor.stop();
        } catch ( Exception e ) {
            logger.warn( "Error stopping openvpn monitor", e );
        }
        
        try {
            this.openVpnManager.stop();
        } catch ( TransformException e ) {
            logger.warn( "Error stopping openvpn manager", e );
        }
    }

    @Override protected void postDestroy() throws TransformException
    {
        super.postDestroy();

        unDeployWebApp();
    }

    @Override protected void uninstall()
    {
        super.uninstall();

        unDeployWebApp();

        try {
            MvvmContextFactory.context().argonManager().deregisterIntf( IntfConstants.VPN_INTF );
        } catch ( Exception e ) {
            /* There is nothing else to do but print out the message */
            logger.error( "Unable to deregister vpn interface", e );
        }

        try {
            ScriptRunner.getInstance().exec( CLEANUP_SCRIPT );
        } catch ( Exception e ) {
            logger.error( "Unable to cleanup data.", e );
        }
    }

    @Override public TransformStats getStats()
    {
        /* Track the session info separately */
        TransformStats stats = super.getStats();
        return this.openVpnMonitor.updateStats( stats );
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

    public ConfigState getConfigState()
    {
        if ( settings == null || !settings.isConfigured()) return ConfigState.UNCONFIGURED;

        if ( settings.getIsEdgeGuardClient()) return ConfigState.CLIENT;
        
        return ( settings.isBridgeMode() ? ConfigState.SERVER_BRIDGE : ConfigState.SERVER_ROUTE );
    }

    public void startConfig( ConfigState state ) throws ValidateException
    {
        if ( state == ConfigState.UNCONFIGURED || state == ConfigState.SERVER_BRIDGE ) {
            throw new ValidateException( "Cannot run wizard for the selected state: " + state );
        }

        this.sandbox = new Sandbox( state );
    }

    public void completeConfig() throws Exception
    {
        VpnSettings newSettings = this.sandbox.completeConfig( this.getTid());

        /* Generate new settings */
        if ( settings.getIsEdgeGuardClient()) {
            /* Finish the configuration for clients, nothing left to do,
             * it is all done at download time */
        } else {
            /* Try to cleanup the previous data */
            ScriptRunner.getInstance().exec( CLEANUP_SCRIPT );
            
            /* Create the base certificate and parameters */
            this.certificateManager.createBase( newSettings );
            
            /* Create clients certificates */
            this.certificateManager.createAllClientCertificates( newSettings );
            
            /* Distribute emails */
            distributeAllClientFiles( newSettings );
        }
        
        setVpnSettings( newSettings );
        
        /* No reusing the sanbox */
        this.sandbox = null;
    }

    //// the stages of the setup wizard ///
    public List<String> getAvailableUsbList() throws TransformException
    {
        return this.sandbox.getAvailableUsbList();
    }
    
    public void downloadConfig( IPaddr address, int port, String key ) throws Exception
    {
        this.sandbox.downloadConfig( address, port, key );
    }

    public void downloadConfigUsb( String name ) throws Exception
    {
        this.sandbox.downloadConfigUsb( name );
    }
    
    public void generateCertificate( CertificateParameters parameters ) throws Exception
    {
        this.sandbox.generateCertificate( parameters );
    }

    public GroupList getAddressGroups() throws Exception
    {
        return this.sandbox.getGroupList();
    }
    
    public void setAddressGroups( GroupList parameters ) throws Exception
    {
        this.sandbox.setGroupList( parameters );
    }
    
    public void setExportedAddressList( ExportList parameters ) throws Exception
    {
        this.sandbox.setExportList( parameters );

    }
    
    public void setClients( ClientList parameters) throws Exception
    {
        this.sandbox.setClientList( parameters );
    }
    
    public void setSites( SiteList parameters) throws Exception
    {
        this.sandbox.setSiteList( parameters );
    }
}
