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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.mvvm.tran.Validatable;
import com.untangle.mvvm.tran.ValidateException;

/** These are network settings that don't really have a clear home. */
@Entity
@Table(name="mvvm_misc_settings", schema="settings")
public class MiscSettings implements Serializable, Validatable
{
    private Long id;

    private boolean isClean;

    private boolean isExceptionReportingEnabled;
    private boolean isTcpWindowScalingEnabled;
    private String postConfigurationScript;
    private String customRules;
    
    public MiscSettings()
    {
    }
    
    @Id
    @Column(name="settings_id")
    @GeneratedValue
    Long getId()
    {
        return id;
    }

    void setId( Long id )
    {
        this.id = id;
    }

    /* Get whether or not exception reporting is enabled */
    @Column(name="report_exceptions")
    public boolean getIsExceptionReportingEnabled()
    {
        return this.isExceptionReportingEnabled;
    }

    /* Set whether or not exception reporting is enabled */
    public void setIsExceptionReportingEnabled( boolean newValue )
    {
        if ( newValue != this.isExceptionReportingEnabled ) this.isClean = false;
        this.isExceptionReportingEnabled = newValue;
    }
    
    /* Get whether tcp window scaling is enabled */
    @Column(name="tcp_window_scaling")
    public boolean getIsTcpWindowScalingEnabled()
    {
        return this.isTcpWindowScalingEnabled;
    }

    /* Set whether tcp window scaling is enabled */
    public void setIsTcpWindowScalingEnabled( boolean newValue )
    {
        if ( newValue != this.isTcpWindowScalingEnabled ) this.isClean = false;
        this.isTcpWindowScalingEnabled = newValue;
    }

    /* Get the script to run once the box is configured */
    @Column(name="post_configuration")
    public String getPostConfigurationScript()
    {
        if ( this.postConfigurationScript == null ) this.postConfigurationScript = "";
        return this.postConfigurationScript;
    }
    
    /* Set the script to run once the box is configured */
    public void setPostConfigurationScript( String newValue )
    {
        if ( newValue == null ) {
            if ( this.postConfigurationScript != null ) this.isClean = false;
        } else if ( !newValue.equals( this.postConfigurationScript )) this.isClean = false;

        if ( newValue == null ) newValue = "";
        
        this.postConfigurationScript = newValue;
    }

    /* Get the post configuration script */
    @Column(name="custom_rules")
    public String getCustomRules()
    {
        if ( this.customRules == null ) this.customRules = "";
        return this.customRules;
    }
    
    /* This should be validated */
    public void setCustomRules( String newValue )
    {
        if ( newValue == null ) {
            if ( this.customRules != null ) this.isClean = false;
        } else if ( !newValue.equals( this.customRules )) this.isClean = false;

        if ( newValue == null ) newValue = "";
        
        this.customRules = newValue;
    }

    @Transient
    public boolean isClean()
    {
        return this.isClean;
    }

    public void isClean( boolean newValue )
    {
        this.isClean = newValue;
    }

    @Transient
    public void validate() throws ValidateException
    {
        /* nothing appears to be necessary here for now */
    }
}
