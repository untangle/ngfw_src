/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;

public class CertificateManager
{
    private static final String DOMAIN_FLAG       = "DOMAIN";
    private static final String KEY_SIZE_FLAG     = "KEY_SIZE";
    private static final String COUNTRY_FLAG      = "KEY_COUNTRY";
    private static final String PROVINCE_FLAG     = "KEY_PROVINCE";
    private static final String LOCALITY_FLAG     = "KEY_CITY";
    private static final String ORGANIZATION_FLAG = "KEY_ORG";
    private static final String ORG_UNIT_FLAG     = "KEY_ORG_UNIT";
    private static final String EMAIL_FLAG        = "KEY_EMAIL";
    private static final String SERVER_NAME_FLAG  = "SERVER_NAME";
    private static final String CA_NAME_FLAG      = "CA_NAME";

    private static final String HEADER[]          = new String[] {
        "# VPN Base parameter generator configuration file\n"
    };

    private static final String GENERATE_BASE_SCRIPT   = Constants.SCRIPT_DIR + "/generate-base";
    private static final String GENERATE_CLIENT_SCRIPT = Constants.SCRIPT_DIR + "/generate-client";
    private static final String REVOKE_CLIENT_SCRIPT   = Constants.SCRIPT_DIR + "/revoke-client";
    
    /* Name of the file that stores the configuration data */
    /* The first item is V if the client common name is valid, and R if it has been revoked */
    private static final String OPENSSL_VALID_FLAG = "V";

    private static final String VPN_CLIENT_STATUS_FILE = Constants.MISC_DIR + "/client_status.txt";

    private final Logger logger = Logger.getLogger( this.getClass());

    private final Random random = new Random();

    public CertificateManager()
    {
    }

    public void createBase() throws Exception
    {
        UvmContextFactory.context().execManager().exec( GENERATE_BASE_SCRIPT );
    }

    /* Update the status of the certificate, (granted or revoked), and automatically
     * create a cert if a client doesn't have one */
    public void updateCertificateStatus( VpnSettings settings )
    {
        /* Read out the index file */
        Map<String,Boolean> certificateStatusMap = generateCertificateStatusMap();
        Set<String> usedNameSet = new HashSet<String>();

        for ( VpnClient client : settings.trans_getCompleteClientList()) {
            updateClientCertificateStatus( settings, client, certificateStatusMap, usedNameSet );
        }
        
        /* Revoke all of the clients that have been deleted */
        for ( Map.Entry<String,Boolean> entry  : certificateStatusMap.entrySet()) {
            String name    = entry.getKey();
            Boolean status = entry.getValue();
            
            try {
                if ( status ) {
                    /* If necessary revoke the certificate for this user */
                    logger.info( "Revoking the certificate for [" + name + "]" );
                    UvmContextFactory.context().execManager().exec( REVOKE_CLIENT_SCRIPT + " \""  + name + "\"");
                }
            } catch ( Exception e ) {
                logger.error( "Unable to revoke the certificate for [" + name + "]", e );
            }
        }
    }

    public void createAllClientCertificates( VpnSettings settings ) throws Exception
    {
        for ( VpnClient client : settings.trans_getCompleteClientList()) createClient( client );
    }

    public void createClient( VpnClient client ) throws Exception
    {
        UvmContextFactory.context().execManager().exec( GENERATE_CLIENT_SCRIPT + " \""  + client.trans_getInternalName() + "\" recreate");
    }

    public void revokeClient( VpnClient client ) throws Exception
    {
        UvmContextFactory.context().execManager().exec( REVOKE_CLIENT_SCRIPT + " \""  + client.trans_getInternalName() + "\"");
    }

    private void updateClientCertificateStatus( VpnSettings settings, VpnClient client, 
                                                Map<String,Boolean> certificateStatusMap,
                                                Set<String> usedNameSet )
    {
        String name = client.trans_getInternalName();
        Boolean status = certificateStatusMap.remove( name );
        
        /* Cert doesn't exist for this client, create a new one */
        if ( status == null ) {
            if ( usedNameSet.contains( name )) {
                logger.error( "Client [" + name + " ] common name is listed twice" );
                return;
            }
            
            try {
                UvmContextFactory.context().execManager().exec( GENERATE_CLIENT_SCRIPT + " \""  + name + "\" recreate");
                
                logger.info( "Creating a certificate for [" + name + "]" );
                /* Indicate that the client has a valid certificate */
                client.trans_setCertificateStatusValid();
            } catch ( Exception e ) {
                logger.error( "Unable to create a certificate for '" + name + "'", e );
                client.trans_setCertificateStatusRevoked();
            }
        } else {
            if ( status ) client.trans_setCertificateStatusValid();
            else          client.trans_setCertificateStatusRevoked();                
        }
        
        if ( !usedNameSet.add( name )) logger.warn( "Used name set already contained [" + name + "]" );
    }
        
    
    private Map<String,Boolean> generateCertificateStatusMap()
    {
        BufferedReader in = null;
        Map<String,Boolean> certificateStatusMap = new HashMap<String,Boolean>();

        try {
            in = new BufferedReader(new FileReader( VPN_CLIENT_STATUS_FILE ));
            String line;
            while (( line = in.readLine()) != null ) parseCertificate( line, certificateStatusMap );
        } catch ( FileNotFoundException ex ) {
            logger.warn( "The file: " + VPN_CLIENT_STATUS_FILE  + " does not exist" );
        } catch ( Exception ex ) {
            logger.error( "Error reading file: " + VPN_CLIENT_STATUS_FILE, ex );
        } finally {
            try {
                if ( in != null )  in.close();
            } catch ( Exception ex ) {
                logger.error( "Unable to close file: " + VPN_CLIENT_STATUS_FILE, ex );
            }
        }
        
        return certificateStatusMap;
    }
    
    private void parseCertificate( String line, Map<String,Boolean> map )
    {
        String data[] = line.split( " " );
        
        if ( data.length != 2 ) {
            logger.error( "Invalid line: '" + line + "'" );
            return;
        }
        
        boolean isValid   = ( data[0].equalsIgnoreCase( OPENSSL_VALID_FLAG ));
        String clientName = data[1];

        Boolean status = map.get( clientName );
        if ( status == null || status == false ) {
            map.put( clientName, isValid );
        } else {
            /* This means the status is true */
            if ( isValid == true ) {
                logger.error( "Client " + clientName + " has two valid certificates" );
            }
            /* Otherwise, ignore, because one valid cert means at least one of the client's certs
             * hasn't been revoked */
        }
    }

}
