/*
 * Copyright (c) 2003, 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.addrbook;

/**
 * ...name says it all
 */
public class NoSuchEmailException
  extends Exception {

  public NoSuchEmailException(String email) {
    super("No such email address \"" + email + "\"");
  }

}


