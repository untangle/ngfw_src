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

  public List<Inbox> listInboxes()
    throws QuarantineUserActionFailedException;

  public void deleteInbox(String account)
    throws NoSuchInboxException, QuarantineUserActionFailedException;
}