/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.mail.impl.imap;
import static com.untangle.node.util.Ascii.PLUS_B;
import static com.untangle.node.util.Ascii.STAR_B;

import java.nio.ByteBuffer;

import com.untangle.node.mail.papi.imap.IMAPTokenizer;

/**
 * Abstract class for a <b>Tok</b>en <b>Mon</b>itor.  A TokMon
 * is placed into an Imap byte stream, to observe the client/server
 * interaction.  Unlike the role of the Parser/Unparser, the TokMon
 * does not (a) modify the stream or (b) create TAPI tokens.
 * <br><br>
 * TokMons are not part of any <i>public</i> API, but have been broken
 * into a little pattern since it is likely that we'll have to alter
 * the things we look for in IMAP as we gain understanding of the protocol.
 * <br><br>
 * A note about counts and line types.  There are a few methods which
 * subclasses may call to determine the current type of line, and the number
 * of tokens on that line.  These values hold true <b>as the terminating EOL is encountered</b>
 *
 */
abstract class TokMon {

    /**
     * Enumeration of the different Client Request types
     * (see RFC 3501 sec 2.2).
     */
    enum ClientReqType {
        CONTINUATION,
        TAGGED,
        UNKNOWN//Positioned at start of new line
    };

    /**
     * Enumeration of the different Server Response types
     * (see RFC 3501 sec 2.2).
     */
    enum ServerRespType {
        CONTINUATION_REQUEST,
        UNTAGGED,
        TAGGED,
        UNKNOWN//Positioned at start of new line
    };

    private ServerRespType m_serverLineType = ServerRespType.UNKNOWN;
    private ClientReqType m_clientLineType = ClientReqType.UNKNOWN;
    private boolean m_clientAtNewLine = true;
    private boolean m_serverAtNewLine = true;
    private boolean m_lastServerLineContReq = false;
    private int m_serverLineTokenCount = 0;
    private int m_clientLineTokenCount = 0;
    private final ImapSessionMonitor m_sessionMonitor;

    /**
     * Construct a new TokMon, driven by the
     * given ImapSessionMonitor
     *
     * @param sesMon the parent caller
     */
    TokMon(ImapSessionMonitor sesMon) {
        this(sesMon, null);
    }

    /**
     * Construct a new TokMon, driven by the
     * given ImapSessionMonitor.  The new ImapSessionMonitor
     * will get its current state from the
     * passed-in TokMon
     *
     * @param sesMon the parent caller
     * @param cloneState a TokMon to clone current state from
     *
     */
    TokMon(ImapSessionMonitor sesMon,
           TokMon cloneState) {

        m_sessionMonitor = sesMon;
        if(cloneState != null) {
            m_serverLineType = cloneState.m_serverLineType;
            m_clientLineType = cloneState.m_clientLineType;
            m_clientAtNewLine = cloneState.m_clientAtNewLine;
            m_serverAtNewLine = cloneState.m_serverAtNewLine;
            m_lastServerLineContReq = cloneState.m_lastServerLineContReq;
            m_serverLineTokenCount = cloneState.m_serverLineTokenCount;
            m_clientLineTokenCount = cloneState.m_clientLineTokenCount;
        }
    }

    /**
     * Get the ImapSessionMonitor associated with this
     * TokMon
     *
     * @return the ImapSessionMonitor
     */
    protected final ImapSessionMonitor getSessionMonitor() {
        return m_sessionMonitor;
    }

    /**
     * Get the count of tokens on the current client request line.
     * This does <b>not</b> include the terminating EOL.
     */
    protected final int getClientRequestTokenCount() {
        return m_clientLineTokenCount;
    }

    /**
     * Get the count of tokens on the current client request line.
     * This does <b>not</b> include the terminating EOL.
     */
    protected final int getServerResponseTokenCount() {
        return m_serverLineTokenCount;
    }

    /**
     * Get the current client request type.  Note that this property
     * still holds true while the terminating EOL is being handled.
     */
    protected final ClientReqType getClientReqType() {
        return m_clientLineType;
    }

    /**
     * Get the current server response type.  Note that this property
     * still holds true while the terminating EOL is being handled.
     */
    protected final ServerRespType getServerRespType() {
        return m_serverLineType;
    }

    /**
     * Call from the ImapSessionMonitor to observe a literal.
     * The token which defined the literal should have
     * already been passed to {@link #handleToken handleToken}
     *
     * @param buf the buffer
     * @param bytesFromPosAsLiteral bytes (from buffer's position)
     *        containing bytes from the literal
     * @param client true if this literal is from the client.
     *
     * @return true if the ImapSessionMonitor should declare this
     *         session unparsable, and abandon scanning (i.e. if
     *         encryption is encountered).
     */
    final boolean handleLiteral(ByteBuffer buf, int bytesFromPosAsLiteral, boolean client) {
        if(client) {
            return handleLiteralFromClient(buf, bytesFromPosAsLiteral);
        }
        else {
            return handleLiteralFromServer(buf, bytesFromPosAsLiteral);
        }
    }


    /**
     * Handle a token, called by ImapSessionMonitor.
     *
     * @param tokenizer the tokenizer
     * @param buf the buffer
     * @param fromClient true if from the client
     *
     * @return true if the ImapSessionMonitor should declare this
     *         session unparsable, and abandon scanning (i.e. if
     *         encryption is encountered).
     */
    final boolean handleToken(IMAPTokenizer tokenizer,
                              ByteBuffer buf,
                              boolean fromClient) {
        if(fromClient) {
            previewTokenFromClient(tokenizer, buf);
            return handleTokenFromClient(tokenizer, buf);
        }
        else {
            previewTokenFromServer(tokenizer, buf);
            return handleTokenFromServer(tokenizer, buf);
        }
    }

    /**
     * This method is called before {@link #handleTokenFromClient handleTokenFromClient}.
     * If subclasses override, <b>make sure to call super</b>
     *
     * @param tokenizer the tokenizer
     * @param buf the buffer
     */
    protected void previewTokenFromClient(IMAPTokenizer tokenizer,
                                          ByteBuffer buf) {
        if(m_clientAtNewLine) {
            //Two EOLs in a row we'll skip
            if(tokenizer.isTokenEOL()) {
                //Just leave the state as-is
                return;
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
    }

    /**
     * This method is called before {@link handleTokenFromServer handleTokenFromServer}.
     * If subclasses override, <b>make sure to call super</b>
     *
     * @param tokenizer the tokenizer
     * @param buf the buffer
     */
    protected void previewTokenFromServer(IMAPTokenizer tokenizer,
                                          ByteBuffer buf) {
        if(m_serverAtNewLine) {
            //This means the last token we saw was an EOL.

            //Two EOLs in a row we'll skip
            if(tokenizer.isTokenEOL()) {
                //Just leave the state as-is
                return;
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
    }

    /**
     * Handle portion of an IMAP literal.  Note that the literal
     * declaration ("{nnn}EOL") should already have been passed
     * to {@link #handleTokenFromClient handleTokenFromClient}.
     *
     * @param tokenizer the tokenizer
     * @param buf the buffer
     *
     * @return true if the ImapSessionMonitor should declare this
     *         session unparsable, and abandon scanning (i.e. if
     *         encryption is encountered).
     */
    protected boolean handleLiteralFromClient(ByteBuffer buf, int bytesFromPosAsLiteral) {
        return false;
    }

    /**
     * Handle portion of an IMAP literal.  Note that the literal
     * declaration ("{nnn}EOL") should already have been passed
     * to {@link handleTokenFromServer handleTokenFromServer}.
     *
     * @param tokenizer the tokenizer
     * @param buf the buffer
     *
     * @return true if the ImapSessionMonitor should declare this
     *         session unparsable, and abandon scanning (i.e. if
     *         encryption is encountered).
     */
    protected boolean handleLiteralFromServer(ByteBuffer buf, int bytesFromPosAsLiteral) {
        return false;
    }

    /**
     * Handle a server token
     *
     * @param tokenizer the tokenizer
     * @param buf the buffer
     *
     * @return true if the ImapSessionMonitor should declare this
     *         session unparsable, and abandon scanning (i.e. if
     *         encryption is encountered).
     */
    protected boolean handleTokenFromServer(IMAPTokenizer tokenizer,
                                            ByteBuffer buf) {
        return false;
    }

    /**
     * Handle a client token
     *
     * @param tokenizer the tokenizer
     * @param buf the buffer
     *
     * @return true if the ImapSessionMonitor should declare this
     *         session unparsable, and abandon scanning (i.e. if
     *         encryption is encountered).
     */
    protected boolean handleTokenFromClient(IMAPTokenizer tokenizer,
                                            ByteBuffer buf) {
        return false;
    }
}
