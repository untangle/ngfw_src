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

package com.untangle.tran.mail.impl.quarantine.store;

/**
 * Exception used by {@link com.untangle.tran.mail.impl.quarantine.store.AbstractDriver}
 * to convey that something read from a file was in a bad format.
 */
class BadFileEntry
    extends Exception {

    BadFileEntry() {}
    BadFileEntry(String s) {super(s);}

}
