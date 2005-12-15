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
 * Interface for the end-user interface
 * to the Quarantine system.  
 */
public interface QuarantineUserView
  extends QuarantineManipulation {

  /**
   * Get the account name from an encrypted
   * token.
   * <br><br>
   * Note that this does <b>not</b> throw
   * NoSuchInboxException.
   */
  public String getAccountFromToken(String token)
    throws BadTokenException;

  /**
   * Request a digest email to the given address
   *
   * @param account the target account
   *
   * @return true if the digest email could be sent (not nessecerially
   *         delivered yet).  False if some rules (based on the address
   *         mean that this could never be delivered).
   */
  public boolean requestDigestEmail(String account)
    throws NoSuchInboxException, QuarantineUserActionFailedException;    
     

}