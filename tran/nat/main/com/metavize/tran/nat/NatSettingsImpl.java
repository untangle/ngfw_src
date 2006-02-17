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
import com.metavize.mvvm.networking.DhcpLeaseRule;
import com.metavize.mvvm.networking.NetworkUtil;
import com.metavize.mvvm.networking.RedirectRule;
import com.metavize.mvvm.networking.SetupState;

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
 * table="tr_nat_settings"
 */
public class NatSettingsImpl implements NatSettings
{
    // !!!! private static final long serialVersionUID = 4349679825783697834L;

    private Long id;
    private Tid tid;
    
    private SetupState setupState = SetupState.BASIC;

    /* Nat Settings */
    private boolean natEnabled = false;
    private IPaddr  natInternalAddress;
    private IPaddr  natInternalSubnet;

    /* DMZ settings */
    private boolean dmzEnabled;
    private IPaddr  dmzAddress;
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

    /**
     * Hibernate constructor.
     */
    private NatSettingsImpl()
    {
    }

    /**
     * Real constructor
     */
    public NatSettingsImpl( Tid tid )
    {
        this.tid = tid;
    }

    public NatSettingsImpl( Tid tid, SetupState setupState )
    {
        this.tid = tid;
        this.setupState = setupState;
    }


    public void validate() throws ValidateException
    {
        validate( null );
    }

    /* Validation method */
    public void validate( NetworkingConfiguration netConfig ) throws ValidateException
    {
        boolean isStartAddressValid = true;
        boolean isEndAddressValid   = true;
        boolean isValid             = true;

        for ( Iterator iter = this.redirectList.iterator(); iter.hasNext() ; ) {
            ((RedirectRule)iter.next()).fixPing();
        }

        if ( natEnabled &&
             ( natInternalAddress == null || natInternalSubnet == null  ||
               natInternalAddress.isEmpty() || natInternalSubnet.isEmpty())) {
            throw new ValidateException( "Enablng NAT requires an \"Internal IP address\" and " +
                                         "an \"Internal Subnet\"" );
        }

        if ( dmzEnabled ) {
            if ( dmzAddress == null ) {
                throw new ValidateException( "Enabling DMZ requires a target IP address" );
            }

            if ( natEnabled && !dmzAddress.isInNetwork( natInternalAddress, natInternalSubnet )) {
                throw new ValidateException( "When NAT is enabled, the \"DMZ address\" in the DMZ Host " +
                                             "panel must be in the internal network." );
            }
        }

        if ( dhcpEnabled ) {
            IPaddr host = null;
            IPaddr netmask = null;

            if ( natEnabled ) {
                host    = natInternalAddress;
                netmask = natInternalSubnet;
            } else {
                /* Need the network settings */
                /* XXX This inefficient since it has to call to the server */
                /* XXX Currently a bug, getting around by ignoring */
                if ( netConfig == null ) {
                    //netConfig = MvvmContextFactory.context().networkingManager().get();
                }

                if ( netConfig != null ) {
                    host    = netConfig.host();
                    netmask = netConfig.netmask();
                }
            }

            if ( host != null && !dhcpStartAddress.isInNetwork( host, netmask )) {
                isStartAddressValid = false;

                throw new ValidateException( "\"IP Address Range Start\" in the DHCP panel must " + 
                                             "be in the network: "
                                             + host.toString() + "/" + netmask.toString());

            }

            if ( host != null && !dhcpEndAddress.isInNetwork( host, netmask )) {
                isEndAddressValid = false;

                throw new ValidateException( "\"IP Address Range End\" in the DHCP panel must "+
                                             "be in the network: "
                                             + host.toString() + "/" + netmask.toString());
            }
            
            if ( host != null && netmask != null && !host.isEmpty() && !netmask.isEmpty()) {
                int c = 1;
                for ( Iterator iter = this.dhcpLeaseList.iterator() ; iter.hasNext() ; c++ ) {
                    DhcpLeaseRule rule =  (DhcpLeaseRule)iter.next();
                    IPaddr address = rule.getStaticAddress();
                    if ( address.getAddr() == null ) continue;
                    
                    if ( !address.isInNetwork( host, netmask )) {
                        throw new 
                            ValidateException( "\"target static IP address\" for DHCP Address Map entry '" +
                                               address.toString() + "' must be in the network: " + 
                                               host.toString() + "/" + netmask.toString());
                    }
                }
            }
        }

        /* Setup this way to allow reporting of multiple errors in one place */
        isValid = isStartAddressValid & isEndAddressValid;
    }

    /**
     * @hibernate.id
     * column="settings_id"
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
     * column="tid"
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
     * The current setup state for this tranform.  (deprecated, unconfigured, basic, advanced).
     * @return The current setup state for this transform.
     * @hibernate.property
     * type="com.metavize.mvvm.networking.SetupStateUserType"
     * @hibernate.column
     * name="setup_state"
     */
    public SetupState getSetupState()
    {
        return this.setupState;
    }

    void setSetupState( SetupState newValue )
    {
        this.setupState = newValue;
    }

    /**
     * Get whether or not nat is enabled.
     *
     * @return is NAT is being used.
     * @hibernate.property
     * column="nat_enabled"
     */
    public boolean getNatEnabled()
    {
    return natEnabled;
    }

    public void setNatEnabled( boolean enabled )
    {
    natEnabled = enabled;
    }

    /**
     * Get the base of the internal address.
     *
     * @return internal Address.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="nat_internal_addr"
     * sql-type="inet"
     */
    public IPaddr getNatInternalAddress()
    {
        if ( this.natInternalAddress == null ) this.natInternalAddress = NetworkUtil.EMPTY_IPADDR;
        return natInternalAddress;
    }

    public void setNatInternalAddress( IPaddr addr )
    {
        if ( addr == null ) addr = NetworkUtil.EMPTY_IPADDR;
        natInternalAddress = addr;
    }

    /**
     * Get the subnet of the internal addresses.
     *
     * @return internal subnet.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="nat_internal_subnet"
     * sql-type="inet"
     */
    public IPaddr getNatInternalSubnet()
    {
        if ( this.natInternalSubnet == null ) this.natInternalSubnet = NetworkUtil.EMPTY_IPADDR;
        return natInternalSubnet;
    }

    public void setNatInternalSubnet( IPaddr addr )
    {
        if ( addr == null ) addr = NetworkUtil.EMPTY_IPADDR;
        this.natInternalSubnet = addr;
    }

    /**
     * Get whether or not DMZ is being used.
     *
     * @return is NAT is being used.
     * @hibernate.property
     * column="dmz_enabled"
     */
    public boolean getDmzEnabled()
    {
        return dmzEnabled;
    }

    public void setDmzEnabled( boolean enabled )
    {
        dmzEnabled = enabled;
    }

    /**
     * Get whether or not DMZ is being used.
     *
     * @return is NAT is being used.
     * @hibernate.property
     * column="dmz_logging_enabled"
     */
    public boolean getDmzLoggingEnabled()
    {
        return this.dmzLoggingEnabled;
    }

    public void setDmzLoggingEnabled( boolean enabled )
    {
        this.dmzLoggingEnabled = enabled;
    }


    /**
     * Get the address of the dmz host
     *
     * @return dmz address.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="dmz_address"
     * sql-type="inet"
     */
    public IPaddr getDmzAddress()
    {
        return dmzAddress;
    }

    public void setDmzAddress( IPaddr dmzAddress )
    {
        this.dmzAddress = dmzAddress;
    }

    /**
     * List of the redirect rules.
     *
     * @return the list of the redirect rules.
     * @hibernate.list
     * cascade="all"
     * table="tr_nat_redirects"
     * @hibernate.collection-key
     * column="setting_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.networking.RedirectRule"
     * column="rule_id"
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
     * column="dhcp_enabled"
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
     * name="dhcp_s_address"
     * sql-type="inet"
     */
    public IPaddr getDhcpStartAddress()
    {
        if ( this.dhcpStartAddress == null ) this.dhcpStartAddress = NetworkUtil.EMPTY_IPADDR;
        return dhcpStartAddress;
    }

    public void setDhcpStartAddress( IPaddr address )
    {
        if ( address == null ) address = NetworkUtil.EMPTY_IPADDR;
        this.dhcpStartAddress = address;
    }

    /**
     * Get the end address of the range of addresses to server.
     *
     * @return DHCP end address.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="dhcp_e_address"
     * sql-type="inet"
     */
    public IPaddr getDhcpEndAddress()
    {
        if ( this.dhcpEndAddress == null ) this.dhcpEndAddress = NetworkUtil.EMPTY_IPADDR;
        return dhcpEndAddress;
    }

    public void setDhcpEndAddress( IPaddr address )
    {
        if ( address == null ) address = NetworkUtil.EMPTY_IPADDR;
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
     * column="dhcp_lease_time"
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
     * table="tr_dhcp_leases"
     * @hibernate.collection-key
     * column="setting_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.networking.DhcpLeaseRule"
     * column="rule_id"
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
     * column="dns_enabled"
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
     * name="dns_local_domain"
     */
    public HostName getDnsLocalDomain()
    {
        if ( this.dnsLocalDomain == null ) this.dnsLocalDomain = HostName.getEmptyHostName();
        return dnsLocalDomain;
    }

    public void setDnsLocalDomain( HostName s )
    {
        if ( s == null ) s = HostName.getEmptyHostName();
        this.dnsLocalDomain = s;
    }


    /**
     * List of the DNS Static Host rules.
     *
     * @return the list of the DNS Static Host rules.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="tr_nat_dns_hosts"
     * @hibernate.collection-key
     * column="setting_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.networking.DnsStaticHostRule"
     * column="rule_id"
     */
    public List getDnsStaticHostList()
    {
        return dnsStaticHostList;
    }

    public void setDnsStaticHostList( List s )
    {
        dnsStaticHostList = s;
    }
}
