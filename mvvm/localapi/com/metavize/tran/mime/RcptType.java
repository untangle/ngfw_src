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
 * Enum of recipient types (TO, CC).  Note that "Bcc" is part of this
 * enumeration, but will never be returned from a MIMEMessage.  It
 * has been added to assist in SMTP-type stuff.
 */
public enum RcptType {
  TO,
  CC,
  BCC
}