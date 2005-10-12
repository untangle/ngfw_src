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

package com.metavize.tran.mail.papi.quarantine;
import java.io.Serializable;

/**
 * ...name says it all...
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