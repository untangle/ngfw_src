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

package com.metavize.tran.mail.impl.quarantine;
import com.metavize.tran.util.JSEscape;

/**
 * Silly little class which acts as a wrapper around
 * a static JavaScript escaping method.  This was required
 * to be on a <b>public</b> Object for the Velocity template
 * engine.
 */
public class QuarJSEscaper {

  public String escapeJS(String str) {
    return JSEscape.escapeJS(str);
  }

}