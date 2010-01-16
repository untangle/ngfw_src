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

public interface ADPhoneBookAssistant extends Assistant
{
    public void addOrUpdate(InetAddress inetAddress, String username, String domain, String hostname);
    
    /**
     * Return true iff user is a member of group.
     * @param user The user to test
     * @param group The group to see if users is a member.
     * @return True if the user is a member of the group.
     */
    public boolean isMemberOf( String user, String group );
    
    public void start();
    
    public void stop();
    
    /**
     * Refresh the group cache, normally this is done every x minutes.
     */
    public void refreshGroupCache();

    public String toString();
}
