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
package com.metavize.tran.mime;



/**
 * Exception thrown when parsing MIMEParts, if they
 * cannot be parsed.
 */
public class MIMEPartParseException 
  extends Exception {
  
  public MIMEPartParseException() {
    super();
  }
  public MIMEPartParseException(Exception ex) {
    super(ex);
  }  
  public MIMEPartParseException(String msg) {
    super(msg);
  } 
  public MIMEPartParseException(String msg, Exception ex) {
    super(msg, ex);
  }     
  
}