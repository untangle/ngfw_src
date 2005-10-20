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
import java.util.List;

/**
 * Interface for Admins to browse/manipulate
 * the Quarantine.
 */
public interface QuarantineMaintenenceView
  extends QuarantineManipulation {

  /**
   * List all inboxes maintained by this Quarantine
   *
   * @return the list of all inboxes
   */
  public List<Inbox> listInboxes()
    throws QuarantineUserActionFailedException;

  /**
   * Delete the given inbox, even if there are messages within.  This
   * does <b>not</b> prevent the account from automagically
   * being recreated next time SPAM is sent its way.
   *
   * @param account the email address
   */
  public void deleteInbox(String account)
    throws NoSuchInboxException, QuarantineUserActionFailedException;
}