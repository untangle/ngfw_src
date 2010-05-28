/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: PortalServlet.java 18363 2008-08-19 01:07:14Z amread $
 */

package com.untangle.uvm.rup;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.util.TransactionWork;

@SuppressWarnings("serial")
public class NonceServlet extends HttpServlet
{
    private final Random rand = new SecureRandom();
    private final Logger logger = Logger.getLogger(getClass());

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        LocalUvmContext uvm = LocalUvmContextFactory.context();

        final ProxyNonce nonce = new ProxyNonce(Long.toHexString(rand.nextLong()));
        uvm.runTransaction(new TransactionWork() {
                public boolean doWork(Session s)
                {
                    s.save(nonce);
                    return true;
                }
            });

        try {
            resp.getWriter().println(nonce.getNonce());
        } catch (IOException exn) {
            logger.warn("could not send nonce", exn);
            throw new ServletException(exn);
        }
    }
}