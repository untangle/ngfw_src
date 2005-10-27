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
package com.metavize.tran.vpn;

import java.util.Random;

import com.metavize.mvvm.NetworkingConfiguration;
import com.metavize.mvvm.tran.TransformException;

import com.metavize.mvvm.tran.ScriptWriter;

import static com.metavize.tran.vpn.Constants.*;

class BaseGenerator
{
    private static final String DOMAIN_FLAG       = "DOMAIN";
    private static final String DEFAULT_DOMAIN    = "does.not.exists";
    private static final String KEY_SIZE_FLAG     = "KEY_SIZE";
    private static final String COUNTRY_FLAG      = "KEY_COUNTRY";
    private static final String DEFAULT_COUNTRY   = "US";

    private static final String PROVINCE_FLAG     = "KEY_CITY";
    private static final String DEFAULT_PROVINCE  = "VPN Land";
    
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
    
    /* Name of the file that stores the configuration data */
    private static final String CONFIG_FILE       = VPN_CONF_BASE + "/vpn_base_cfg";

    private final Random random = new Random();

    BaseGenerator()
    {
    }

    public void createBase( VpnSettings settings )
    {
        ScriptWriter sw = new ScriptWriter( HEADER );

        setDefaults( settings );

        /* Need to use reasonable defaults */
        sw.appendVariable( DOMAIN_FLAG,   settings.getDomain());

        sw.appendComment( "Certificate configuration parameters" );
        sw.appendVariable( COUNTRY_FLAG,  settings.getCountry(), true );
        sw.appendVariable( PROVINCE_FLAG, settings.getProvince(), true );
        sw.appendVariable( ORGANIZATION_FLAG, settings.getOrganization(), true );
        sw.appendVariable( ORG_UNIT_FLAG, settings.getOrganizationUnit(), true );
        sw.appendVariable( EMAIL_FLAG, settings.getEmail(), true );

        sw.appendComment( "USB Key indicator" );
        sw.appendVariable( USB_FLAG, String.valueOf( settings.isCaKeyOnUsb()));

        sw.appendLines( DEFAULTS );

        sw.writeFile( CONFIG_FILE );
    }

    private void setDefaults( VpnSettings settings )
    {
        if ( !isSet( settings.getDomain()))       settings.setDomain( DEFAULT_DOMAIN );
        if ( !isSet( settings.getCountry()))      settings.setCountry( DEFAULT_COUNTRY );        
        if ( !isSet( settings.getProvince()))     settings.setProvince( DEFAULT_PROVINCE );
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

    
}
