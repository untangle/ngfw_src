/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.networking;

import java.net.InetAddress;

import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.ArgonInterface;

import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;

import com.untangle.uvm.IntfConstants;

import com.untangle.uvm.node.firewall.intf.IntfMatcherFactory;

class IntfMatcherListener implements NetworkSettingsListener
{
    private final Logger logger = Logger.getLogger(getClass());

    IntfMatcherListener()
    {
    }

    public void event( NetworkSpacesInternalSettings settings )
    {
        IntfMatcherFactory imf = IntfMatcherFactory.getInstance();

        BitSet wanBitSet = new BitSet( IntfConstants.MAX_INTF );
        for ( ArgonInterface ai : LocalUvmContextFactory.context().localIntfManager().getIntfList()) {
            if ( ai.isWanInterface()) {
                wanBitSet.set( ai.getArgon());
            }
        }
        
        imf.setWanBitSet( wanBitSet );
    }
}
