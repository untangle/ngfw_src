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
package com.untangle.tran.mail.web.euv.tags;

import javax.servlet.ServletRequest;
import java.net.URLEncoder;


/**
 * Outputs the current auth token (URL encoded optionaly), or null
 * if there 'aint one
 */
public final class CurrentAuthTokenTag
  extends SingleValueTag {

  private static final String AUTH_TOKEN_KEY = "untangle.auth_token";

  private boolean m_encoded = false;

  public void setEncoded(boolean encoded) {
    m_encoded = encoded;
  }
  public boolean isEncoded() {
    return m_encoded;
  }

  @Override
  protected String getValue() {
    String s = null;
    if(hasCurrent(pageContext.getRequest())) {
      s = getCurrent(pageContext.getRequest());
      if(isEncoded()) {
        s = URLEncoder.encode(s);
      }
    }
    return s;
  }

  public static final void setCurrent(ServletRequest request,
    String token) {
    request.setAttribute(AUTH_TOKEN_KEY, token);
  }
  public static final void clearCurret(ServletRequest request) {
    request.removeAttribute(AUTH_TOKEN_KEY);
  }

  /**
   * Returns null if there is no current token
   */
  static String getCurrent(ServletRequest request) {
    return (String) request.getAttribute(AUTH_TOKEN_KEY);
  }

  static boolean hasCurrent(ServletRequest request) {
    return getCurrent(request) != null;
  }  
}
