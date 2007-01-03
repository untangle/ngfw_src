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

/**
 * Callback interface for an Object wishing to
 * be informed when a Headers object changes.
 * Not called a "Listener" because I see no need
 * for there to be more than one.
 * <br>
 * Note that for all callbacks, the change has already
 * taken place (if the tense of the method verb didn't
 * already give that away).
 */
public interface HeadersObserver
  extends HeaderFieldObserver {


  /**
   * A HeaderField with the given name has been removed.
   */
  public void headerFieldsRemoved(LCString headerName);


  /**
   * A HeaderField has been added.
   */
  public void headerFieldAdded(HeaderField field);

}