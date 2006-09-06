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
 * ...name says it all...
 * <p>
 * XXXXXX bscott a better base class?
 */
public class LineTooLongException 
  extends Exception {

  public LineTooLongException(int limit) {
    super("Line exceeded " + limit + " byte limit");
  }
  
}