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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.node.Validatable;
import com.untangle.uvm.node.ValidateException;

/**
 * These are settings related to networking.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="u_misc_settings", schema="settings")
@SuppressWarnings("serial")
public class MiscSettings implements Serializable, Validatable
{
    private Long id;

    /* boolean which can be used by the untangle to determine if the
     * object returned by a user interface has been modified. */
    private boolean isClean;

    /* Flag for whether or not the untangle should report problems to
     * untangle */
    private boolean isExceptionReportingEnabled;

    /* Flag for whether or not TCP Window Scaling is enabled */
    private boolean isTcpWindowScalingEnabled;

    /* Script that runs after running /etc/init.d/networking. */
    private String postConfigurationScript;
    
    /* Script that runs after regenerating the iptables rules rule-generator. */
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

    /**
     * Get whether or not exception reporting is enabled
     *
     * @return True iff the untangle should reports exceptions to
     * Untangle.
     */
    @Column(name="report_exceptions")
    public boolean getIsExceptionReportingEnabled()
    {
        return this.isExceptionReportingEnabled;
    }

    /**
     * Set whether or not exception reporting is enabled
     *
     * @param newValue True iff the untangle should reports
     * exceptions to Untangle.
     */
    public void setIsExceptionReportingEnabled( boolean newValue )
    {
        if ( newValue != this.isExceptionReportingEnabled ) this.isClean = false;
        this.isExceptionReportingEnabled = newValue;
    }
    
    /**
     * Get whether tcp window scaling is enabled.
     *
     * @return True iff TCP Window Scaling is enabled.
     */
    @Column(name="tcp_window_scaling")
    public boolean getIsTcpWindowScalingEnabled()
    {
        return this.isTcpWindowScalingEnabled;
    }

    /**
     * Set whether tcp window scaling is enabled.
     *
     * @param newValue True iff TCP Window Scaling is enabled.
     */
    public void setIsTcpWindowScalingEnabled( boolean newValue )
    {
        if ( newValue != this.isTcpWindowScalingEnabled ) this.isClean = false;
        this.isTcpWindowScalingEnabled = newValue;
    }

    /**
     * Get the script to run once the box is configured
     *
     * @return The new value for the post configuration script.
     */
    @Column(name="post_configuration")
    public String getPostConfigurationScript()
    {
        if ( this.postConfigurationScript == null ) this.postConfigurationScript = "";
        return this.postConfigurationScript;
    }
    
    /**
     * Set the script to run once the box is configured
     *
     * @param newValue The new value for the post configuration script.
     */
    public void setPostConfigurationScript( String newValue )
    {
        if ( newValue == null ) {
            if ( this.postConfigurationScript != null ) this.isClean = false;
        } else if ( !newValue.equals( this.postConfigurationScript )) this.isClean = false;

        if ( newValue == null ) newValue = "";
        
        this.postConfigurationScript = newValue;
    }

    /**
     * Get the script that is executed after the iptables rule
     * generator.
     *
     * @return The custom script to run after the rule-generator.
     */
    @Column(name="custom_rules")
    public String getCustomRules()
    {
        if ( this.customRules == null ) this.customRules = "";
        return this.customRules;
    }
    
    /**
     * Set the script that is executed after the iptables rule
     * generator.
     *
     * @param newValue The custom script to run after the
     * rule-generator.
     */
    public void setCustomRules( String newValue )
    {
        if ( newValue == null ) {
            if ( this.customRules != null ) this.isClean = false;
        } else if ( !newValue.equals( this.customRules )) this.isClean = false;

        if ( newValue == null ) newValue = "";
        
        this.customRules = newValue;
    }


    /**
     * Return true iff the settings haven't been modified since the
     * last time <code>isClean( true )</code> was called.
     */
    @Transient
    public boolean isClean()
    {
        return this.isClean;
    }

    /**
     * Clear or set the isClean flag.
     *
     * @param newValue The new value for the isClean flag.
     */
    public void isClean( boolean newValue )
    {
        this.isClean = newValue;
    }

    /**
     * Validate that the settings are free of errors.
     *
     * @exception ValidateException If the settings contains errors.
     */
    @Transient
    public void validate() throws ValidateException
    {
        /* nothing appears to be necessary here for now */
    }
}
