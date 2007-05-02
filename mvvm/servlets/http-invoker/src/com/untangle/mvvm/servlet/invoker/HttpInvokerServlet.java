/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.servlet.invoker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.mvvm.engine.HttpInvoker;

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
