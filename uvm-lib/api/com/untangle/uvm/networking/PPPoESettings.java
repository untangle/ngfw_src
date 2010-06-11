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

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.xml.bind.ValidationException;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

import com.untangle.uvm.node.Validatable;
import com.untangle.uvm.node.ValidateException;

/**
 * Settings to hold the configuration for all of the PPPoE
 * connections.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="u_pppoe", schema="settings")
@SuppressWarnings("serial")
public class PPPoESettings implements Serializable, Validatable
{
    private Long id;

    /** Set to true in order to enable all of the PPPoE connections that are enabled. */
    private boolean isEnabled = false;

    /** List of all of the available PPPoE connections */
    private List<PPPoEConnectionRule> connectionList;

    public PPPoESettings()
    {
    }

    @Id
    @Column(name="settings_id")
    @GeneratedValue
    protected Long getId()
    {
        return id;
    }

    protected void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Main controller to enable/disable of the PPPoE connections.
     *
     * @return True iff PPPoE connections can be enabled.
     */
    @Column(name="live")
    public boolean getIsEnabled()
    {
        return this.isEnabled;
    }

    /**
     * Set the main controller to enable/disable of the PPPoE
     * connections.
     *
     * @param newValue True iff PPPoE connections can be enabled.
     */
    public void setIsEnabled( boolean newValue )
    {
        this.isEnabled = newValue;
    }

    /**
     * A list of the configurations for all of the PPPoE connections.
     *
     * @return List of configuration.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
    org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<PPPoEConnectionRule> getConnectionList()
    {
        if ( this.connectionList == null )
            this.connectionList = new LinkedList<PPPoEConnectionRule>();
        
        this.connectionList.removeAll(java.util.Collections.singleton(null));
        return this.connectionList;
    }

    /**
     * Set the list of the configurations for all of the PPPoE
     * connections.
     *
     * @param newValue List of configuration.
     */
    public void setConnectionList( List<PPPoEConnectionRule> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList<PPPoEConnectionRule>();

        this.connectionList = newValue;
    }


    /**
     * Validate these PPPoE setting are free of errors.
     *
     * @exception ValidationException Occurs if there is an error in
     * these settings.  This also validates all of the connection.
     */
    public void validate() throws ValidateException
    {
        /* Nothing to validate if the field is non-empty */
        if ( !this.isEnabled ) return;

        /* Used to verify there are not two enabled PPPoE Connections for the same interface */
        Set<Byte> argonIntfSet = new HashSet<Byte>();

        for ( PPPoEConnectionRule connection : getConnectionList()) {
            connection.validate();

            if ( connection.isLive() && !argonIntfSet.add( connection.getArgonIntf())) {
                throw new ValidateException( "The interface: " + connection.getArgonIntf() +
                                             " is in two connections" );
            }
        }
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "PPPoE Settings[" + getIsEnabled() + "]\n" );

        for ( PPPoEConnectionRule rule : getConnectionList()) sb.append( rule + "\n" );

        sb.append( "PPPoE Settings END" );

        return sb.toString();
    }
}
