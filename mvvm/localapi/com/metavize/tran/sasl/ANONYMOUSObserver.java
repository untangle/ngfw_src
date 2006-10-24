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
package com.metavize.tran.sasl;
import java.nio.ByteBuffer;
import static com.metavize.tran.util.ASCIIUtil.*;


/**
 * Observer for ANONYMOUS (RFC 2245) mechanism.
 */
class ANONYMOUSObserver
  extends InitialIDObserver {

  static final String[] MECH_NAMES = new String[] {
    "ANONYMOUS".toLowerCase()
  };

  ANONYMOUSObserver() {
    super(MECH_NAMES[0], DEF_MAX_MSG_SZ);
  }
}