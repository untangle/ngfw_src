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

import java.util.Random;

import com.metavize.mvvm.NetworkingConfiguration;
import com.metavize.mvvm.tran.TransformException;

import com.metavize.mvvm.tran.ScriptWriter;

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

    private static final String GENERATE_BASE_SCRIPT   = VPN_SCRIPT_BASE + "/generate-base";
    private static final String GENERATE_CLIENT_SCRIPT = VPN_SCRIPT_BASE + "/generate-client";
    private static final String REVOKE_CLIENT_SCRIPT   = VPN_SCRIPT_BASE + "/revoke-client";
    
    /* Name of the file that stores the configuration data */
    private static final String CONFIG_FILE       = VPN_CONF_BASE + "/openvpn_base_cfg";

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
        sw.appendVariable( USB_FLAG, String.valueOf( settings.isCaKeyOnUsb()));

        sw.appendLines( DEFAULTS );

        sw.writeFile( CONFIG_FILE );
        
        callScript( GENERATE_BASE_SCRIPT );
    }

    private void setDefaults( VpnSettings settings )
    {
        if ( !isSet( settings.getDomain()))       settings.setDomain( DEFAULT_DOMAIN );
        if ( !isSet( settings.getCountry()))      settings.setCountry( DEFAULT_COUNTRY );        
        if ( !isSet( settings.getProvince()))     settings.setProvince( DEFAULT_PROVINCE );
        if ( !isSet( settings.getLocality()))     settings.setLocality( DEFAULT_LOCALITY );
        if ( !isSet( settings.getOrganization())) settings.setOrganization( DEFAULT_ORGANIZATION );

        /* For now this is alwayts just set to a random string */
        settings.setOrganizationUnit( String.format( "%04x%04x", random.nextInt(), random.nextInt()));
        
        if ( !isSet( settings.getEmail())) settings.setEmail( DEFAULT_EMAIL + "@" + settings.getDomain());
    }

    private boolean isSet( String setting )
    {
        if ( setting == null || setting.trim().length() == 0 ) return false;
        return true;
    }

    private void callClientScript( String commonName ) throws TransformException
    {
        callScript( GENERATE_CLIENT_SCRIPT + " '" + commonName + "'" );
    }

    private void callRevokeClientScript( String commonName ) throws TransformException
    {
        callScript( REVOKE_CLIENT_SCRIPT + " '" + commonName + "'" );
    }

    private void callScript( String scriptName ) throws TransformException
    {
        /* Run the script to generate the base parameters */
        /* Call the rule generator */
        try {
            int code = 0;
            Process p = Runtime.getRuntime().exec( "sh " + scriptName );
            code = p.waitFor();
            
            if ( code != 0 ) throw new TransformException( "Error generating base parameters: " + code );
        } catch ( TransformException e ) {
            throw e;
        } catch( Exception e ) {
            throw new TransformException( "Error generating base parameters", e );
        }
    }

    
}
