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

package com.metavize.tran.mail.impl.imap;

import com.metavize.tran.mail.papi.imap.IMAPTokenizer;
import java.nio.ByteBuffer;
import static com.metavize.tran.util.Ascii.*;
import org.apache.log4j.Logger;
import sun.misc.BASE64Decoder;

/**
 * Receives ByteBuffers to/from server.  A subtle point
 * is that this may not see mails (depending on if it
 * is on the client or server casing), and cannot
 * request more data (via "returning" a read buffer).  
 * The first point does not matter, as this class does
 * not care about mail data.  For the second point, we ensure
 * that the Parser takes care to only pass ByteBuffers
 * aligned on token boundaries.  Even if this is located
 * before the Parser's logic, the parser will cause bytes
 * to be pushed-back and re-seen.
 */
class ImapSessionMonitor {

  private final Logger m_logger =
    Logger.getLogger(ImapSessionMonitor.class);

  private String m_userName;
  private IMAPTokenizer m_fromServerTokenizer;
  private IMAPTokenizer m_fromClientTokenizer;
  private IntHolder m_literalFromServerCount;
  private IntHolder m_literalFromClientCount;

  private TokenMonitor[] m_tokMons;

  private static final String LOGIN_SASL_MECH_NAME = "LOGIN";
  private static final String PLAIN_SASL_MECH_NAME = "PLAIN";
  private static final String GSSAPI_SASL_MECH_NAME = "GSSAPI";
  private static final String ANONYMOUS_SASL_MECH_NAME = "ANONYMOUS";
  private static final String CRAM_MD5_SASL_MECH_NAME = "CRAM-MD5";
  private static final String DIGEST_MD5_SASL_MECH_NAME = "DIGEST-MD5";
  private static final String KERBEROS_V4_SASL_MECH_NAME = "KERBEROS_V4";
  private static final String SKEY_SASL_MECH_NAME = "SKEY";
  private static final String EXTERNAL_SASL_MECH_NAME = "EXTERNAL";
  private static final String SECURID_SASL_MECH_NAME = "SECURID";
  private static final String SRP_SASL_MECH_NAME = "SRP";
  private static final String SCRAM_MD5_SASL_MECH_NAME = "SCRAM-MD5";
  private static final String NTLM_SASL_MECH_NAME = "NTLM";


  ImapSessionMonitor() {
    m_fromServerTokenizer = new IMAPTokenizer();
    m_fromClientTokenizer = new IMAPTokenizer();
    m_literalFromServerCount = new IntHolder();
    m_literalFromClientCount = new IntHolder();
    m_tokMons = new TokenMonitor[] {
//      new AuthenticateTokenMonitor(),
      new LoginTokenMonitor(),
      new TLSMonitor()
    };
  }

  boolean hasUserName() {
    return m_userName != null;
  }
  String getUserName() {
    return m_userName;
  }

  /**
   * The ByteBuffer is assumed to be a duplicate, so its position
   * can be messed-with but not its contents.
   * 
   * @return true if passthru should be entered (and this object should
   *         never be called again).
   *
   */
  boolean bytesFromClient(ByteBuffer buf) {
    return handleBytes(buf, true);
  }

  /**
   * @return true if passthru should be entered.
   */
  boolean bytesFromServer(ByteBuffer buf) {
    return handleBytes(buf, false);
  }

  /**
   * Replace one TokenMonitor with another.  This is used for
   * the situation like SASL, where one monitor detects the start
   * of a SASL negotiation, and delegates to the specfic mechanism's
   * Monitor.  When complete, the reverse replacement can be made.
   */
  private void replaceMonitor(TokenMonitor old, TokenMonitor replacement) {
    for(int i = 0; i<m_tokMons.length; i++) {
      if(m_tokMons[i] == old) {
        m_tokMons[i] = replacement;
        break;
      }
    }
  }

  private TokenMonitor getSASLMonitor(TokenMonitor currentMonitor,
    String mechanismName) {
    //Just to be anoying, RFC 3501 doesn't define the valid syntax for
    //mechanism name other than ATOM, which I *think* excludes QString
    //or literal
    //
    //
    return null;
  }

  private boolean handleBytes(final ByteBuffer buf,
    final boolean fromClient) {

    final IMAPTokenizer tokenizer = fromClient?
      m_fromClientTokenizer:m_fromServerTokenizer;
      
    final IntHolder intHolder = fromClient?
      m_literalFromClientCount:m_literalFromServerCount;

    while(buf.hasRemaining()) {
      if(intHolder.val > 0) {
        int toSkip = intHolder.val > buf.remaining()?
          buf.remaining():intHolder.val;
        for(TokenMonitor tm : m_tokMons) {
          if(tm.handleLiteral(buf, toSkip, fromClient)) {
            return true;
          }
        }
        intHolder.val-=toSkip;
        buf.position(buf.position() + toSkip);
        continue;
      }

      IMAPTokenizer.IMAPNextResult result = tokenizer.next(buf);
      
      if(result == IMAPTokenizer.IMAPNextResult.NEED_MORE_DATA) {
        m_logger.debug("Need more data");
        return true;
      }
      if(result == IMAPTokenizer.IMAPNextResult.EXCEEDED_LONGEST_WORD) {
        m_logger.warn("Exceeded longest WORD.  Assume some encryption and enter passthru");
        return true;
      }

      for(TokenMonitor tm : m_tokMons) {
        if(tm.handleToken(tokenizer, buf, fromClient)) {
          return true;
        }
      }
      if(tokenizer.isTokenLiteral()) {
        intHolder.val = tokenizer.getLiteralOctetCount();
      }
      
    }
    return false;
  }

  private final class IntHolder {
    int val = 0;
  }

  private static final byte[] STARTTLS_BYTES = "starttls".getBytes();
  private static final byte[] LOGIN_BYTES = "login".getBytes();
  private static final int MAX_REASONABLE_UID_AS_LITERAL = 1024*2;



  private enum ClientReqType {
    CONTINUATION,
    TAGGED,
    UNKNOWN//Positioned at start of new line
  };

  private enum ServerRespType {
    CONTINUATION_REQUEST,
    UNTAGGED,
    TAGGED,
    UNKNOWN//Positioned at start of new line
  };

  private static final String USERNAME_CHALLENGE = "User Name";
  private static final String PWD_CHALLENGE = "Password";
  /**
   * After extensive investigations, I've determined that
   * there is no standard for this type of authentication
   *
   * Begins life just after the "plain" mechanism
   * has been declared.
   *
   * General protocol *seems* to be as follows:
   *
   * s: User Name
   * c: + my_user_name
   * s: Password
   * c: + my_password
   *
   * Where all data is base64 encoded (except the "+").
   *
   * We thus wait for the first EOL from the server, and begin
   * examining.  We end when a line from the server is
   * not a continuation request.
   *
   * The anoying thing is that "+" is part of the Base64 alphabet,
   * as well as the IMAP token set.  Rather than changing the delimiters/tokens
   * for the Tokenizer, I'll just concatenate concurrent tokens until an EOL.
   *
   * This will break if Client pipelines (sends UID/PWD before the server
   * prompts).  The alternative is to simply use the first complete
   * line from the client, but we risk (if things were out-or-order) printing
   * folks passwords into reports.
   *
   * TODO Handle the case of the failed login request!
   */
  private class AUTH_PLAINMonitor
    extends TokenMonitor {

    private boolean m_seenFirstServerNewLine = false;
    private boolean m_prevServerLineWasUsername = false;
    private StringBuilder m_serverLineBuilder;
    private StringBuilder m_clientLineBuilder;

    AUTH_PLAINMonitor(TokenMonitor state) {
      super(state);
    }

    boolean handleTokenFromClient(IMAPTokenizer tokenizer,
      ByteBuffer buf) {
      if(m_prevServerLineWasUsername) {
        //Previious server response was "User Name".  Now,
        //This can only be the username if the type of this
        //line is ClientReqType.CONTINUATION
        if(getClientReqType() != ClientReqType.CONTINUATION) {
          m_logger.debug("Previous server line was \"UserName\" yet " +
            "this is not a continued line.  Give up");
          replaceMonitor(this, new AuthenticateTokenMonitor());
          return false;
        }
        if(tokenizer.isTokenEOL()) {
          if(m_clientLineBuilder != null) {
            m_userName = base64Decode(m_clientLineBuilder);
            m_logger.debug("Found Username to be \"" +
              m_userName + "\"");
          }
          else {
            m_logger.debug("Giving up without having found Username (no username provided)");
          }
          replaceMonitor(this, new AuthenticateTokenMonitor());
          return false;
        }
        else {
          if(m_clientLineBuilder == null) {
            m_clientLineBuilder = new StringBuilder();
          }
          m_clientLineBuilder.append(tokenizer.tokenToStringDebug(buf));
          return false;
        }
      }
      else {
        return false;
      }
    }
    boolean handleTokenFromServer(IMAPTokenizer tokenizer,
      ByteBuffer buf) {
      if(!m_seenFirstServerNewLine) {
        m_seenFirstServerNewLine = tokenizer.isTokenEOL();
        return false;
      }
      if(getServerRespType() != ServerRespType.CONTINUATION_REQUEST) {
        m_logger.debug("Done with AUTHENTICATE PLAIN");
        replaceMonitor(this, new AuthenticateTokenMonitor());
        return false;
      }
      if(getServerResponseTokenCount() > 1) {
        if(tokenizer.isTokenEOL()) {
          if(m_serverLineBuilder != null) {
            m_prevServerLineWasUsername =
              compare(m_serverLineBuilder, USERNAME_CHALLENGE);
            m_serverLineBuilder = null;
          }
        }
        else {
          //Ignore literals for now
          if(m_serverLineBuilder == null) {
            m_serverLineBuilder = new StringBuilder();
          }
          m_serverLineBuilder.append(tokenizer.tokenToStringDebug(buf));
        }
        return false;
      }
      return false;
    }


    private boolean compare(StringBuilder base64String, String str) {
      String s = base64Decode(base64String);
      if(s == null) {
        return false;
      }
      s = s.trim();
      return s.equalsIgnoreCase(str);
    }

    private String base64Decode(StringBuilder sb) {
      try {
        return new String(new BASE64Decoder().decodeBuffer(sb.toString()));
      }
      catch(Exception ex) {
        m_logger.warn(ex);
        return null;
      }
    }
    
  }
  
  
  private class TLSMonitor
    extends TokenMonitor {
    
    boolean handleTokenFromClient(IMAPTokenizer tokenizer,
      ByteBuffer buf) {

      if(
        getClientRequestTokenCount() == 2 &&
        !tokenizer.isTokenEOL() &&
        getClientReqType() == ClientReqType.TAGGED &&
        tokenizer.compareWordAgainst(buf, STARTTLS_BYTES, true)
        ) {
        m_logger.debug("STARTTLS command issued from client.  Assume" +
          "this will succeed and thus go into passthru mode");
        return true;
      }
      return false;
    }
  }  


  private enum LTMState {
    NONE,
    TAGGED_SUSPECT,
    LOGIN_FOUND
  }    
    
  private class LoginTokenMonitor
    extends TokenMonitor {

    private LTMState m_state = LTMState.NONE;
    private byte[] m_literalUID;
    private int m_nextLiteralPos;


    boolean handleLiteralFromClient(ByteBuffer buf, int bytesFromPosAsLiteral) {
      if(m_literalUID == null) {
        return false;
      }
      if((m_literalUID.length - m_nextLiteralPos) < bytesFromPosAsLiteral) {
        m_logger.error("Expecting to collect a literal of length " +
          m_literalUID.length + " as username, yet received too many" +
          "bytes.  Tracking error");
        m_literalUID = null;
        return false;
      }
      for(int i = 0; i<bytesFromPosAsLiteral; i++) {
        m_literalUID[m_nextLiteralPos++] = buf.get(buf.position() + i);
      }
      if(m_nextLiteralPos >= m_literalUID.length) {
        m_literalUID = null;
        m_nextLiteralPos = 0;
        m_userName = new String(m_literalUID);
        m_logger.debug("Completed accumulation of literal for username (\"" +
          m_userName + "\"");
      }
      return false;
    }    
    
    boolean handleTokenFromClient(IMAPTokenizer tokenizer,
      ByteBuffer buf) {

      //Quick bypass for impossible lines
      if(getClientRequestTokenCount() > 3) {
//        m_logger.debug("FOOO (login guy) getClientRequestTokenCount() " + getClientRequestTokenCount());
        m_state = LTMState.NONE;
        return false;
      }

//      m_logger.debug("FOOO (login guy) Token: " +
//        tokenizer.tokenToStringDebug(buf) + ", state: " +
//        m_state + ", getClientRequestTokenCount(): " + getClientRequestTokenCount() +
//        ", getClientReqType(): " + getClientReqType());
      
      switch(m_state) {
        case NONE:
          if(
            getClientRequestTokenCount() == 1 &&
            !tokenizer.isTokenEOL() &&
            getClientReqType() == ClientReqType.TAGGED
            ) {
            m_state = LTMState.TAGGED_SUSPECT;
          }
          break;
        case TAGGED_SUSPECT:
          if(getClientRequestTokenCount() == 2 &&
            !tokenizer.isTokenEOL() &&
            tokenizer.compareWordAgainst(buf, LOGIN_BYTES, true)
            ) {
            m_state = LTMState.LOGIN_FOUND;
          }
          else {
            m_state = LTMState.NONE;
          }
          break;
        case LOGIN_FOUND:
          //TODO bscott Remove this from the list of Monitors.  The odds
          //of someone re-authenticating or having the login fail and another
          //come in is really low
          switch(tokenizer.getTokenType()) {
            case WORD:
              m_userName = tokenizer.getWordAsString(buf);
              m_logger.debug("Found WORD username \"" + m_userName + "\"");
              break;
            case QSTRING:
              m_userName = new String(tokenizer.getQStringToken(buf));
              m_logger.debug("Found QSTRING username \"" + m_userName + "\"");
              break;            
            case LITERAL:
              m_logger.debug("username is a LITERAL (collect on subsequent calls)");
              if(tokenizer.getLiteralOctetCount() > MAX_REASONABLE_UID_AS_LITERAL) {
                m_logger.error("Received a LOGIN uid as a literal or length: " +
                  tokenizer.getLiteralOctetCount() + ".  This exceeds the reasonable" +
                  " limit of " + MAX_REASONABLE_UID_AS_LITERAL + ".  This is either a " +
                  "state-tracking bug, or someone really clever trying to cause some " +
                  "DOS-style attack on this process");
              }
              else {
                m_literalUID = new byte[tokenizer.getLiteralOctetCount()];
                m_nextLiteralPos = 0;
              }
              break;
            case CONTROL_CHAR:
              m_userName = new String(new byte[] {buf.get(tokenizer.getTokenStart())});
              m_logger.warn("Username is also a control character \"" + m_userName + "\" (?!?)");
              break;
            case NEW_LINE:
              m_logger.debug("Expecting username token, got EOL.  Assume server will return error");
            case NONE:
          }
          m_state = LTMState.NONE;
          break;//Redundant
      }
      return false;
    }     
    
  }  

  private class AuthenticateTokenMonitor
    extends TokenMonitor {

    boolean handleTokenFromServer(IMAPTokenizer tokenizer,
      ByteBuffer buf) {
      if(getServerResponseTokenCount() == 1 && !tokenizer.isTokenEOL()) {
//        m_logger.debug("FOOO " +
//          "First token of " + getServerRespType() + " server line: " + 
//          tokenizer.tokenToStringDebug(buf));
      }
      return false;
    }
      
    boolean handleTokenFromClient(IMAPTokenizer tokenizer,
      ByteBuffer buf) {
      if(getClientRequestTokenCount() == 1 && !tokenizer.isTokenEOL()) {
//        m_logger.debug("FOOO " +
//          "First token of " + getClientReqType() + " client line: " +
//          tokenizer.tokenToStringDebug(buf));
      }
      return false;
    }     
    
  }


  private abstract class TokenMonitor {

    private ServerRespType m_serverLineType = ServerRespType.UNKNOWN;
    private ClientReqType m_clientLineType = ClientReqType.UNKNOWN;
    private boolean m_clientAtNewLine = true;
    private boolean m_serverAtNewLine = true;
    private boolean m_lastServerLineContReq = false;
    private int m_serverLineTokenCount = 0;
    private int m_clientLineTokenCount = 0;

    TokenMonitor() {
    }
    TokenMonitor(TokenMonitor cloneState) {
      m_serverLineType = cloneState.m_serverLineType;
      m_clientLineType = cloneState.m_clientLineType;
      m_clientAtNewLine = cloneState.m_clientAtNewLine;
      m_serverAtNewLine = cloneState.m_serverAtNewLine;
      m_lastServerLineContReq = cloneState.m_lastServerLineContReq;
      m_serverLineTokenCount = cloneState.m_serverLineTokenCount;
      m_clientLineTokenCount = cloneState.m_clientLineTokenCount;
    }

    /**
     * Transfer state from <i>this</i>
     * monitor to the other.
     */
    private void transferState(TokenMonitor other) {
    }
    

    final int getClientRequestTokenCount() {
      return m_clientLineTokenCount;
    }
    final int getServerResponseTokenCount() {
      return m_serverLineTokenCount;
    }
    final ClientReqType getClientReqType() {
      return m_clientLineType;
    }
    final ServerRespType getServerRespType() {
      return m_serverLineType;
    }

    final boolean handleLiteral(ByteBuffer buf, int bytesFromPosAsLiteral, boolean client) {
      if(client) {
        return handleLiteralFromClient(buf, bytesFromPosAsLiteral);
      }
      else {
        return handleLiteralFromServer(buf, bytesFromPosAsLiteral);
      }
    }

    
    
    final boolean handleToken(IMAPTokenizer tokenizer,
      ByteBuffer buf,
      boolean fromClient) {
      if(fromClient) {
        if(m_clientAtNewLine) {
          //Two EOLs in a row we'll skip
          if(tokenizer.isTokenEOL()) {
            //Just leave the state as-is
            return handleTokenFromClient(tokenizer, buf);
          }
          //Based on the last Server line, determine
          //the type of this client request
          m_clientLineType = m_lastServerLineContReq?
            ClientReqType.CONTINUATION:ClientReqType.TAGGED;
            
          m_clientAtNewLine = false;
          m_clientLineTokenCount = 1;
        }
        else {
          if(tokenizer.isTokenEOL()) {
            m_clientAtNewLine = true;
          }
          else {
            m_clientLineTokenCount++;
          }
        }
        return handleTokenFromClient(tokenizer, buf);
      }
      else {//BEGIN Server Token
        if(m_serverAtNewLine) {
          //This means the last token we saw was an EOL.

          //Two EOLs in a row we'll skip
          if(tokenizer.isTokenEOL()) {
            //Just leave the state as-is
            return handleTokenFromServer(tokenizer, buf);
          }
          else {
            m_serverAtNewLine = false;
            m_serverLineTokenCount = 1;

            //Figure out what type of response this is
            if(tokenizer.compareCtlAgainstByte(buf, PLUS_B)) {
              m_serverLineType = ServerRespType.CONTINUATION_REQUEST;
            }
            else if(tokenizer.compareCtlAgainstByte(buf, STAR_B)) {
              m_serverLineType = ServerRespType.UNTAGGED;
            }
            else {
              m_serverLineType = ServerRespType.TAGGED;
            }
          }
        }
        else {
          if(tokenizer.isTokenEOL()) {
            m_serverAtNewLine = true;
            //Record if the *previous* server line was a continuation request
            m_lastServerLineContReq = m_serverLineType==ServerRespType.CONTINUATION_REQUEST;
          }
          else {
            m_serverLineTokenCount++;
          }
        }
        return handleTokenFromServer(tokenizer, buf);
      }//ENDOF Server Token
    }

    boolean handleLiteralFromClient(ByteBuffer buf, int bytesFromPosAsLiteral) {
      return false;
    }
    boolean handleLiteralFromServer(ByteBuffer buf, int bytesFromPosAsLiteral) {
      return false;
    }
  
    boolean handleTokenFromServer(IMAPTokenizer tokenizer,
      ByteBuffer buf) {
      return false;
    }
      
    boolean handleTokenFromClient(IMAPTokenizer tokenizer,
      ByteBuffer buf) {
      return false;
    }      
  }
  

/*  
  private abstract class TokenMonitor {

    private int m_skippingClientLiteralCount;
    private int m_skippingServerLiteralCount;
    private boolean m_skippingLineClient = false;
    private boolean m_skippingLineServer = false;

    final void skipLiteral(int length, boolean fromClient) {
      if(fromClient) {
        m_skippingClientLiteralCount=length;
      }
      else {
        m_skippingServerLiteralCount=length;
      }
    }
    final void skipToNewLine(boolean fromClient) {
      if(fromClient) {
        m_skippingLineClient=true;
      }
      else {
        m_skippingLineServer=true;
      }      
    }

    final boolean handleLiteral(ByteBuffer buf, int bytesFromPosAsLiteral, boolean client) {
      if(client) {
        if(m_skippingClientLiteralCount>0) {
          m_skippingClientLiteralCount-=bytesFromPosAsLiteral;
          return false;
        }
        return handleLiteralFromClient(buf, bytesFromPosAsLiteral);
      }
      else {
        if(m_skippingServerLiteralCount>0) {
          m_skippingServerLiteralCount-=bytesFromPosAsLiteral;
          return false;
        }
        return handleLiteralFromServer(buf, bytesFromPosAsLiteral);
      }
    }
    
    final boolean handleToken(IMAPTokenizer tokenizer,
      ByteBuffer buf,
      boolean fromClient) {
      if(fromClient) {
        if(m_skippingLineClient && !tokenizer.isTokenEOL()) {
          return false;
        }
        m_skippingLineClient = false;
        return handleTokenFromClient(tokenizer, buf);
      }
      else {
        if(m_skippingLineServer && !tokenizer.isTokenEOL()) {
          return false;
        }
        m_skippingLineServer = false;
        return handleTokenFromServer(tokenizer, buf);
      }
    }

    boolean handleLiteralFromClient(ByteBuffer buf, int bytesFromPosAsLiteral) {
      return false;
    }
    boolean handleLiteralFromServer(ByteBuffer buf, int bytesFromPosAsLiteral) {
      return false;
    }
  
    boolean handleTokenFromServer(IMAPTokenizer tokenizer,
      ByteBuffer buf) {
      return false;
    }
      
    boolean handleTokenFromClient(IMAPTokenizer tokenizer,
      ByteBuffer buf) {
      return false;
    }      
    
  }
*/  
}