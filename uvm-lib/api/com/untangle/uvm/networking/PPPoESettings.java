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
import javax.persistence.Table;
import javax.xml.bind.ValidationException;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.node.Rule;
import com.untangle.uvm.node.Validatable;
import com.untangle.uvm.node.ValidateException;


/**
 * Settings used for a single PPPoE connection.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class PPPoESettings extends Rule implements Serializable, Validatable
{
    /* the username */
    private String username   = "pppoe";

    /* the password */
    private String password   = "eoppp";

    private boolean live = false;
    
    public PPPoESettings() {}
    
    /**
     * Get the username.
     *
     * @return The username.
     */
    public String getUsername()
    {
        if ( this.username == null ) this.username = "";
        return this.username;
    }
    
    /**
     * Set the username.
     *
     * @param newValue The username.
     */
    public void setUsername( String newValue )
    {
        newValue = ( null == newValue ) ? "" : newValue;
        this.username = newValue.trim();
    }

    /**
     * Get the password.
     *
     * @return The password.
     */
    public String getPassword()
    {
        if ( this.password == null ) this.password = "";
        return this.password;
    }

    /**
     * Set the password.
     *
     * @param newValue The password.
     */
    public void setPassword( String newValue )
    {
        newValue = ( null == newValue ) ? "" : newValue;
        this.password = newValue.trim();
    }

    /**
     * Is PPPoE live (in use)
     *
     * @return The live flag
     */
    public boolean isLive()
    {
        return getLive();
    }

    /**
     * Is PPPoE live (in use)
     *
     * @return The live flag
     */
    public boolean getLive()
    {
        return this.live;
    }
    
    /**
     * Set the live flag
     *
     * @param newValue The live flag.
     */
    public void setLive( boolean newValue )
    {
        this.live = newValue;
    }
    

    
    public String toString()
    {
        return "[PPPoESettings: live:" + this.getLive() + " username:" + this.username + "]";
    }

    /**
     * Validate these PPPoE setting are free of errors.
     *
     * @exception ValidationException Occurs if there is an error in
     * these settings.
     */
    public void validate() throws ValidateException
    {
        if ( null == this.username || ( 0 == this.username.length())) {
            throw new ValidateException( "Empty username for PPPoE" );
        }

        if ( null == this.password || ( 0 == this.password.length())) {
            throw new ValidateException( "Empty password for PPPoE" );
        }
    }
}
