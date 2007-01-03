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

package com.untangle.tran.mail.papi.quarantine;
import java.io.Serializable;

/**
 * Generic "something went wrong" exception.  <b>Not</b>
 * the fault of the user or the data - the back-end
 * is simply hosed.
 */
public class QuarantineUserActionFailedException
  extends Exception
  implements Serializable {

  public QuarantineUserActionFailedException() {
  }
  public QuarantineUserActionFailedException(String msg) {
    super(msg);
  }
  public QuarantineUserActionFailedException(Throwable cause) {
    super(cause);
  }  
  public QuarantineUserActionFailedException(String msg, Throwable cause) {
    super(msg, cause);
  }

}