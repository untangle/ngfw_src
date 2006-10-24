/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.impl.quarantine.store;

/**
 * Exception used by {@link com.metavize.tran.mail.impl.quarantine.store.AbstractDriver}
 * to convey that something read from a file was in a bad format.
 */
class BadFileEntry
  extends Exception {

  BadFileEntry() {}
  BadFileEntry(String s) {super(s);}  
  
}