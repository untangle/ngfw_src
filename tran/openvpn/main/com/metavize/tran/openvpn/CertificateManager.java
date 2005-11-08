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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import java.util.Random;

import org.apache.log4j.Logger;

import com.metavize.mvvm.NetworkingConfiguration;
import com.metavize.mvvm.tran.TransformException;

import com.metavize.mvvm.tran.script.ScriptWriter;
import com.metavize.mvvm.tran.script.ScriptRunner;

import static com.metavize.tran.openvpn.Constants.*;

class CertificateManager
{
    private static final String DOMAIN_FLAG       = "DOMAIN";
    private static final String DEFAULT_DOMAIN    = "does.not.exists";
    private static final String KEY_SIZE_FLAG     = "KEY_SIZE";
    private static final String COUNTRY_FLAG      = "KEY_COUNTRY";
    private static final String DEFAULT_COUNTRY   = "US";

    private static final String PROVINCE_FLAG     = "KEY_PROVINCE";
    private static final String DEFAULT_PROVINCE  = "VPN Land";

    private static final String LOCALITY_FLAG     = "KEY_CITY";
    private static final String DEFAULT_LOCALITY  = "VPN City";
    
    private static final String ORGANIZATION_FLAG = "KEY_ORG";
    private static final String DEFAULT_ORGANIZATION = "VPN Organization";
    private static final String ORG_UNIT_FLAG     = "KEY_ORG_UNIT";
    
    /* Email is unused */
    private static final String EMAIL_FLAG        = "KEY_EMAIL";
    private static final String DEFAULT_EMAIL     = "vpn";

    private static final String SERVER_NAME_FLAG  = "SERVER_NAME";
    private static final String CA_NAME_FLAG      = "CA_NAME";
    
    /* Set to true to store the certificate private key onto the usb key */
    private static final String USB_FLAG          = "USE_USB_KEY";

    private static final String HEADER[]          = new String[] {
        "# VPN Base parameter generator configuration file\n"
    };

    private static final String DEFAULTS[]        = new String[] {
        SERVER_NAME_FLAG + "=" + "server.${DOMAIN}",
        CA_NAME_FLAG     + "=" + "ca.${DOMAIN}",
    };

    private static final String EMPTY_ARRAY[] = new String[0];

    private static final String GENERATE_BASE_SCRIPT   = VPN_SCRIPT_BASE + "/generate-base";
    private static final String GENERATE_CLIENT_SCRIPT = VPN_SCRIPT_BASE + "/generate-client";
    private static final String REVOKE_CLIENT_SCRIPT   = VPN_SCRIPT_BASE + "/revoke-client";
    
    /* Name of the file that stores the configuration data */
    /* The first item is V if the client common name is valid, and R if it has been revoked */
    private static final String OPENSSL_VALID_FLAG = "V";

    private static final String VPN_CLIENT_STATUS_FILE = VPN_CONF_BASE + "/openvpn/client_status.txt";
    private static final String CONFIG_FILE            = VPN_CONF_BASE + "/openvpn_base_cfg";

    private final Logger logger = Logger.getLogger( this.getClass());

    private final Random random = new Random();

    CertificateManager()
    {
    }

    public void createBase( VpnSettings settings ) throws TransformException
    {
        ScriptWriter sw = new ScriptWriter( HEADER );

        setDefaults( settings );

        /* Need to use reasonable defaults */
        sw.appendVariable( DOMAIN_FLAG,   settings.getDomain());

        sw.appendComment( "Certificate configuration parameters" );
        sw.appendVariable( COUNTRY_FLAG,  settings.getCountry(), true );
        sw.appendVariable( PROVINCE_FLAG, settings.getProvince(), true );
        sw.appendVariable( LOCALITY_FLAG, settings.getLocality(), true );
        sw.appendVariable( ORGANIZATION_FLAG, settings.getOrganization(), true );
        sw.appendVariable( ORG_UNIT_FLAG, settings.getOrganizationUnit(), true );
        sw.appendVariable( EMAIL_FLAG, settings.getEmail(), true );

        sw.appendComment( "Key size" );
        sw.appendVariable( KEY_SIZE_FLAG, String.valueOf( settings.getKeySize()), true );

        sw.appendComment( "USB Key indicator" );
        sw.appendVariable( USB_FLAG, String.valueOf( settings.getCaKeyOnUsb()));

        sw.appendLines( DEFAULTS );

        sw.writeFile( CONFIG_FILE );
        
        callScript( GENERATE_BASE_SCRIPT );
    }

    /* Update the status of the certificate, (granted or revoked), and automatically
     * create a cert if a client doesn't have one */
    void updateCertificateStatus( VpnSettings settings )
    {
        /* Read out the index file */
        Map<String,Boolean> certificateStatusMap = generateCertificateStatusMap();
        Set<String> usedNameSet = new HashSet<String>();

        for ( VpnClient client : (List<VpnClient>)settings.getClientList()) {
            String name = client.getInternalName();
            Boolean status = certificateStatusMap.remove( name );

            /* Cert doesn't exist for this client, create a new one */
            if ( status == null ) {
                if ( usedNameSet.contains( name )) {
                    logger.error( "Client [" + name + " ] common name is listed twice" );
                    continue;
                }

                try {
                    callCreateClientScript( name );
                    
                    logger.info( "Creating a certificate for [" + name + "]" );
                    /* Indicate that the client has a valid certificate */
                    client.setCertificateStatusValid();
                } catch ( TransformException e ) {
                    logger.error( "Unable to create a certificate for '" + name + "'", e );
                    client.setCertificateStatusRevoked();
                }
            } else {
                if ( status ) client.setCertificateStatusValid();
                else          client.setCertificateStatusRevoked();                
            }

            if ( !usedNameSet.add( name )) logger.warn( "Used name set already contained [" + name + "]" );
        }
        
        /* Revoke all of the clients that have been deleted */
        for ( Map.Entry<String,Boolean> entry  : certificateStatusMap.entrySet()) {
            String name    = entry.getKey();
            Boolean status = entry.getValue();
            
            try {
                if ( status ) {
                    /* If necessary revoke the certificate for this user */
                    logger.info( "Revoking the certificate for [" + name + "]" );
                    callRevokeClientScript( name );
                }
            } catch ( TransformException e ) {
                logger.error( "Unable to revoke the certificate for [" + name + "]", e );
            }
        }
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

    public void createClient( VpnClient client ) throws TransformException
    {
        callCreateClientScript( client.getInternalName());
    }

    public void revokeClient( VpnClient client ) throws TransformException
    {
        callRevokeClientScript( client.getInternalName());
    }

    private void setDefaults( VpnSettings settings )
    {
        if ( !isSet( settings.getDomain()))       settings.setDomain( DEFAULT_DOMAIN );
        if ( !isSet( settings.getCountry()))      settings.setCountry( DEFAULT_COUNTRY );        
        if ( !isSet( settings.getProvince()))     settings.setProvince( DEFAULT_PROVINCE );
        if ( !isSet( settings.getLocality()))     settings.setLocality( DEFAULT_LOCALITY );
        if ( !isSet( settings.getOrganization())) settings.setOrganization( DEFAULT_ORGANIZATION );

        /* For now this is always just set to a random string */
        settings.setOrganizationUnit( String.format( "%04x%04x", random.nextInt(), random.nextInt()));
        
        /* Presently the email address isn't used */
        if ( !isSet( settings.getEmail())) settings.setEmail( DEFAULT_EMAIL + "@" + settings.getDomain());
    }

    private boolean isSet( String setting )
    {
        if ( setting == null || setting.trim().length() == 0 ) return false;
        return true;
    }

    /* These function have been less useful since the functionality in
     * callScript was moved into the script runner
     */
    private void callCreateClientScript( String commonName ) throws TransformException
    {
        ScriptRunner.getInstance().exec( GENERATE_CLIENT_SCRIPT, new String[] { commonName } );
    }

    private void callRevokeClientScript( String commonName ) throws TransformException
    {
        ScriptRunner.getInstance().exec( REVOKE_CLIENT_SCRIPT, new String[] { commonName } );
    }

    private void callScript( String scriptName ) throws TransformException
    {
        ScriptRunner.getInstance().exec( scriptName );
    }
}
