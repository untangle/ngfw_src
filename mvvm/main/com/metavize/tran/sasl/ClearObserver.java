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


/**
 * Base class for Observers of mechanisms
 * which do not support privacy/integrity protection.
 * <br><br>
 * By default, this class does not inspect the
 * protocol yet advertizes that integrity and
 * privacy cannot result from the exchange.
 */
abstract class ClearObserver
  implements SASLObserver {

  public boolean mechanismSupportsPrivacy() {
    return false;
  }

  public boolean mechanismSupportsIntegrity() {
    return false;
  }

  public FeatureStatus exchangeUsingPrivacy() {
    return FeatureStatus.NO;
  }

  public FeatureStatus exchangeUsingIntegrity() {
    return FeatureStatus.NO;
  }

  public FeatureStatus exchangeAuthIDFound() {
    return FeatureStatus.UNKNOWN;
  }

  public String getAuthID() {
    return null;
  }

  public FeatureStatus exchangeComplete() {
    return FeatureStatus.UNKNOWN;
  }

  public boolean initialClientData(ByteBuffer buf) {
    return false;
  }

  public boolean clientData(ByteBuffer buf) {
    return false;
  }

  public boolean serverData(ByteBuffer buf) {
    return false;
  }

}