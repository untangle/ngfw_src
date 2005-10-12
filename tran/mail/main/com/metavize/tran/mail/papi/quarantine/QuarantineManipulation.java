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
import java.io.Serializable;

/**
 * Base-interface for Admin and User views
 * into the Quarantine
 */
public interface QuarantineManipulation {

  public InboxIndex purge(String account,
    String...doomedMails)
    throws NoSuchInboxException, QuarantineUserActionFailedException;

  public InboxIndex rescue(String account,
    String...rescuedMails)
    throws NoSuchInboxException, QuarantineUserActionFailedException;

  public InboxIndex getInboxIndex(String account)
    throws NoSuchInboxException, QuarantineUserActionFailedException;

  /**
   * Total hack for servlets, to test if a connection is still alive
   */
  public void test(); 
     

}