/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.networking;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.untangle.mvvm.tran.Validatable;
import com.untangle.mvvm.tran.ValidateException;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

/**
 * Settings to hold the configuration for all of the PPPoE
 * connections.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="mvvm_pppoe", schema="settings")
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
        if ( this.connectionList == null ) this.connectionList = new LinkedList<PPPoEConnectionRule>();
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
