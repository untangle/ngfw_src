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

package com.untangle.uvm.servlet.invoker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.engine.HttpInvoker;

public class HttpInvokerServlet extends HttpServlet
{
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        InputStream is = req.getInputStream();
        OutputStream os = resp.getOutputStream();
        HttpInvoker hi = (HttpInvoker)getServletContext()
            .getAttribute("invoker");

        InetAddress clientAddr;
        try {
            clientAddr = InetAddress.getByName(req.getRemoteAddr());
        } catch (Exception x) {
            // Can't happen
            throw new Error(x);
        }
        if (HttpInvoker.GZIP_RESPONSE)
            resp.setHeader("Content-Encoding", "gzip");
        hi.handle(is, os, req.getLocalAddr().equals("127.0.0.1"), clientAddr);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        PrintWriter pr = new PrintWriter(resp.getOutputStream());

        Object o = getServletContext().getAttribute("invoker");

        pr.println("HttpInvoker: " + o);
        pr.close();
    }
}
