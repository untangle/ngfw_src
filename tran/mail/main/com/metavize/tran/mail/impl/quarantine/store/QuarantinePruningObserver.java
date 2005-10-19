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

package com.metavize.tran.mail.impl.quarantine.store;

import com.metavize.tran.mail.papi.quarantine.InboxRecord;
import java.io.File;

/**
 * Callback interface, used while the
 * {@link com.metavize.tran.mail.impl.quarantine.store.QuatantineStore#prune store is pruning}.
 * <br><br>
 * Some day, this may make for interesting GUI stuff.
 */
public interface QuarantinePruningObserver {

  public static final QuarantinePruningObserver NOOP =
    new QuarantinePruningObserver() {
    public void preVisitInboxForOldMessages(String address, RelativeFileName inboxDir) {}
  
    public void pruningOldMessage(String recipient,
      File data,
      InboxRecord record) {}
    
    public void postVisitInboxForOldMessages(String address, RelativeFileName inboxDir) {}
  
    public void pruningOldMailbox(String account,
      RelativeFileName dirName,
      long lastTouched) {}    
    
  };


  public void preVisitInboxForOldMessages(String address, RelativeFileName inboxDir);

  public void pruningOldMessage(String recipient,
    File data,
    InboxRecord record);
  
  public void postVisitInboxForOldMessages(String address, RelativeFileName inboxDir);

  public void pruningOldMailbox(String account,
    RelativeFileName dirName,
    long lastTouched);

}