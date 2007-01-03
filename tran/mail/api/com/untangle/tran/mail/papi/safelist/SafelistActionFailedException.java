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

package com.untangle.tran.mail.papi.safelist;
import java.io.Serializable;

/**
 * Generic "something went wrong" exception.  <b>Not</b>
 * the fault of the user or the data - the back-end
 * is simply hosed.
 */
public class SafelistActionFailedException
  extends Exception
  implements Serializable {

  public SafelistActionFailedException() {
  }
  public SafelistActionFailedException(String msg) {
    super(msg);
  }
  public SafelistActionFailedException(Throwable cause) {
    super(cause);
  }  
  public SafelistActionFailedException(String msg, Throwable cause) {
    super(msg, cause);
  }

}