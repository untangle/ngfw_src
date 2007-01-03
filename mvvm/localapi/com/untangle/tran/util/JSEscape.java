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

package com.untangle.tran.util;

/**
 * Class to escape Strings which may contain JavaScript (to avoid
 * JavaScript injection in applications like Quarantine where
 * someone may put JavaScript in a subject).
 */
public class JSEscape {

  /**
   * Neuter any JavaScript found in the given String
   */
  public static String escapeJS(String str) {

    if(str == null) {
      return str;
    }

    //TODO: bscott Someday we may learn more about
    //      this type of attack, but for now it seems that this
    //      is all Yahoo does
    return str.replace(">", "&gt;").replace("<", "&lt;");
  }

}