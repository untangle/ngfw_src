/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: HttpInvokerServlet.java,v 1.1 2004/12/29 07:57:20 amread Exp $
 */

package com.metavize.mvvm.servlet.invoker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.metavize.mvvm.engine.InvokerBase;

public class HttpInvokerServlet extends HttpServlet
{
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        InputStream is = req.getInputStream();
        OutputStream os = resp.getOutputStream();
        InvokerBase ib = (InvokerBase)getServletContext()
            .getAttribute("invoker");
        ib.handle(is, os, req.getLocalAddr().equals("127.0.0.1"));
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        PrintWriter pr = new PrintWriter(resp.getOutputStream());

        Object o = getServletContext().getAttribute("invoker");

        pr.println("InvokerBase: " + o);
        pr.close();
    }
}
