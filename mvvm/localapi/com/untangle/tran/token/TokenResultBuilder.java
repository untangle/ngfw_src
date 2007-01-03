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

package com.untangle.tran.token;


import java.util.*;

import com.untangle.mvvm.tapi.Pipeline;


/**
 * Class to assemble a TokenResult incrementally.  Handles
 * the impendence mis-match between Tokens and TokenStreamers.
 * <br>
 * The order in-which tokens/streamers are added is maintained
 * in the {@link #getTokenResult returned result}.
 */
public class TokenResultBuilder {

  private List<Token> m_forClientTokens;
  private List<Token> m_forServerTokens;

  private DynTokenStreamer m_forClientStreamer;
  private DynTokenStreamer m_forServerStreamer;

  private Pipeline m_pipeline;


  public TokenResultBuilder(Pipeline pipeline) {
    m_pipeline = pipeline;
  }

  /**
   * Add a token intended to go from
   * server to client
   */
  public void addTokenForClient(Token token) {
    addToken(token, true);
  }
  /**
   * Add a token intended to go from
   * client to server
   */
  public void addTokenForServer(Token token) {
    addToken(token, false);
  }
  /**
   * Add a streamer intended to go from
   * server to client
   */
  public void addStreamerForClient(TokenStreamer streamer) {
    addStreamer(streamer, true);
  }
  /**
   * Add a streamer intended to go from
   * client to server
   */
  public void addStreamerForServer(TokenStreamer streamer) {
    addStreamer(streamer, false);
  }

  /**
   * Query method to detect if this TokenResultBuilder currently
   * has any Tokens/Streamers bound for the server
   */
  public boolean hasDataForServer() {
    return m_forServerStreamer != null ||
      m_forServerTokens != null;
  }
  /**
   * Query method to detect if this TokenResultBuilder currently
   * has any Tokens/Streamers bound for the client
   */
  public boolean hasDataForClient() {
    return m_forClientStreamer != null ||
      m_forClientTokens != null;
  }  

  /**
   * Get a TokenResult from any internally gathered
   * Tokens and/or streamers.
   */
  public TokenResult getTokenResult() {
    //Since the TokenResult only accepts
    //all streamers or all tokens, we need
    //to get creative with the return
    if(m_forClientStreamer != null || m_forServerStreamer != null) {
      //We're returning a Streamer-style result
      if(m_forServerTokens != null) {
        m_forServerStreamer = new DynTokenStreamer(m_forServerTokens);
        m_forServerTokens = null;
      }
      if(m_forClientTokens != null) {
        m_forClientStreamer = new DynTokenStreamer(m_forClientTokens);
        m_forClientTokens = null;
      }
      return new TokenResult(m_forClientStreamer, m_forServerStreamer);
    }
    else if(m_forClientTokens != null || m_forServerTokens != null) {
      return new TokenResult(tokenListToArray(m_forClientTokens),
        tokenListToArray(m_forServerTokens));
    }
    return TokenResult.NONE;
  }

  private Token[] tokenListToArray(List<Token> list) {
    if(list == null) {
      return null;
    }
    return (Token[]) list.toArray(new Token[list.size()]);
  }

  private void addStreamer(TokenStreamer streamer, boolean forClient) {
    if(forClient) {
      if(m_forClientStreamer != null) {
        //Already have a streamer.  Create a new one chaining
        //new and old
        m_forClientStreamer = new DynTokenStreamer(m_forClientStreamer, streamer);
      }
      else {
        //Streamer is null (must create anew)
        if(m_forClientTokens != null) {
          //Create Streamer prepending the existing tokens
          m_forClientStreamer = new DynTokenStreamer(m_forClientTokens, streamer);
          m_forClientTokens = null;
        }
        else {
          //No existing tokens.  Simply create a new streamer
          //wrapping theirs.
          m_forClientStreamer = new DynTokenStreamer(streamer);
        }
      }
    }
    else {
      if(m_forServerStreamer != null) {
        //Already have a streamer.  Create a new one chaining
        //new and old
        m_forServerStreamer = new DynTokenStreamer(m_forServerStreamer, streamer);
      }
      else {
        //Streamer is null (must create anew)
        if(m_forServerTokens != null) {
          //Create Streamer prepending the existing tokens
          m_forServerStreamer = new DynTokenStreamer(m_forServerTokens, streamer);
          m_forServerTokens = null;
        }
        else {
          //No existing tokens.  Simply create a new streamer
          //wrapping theirs.
          m_forServerStreamer = new DynTokenStreamer(streamer);
        }
      }
    }
  }

  private void addToken(Token t, boolean forClient) {


    if(forClient) {
      if(m_forClientStreamer != null) {
        //Add to the streamer
        m_forClientStreamer.appendToken(t);
      }
      else {
        //No streamer.  Create the array of tokens
        //(if we have to) and add the token
        if(m_forClientTokens == null) {
          m_forClientTokens = new ArrayList<Token>();
        }
        m_forClientTokens.add(t);
      }
    }
    else {
      if(m_forServerStreamer != null) {
        m_forServerStreamer.appendToken(t);
      }
      else {
        if(m_forServerTokens == null) {
          m_forServerTokens = new ArrayList<Token>();
        }
        m_forServerTokens.add(t);
      }
    }
  }

  private enum StreamState {
    INIT_TOKENS,
    S1,
    S2,
    TAIL_TOKENS,
    DONE
  }

  private class DynTokenStreamer
    implements TokenStreamer {

    private TokenResultBuilder.StreamState m_state;
    private final List<Token> m_pre;
    private final TokenStreamer m_s1;
    private final TokenStreamer m_s2;
    private List<Token> m_tail;
    private Iterator<Token> m_iterator;

    DynTokenStreamer(List<Token> pre) {
      this(pre, null, null);
    }

    DynTokenStreamer(TokenStreamer s1) {
      this(null, s1, null);
    }

    DynTokenStreamer(TokenStreamer s1,
      TokenStreamer s2) {
      this(null, s1, s2);
    }

    DynTokenStreamer(List<Token> pre,
      TokenStreamer s1) {
      this(pre, s1, null);
    }

    DynTokenStreamer(List<Token> pre,
      TokenStreamer s1,
      TokenStreamer s2) {
      super();
      m_pre = pre;
      m_s1 = s1;
      m_s2 = s2;
    }

    public boolean closeWhenDone() {
      return false;
    }

    void appendToken(Token token) {
      if(m_tail == null) {
        m_tail = new ArrayList<Token>();
      }
      m_tail.add(token);
    }

      public Token nextToken() {
      Token ret = null;
      while(true) {
        switch(m_state) {
          case INIT_TOKENS:
            if(m_pre == null) {
              m_state = TokenResultBuilder.StreamState.S1;
              break;
            }
            if(m_iterator == null) {
              m_iterator = m_pre.iterator();
            }
            if(m_iterator.hasNext()) {
              return m_iterator.next();
            }
            else {
              m_iterator = null;
              m_state = TokenResultBuilder.StreamState.S1;
            }
            break;
          case S1:
            if(m_s1 == null) {
              m_state = TokenResultBuilder.StreamState.S2;
              break;
            }
            ret = m_s1.nextToken();
            if(ret == null) {
              m_state = TokenResultBuilder.StreamState.S2;
              break;
            }
            else {
              return ret;
            }
          case S2:
            if(m_s2 == null) {
              m_state = TokenResultBuilder.StreamState.TAIL_TOKENS;
              break;
            }
            ret = m_s2.nextToken();
            if(ret == null) {
              m_state = TokenResultBuilder.StreamState.TAIL_TOKENS;
              break;
            }
            else {
              return ret;
            }
          case TAIL_TOKENS:
            if(m_tail == null) {
              m_iterator = null;
              m_state = TokenResultBuilder.StreamState.DONE;
              return null;
            }
            if(m_iterator == null) {
              m_iterator = m_tail.iterator();
            }
            if(m_iterator.hasNext()) {
              return m_iterator.next();
            }
            else {
              m_iterator = null;
              m_state = TokenResultBuilder.StreamState.DONE;
              return null;
            }
         }
      }
    }

  }
}
