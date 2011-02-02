/*
 * $HeadURL: svn://chef/work/src/uvm/impl/com/untangle/uvm/engine/TomcatManager.java $
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

package com.untangle.uvm;

import javax.servlet.ServletContext;

import org.apache.catalina.Realm;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.Valve;

public interface LocalTomcatManager
{
    public ServletContext loadPortalApp(String urlBase, String rootDir, Realm realm, AuthenticatorBase auth);

    public ServletContext loadInsecureApp(String urlBase, String rootDir);

    public ServletContext loadInsecureApp(String urlBase, String rootDir, Valve valve);

    public boolean unloadWebApp(String contextRoot);
    
}
