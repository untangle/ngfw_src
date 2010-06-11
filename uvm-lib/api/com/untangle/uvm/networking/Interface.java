/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.networking;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.uvm.node.Rule;

/**
 * A description of an interface.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="u_network_intf", schema="settings")
public class Interface extends Rule
{

    /* The argon interface identifier for this interface */
    private byte argonIntf;
    
    /* The network space this interface belongs to */
    private NetworkSpace networkSpace;

    /* The current media for this interface */
    private EthernetMedia ethernetMedia = EthernetMedia.AUTO_NEGOTIATE;

    /* This is the current status of the ethernet port */
    private String currentMedia = "";

    /* True when the untangle should answer to ping on this interface */
    private boolean isPingable = true;

    /* The current state of the connection */
    private String connectionState = "";

    private final boolean isPhysicalInterface;

    /* This is the system name of the interface, this isn't saved to the database. */
    private String systemName = "";

    public Interface()
    {
        this.isPhysicalInterface = true;
    }

    public Interface( boolean isPhysicalInterface )
    {
        this.isPhysicalInterface = isPhysicalInterface;
    }

    public Interface( byte argonIntf, EthernetMedia ethernetMedia, boolean isPingable, 
                      boolean isPhysicalInterface )
    {
        this.argonIntf     = argonIntf;
        this.ethernetMedia = ethernetMedia;
        this.isPingable    = isPingable;
        this.isPhysicalInterface = isPhysicalInterface;
    }

    /**
     * Retrieve the argon interface id for this interface.
     *
     * @return The argon interface id for this interface.
     */
    @Column(name="argon_intf", nullable=false)
    public byte getArgonIntf()
    {
        return this.argonIntf;
    }

    /**
     * Set the argon interface id for this interface.
     *
     * @param newValue The new argon interface id for this interface.
     */
    public void setArgonIntf( byte newValue )
    {
        this.argonIntf = newValue;
    }

    /**
     * Retrieve the network space this interface belongs to.
     *
     * @return The network space this interface belongs to.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="network_space")
    public NetworkSpace getNetworkSpace()
    {
        return this.networkSpace;
    }

    /**
     * Set the network space this interface belongs to.
     *
     * @param newValue The network space this interface belongs to.
     */
    public void setNetworkSpace( NetworkSpace newValue )
    {
        this.networkSpace = newValue;
    }

    /**
     * Retrieve the media for this interface.
     * 
     * @return The media for this interface.
     */
    @Column(name="media")
    @Type(type="com.untangle.uvm.networking.EthernetMediaUserType")
    public EthernetMedia getEthernetMedia()
    {
        if ( this.ethernetMedia == null ) this.ethernetMedia = EthernetMedia.AUTO_NEGOTIATE;
        return this.ethernetMedia;
    }

    /**
     * Set the media for this interface.
     * 
     * @param newValue The media for this interface.
     */
    public void setEthernetMedia( EthernetMedia newValue )
    {
        if ( newValue == null ) newValue = EthernetMedia.AUTO_NEGOTIATE;
        this.ethernetMedia = newValue;
    }

    /**
     * Whether or not this interface should respond to ping.  This may
     * be more appropriate as a property of the space.
     *
     * @return True iff this interface responds to ping.
     */
    @Column(name="pingable", nullable=false)
    public boolean getIsPingable()
    {
        return this.isPingable;
    }

    /**
     * Set Whether or not this interface should respond to ping.
     *
     * @param newValue True iff this interface responds to ping.
     */
    public void setIsPingable( boolean newValue )
    {
        this.isPingable = newValue;
    }

    /** The following are not stored in the database ***/
    
    /**
     * A string representation of whether or not the interface is
     * connected.
     *
     * @return Whether or not the interface is connected.
     */
    @Transient
    public String getConnectionState()
    {
        return this.connectionState;
    }

    /**
     * Set the string representing whether or not the interface is
     * connected.
     *
     * @param newValue Whether or not the interface is
     * connected.
     */
    public void setConnectionState( String newValue )
    {
        this.connectionState = newValue;
    }

    /**
     * A user string describing the current media of this interface.
     *
     * @return The current interface media.
     */
    @Transient
    public String getCurrentMedia()
    {
        return this.currentMedia;
    }

    /**
     * Set the user representation the current media of this
     * interface.
     *
     * @param newValue The current interface media.
     */
    public void setCurrentMedia( String newValue )
    {
        this.currentMedia = newValue;
    }

    /**
     * Get whether or not this is a physical interface
     */
    @Transient
    public boolean isPhysicalInterface()
    {
        return this.isPhysicalInterface;
    }

    /**
     * Get the system name (eth0, etc)
     */
    @Transient
    public String getSystemName()
    {
        return this.systemName;
    }

    public void setSystemName( String newValue )
    {
        this.systemName = newValue;
    }
}
