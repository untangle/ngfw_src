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

package com.metavize.tran.mail.papi.quarantine;

import java.io.File;
import com.metavize.tran.mime.EmailAddress;

/**
 * Interface for the transforms to insert
 * messages into the quarantine.  This is
 * not intended to be "remoted".
 */
public interface QuarantineTransformView {

  /**
   * Quarantine the given message, destined for
   * the named recipients.
   * <br><br>
   * Callers should be prepared for the case
   * that after making this call, the underlying
   * File from the MIMEMessage may have been
   * "stolen" (moved).
   */
  public boolean quarantineMail(File file,
    MailSummary summary,
    EmailAddress...recipients);
     

}