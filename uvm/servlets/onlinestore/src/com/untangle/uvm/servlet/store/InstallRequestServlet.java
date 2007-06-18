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

package com.untangle.uvm.servlet.store;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.security.UvmPrincipal;
import org.apache.log4j.Logger;

public class InstallRequestServlet extends HttpServlet
{
    private final Logger logger = Logger.getLogger(getClass());

    @Override
    public void init()
    {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        UvmPrincipal p = (UvmPrincipal)req.getUserPrincipal();

        if (p.isReadOnly()) {
            logger.debug("ignoring install request in read-only mode");
        } else {
            String mackageName = req.getParameter("mackage");

            if (null != mackageName) {
                LocalUvmContext ctx = LocalUvmContextFactory.context();
                ctx.toolboxManager().requestInstall(mackageName);
            } else {
                try {
                    resp.sendError(406, "need mackage-name");
                } catch (IOException exn) {
                    logger.warn("could not send error page", exn);
                }
            }
        }
    }
}
