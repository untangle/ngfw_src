/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.nat;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.NetworkingConfiguration;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.HostName;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.Validatable;
import com.metavize.mvvm.tran.ValidateException;

/**
 * Settings for the Nat transform.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_NAT_SETTINGS"
 */
public class NatSettings implements Serializable
{
    private Long id;
    private Tid tid;

    // !!!!!! private static final long serialVersionUID = 4349679825783697834L;

    /* Nat Settings */
    private boolean natEnabled = false;
    private IPaddr  natAddress;
    private IPaddr  natNetmask;

    /* DMZ settings */
    private boolean dmzEnabled;
    private IPaddr  dmzHost;
    private boolean dmzLoggingEnabled = false;

    /* Redirect rules */
    private List    redirectList = new LinkedList();

    /* Is dhcp enabled */
    private boolean dhcpEnabled = false;
    private IPaddr  dhcpStartAddress;
    private IPaddr  dhcpEndAddress;
    private int     dhcpLeaseTime = 0;

    /* Dhcp leasess */
    private List    dhcpLeaseList = new LinkedList();

    /* DNS Masquerading settings */
    private boolean  dnsEnabled = false;
    private HostName dnsLocalDomain = HostName.getEmptyHostName();

    /* DNS Static Hosts */
    private List    dnsStaticHostList = new LinkedList();

    /* Setup state (simple,advanced, possibly unconfigured) */
    private SetupState setupState = SetupState.BASIC;

    /**
     * Hibernate constructor.
     */
    private NatSettings()
    {
    }

    /**
     * Real constructor
     */
    public NatSettings( Tid tid )
    {
        this.tid = tid;
    }

    public void validate() throws ValidateException
    {
    }

//     public void validate() throws Exception
//     {
//         validate( null );
//     }

//     /* Validation method */
//     public void validate( NetworkingConfiguration netConfig ) throws Exception
//     {
//         boolean isStartAddressValid = true;
//         boolean isEndAddressValid   = true;
//         boolean isValid             = true;

//         for ( Iterator iter = this.redirectList.iterator(); iter.hasNext() ; ) {
//             ((RedirectRule)iter.next()).fixPing();
//         }

//         if ( natEnabled &&
//              ( natInternalAddress == null || natInternalSubnet == null  ||
//                natInternalAddress.isEmpty() || natInternalSubnet.isEmpty())) {
//             throw new Exception( "Enablng NAT requires an \"Internal IP address\" and " +
//                                  "an \"Internal Subnet\"" );
//         }

//         if ( dmzEnabled ) {
//             if ( dmzAddress == null ) {
//                 throw new Exception( "Enabling DMZ requires a target IP address" );
//             }

//             if ( natEnabled && !dmzAddress.isInNetwork( natInternalAddress, natInternalSubnet )) {
//                 throw new Exception( "When NAT is enabled, the \"DMZ address\" in the DMZ Host panel " +
//                                      "must be in the internal network." );
//             }
//         }

//         if ( dhcpEnabled ) {
//             IPaddr host = null;
//             IPaddr netmask = null;

//             if ( natEnabled ) {
//                 host    = natInternalAddress;
//                 netmask = natInternalSubnet;
//             } else {
//                 /* Need the network settings */
//                 /* XXX This inefficient since it has to call to the server */
//                 /* XXX Currently a bug, getting around by ignoring */
//                 if ( netConfig == null ) {
//                     //netConfig = MvvmContextFactory.context().networkingManager().get();
//                 }

//                 if ( netConfig != null ) {
//                     host    = netConfig.host();
//                     netmask = netConfig.netmask();
//                 }
//             }

//             if ( host != null && !dhcpStartAddress.isInNetwork( host, netmask )) {
//                 isStartAddressValid = false;

//                 throw new Exception( "\"IP Address Range Start\" in the DHCP panel must be in the network: "
//                                      + host.toString() + "/" + netmask.toString());

//             }

//             if ( host != null && !dhcpEndAddress.isInNetwork( host, netmask )) {
//                 isEndAddressValid = false;

//                 throw new Exception( "\"IP Address Range End\" in the DHCP panel must be in the network: " 
//                                      + host.toString() + "/" + netmask.toString());
//             }
            
//             if ( host != null && netmask != null && !host.isEmpty() && !netmask.isEmpty()) {
//                 int c = 1;
//                 for ( Iterator iter = this.dhcpLeaseList.iterator() ; iter.hasNext() ; c++ ) {
//                     DhcpLeaseRule rule =  (DhcpLeaseRule)iter.next();
//                     IPaddr address = rule.getStaticAddress();
//                     if ( address.getAddr() == null ) continue;
                    
//                     if ( !address.isInNetwork( host, netmask )) {
//                         throw new 
//                             Exception( "\"target static IP address\" for DHCP Address Map entry '" +
//                                        address.toString() + "' must be in the network: " + 
//                                        host.toString() + "/" + netmask.toString());
//                     }
//                 }
//             }
//         }

//         /* Setup this way to allow reporting of multiple errors in one place */
//         isValid = isStartAddressValid & isEndAddressValid;
//     }

    /**
     * @hibernate.id
     * column="SETTINGS_ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId( Long id )
    {
        this.id = id;
    }

    /**
     * Transform id for these settings.
     *
     * @return tid for these settings
     * @hibernate.many-to-one
     * column="TID"
     * not-null="true"
     */
    public Tid getTid()
    {
        return tid;
    }

    public void setTid( Tid tid )
    {
        this.tid = tid;
    }

    /**
     * List of the redirect rules.
     *
     * @return the list of the redirect rules.
     * @hibernate.list
     * cascade="all"
     * table="TR_NAT_REDIRECTS"
     * @hibernate.collection-key
     * column="SETTING_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.tran.nat.RedirectRule"
     * column="RULE_ID"
     */
    public List getRedirectList()
    {
        return redirectList;
    }

    public void setRedirectList( List s )
    {
        redirectList = s;
    }

    /**
     * @return If DHCP is enabled.
     *
     * @hibernate.property
     * column="DHCP_ENABLED"
     */
    public boolean getDhcpEnabled()
    {
        return dhcpEnabled;
    }

    public void setDhcpEnabled( boolean b )
    {
        this.dhcpEnabled = b;
    }

    /**
     * Get the start address of the range of addresses to server.
     *
     * @return DHCP start address.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="DHCP_S_ADDRESS"
     * sql-type="inet"
     */
    public IPaddr getDhcpStartAddress()
    {
        return dhcpStartAddress;
    }

    public void setDhcpStartAddress( IPaddr address )
    {
        this.dhcpStartAddress = address;
    }

    /**
     * Get the end address of the range of addresses to server.
     *
     * @return DHCP end address.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="DHCP_E_ADDRESS"
     * sql-type="inet"
     */
    public IPaddr getDhcpEndAddress()
    {
        return dhcpEndAddress;
    }

    public void setDhcpEndAddress( IPaddr address )
    {
        this.dhcpEndAddress = address;
    }

    /** Set the starting and end address of the dns server */
    public void setDhcpStartAndEndAddress( IPaddr start, IPaddr end )
    {
        if ( start == null ) {
            setDhcpStartAddress( end );
            setDhcpEndAddress( end );
        } else if ( end == null )  {
            setDhcpStartAddress( start );
            setDhcpEndAddress( start );
        } else {
            if ( start.isGreaterThan( end )) {
                setDhcpStartAddress( end );
                setDhcpEndAddress( start );
            } else {
                setDhcpStartAddress( start );
                setDhcpEndAddress( end );
            }
        }
    }

    /**
     * Get the default length of the DHCP lease in seconds.
     *
     * @return the length of the DHCP lease in seconds.
     * @hibernate.property
     * column="DHCP_LEASE_TIME"
     */
    public int getDhcpLeaseTime()
    {
        return this.dhcpLeaseTime;
    }

    public void setDhcpLeaseTime( int time )
    {
        this.dhcpLeaseTime = time;
    }

    /**
     * List of the dhcp leases.
     *
     * @return the list of the dhcp leases.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_DHCP_LEASES"
     * @hibernate.collection-key
     * column="SETTING_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.tran.nat.DhcpLeaseRule"
     * column="RULE_ID"
     */
    public List getDhcpLeaseList()
    {
        return dhcpLeaseList;
    }

    public void setDhcpLeaseList( List s )
    {
        dhcpLeaseList = s;
    }

    /**
     * @return If DNS Masquerading is enabled.
     *
     * @hibernate.property
     * column="DNS_ENABLED"
     */
    public boolean getDnsEnabled()
    {
        return dnsEnabled;
    }

    public void setDnsEnabled( boolean b )
    {
        this.dnsEnabled = b;
    }

    /**
     * Local Domain
     *
     * @return the local domain
     * @hibernate.property
     * type="com.metavize.mvvm.type.HostNameUserType"
     * @hibernate.column
     * name="DNS_LOCAL_DOMAIN"
     */
    public HostName getDnsLocalDomain()
    {
        return dnsLocalDomain;
    }

    public void setDnsLocalDomain( HostName s )
    {
        this.dnsLocalDomain = s;
    }


    /**
     * List of the DNS Static Host rules.
     *
     * @return the list of the DNS Static Host rules.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_NAT_DNS_HOSTS"
     * @hibernate.collection-key
     * column="SETTING_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.tran.nat.DnsStaticHostRule"
     * column="RULE_ID"
     */
    public List getDnsStaticHostList()
    {
        return dnsStaticHostList;
    }

    public void setDnsStaticHostList( List s )
    {
        dnsStaticHostList = s;
    }

    /**
     * The current setup type, this shouldn't be modified outside of this package.
     * @return The media for type for this interface.
     * @hibernate.property
     * type="com.metavize.tran.nat.SetupStateUserType"
     * @hibernate.column
     * name="setup_state"
     */
    public SetupState getSetupState()
    {
        return this.setupState;
    }
    
    void setSetupState( SetupState setupState )
    {
        if ( setupState == null ) setupState = SetupState.BASIC;
        this.setupState = setupState;
    }

    /* ******************** non-hibernate variables *********/
    
    /** These should only be used when in basic mode */

    /* NAT functions */
    boolean getIsNatEnabled()
    {
        return this.natEnabled;
    }
    
    void setIsNatEnabled( boolean newValue )
    {
        this.natEnabled = newValue;
    }

    IPaddr getNatAddress()
    {
        return this.natAddress;
    }

    void setNatAddress( IPaddr newValue )
    {
        this.natAddress = newValue;
    }

    IPaddr getNatNetmask()
    {
        return this.natNetmask;
    }

    void setNatNetmask( IPaddr newValue )
    {
        this.natNetmask = newValue;
    }

    /* DMZ functions */
    boolean getIsDmzHostEnabled()
    {
        return this.dmzEnabled;
    }
    
    void setIsDmzHostEnabled( boolean newValue )
    {
        this.dmzEnabled = newValue;
    }

    IPaddr getDmzHost()
    {
        return this.dmzHost;
    }

    void setDmzHost( IPaddr newValue )
    {
        this.dmzHost = newValue;
    }
    
    boolean getIsDmzLoggingEnabled()
    {
        return this.dmzLoggingEnabled;
    }
    
    void setIsDmzLoggingEnabled( boolean newValue )
    {
        this.dmzLoggingEnabled = newValue;
    }
}
