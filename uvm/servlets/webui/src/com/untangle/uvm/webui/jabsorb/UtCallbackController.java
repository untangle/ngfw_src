/*
 * $HeadURL: svn://chef/branch/prod/jabsorb/work/src/uvm/servlets/alpaca/src/com/untangle/uvm/servlet/alpaca/ProxyServlet.java $
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

package com.untangle.uvm.webui.jabsorb;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.jabsorb.JSONRPCBridge;
import org.jabsorb.callback.CallbackController;


@SuppressWarnings("serial")
public class UtCallbackController extends CallbackController
{
    private final JSONRPCBridge bridge;

    UtCallbackController(JSONRPCBridge bridge)
    {
        this.bridge = bridge;
    }

    @Override
    public void preInvokeCallback(Object context, Object instance,
                                  Method method, Object[] arguments)
    {
    }

    @Override
    public void postInvokeCallback(Object context, Object instance,
                                   Method method, Object result)
        throws Exception
    {
        if (result!= null && !(result instanceof Serializable)) {
            bridge.registerCallableReference(result.getClass());
        }
    }
}