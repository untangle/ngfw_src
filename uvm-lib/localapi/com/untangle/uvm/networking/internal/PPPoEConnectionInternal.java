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

package com.untangle.uvm.networking.internal;

import com.untangle.uvm.networking.PPPoEConnectionRule;
import com.untangle.uvm.node.ImmutableRule;
import com.untangle.uvm.node.ValidateException;

/**
 * Immutable PPPoE Connection settings values.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class PPPoEConnectionInternal extends ImmutableRule
{
    /** Set to true in order to enable the PPPoE connection */
    private final String username;
    private final String password;

    /* additional (unanticipated) parameters for the PPP options file */
    private final String secretField;

    /** Index of the argon interface to run PPPoE on */
    private final byte argonIntf;
    
    /** This is the name of the ppp device (eg ppp0 or ppp1) */
    private final String deviceName;
    
    /** This is most likely going to not be used.  Set to true to
     * automatically redial every minute, even if there is no traffic.
     * This should be the default and I can't imagine anyone every
     * turning it off. */
    private final boolean keepalive;

    private PPPoEConnectionInternal( PPPoEConnectionRule rule, String deviceName )
    {
        super( rule );
        this.username    = rule.getUsername();
        this.password    = rule.getPassword();
        this.keepalive   = rule.getKeepalive();
        this.argonIntf   = rule.getArgonIntf();
        this.secretField = rule.getSecretField();
        this.deviceName  = deviceName;
    }
    
    public String getUsername()
    {
        return this.username;
    }

    public String getPassword()
    {
        return this.password;
    }

    public String getSecretField()
    {
        return this.secretField;
    }

    public byte getArgonIntf()
    {
        return this.argonIntf;
    }

    public String getDeviceName()
    {
        return this.deviceName;
    }

    public boolean getKeepalive()
    {
        return this.keepalive;
    }

    public String toString()
    {
        return "[" + this.argonIntf + "," + this.deviceName + "," + this.keepalive + 
            "," + this.username + "]";
    }
    
    /* Return a new rule objects prepopulated with these values */
    public PPPoEConnectionRule toRule()
    {
        PPPoEConnectionRule rule = new PPPoEConnectionRule();
        super.toRule( rule );
        rule.setUsername( getUsername());
        rule.setPassword( getPassword());
        rule.setKeepalive( getKeepalive());
        rule.setArgonIntf( getArgonIntf());
        rule.setSecretField( getSecretField());
        return rule;
    }
    
    static PPPoEConnectionInternal makeInstance( PPPoEConnectionRule rule, String deviceName )
        throws ValidateException
    {
        rule.validate();
        
        deviceName = ( null == deviceName ) ? "" : deviceName.trim();
        
        if ( rule.isLive() && ( deviceName.length() == 0 )) {
            throw new ValidateException( "null device name for enabled pppoe rule" + rule );
        }

        return new PPPoEConnectionInternal( rule, deviceName );
    }
}