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
package com.untangle.tran.openvpn;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.untangle.mvvm.ArgonException;
import com.untangle.mvvm.IntfConstants;
import com.untangle.mvvm.MailSender;
import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.tapi.AbstractTransform;
import com.untangle.mvvm.tapi.Affinity;
import com.untangle.mvvm.tapi.Fitting;
import com.untangle.mvvm.tapi.PipeSpec;
import com.untangle.mvvm.tapi.SoloPipeSpec;
import com.untangle.mvvm.tran.HostAddress;
import com.untangle.mvvm.tran.HostName;
import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.TransformException;
import com.untangle.mvvm.tran.TransformStartException;
import com.untangle.mvvm.tran.TransformState;
import com.untangle.mvvm.tran.TransformStats;
import com.untangle.mvvm.tran.TransformStopException;
import com.untangle.mvvm.tran.UnconfiguredException;
import com.untangle.mvvm.tran.ValidateException;
import com.untangle.mvvm.tran.script.ScriptRunner;
import com.untangle.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class VpnTransformImpl extends AbstractTransform
    implements VpnTransform
{
    private static final String TRAN_NAME    = "openvpn";
    private static final String WEB_APP      = TRAN_NAME;
    private static final String WEB_APP_PATH = "/" + WEB_APP;
    private static final String MAIL_IMAGE_DIR_PREFIX = "images";
    private static final String MAIL_IMAGE_DIR = Constants.DATA_DIR + "/images";

    private static final String CLEANUP_SCRIPT = Constants.SCRIPT_DIR + "/cleanup";

    private static final HostAddress EMPTY_HOST_ADDRESS = new HostAddress( HostName.getEmptyHostName());

    private static final String SERVICE_NAME = "openvpn";

    private final Logger logger = Logger.getLogger( VpnTransformImpl.class );

    private boolean isWebAppDeployed = false;

    private final SoloPipeSpec pipeSpec;
    private final SoloPipeSpec[] pipeSpecs;

    private final Random random = new Random();

    private final OpenVpnManager openVpnManager = new OpenVpnManager();
    private final CertificateManager certificateManager = new CertificateManager();
    private final AddressMapper addressMapper = new AddressMapper();
    private final OpenVpnMonitor openVpnMonitor;
    private final OpenVpnCaretaker openVpnCaretaker = new OpenVpnCaretaker();
    private final PhoneBookAssistant assistant;

    private final EventHandler handler;

    VpnSettings settings;

    private Sandbox sandbox = null;

    // constructor ------------------------------------------------------------

    public VpnTransformImpl()
    {
        this.handler          = new EventHandler( this );
        this.openVpnMonitor   = new OpenVpnMonitor( this );
        this.assistant        = new PhoneBookAssistant();

        /* Have to figure out pipeline ordering, this should always
         * next to towards the outside, then there is OpenVpn and then Nat */
        this.pipeSpec = new SoloPipeSpec
            ( TRAN_NAME, this, handler, Fitting.OCTET_STREAM, Affinity.OUTSIDE,
              SoloPipeSpec.MAX_STRENGTH - 2);
        this.pipeSpecs = new SoloPipeSpec[] { pipeSpec };
    }

    @Override public void initializeSettings()
    {
        VpnSettings settings = new VpnSettings( this.getTid());
        logger.info( "Initializing Settings... to unconfigured" );

        setVpnSettings( settings );

        /* Stop the open vpn monitor(I don't think this actually does anything, rbs) */
        this.openVpnMonitor.stop();
        this.openVpnCaretaker.stop();
    }

    // VpnTransform methods --------------------------------------------------
    public void setVpnSettings( final VpnSettings newSettings )
    {
        final VpnSettings oldSettings = this.settings;

        /* Attempt to assign all of the clients addresses only if in server mode */
        try {
            if ( !newSettings.isUntanglePlatformClient() && newSettings.isConfigured()) {
                addressMapper.assignAddresses( newSettings );
            }
        } catch ( TransformException exn ) {
            logger.warn( "Could not assign client addresses, continuing", exn );
        }

        if ( !newSettings.isUntanglePlatformClient()) {
            /* Update the status/generate all of the certificates for clients */
            this.certificateManager.updateCertificateStatus( newSettings );
        }

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork( Session s )
                {
                    if (null != newSettings.getId()) {
                        s.update(newSettings);
                    }

                    /* Delete all of the old settings */
                    Query q = null;
                    Long newSettingsId = newSettings.getId();
                    if ( newSettingsId == null ) {
                        q = s.createQuery( "from VpnSettings ts where ts.tid = :tid" );
                    } else {
                        q = s.createQuery( "from VpnSettings ts where ts.tid = :tid and ts.id != :id" );
                        q.setParameter( "id", newSettingsId );
                    }

                    q.setParameter( "tid", getTid());

                    for ( Object o : q.list()) s.delete((VpnSettings)o );

                    /* Save the new settings */
                    s.saveOrUpdate( newSettings );
                    VpnTransformImpl.this.settings = newSettings;
                    return true;
                }
            };

        getTransformContext().runTransaction( tw );

        try {
            if ( getRunState() == TransformState.RUNNING ) {
                /* This stops then starts openvpn */
                this.openVpnManager.configure( this.settings );
                this.openVpnManager.restart( this.settings );
                this.handler.configure( this.settings );

                if ( this.settings.isUntanglePlatformClient()) {
                    this.openVpnMonitor.disable();
                    this.openVpnCaretaker.start();
                } else {
                    this.openVpnMonitor.enable();
                }
            }

            this.assistant.configure( this.settings, getRunState() == TransformState.RUNNING );
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

    public VpnClientBase generateClientCertificate( VpnSettings settings, VpnClientBase client )
    {
        try {
            certificateManager.createClient( client );
        } catch ( TransformException e ) {
            logger.error( "Unable to create a client certificate", e );
        }

        return client;
    }

    public VpnClientBase revokeClientCertificate( VpnSettings settings, VpnClientBase client )
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
        for ( VpnClientBase client : (List<VpnClientBase>)settings.getCompleteClientList()) {
            if ( !client.getDistributeClient()) continue;
            distributeRealClientConfig( client );
        }
    }

    public void distributeClientConfig( VpnClientBase client )
        throws TransformException
    {
        /* Retrieve the client configuration object from the settings */
        boolean foundRealClient = false;
        for ( VpnClientBase realClient : (List<VpnClientBase>)settings.getCompleteClientList()) {
            if ( client.getInternalName().equals( realClient.getInternalName())) {
                realClient.setDistributionEmail( client.getDistributionEmail());
                realClient.setDistributeUsb( client.getDistributeUsb());
                client = realClient;
                foundRealClient = true;
                break;
            }
        }

        if ( foundRealClient ) distributeRealClientConfig( client );
        else throw new TransformException( "Attempt to distribute an unsaved client" );
    }

    /** The client config is the same client configuration object that is in settings */
    private void distributeRealClientConfig( final VpnClientBase client )
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
                if ( client.isUntanglePlatform()) {
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
                    s.merge( client );
                    return true;
                }
            };

        getTransformContext().runTransaction( tw );

        this.openVpnManager.writeClientConfigurationFiles( settings, client );

        if ( client.getDistributeUsb()) distributeClientConfigUsb( client );
        else distributeClientConfigEmail( client, client.getDistributionEmail());
    }

    private void distributeClientConfigUsb( VpnClientBase client )
        throws TransformException
    {
        /* XXX Nothing to do here, it is copied in writeConfigurationFiles */
    }

    private void distributeClientConfigEmail( VpnClientBase client, String email )
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
        if (logger.isDebugEnabled()) {
            logger.debug( "Looking up client for key: " + key );
        }

        /* Could use a hash map, but why bother ? */
        for ( final VpnClientBase client : ((List<VpnClientBase>)this.settings.getCompleteClientList())) {
            if ( lookupClientDistributionKey( key, clientAddress, client )) return client.getInternalName();
        }

        return null;
    }

    private boolean lookupClientDistributionKey( String key, IPaddr clientAddress, final VpnClientBase client )
    {
        String clientKey = client.getDistributionKey();

        /* XXX, possible check if it is live ??? */

        if ( clientKey != null ) clientKey = clientKey.trim();
        if (logger.isDebugEnabled()) {
            logger.debug( "Checking: " + clientKey );
        }

        if ( clientKey == null || clientKey.length() == 0 ) return false;
        if ( !clientKey.equalsIgnoreCase( key )) return false;

        client.setDistributionKey( null );

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork( Session s )
                {
                    s.merge( client );
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
            if ( MvvmContextFactory.context().appServerManager().loadInsecureApp( WEB_APP_PATH, WEB_APP )) {
                logger.debug( "Deployed openvpn web app" );
            }
            else logger.warn( "Unable to deploy openvpn web app" );
        }
        isWebAppDeployed = true;

        /* unregister the service with the MVVM */
        MvvmContextFactory.context().networkManager().registerService( SERVICE_NAME );
    }

    private synchronized void unDeployWebApp()
    {
        if ( isWebAppDeployed ) {
            if( MvvmContextFactory.context().appServerManager().unloadWebApp(WEB_APP_PATH )) {
                logger.debug( "Unloaded openvpn web app" );
            } else logger.warn( "Unable to unload openvpn web app" );
        }
        isWebAppDeployed = false;


        /* unregister the service with the MVVM */
        MvvmContextFactory.context().networkManager().unregisterService( SERVICE_NAME );
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

        try {
            this.openVpnMonitor.start();
        } catch ( Exception e ) {
            logger.warn( "Unable to start openvpn monitor." );
        }

        /* Initially use tun0, even though it could eventually be configured to the tap interface  */
        try {
            MvvmContextFactory.context().localIntfManager().registerIntf( "tun0", IntfConstants.VPN_INTF );
        } catch ( ArgonException e ) {
            throw new TransformException( "Unable to register VPN interface", e );
        }
    }

    @Override protected void postInit(final String[] args) throws TransformException
    {
        super.postInit( args );

        /* register the assistant with the phonebook */
        MvvmContextFactory.context().localPhoneBook().registerAssistant( this.assistant );

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
            this.assistant.configure( settings, true );
        } catch( Exception e ) {
            try {
                this.openVpnManager.stop();
            } catch ( Exception stopException ) {
                logger.error( "Unable to stop the openvpn process", stopException );
            }
            throw new UnconfiguredException( e );
        }

        deployWebApp();

        /* Only start the monitor for servers */
        if ( settings.isUntanglePlatformClient()) {
            this.openVpnMonitor.disable();
            this.openVpnCaretaker.start();
        } else {
            this.openVpnMonitor.enable();
        }
    }

    @Override protected void postStop() throws TransformStopException
    {
        super.postStop();
    }

    @Override protected void preStop() throws TransformStopException {
        super.preStop();

        try {
            this.openVpnMonitor.disable();
            this.openVpnCaretaker.stop();
        } catch ( Exception e ) {
            logger.warn( "Error disabling openvpn monitor", e );
        }

        try {
            this.openVpnManager.stop();
        } catch ( TransformException e ) {
            logger.warn( "Error stopping openvpn manager", e );
        }

        this.assistant.configure( settings, false );

        /* unregister the service with the MVVM */
        MvvmContextFactory.context().networkManager().unregisterService( SERVICE_NAME );
    }

    @Override protected void postDestroy() throws TransformException
    {
        super.postDestroy();

        try {
            this.openVpnMonitor.stop();
            this.openVpnCaretaker.stop();
        } catch ( Exception e ) {
            logger.warn( "Error stopping openvpn monitor", e );
        }

        MvvmContextFactory.context().localPhoneBook().unregisterAssistant( this.assistant );

        unDeployWebApp();
    }

    @Override protected void uninstall()
    {
        super.uninstall();

        unDeployWebApp();

        try {
            MvvmContextFactory.context().localIntfManager().unregisterIntf( IntfConstants.VPN_INTF );
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

        if ( settings.isUntanglePlatformClient()) return ConfigState.CLIENT;

        return ( settings.isBridgeMode() ? ConfigState.SERVER_BRIDGE : ConfigState.SERVER_ROUTE );
    }

    public HostAddress getVpnServerAddress()
    {
        /* Return the empty address */
        if (( this.settings == null ) || ( getConfigState() != ConfigState.CLIENT )) {
            logger.info( "non-client state, and request for server address" );
            return EMPTY_HOST_ADDRESS;
        }

        HostAddress address = this.settings.getServerAddress();

        if ( address == null ) {
            logger.info( "null, host address, returning empty address." );
            return EMPTY_HOST_ADDRESS;
        }

        return address;
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
        if ( newSettings.isUntanglePlatformClient()) {
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

    public void downloadConfig( HostAddress address, int port, String key ) throws Exception
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

    public EventManager<ClientConnectEvent> getClientConnectEventManager()
    {
        return this.openVpnMonitor.getClientConnectLogger();
    }

    public EventManager<VpnStatisticEvent> getVpnStatisticEventManager()
    {
        return this.openVpnMonitor.getVpnStatsDistLogger();
    }

    public EventManager<ClientDistributionEvent> getClientDistributionEventManager()
    {
        return this.openVpnMonitor.getClientDistLogger();
    }

}
