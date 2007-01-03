/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.tran.sasl;
import java.nio.ByteBuffer;
import static com.untangle.tran.util.ASCIIUtil.*;


/**
 * Abstract base class for SASL mechanisms which
 * send a clear UID as the first client message
 * (ANONYMOUS and SKEY that I know of).
 */
abstract class InitialIDObserver
  extends ClearObserver {

  private String m_id;
  private boolean m_seenInitialClientData = false;
  
  InitialIDObserver(String mechName,
    int maxMsgSize) {
    super(mechName, maxMsgSize);
  }

  @Override
  public FeatureStatus exchangeAuthIDFound() {
    return m_seenInitialClientData?
      (m_id==null?FeatureStatus.NO:FeatureStatus.YES):
      FeatureStatus.UNKNOWN;
  }

  @Override
  public String getAuthID() {
    return m_id;
  }

  @Override
  public boolean initialClientData(ByteBuffer buf) {
    return clientMessage(buf);
  }  

  @Override
  public boolean clientData(ByteBuffer buf) {
    return clientMessage(buf);
  }

  private boolean clientMessage(ByteBuffer buf) {

    if(m_seenInitialClientData) {
      return false;
    }

    if(!buf.hasRemaining()) {
      return false;
    }

    m_seenInitialClientData = true;
    m_id = bbToString(buf);
    return true;
  }
}