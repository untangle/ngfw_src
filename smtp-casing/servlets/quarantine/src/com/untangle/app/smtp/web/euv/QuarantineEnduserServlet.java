/**
 * $Id$
 */
package com.untangle.app.smtp.web.euv;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.untangle.app.smtp.SmtpImpl;
import com.untangle.app.smtp.quarantine.QuarantineSettings;
import com.untangle.app.smtp.quarantine.QuarantineUserView;
import com.untangle.app.smtp.safelist.SafelistManipulation;
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
    private SmtpImpl m_mailApp;
    private QuarantineUserView m_quarantine;
    private SafelistManipulation m_safelist;

    /**
     * Initialize the container.
     */
    public QuarantineEnduserServlet()
    {
        assignInstance(this);
    }

    /**
     * Container service
     * 
     * @param  req              HttpServletRequest object.
     * @param  resp             HttpServletResponse object.
     * @throws ServletException If there's an problem with the servlet.
     * @throws IOException      General input/ooutput error.
     */
    protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {

        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Hack, 'cause servlets suck
     *
     * @return QuarantineEnduserServlet instance.
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
     * Access the remote references to the QuarantineAppView
     *
     * @return the Quarantine app view.
     */
    public SmtpImpl getSmtpApp()
    {
        if(m_safelist == null) {
            initRemoteRefs();
        }
        
        return m_mailApp;
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

    /**
     * Get the maximum time (in ms) that a message will be retained before it is deleted.
     * 
     * @return String of maximum days.
     */
    public String getMaxDaysToIntern()
    {
        if (null == m_mailApp) {
            initRemoteRefs();
        }
        QuarantineSettings qSettings = m_mailApp.getSmtpSettings().getQuarantineSettings();
        String maxDaysToIntern = Long.valueOf(qSettings.getMaxMailIntern() / QuarantineSettings.DAY).toString();
        //m_logger.info("maxDaysToIntern: " + maxDaysToIntern);
        return maxDaysToIntern;
    }

    /**
     * Get the maximum time (in ms) that an inbox can be untocuhed before is cleaned up.
     * 
     * @return String of maximum days.
     */
    public String getMaxDaysIdleInbox()
    {
        if (null == m_mailApp) {
            initRemoteRefs();
        }
        QuarantineSettings qSettings = m_mailApp.getSmtpSettings().getQuarantineSettings();
        String maxDaysIdleInbox = Long.valueOf(qSettings.getMaxIdleInbox() / QuarantineSettings.DAY).toString();
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
            m_mailApp = mt;
            m_quarantine = mt.getQuarantineUserView();
            m_safelist = mt.getSafelistManipulation();
        }
        catch(Exception ex) {
            m_logger.error("Unable to create reference to Quarantine/Safelist", ex);
        }
    }

    /**
     * If instance is null, assign to this container.
     * 
     * @param servlet QuarantineEnduserServlet to assign.
     */
    private static synchronized void assignInstance(QuarantineEnduserServlet servlet)
    {
        if(s_instance == null) {
            s_instance = servlet;
        }
    }
}
