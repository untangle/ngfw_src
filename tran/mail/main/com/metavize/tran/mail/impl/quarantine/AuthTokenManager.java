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

package com.metavize.tran.mail.impl.quarantine;

import org.apache.log4j.Logger;
import com.metavize.tran.util.Pair;

//May be removed later
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;


//===============================================
// TODO For now, this just uses Base64.  FIXME

/**
 * Class responsible for wrapping/unwrapping
 * Authentication Tokens.  Not based on any
 * strong crypto - just keeping it hard for
 * bad guys to do bad things.
 */
class AuthTokenManager {

  enum DecryptOutcome {
    OK,
    NOT_A_TOKEN,
    MALFORMED_TOKEN
  };
  

  private final Logger m_logger =
    Logger.getLogger(AuthTokenManager.class);   


  void setKey(byte[] key) {
    //Do nothing for now
  }
    
  /**
   * Create an authentication token for the given username.  The
   * returned token is a String, but may not be web-safe (i.e. URLEncoding).
   */
  String createAuthToken(String username) {
    return base64Encode(username);
  }

  /**
   * Attempt to decrypt the auth token.
   */
  Pair<DecryptOutcome, String> decryptAuthToken(String token) {
    byte[] bytes = base64Decode(token);
    if(bytes == null) {
      return new Pair<DecryptOutcome, String>(DecryptOutcome.MALFORMED_TOKEN);
    }
    return new Pair<DecryptOutcome, String>(DecryptOutcome.OK, new String(bytes));
  }

  private String base64Encode(String s) {
    if(s == null) {
      return null;
    }
    try {
      return new BASE64Encoder().encode(s.getBytes());
    }
    catch(Exception ex) {
      m_logger.warn("Exception base 64 encoding \"" + s + "\"", ex);
      return null;
    }
  }   
   
  private byte[] base64Decode(String s) {
    if(s == null) {
      return null;
    }
    try {
      return new BASE64Decoder().decodeBuffer(s);
    }
    catch(Exception ex) {
      m_logger.warn("Exception base 64 decoding \"" + s + "\"", ex);
      return null;
    }
  }  
    
}