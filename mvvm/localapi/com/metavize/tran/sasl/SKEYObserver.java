/*
 * Copyright (c) 2004,2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.sasl;
import java.nio.ByteBuffer;
import static com.metavize.tran.util.ASCIIUtil.*;


/**
 * Observer for SKEY (RFC 2222) mechanism.
 */
class SKEYObserver
  extends InitialIDObserver {

  static final String[] MECH_NAMES = new String[] {
    "SKEY".toLowerCase()
  };

  SKEYObserver() {
    super(MECH_NAMES[0], DEF_MAX_MSG_SZ);
  }
}