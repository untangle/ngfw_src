/*
 * Copyright (c) 2003-2006 Untangle, Inc.
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

/**
 * Outputs the max days an inbox record can be interned or 14 if not set
 */
public final class MaxDaysToInternTag extends SingleValueTag {
  private static final String DAYS_TO_INTERN = "untangle.days.to_intern";

  @Override
  protected String getValue() {
    return getMaxDays(pageContext.getRequest());
  }  

  public static final void setMaxDays(ServletRequest request, String maxDays) {
    request.setAttribute(DAYS_TO_INTERN, maxDays);
  }

  public static final void clearMaxDays(ServletRequest request) {
    request.removeAttribute(DAYS_TO_INTERN);
  }

  /**
   * Returns default of 14 if there is no max days
   */
  static String getMaxDays(ServletRequest request) {
    String maxDays = (String) request.getAttribute(DAYS_TO_INTERN);
    if (null == maxDays) {
        return "14";
    }
    return maxDays;
  }
}
