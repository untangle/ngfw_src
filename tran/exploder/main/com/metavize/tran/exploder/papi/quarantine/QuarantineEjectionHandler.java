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


/**
 * Callback interface from the Quarantine system.  This
 * is called as mails are being ejected (rescued or purged)
 * from the system.
 * <br><br>
 * Implementations must "remove" the file from the Quarantine,
 * but may do so by copying to another location.
 */
public interface QuarantineEjectionHandler {

  /**
   * Eject the given mail.
   *
   * @param record the record (metadata) for the mail
   * @param recipient the recipient of the mail
   * @param data the data file (MIME).
   */
  public void ejectMail(InboxRecord record,
    String recipient,
    File data);

}