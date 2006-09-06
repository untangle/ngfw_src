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
 * Exception thrown when parsing headers, if there 
 * is invalid data.
 */
public class InvalidHeaderDataException 
  extends Exception {
  
  public InvalidHeaderDataException() {
    super();
  }
  public InvalidHeaderDataException(Exception ex) {
    super(ex);
  }  
  public InvalidHeaderDataException(String msg) {
    super(msg);
  } 
  public InvalidHeaderDataException(String msg, Exception ex) {
    super(msg, ex);
  } 
  
}