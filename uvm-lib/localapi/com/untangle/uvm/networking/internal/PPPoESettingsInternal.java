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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.networking.PPPoEConnectionRule;
import com.untangle.uvm.networking.PPPoESettings;
import com.untangle.uvm.node.ValidateException;

/**
 * Immutable PPPoE Settings values.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class PPPoESettingsInternal
{
    /** Set to true in order to enable the PPPoE connection */
    private final boolean isEnabled;
    
    private final List<PPPoEConnectionInternal> connectionList;

    private PPPoESettingsInternal( PPPoESettings settings, List<PPPoEConnectionInternal> connectionList )
    {
        this.isEnabled = settings.getIsEnabled();
        this.connectionList = 
            Collections.unmodifiableList( new LinkedList<PPPoEConnectionInternal>( connectionList ));        
    }

    public boolean getIsEnabled()
    {
        return this.isEnabled;
    }
    
    public List<PPPoEConnectionInternal> getConnectionList()
    {
        return this.connectionList;
    }
    
    /* Return a new settings objects prepopulated with these values */
    public PPPoESettings toSettings()
    {
        PPPoESettings settings = new PPPoESettings();
        settings.setIsEnabled( getIsEnabled());
        
        List<PPPoEConnectionRule> connectionRuleList = new LinkedList<PPPoEConnectionRule>();
        
        for ( PPPoEConnectionInternal internal : getConnectionList()) {
            connectionRuleList.add( internal.toRule());
        }

        settings.setConnectionList( connectionRuleList );
        
        return settings;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "PPPoE Internal Settings[" + getIsEnabled() + "]\n" );
        
        for ( PPPoEConnectionInternal connection : getConnectionList()) sb.append( connection + "\n" );

        sb.append( "PPPoE Internal Settings END" );

        return sb.toString();
    }

    public static PPPoESettingsInternal makeInstance( PPPoESettings settings )
        throws ValidateException
    {
        settings.validate();
        
        List<PPPoEConnectionInternal> connectionList = new LinkedList<PPPoEConnectionInternal>();
        
        for ( PPPoEConnectionRule rule : settings.getConnectionList()) {
            /* Use the argon interface as the suffix for the device
             * name, they do not have to be sequential. */
            String deviceName = ( rule.isLive()) ? ( "ppp" + rule.getArgonIntf() ) : null;
            connectionList.add( PPPoEConnectionInternal.makeInstance( rule, deviceName ));
        }

        return new PPPoESettingsInternal( settings, connectionList );
    }
}