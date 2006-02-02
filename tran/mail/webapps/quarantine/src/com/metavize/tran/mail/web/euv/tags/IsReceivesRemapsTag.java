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
package com.metavize.tran.mail.web.euv.tags;

import javax.servlet.ServletRequest;


/**
 * Includes/excludes body chunks if the
 * current inbox receives remappings from other inboxes
 */
public final class IsReceivesRemapsTag
  extends IfElseTag {

  private static final String RECEIVES_REMAPS_KEY = "metavize.remapping.IsReceivesRemapsTag";
  
  @Override
  protected boolean isConditionTrue() {
    Boolean ret = (Boolean) pageContext.getRequest().getAttribute(RECEIVES_REMAPS_KEY);
    return ret==null?
      false:
      ret.booleanValue();
  }

  public static void setCurrent(ServletRequest req, boolean receivesRemaps) {
    req.setAttribute(RECEIVES_REMAPS_KEY,
      receivesRemaps?Boolean.TRUE:Boolean.FALSE);
  }  
}
