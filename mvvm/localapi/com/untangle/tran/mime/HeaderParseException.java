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

package com.untangle.tran.mime;

//XXXXXXX bscott - this is redundant with InvalidHeaderDataException

/**
 * Exception thrown when parsing headers, if they
 * cannot be parsed.
 */
public class HeaderParseException
  extends Exception {

  public HeaderParseException() {
    super();
  }
  public HeaderParseException(Exception ex) {
    super(ex);
  }
  public HeaderParseException(String msg) {
    super(msg);
  }
  public HeaderParseException(String msg, Exception ex) {
    super(msg, ex);
  }

}