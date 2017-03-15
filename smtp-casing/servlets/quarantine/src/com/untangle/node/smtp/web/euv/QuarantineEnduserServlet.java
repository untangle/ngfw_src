/**
 * $Id$
 */
package com.untangle.node.smtp.web.euv;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.SmtpImpl;
import com.untangle.node.smtp.quarantine.QuarantineSettings;
import com.untangle.node.smtp.quarantine.QuarantineUserView;
import com.untangle.node.smtp.safelist.SafelistManipulation;
import com.untangle.uvm.UvmContextFactory;

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
    private SmtpImpl m_mailNode;
    private QuarantineUserView m_quarantine;
    private SafelistManipulation m_safelist;

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
     * Access the remote reference to the SafelistManipulation.  If this
     * method returns null, the caller should not attempt to fix
     * the situation (i.e. you're hosed).
     * <br><br>
     * Also, no need for caller to log issue if null is returned.  This
     * method already makes a log message
     *
     * @return the safelist.
     */
    public SafelistManipulation getSafelist()
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
    public SmtpImpl getSmtpNode()
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
        QuarantineSettings qSettings = m_mailNode.getSmtpSettings().getQuarantineSettings();
        String maxDaysToIntern = new Long(qSettings.getMaxMailIntern() / QuarantineSettings.DAY).toString();
        //m_logger.info("maxDaysToIntern: " + maxDaysToIntern);
        return maxDaysToIntern;
    }

    public String getMaxDaysIdleInbox()
    {
        if (null == m_mailNode) {
            initRemoteRefs();
        }
        QuarantineSettings qSettings = m_mailNode.getSmtpSettings().getQuarantineSettings();
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
            SmtpImpl mt = (SmtpImpl) UvmContextFactory.context().appManager().appInstances("smtp").get(0);
            m_mailNode = mt;
            m_quarantine = mt.getQuarantineUserView();
            m_safelist = mt.getSafelistManipulation();
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
