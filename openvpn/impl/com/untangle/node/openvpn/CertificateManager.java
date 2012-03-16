/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
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

import com.untangle.uvm.node.script.ScriptRunner;
import com.untangle.uvm.node.script.ScriptWriter;

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

    private static final String HEADER[]          = new String[] {
        "# VPN Base parameter generator configuration file\n"
    };

    private static final String DEFAULTS[]        = new String[] {
        SERVER_NAME_FLAG + "=" + "server.${DOMAIN}",
        CA_NAME_FLAG     + "=" + "ca.${DOMAIN}",
    };

    private static final String GENERATE_BASE_SCRIPT   = Constants.SCRIPT_DIR + "/generate-base";
    private static final String GENERATE_CLIENT_SCRIPT = Constants.SCRIPT_DIR + "/generate-client";
    private static final String REVOKE_CLIENT_SCRIPT   = Constants.SCRIPT_DIR + "/revoke-client";
    
    /* Name of the file that stores the configuration data */
    /* The first item is V if the client common name is valid, and R if it has been revoked */
    private static final String OPENSSL_VALID_FLAG = "V";

    private static final String VPN_CLIENT_STATUS_FILE = Constants.MISC_DIR + "/client_status.txt";
    private static final String CONFIG_FILE            = Constants.MISC_DIR + "/base_cfg";

    private final Logger logger = Logger.getLogger( this.getClass());

    private final Random random = new Random();

    CertificateManager()
    {
    }

    void createBase( VpnSettings settings ) throws Exception
    {
        ScriptWriter sw = new ScriptWriter( HEADER );

        setDefaults( settings );

        /* Need to use reasonable defaults */
        sw.appendVariable( DOMAIN_FLAG,   settings.getDomain());

        sw.appendComment( "Certificate configuration parameters" );
        sw.appendGlobalEscapedVariable( COUNTRY_FLAG,  settings.getCountry());
        sw.appendGlobalEscapedVariable( PROVINCE_FLAG, settings.getProvince());
        sw.appendGlobalEscapedVariable( LOCALITY_FLAG, settings.getLocality());
        sw.appendGlobalEscapedVariable( ORGANIZATION_FLAG, settings.getOrganization());
        sw.appendGlobalEscapedVariable( ORG_UNIT_FLAG, settings.getOrganizationUnit());
        sw.appendGlobalEscapedVariable( EMAIL_FLAG, settings.getEmail());

        sw.appendComment( "Key size" );
        sw.appendVariable( KEY_SIZE_FLAG, String.valueOf( settings.getKeySize()), true );

        sw.appendLines( DEFAULTS );

        try {
            /* Just in case it doesn't already exist */
            File directory = new File( Constants.CONF_DIR );
            directory.mkdir();
            directory = new File( Constants.MISC_DIR );
            directory.mkdir();
        } catch ( Exception e ) {
            throw new Exception( "Unable to create misc directory", e );
        }

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

        for ( VpnClient client : settings.getCompleteClientList()) {
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
                    callRevokeClientScript( name );
                }
            } catch ( Exception e ) {
                logger.error( "Unable to revoke the certificate for [" + name + "]", e );
            }
        }
    }

    private void updateClientCertificateStatus( VpnSettings settings, VpnClient client, 
                                                Map<String,Boolean> certificateStatusMap,
                                                Set<String> usedNameSet )
    {
        String name = client.getInternalName();
        Boolean status = certificateStatusMap.remove( name );
        
        /* Cert doesn't exist for this client, create a new one */
        if ( status == null ) {
            if ( usedNameSet.contains( name )) {
                logger.error( "Client [" + name + " ] common name is listed twice" );
                return;
            }
            
            try {
                callCreateClientScript( name );
                
                logger.info( "Creating a certificate for [" + name + "]" );
                /* Indicate that the client has a valid certificate */
                client.setCertificateStatusValid();
            } catch ( Exception e ) {
                logger.error( "Unable to create a certificate for '" + name + "'", e );
                client.setCertificateStatusRevoked();
            }
        } else {
            if ( status ) client.setCertificateStatusValid();
            else          client.setCertificateStatusRevoked();                
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

    void createAllClientCertificates( VpnSettings settings ) throws Exception
    {
        for ( VpnClient client : settings.getCompleteClientList()) createClient( client );
    }

    void createClient( VpnClient client ) throws Exception
    {
        callCreateClientScript( client.getInternalName());
    }

    void revokeClient( VpnClient client ) throws Exception
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
    private void callCreateClientScript( String commonName ) throws Exception
    {
        /* Always set the recreate flag */
        ScriptRunner.getInstance().exec( GENERATE_CLIENT_SCRIPT, commonName, "recreate" );
    }

    private void callRevokeClientScript( String commonName ) throws Exception
    {
        ScriptRunner.getInstance().exec( REVOKE_CLIENT_SCRIPT, commonName );
    }

    private void callScript( String scriptName ) throws Exception
    {
        ScriptRunner.getInstance().exec( scriptName );
    }
}
