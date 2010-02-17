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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.Type;

import com.untangle.node.util.UvmUtil;
import com.untangle.uvm.node.HostAddress;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.Validatable;
import com.untangle.uvm.node.ValidateException;
import com.untangle.uvm.security.Tid;

/**
 * Settings for the open vpn node.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="n_openvpn_settings", schema="settings")
public class VpnSettings implements Serializable, Validatable
{
    // private static final long serialVersionUID = 1900466626555001143L;

    private static final String INVALID_CHARACTERS_STRING = "[^-a-zA-Z0-9- ]";
    private static final Pattern INVALID_CHARACTERS_PATTERN;

    private static final String MULTISPACE_STRING = " +";
    private static final Pattern MULTISPACE_PATTERN;

    private static final String DEFAULT_SITE_NAME = "untangle-vpn";

    static private final int KEY_SIZE_ENUMERATION[] = new int[]
        {
            1024, 1152, 1280, 1408,
            1536, 1664, 1792, 1920,
            2048
        };

    private static final int KEY_SIZE_DEFAULT    = KEY_SIZE_ENUMERATION[4];
    public static final int DEFAULT_PUBLIC_PORT = 1194;

    private Long id;
    private Tid tid;

    private boolean isBridgeMode = false;
    private boolean isUntanglePlatformClient = false;

    /* The virtual address of the vpn server, or the address of the server to connect to. */
    private HostAddress serverAddress;

    /* List of addresses that should be visible to the VPN */
    private List<ServerSiteNetwork> exportedAddressList;

    private boolean keepAlive;

    private boolean exposeClients;

    private int maxClients = 500;

    private List<VpnGroup> groupList;
    private List<VpnClient> clientList;
    private List<VpnSite> siteList;

    private boolean isDnsOverrideEnabled = false;
    private IPaddr dns1;
    private IPaddr dns2;

    /* This is the port to put into config files */
    private int publicPort = DEFAULT_PUBLIC_PORT;

    /* Certificate information */
    private String  domain = "";
    private int     keySize = KEY_SIZE_DEFAULT;
    private String  country = "";
    private String  locality = "";
    private String  province = "";
    private String  organization = "";
    private String  organizationUnit = "";
    private String  email;
    private boolean caKeyOnUsb;

    /* This is the name of the site for distinguishing the VPN client on user machines */
    private String siteName = "";

    public VpnSettings() { }

    public VpnSettings( Tid tid )
    {
        this.tid = tid;
    }

    public void validate() throws ValidateException
    {
        /* XXXXXXXXXXX */

        /* That is the only setting required for edgeguard client */
        if ( isUntanglePlatformClient ) return;

        if (( groupList == null ) || ( groupList.size() == 0 )) throw new ValidateException( "No groups" );

        GroupList validateGroupList = new GroupList( this.groupList );
        validateGroupList.validate();

        ClientList validateClientList = new ClientList( this.clientList );
        validateClientList.validate();

        SiteList validateSiteList = new SiteList( this.siteList );
        validateSiteList.validate( validateClientList );

        ExportList validateExportList = new ExportList( this.exportedAddressList );
        validateExportList.validate();

        /* If DNS override is enabled, either DNS 1 or DNS 2 must be set */
        if ( this.isDnsOverrideEnabled ) {
            if ( getDnsServerList().isEmpty()) {
                throw new ValidateException( "A DNS server is required when overriding DNS list." );
            }
        }

        /* XXX Check for overlap in all of the settings */
    }

    /* Typically private, but package access so the ID can be reused */

    @Id
    @Column(name="id")
    @GeneratedValue
    Long getId()
    {
        return id;
    }

    void setId( Long id )
    {
        this.id = id;
    }

    /**
     * Node id for these settings.
     *
     * @return tid for these settings
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", nullable=false)
    public Tid getTid()
    {
        return tid;
    }

    public void setTid( Tid tid )
    {
        this.tid = tid;
    }

    /** Network settings for the VPN */


    /**
     * @return whether the vpn is in bridge mode.
     */
    @Column(name="is_bridge", nullable=false)
    public boolean isBridgeMode()
    {
        return this.isBridgeMode;
    }

    public void setBridgeMode( boolean isBridgeMode )
    {
        this.isBridgeMode = isBridgeMode;
    }

    /**
     * @return whether this is an openvpn of another edgeguard client.
     */
    @Column(name="is_edgeguard_client", nullable=false)
    public boolean isUntanglePlatformClient()
    {
        return this.isUntanglePlatformClient;
    }

    public void setUntanglePlatformClient( boolean isUntanglePlatformClient )
    {
        this.isUntanglePlatformClient = isUntanglePlatformClient;
    }

    /**
     * The list of VPN groups associated with this vpn configuration.
     * ??? This may just be infrastructure for down the line, and the
     * current GUI may only support one address group.
     *
     * @return the list of vpn address groups.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<VpnGroup> getGroupList()
    {
        if ( this.groupList == null ) this.groupList = new LinkedList<VpnGroup>();

        return UvmUtil.eliminateNulls(this.groupList);
    }

    public void setGroupList( List<VpnGroup> groupList )
    {
        this.groupList = groupList;
    }

    /**
     * The list of VPN clients.
     *
     * @return the list of Patterns
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<VpnClient> getClientList()
    {
        if ( this.clientList == null ) this.clientList = new LinkedList<VpnClient>();

        return UvmUtil.eliminateNulls(this.clientList);
    }

    public void setClientList( List<VpnClient> clientList )
    {
        this.clientList = clientList;
    }

    /**
     * The list of VPN clients.
     *
     * @return the list of Patterns
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<VpnSite> getSiteList()
    {
        if ( this.siteList == null ) this.siteList = new LinkedList<VpnSite>();

        return UvmUtil.eliminateNulls(this.siteList);
    }

    public void setSiteList( List<VpnSite> siteList )
    {
        this.siteList = siteList;
    }

    /**
     * True if DNS override is enabled.
     * This determines if the user specified DNS servers should be used as opposed to the
     * default ones from the UVM.
     * @return whether or not to override DNS
     */
    @Column(name="is_dns_override", nullable=false)
    public boolean getIsDnsOverrideEnabled()
    {
        return this.isDnsOverrideEnabled;
    }

    public void setIsDnsOverrideEnabled( boolean newValue )
    {
        this.isDnsOverrideEnabled = newValue;
    }

    @Column(name="dns_1")
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getDns1()
    {
        return this.dns1;
    }

    public void setDns1( IPaddr newValue )
    {
        this.dns1 = newValue;
    }

    @Column(name="dns_2")
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getDns2()
    {
        return this.dns2;
    }

    public void setDns2( IPaddr newValue )
    {
        this.dns2 = newValue;
    }

    @Transient
    public List<IPaddr> getDnsServerList()
    {
        List<IPaddr> dnsServerList = new LinkedList<IPaddr>();

        if (( this.dns1 != null ) && ( !this.dns1.isEmpty())) dnsServerList.add( this.dns1 );
        if (( this.dns2 != null ) && ( !this.dns2.isEmpty())) dnsServerList.add( this.dns2 );

        return dnsServerList;
    }

    /**
     * @return a new list containing all of the clients and the
     * sites. A VpnSite is a subclass of a VpnClient.
     */
    @Transient
    public List<VpnClientBase> getCompleteClientList()
    {
        List<VpnClient> clientList = getClientList();
        List<VpnSite> siteList = getSiteList();
        List<VpnClientBase> completeList = 
            new ArrayList<VpnClientBase>( clientList.size() + siteList.size());
        completeList.addAll( clientList );
        completeList.addAll( siteList );
        return completeList;
    }

    /**
     * Static address for the openvpn server.
     *
     * @return virtual address of the open vpn server.
     */
    @Column(name="server_address")
    @Type(type="com.untangle.uvm.type.HostAddressUserType")
    public HostAddress getServerAddress()
    {
        return this.serverAddress;
    }

    public void setServerAddress( HostAddress serverAddress )
    {
        this.serverAddress = serverAddress;
    }

    /**
     * The list of exported networks for this site.
     *
     * @return the list of exported networks for this site.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<ServerSiteNetwork> getExportedAddressList()
    {
        if ( this.exportedAddressList == null ) this.exportedAddressList = new LinkedList<ServerSiteNetwork>();

        return UvmUtil.eliminateNulls(this.exportedAddressList);
    }
    public void setExportedAddressList( List<ServerSiteNetwork> exportedAddressList )
    {
        this.exportedAddressList = exportedAddressList;
    }

    /**
     * True if clients should be allowed to see other clients
     * @return whether the vpn is in bridge mode.
     */
    @Column(name="expose_clients", nullable=false)
    public boolean getExposeClients()
    {
        return this.exposeClients;
    }

    public void setExposeClients( boolean exposeClients )
    {
        this.exposeClients = exposeClients;
    }

    /**
     * True if clients should keep the connection alive with pings. (may want to hide this from the user)
     * @return keep alive
     */
    @Column(name="keep_alive", nullable=false)
    public boolean getKeepAlive()
    {
        return this.keepAlive;
    }

    public void setKeepAlive( boolean keepAlive )
    {
        this.keepAlive = keepAlive;
    }

    /**
     * @return Maximum number of concurrent clients.(probably not exposed)
     */
    @Column(name="max_clients", nullable=false)
    public int getMaxClients()
    {
        return this.maxClients;
    }

    public void setMaxClients( int maxClients )
    {
        this.maxClients = maxClients;
    }

    /**
     * @return Public port where the user can connect to OpenVPN from,
     *         defaults to 1194.
     */
    @Column(name="public_port", nullable=false)
    public int getPublicPort()
    {
        if ( this.publicPort < 0 || this.publicPort > 0xFFFF ) this.publicPort = DEFAULT_PUBLIC_PORT;
        return this.publicPort;
    }

    public void setPublicPort( int newValue )
    {
        if ( newValue < 0 || newValue > 0xFFFF ) newValue = DEFAULT_PUBLIC_PORT;
        this.publicPort = newValue;
    }

    /* Certificate information */

    /**
     * @return domain.
     */
    public String getDomain()
    {
        return this.domain;
    }

    public void setDomain( String domain )
    {
        this.domain = domain;
    }

    /**
     * @return key size.
     */
    @Column(name="key_size", nullable=false)
    public int getKeySize()
    {
        return this.keySize;
    }

    public void setKeySize( int keySize )
    {
        this.keySize = keySize;
    }

    public static int[] getKeySizeEnumeration()
    {
        return KEY_SIZE_ENUMERATION;
    }

    public static int getKeySizeDefault()
    {
        return KEY_SIZE_DEFAULT;
    }

    /**
     * @return country.
     */
    public String getCountry()
    {
        return this.country;
    }

    public void setCountry( String country )
    {
        this.country = country;
    }

    /**
     * @return province.
     */
    public String getProvince()
    {
        return this.province;
    }

    public void setProvince( String province )
    {
        this.province = province;
    }

    /**
     * @return locality(city).
     */
    public String getLocality()
    {
        return this.locality;
    }

    public void setLocality( String locality )
    {
        this.locality = locality;
    }

    /**
     * @return organization.
     */
    @Column(name="org")
    public String getOrganization()
    {
        return this.organization;
    }

    public void setOrganization( String organization )
    {
        this.organization = organization;
    }

    /**
     * @return organizationUnit.
     */
    @Column(name="org_unit")
    public String getOrganizationUnit()
    {
        return organizationUnit;
    }

    public void setOrganizationUnit( String organizationUnit )
    {
        this.organizationUnit = organizationUnit;
    }

    /**
     * @return true if the settings have been configured
     */
    @Transient
    boolean isConfigured()
    {
        if ( isUntanglePlatformClient ) return true;
        return ( !( this.organizationUnit == null ) && ( this.organizationUnit.length() > 0 ));
    }

    /**
     * @return email.
     */
    public String getEmail()
    {
        return this.email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    /**
     * @return true if the CA private key is on a USB key.
     */
    @Column(name="is_ca_on_usb", nullable=false)
    public boolean getCaKeyOnUsb()
    {
        return this.caKeyOnUsb;
    }

    public void setCaKeyOnUsb( boolean caKeyOnUsb )
    {
        this.caKeyOnUsb = caKeyOnUsb;
    }

    @Column(name="site_name")
    public String getSiteName()
    {
        if ( this.siteName == null ) this.siteName = DEFAULT_SITE_NAME;

        this.siteName = this.siteName.trim();

        if ( this.siteName.length() == 0 ) this.siteName = DEFAULT_SITE_NAME;

        return this.siteName;
    }

    public void setSiteName( String newValue )
    {
        if ( newValue == null ) newValue = DEFAULT_SITE_NAME;
        newValue = newValue.trim();
        if ( newValue.length() == 0 ) newValue = DEFAULT_SITE_NAME;
        this.siteName = newValue;
    }

    /**
     * Name of this VPN site.  This is the value that identifies this
     * office in the config file */
    @Transient
    public String getInternalSiteName()
    {
        String site = getSiteName();

        /* try using the organization if the site name is not intialized */
        if ( DEFAULT_SITE_NAME.equals( site )) site = this.organization;

        /* fall back to the site name if necessary */
        if ( site == null ) site = DEFAULT_SITE_NAME;
        site = site.trim();
        if ( site.length() == 0 ) site = DEFAULT_SITE_NAME;

        /* can't substitute the bad characters */
        if (( null == INVALID_CHARACTERS_PATTERN ) || ( null == MULTISPACE_PATTERN )) {
            return DEFAULT_SITE_NAME;
        }

        site = site.toLowerCase();

        /* get rid of the invalid characters */
        site = INVALID_CHARACTERS_PATTERN.matcher( site ).replaceAll( "" );

        /* Trim off whitespace again */
        site = site.trim();

        /* replace all of the spaces with dashes */
        site = MULTISPACE_PATTERN.matcher( site ).replaceAll( "-" );

        return site;
    }

    static
    {
        Pattern pattern = null;

        try {
            pattern = Pattern.compile( INVALID_CHARACTERS_STRING );
        } catch ( PatternSyntaxException e ) {
            System.err.println( "Unable to compile pattern, using null" );
            pattern = null;
        }

        INVALID_CHARACTERS_PATTERN = pattern;

        try {
            pattern = Pattern.compile( MULTISPACE_STRING );
        } catch ( PatternSyntaxException e ) {
            System.err.println( "Unable to compile pattern, using null" );
            pattern = null;
        }

        MULTISPACE_PATTERN = pattern;

    }
}
