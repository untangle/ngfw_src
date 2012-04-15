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
package com.untangle.node.mail.web.euv;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.node.mail.papi.MailNode;
import com.untangle.node.mail.papi.quarantine.QuarantineSettings;
import com.untangle.node.mail.papi.quarantine.QuarantineUserView;
import com.untangle.node.mail.papi.safelist.SafelistEndUserView;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.NodeSettings;
import org.apache.log4j.Logger;

/**
 * Not really a "servlet" so much as a container used
 * to hold the singleton connection to the back-end.
 *
 * This servlet serves no pages.
 */
@SuppressWarnings("serial")
public class QuarantineEnduserServlet extends HttpServlet
{
    private final Logger m_logger = Logger.getLogger(QuarantineEnduserServlet.class);

    private static QuarantineEnduserServlet s_instance;
    private MailNode m_mailNode;
    private QuarantineUserView m_quarantine;
    private SafelistEndUserView m_safelist;

    public QuarantineEnduserServlet()
    {
        assignInstance(this);
    }

    protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {

        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Hack, 'cause servlets suck
     */
    public static QuarantineEnduserServlet instance()
    {
        return s_instance;
    }

    /**
     * Access the remote reference to the SafelistEndUserView.  If this
     * method returns null, the caller should not attempt to fix
     * the situation (i.e. you're hosed).
     * <br><br>
     * Also, no need for caller to log issue if null is returned.  This
     * method already makes a log message
     *
     * @return the safelist.
     */
    public SafelistEndUserView getSafelist()
    {
        if(m_safelist == null) {
            initRemoteRefs();
        }
        else {
            try {
                m_safelist.test();
            }
            catch(Exception ex) {
                m_logger.warn("SafelistEndUserView reference is stale.  Recreating (once)", ex);
                initRemoteRefs();
            }
        }
        return m_safelist;
    }

    /**
     * Access the remote references to the QuarantineNodeView
     *
     * @return the Quarantine node view.
     */
    public MailNode getMailNode()
    {
        if(m_safelist == null) {
            initRemoteRefs();
        }
        
        return m_mailNode;
    }

    /**
     * Access the remote reference to the QuarantineUserView.  If this
     * method returns null, the caller should not attempt to fix
     * the situation (i.e. you're hosed).
     * <br><br>
     * Also, no need for caller to log issue if null is returned.  This
     * method already makes a log message
     *
     * @return the Quarantine.
     */
    public QuarantineUserView getQuarantine()
    {
        if(m_quarantine == null) {
            initRemoteRefs();
        }
        else {
            try {
                m_quarantine.test();
            }
            catch(Exception ex) {
                m_logger.warn("QuarantineUserView reference is stale.  Recreating (once)", ex);
                initRemoteRefs();
            }
        }
        return m_quarantine;
    }

    public String getMaxDaysToIntern()
    {
        if (null == m_mailNode) {
            initRemoteRefs();
        }
        QuarantineSettings qSettings = m_mailNode.getMailNodeSettings().getQuarantineSettings();
        String maxDaysToIntern = new Long(qSettings.getMaxMailIntern() / QuarantineSettings.DAY).toString();
        //m_logger.info("maxDaysToIntern: " + maxDaysToIntern);
        return maxDaysToIntern;
    }

    public String getMaxDaysIdleInbox()
    {
        if (null == m_mailNode) {
            initRemoteRefs();
        }
        QuarantineSettings qSettings = m_mailNode.getMailNodeSettings().getQuarantineSettings();
        String maxDaysIdleInbox = new Long(qSettings.getMaxIdleInbox() / QuarantineSettings.DAY).toString();
        //m_logger.info("maxDaysIdleInbox: " + maxDaysIdleInbox);
        return maxDaysIdleInbox;
    }

    /**
     * Attempts to create a remote references
     */
    private void initRemoteRefs()
    {
        try {
            UvmContext ctx = UvmContextFactory.context();
            NodeSettings tid = ctx.nodeManager().nodeInstances("untangle-casing-mail").get(0);
            MailNode mt = (MailNode) ctx.nodeManager().node( tid.getId() );
            m_mailNode = mt;
            m_quarantine = mt.getQuarantineUserView();
            m_safelist = mt.getSafelistEndUserView();
        }
        catch(Exception ex) {
            m_logger.error("Unable to create reference to Quarantine/Safelist", ex);
        }
    }

    private static synchronized void assignInstance(QuarantineEnduserServlet servlet)
    {
        if(s_instance == null) {
            s_instance = servlet;
        }
    }
}
