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
package com.untangle.uvm.user;

import java.net.InetAddress;

import com.untangle.uvm.node.HostName;
import com.untangle.uvm.user.Assistant;
import com.untangle.uvm.user.UserInfo;
import com.untangle.uvm.user.Username;
import com.untangle.uvm.user.ADPhoneBookAssistant;

public class LocalADPhoneBookAssistantImpl implements ADPhoneBookAssistant
{
    private int PRIORITY=2000000000;
    
    public void addOrUpdate(InetAddress inetAddress, String username, String domain, String hostname){
    }

    public String toString() {
        return "";
    }
    
    public void lookup( UserInfo info ){
    }
    

    /* retrieve the priority of this assistant, higher numbers are lower priority */
    public int priority() {
	return PRIORITY;
    }

}
