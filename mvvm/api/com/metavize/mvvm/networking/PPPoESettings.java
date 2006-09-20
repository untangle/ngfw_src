/*
 * Copyright (c) 2003, 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.networking;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.JoinColumn;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.Type;

import com.metavize.mvvm.tran.Validatable;
import com.metavize.mvvm.tran.ValidateException;

/**
 * Settings used for all of the PPPoE connections.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="mvvm_pppoe", schema="settings")
public class PPPoESettings implements Validatable
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
    
    @Column(name="is_enabled")
    public boolean getIsEnabled()
    {
        return this.isEnabled;
    }

    public void setIsEnabled( boolean newValue )
    {
        this.isEnabled = newValue;
    }
    
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<PPPoEConnectionRule> getConnectionList()
    {
        if ( this.connectionList == null ) this.connectionList = new LinkedList<PPPoEConnectionRule>();
        return this.connectionList;
    }

    public void setConnectionList( List<PPPoEConnectionRule> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList<PPPoEConnectionRule>();
        
        this.connectionList = newValue;
    }
    
    public void validate() throws ValidateException
    {
        /* Nothing to validate if the field is non-empty */
        if ( !this.isEnabled ) return;
        
        for ( PPPoEConnectionRule connection : getConnectionList()) connection.validate();
   }
}