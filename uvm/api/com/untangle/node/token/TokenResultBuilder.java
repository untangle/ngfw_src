/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.token;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class to assemble a TokenResult incrementally.  Handles the
 * impendence mis-match between Tokens and TokenStreamers.  The order
 * in-which tokens/streamers are added is maintained in the {@link
 * #getTokenResult returned result}.
 */
public class TokenResultBuilder 
{
    private List<Token> m_forClientTokens;
    private List<Token> m_forServerTokens;

    private DynTokenStreamer m_forClientStreamer;
    private DynTokenStreamer m_forServerStreamer;

    public TokenResultBuilder() 
    {
    }

    /**
     * Add a token intended to go from
     * server to client
     */
    public void addTokenForClient(Token token) 
    {
        addToken(token, true);
    }
    /**
     * Add a token intended to go from
     * client to server
     */
    public void addTokenForServer(Token token) 
    {
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
        return list.toArray(new Token[list.size()]);
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
