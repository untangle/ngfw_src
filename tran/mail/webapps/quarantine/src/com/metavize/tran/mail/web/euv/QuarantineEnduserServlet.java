/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.mail.web.euv;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;

import com.metavize.mvvm.client.MvvmRemoteContextFactory;
import com.metavize.mvvm.client.MvvmRemoteContext;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.security.Tid;

import com.metavize.tran.mail.papi.quarantine.QuarantineUserView;
import com.metavize.tran.mail.papi.MailTransform;


/**
 * Not really a "servlet" so much as a container used
 * to hold the singleton connection to the back-end.
 *
 * This servlet serves no pages.
 */
public class QuarantineEnduserServlet
  extends HttpServlet {

  private static QuarantineEnduserServlet s_instance;
  private QuarantineUserView m_quarantine;
  private Exception m_ex;

  public QuarantineEnduserServlet() {
    assignInstance(this);
  }
  
  protected void service(HttpServletRequest req,
    HttpServletResponse resp)
    throws ServletException, IOException {

    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
  }

  /**
   * Hack, 'cause servlets suck
   */
  public static QuarantineEnduserServlet instance() {
    return s_instance;
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
  public QuarantineUserView getQuarantine() {
    if(m_quarantine == null) {
      m_quarantine = getRemoteRef();
    }
    else {
      try {
        m_quarantine.test();
      }
      catch(Exception ex) {
        log("QuarantineUserView reference stale.  Recreate (once)", ex);
        m_quarantine = getRemoteRef();
      }
    }
    return m_quarantine;
  }

  /**
   * Attempts to create a remote reference to the
   * QuarantineUserView.  Returns null if it cannot be
   * created (and logs the error).
   */
  private QuarantineUserView getRemoteRef() {
    try {
      MvvmRemoteContext ctx = MvvmRemoteContextFactory.factory().systemLogin(0);
      Tid tid = ctx.transformManager().transformInstances("mail-casing").get(0);
      TransformContext tc = ctx.transformManager().transformContext(tid);
      return ((MailTransform) tc.transform()).getQuarantineUserView();
    }
    catch(Exception ex) {
      log("Unable to create reference to QuarantineUserView", ex);
      return null;
    }
  }


  private static synchronized void assignInstance(QuarantineEnduserServlet servlet) {
    if(s_instance == null) {
      s_instance = servlet;
    }
  }
  
} 
