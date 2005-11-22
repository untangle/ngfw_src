/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.openvpn;

import com.metavize.mvvm.security.Tid;

import com.metavize.mvvm.tran.ValidateException;

/* XXX Probably want to make this an abstract class and make this a little more generic */
class Sandbox
{
    private static final int DEFAULT_MAX_CLIENTS = 20;
    private static final boolean DEFAULT_KEEP_ALIVE  = true;
    private static final boolean DEFAULT_EXPOSE_CLIENTS = true;
        
    private CertificateParameters certificateParameters;
    private GroupList  groupList;
    private ExportList exportList;
    private ClientList clientList;
    private SiteList   siteList;
    private final VpnTransform.ConfigState configState;    

    Sandbox( VpnTransform.ConfigState configState )
    {
        this.configState = configState;
    }

    void generateCertificate( CertificateParameters parameters ) throws Exception
    {
        this.certificateParameters = parameters;
    }

    void setGroupList( GroupList parameters ) throws Exception
    {
        this.groupList = parameters;
    }

    void setExportList( ExportList parameters ) throws Exception
    {
        this.exportList = parameters;
    }

    void setClientList( ClientList parameters ) throws Exception
    {
        this.clientList = parameters;
    }

    void setSiteList( SiteList parameters ) throws Exception
    {
        this.siteList = parameters;
    }

    VpnSettings completeConfig( Tid tid ) throws Exception
    {
        /* Create new settings */
        VpnSettings settings = new VpnSettings( tid );

        switch ( configState ) {
        case SERVER_BRIDGE:
            throw new ValidateException( "Bridge mode is presently unsupported" );
            // settings.setBridgeMode( true );
            // break;
        case SERVER_ROUTE:
            settings.setBridgeMode( false );
            settings.setIsEdgeGuardClient( false );
            break;

        case CLIENT:
            settings.setIsEdgeGuardClient( true );
            settings.setBridgeMode( false ); /* This would come from the other box */

            /* Nothing left to do */
            return settings;
            
        default:
            throw new ValidateException( "Invalid state for sandbox: " + this.configState );
        }

        /* Certificate parameters */
        settings.setOrganization( this.certificateParameters.getOrganization());
        settings.setDomain( this.certificateParameters.getDomain());
        settings.setCountry( this.certificateParameters.getCountry());
        settings.setProvince( this.certificateParameters.getState());
        settings.setLocality( this.certificateParameters.getLocality());
        settings.setCaKeyOnUsb( this.certificateParameters.getStoreCaUsb());

        /* Group list */
        settings.setGroupList( this.groupList.getGroupList());

        /* Client list */
        settings.setClientList( this.clientList.getClientList());
        
        settings.setSiteList( this.siteList.getSiteList());

        settings.setExportedAddressList( this.exportList.getExportList());

        /* Hiding these from the user right now */
        settings.setMaxClients( DEFAULT_MAX_CLIENTS );
        settings.setKeepAlive( DEFAULT_KEEP_ALIVE );
        settings.setExposeClients( DEFAULT_EXPOSE_CLIENTS );
        
        settings.validate();
        
        return settings;
    }
}
