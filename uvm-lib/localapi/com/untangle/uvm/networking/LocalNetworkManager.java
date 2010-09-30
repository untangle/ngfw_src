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

import java.net.InetAddress;

import com.untangle.uvm.RemoteNetworkManager;
import com.untangle.uvm.networking.internal.AccessSettingsInternal;
import com.untangle.uvm.networking.internal.AddressSettingsInternal;
import com.untangle.uvm.networking.internal.MiscSettingsInternal;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.networking.internal.ServicesInternalSettings;
import com.untangle.uvm.node.IPSessionDesc;

public interface LocalNetworkManager extends RemoteNetworkManager
{
    NetworkSpacesInternalSettings getNetworkInternalSettings();

    ServicesInternalSettings getServicesInternalSettings();

    AccessSettingsInternal getAccessSettingsInternal();
    AddressSettingsInternal getAddressSettingsInternal();
    MiscSettingsInternal getMiscSettingsInternal();

    /* Register a service that needs outside access to HTTPs, the name
     * should be unique */
    void registerService( String name );

    /* Remove a service that needs outside access to HTTPs, the name
     * should be unique */
    void unregisterService( String name );

    /* This returns an address where the host should be able to access
     * HTTP.  if HTTP is not reachable, this returns NULL */
    InetAddress getInternalHttpAddress( IPSessionDesc session );

    void registerListener( NetworkSettingsListener networkListener );

    void unregisterListener( NetworkSettingsListener networkListener );

    void registerListener( AddressSettingsListener remoteListener );

    void unregisterListener( AddressSettingsListener remoteListener );

    void registerListener( IntfEnumListener intfEnumListener );

    void unregisterListener( IntfEnumListener intfEnumListener );

    void singleNicRegisterAddress( InetAddress address );

    void refreshIptablesRules();

}
