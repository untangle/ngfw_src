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
 * Callback interface for an Object wishing to 
 * be informed when a HeaderField object changes.
 * Not called a "Listener" because I see no need
 * for there to be more than one.
 *
 */
public interface HeaderFieldObserver {



  /**
   * The given Field has changed.
   */
  public void headerFieldChanged(HeaderField field);

}