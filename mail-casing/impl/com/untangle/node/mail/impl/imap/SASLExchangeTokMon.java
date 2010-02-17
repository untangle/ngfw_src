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

import static com.untangle.node.util.Ascii.STAR;

import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.imap.IMAPTokenizer;
import com.untangle.node.sasl.SASLObserver;

/**
 * Class which understands the semantics of Imap's SASL
 * profile.  Takes care of the client cancel ("*")
 * as well as buffering the Base64 encoded lines
 * back and forth.  Passes only the data
 * in pure SASL format (i.e. removing the encoding/semantics
 * of the IMAP profile RFC 3501 sec 6.2.2) to the
 * passed-in {@link com.untangle.node.sasl.SASLObserver SASLObserver}.
 * <br><br>
 * This class is intended to be swapped-in
 * after AUTHENTICATE XXXXX has been issued.  As-such
 * there is no use to the {@link #testCommand testCommand}
 * method (which simply returns false).
 */
class SASLExchangeTokMon
    extends CommandTokMon {

    /**
     * Enum of the server's final disposition
     * to the SASL transaction (RFC 3501 Section 2.2.2).
     */
    enum SASLCompletion {
        OK,
        NO,
        BAD
    };

    private final Logger m_logger =
        Logger.getLogger(SASLExchangeTokMon.class);

    private StringBuilder m_clientSB;
    private StringBuilder m_serverSB;
    private final SASLObserver m_observer;

    SASLExchangeTokMon(ImapSessionMonitor sesMon,
                       TokMon state,
                       SASLObserver observer) {
        super(sesMon, state);

        m_observer = observer;

        m_logger.debug("Created");
    }

    @Override
    protected final boolean testCommand(IMAPTokenizer tokenizer, ByteBuffer buf) {
        return false;
    }

    @Override
    protected final boolean handleTokenFromServer(IMAPTokenizer tokenizer,
                                                  ByteBuffer buf) {


        if(getCommandState() == CommandState.NOT_ACTIVE) {
            return false;
        }
        else if(getCommandState() == CommandState.OPEN) {
            //Nuke the first token of a line, as
            //this should be a "+"
            if(getServerResponseTokenCount() == 1) {
                return false;
            }
            if(tokenizer.isTokenEOL()) {
                //End of this line
                if(m_serverSB == null || m_serverSB.length() == 0) {
                    //This can occur with "S/KEY"
                    m_logger.debug("Blank line from server during SASL transaction");
                    m_serverSB = null;
                }
                byte[] bytes = base64Decode(m_serverSB);
                m_serverSB = null;
                if(bytes != null) {
                    return serverMessage(tokenizer, buf, bytes);
                }
            }
            else {
                //keep appending line
                if(m_serverSB == null) {
                    m_serverSB = new StringBuilder();
                }
                m_serverSB.append(tokenizer.tokenToStringDebug(buf));
            }
            return false;
        }
        else {
            //CLOSING state
            if(!tokenizer.isTokenEOL() && getServerResponseTokenCount() == 2) {
                //This token should be the final disposition
                //of this transaction
                if(tokenizer.compareWordAgainst(buf, OK_BYTES, true)) {
                    return transactionComplete(SASLCompletion.OK);
                }
                else if(tokenizer.compareWordAgainst(buf, NO_BYTES, true)) {
                    return transactionComplete(SASLCompletion.NO);
                }
                else if(tokenizer.compareWordAgainst(buf, BAD_BYTES, true)) {
                    return transactionComplete(SASLCompletion.BAD);
                }
                else {
                    m_logger.warn("Unknown Server response disposition \"" +
                                  tokenizer.tokenToStringDebug(buf) + "\"");
                }
            }
            return false;
        }
    }

    @Override
    protected final boolean handleTokenFromClient(IMAPTokenizer tokenizer,
                                                  ByteBuffer buf) {
        if(getCommandState() == CommandState.NOT_ACTIVE) {
            return false;
        }

        //Filter out (illegal) nested commands
        //or the opening request
        if(getClientReqType() != ClientReqType.CONTINUATION) {
            return false;
        }

        //It is ambiguiuous if we're in "CLOSING" yet
        //we get client bytes.  Just assume that the state
        //must be "OPEN"

        //Look for EOL.  This is the end of a line.  Take what
        //we've buffered and call it a client token.
        if(tokenizer.isTokenEOL()) {
            if(m_clientSB == null || m_clientSB.length() == 0) {
                m_logger.debug("Blank line from client during SASL transaction");
                m_clientSB = null;
            }
            if(m_clientSB.length() == 1 && m_clientSB.charAt(0) == STAR) {
                m_logger.debug("Client cancel");
                clientCanceled();
                m_clientSB = null;
            }
            else {
                byte[] bytes = base64Decode(m_clientSB);
                m_clientSB = null;
                if(bytes != null) {
                    return clientMessage(tokenizer, buf, bytes);
                }
            }
        }
        else {
            if(m_clientSB == null) {
                m_clientSB = new StringBuilder();
            }
            m_clientSB.append(tokenizer.tokenToStringDebug(buf));
        }
        return false;
    }

    private byte[] base64Decode(StringBuilder sb) {
        if(sb == null) {
            return null;
        }
        try {
            return Base64.decodeBase64(sb.toString().getBytes());
        }
        catch(Exception ex) {
            m_logger.warn("Exception base 64 decoding \"" + sb.toString() + "\"", ex);
            return null;
        }
    }

    /**
     * Convienence method for subclasses to cause them to be
     * {@link com.untangle.node.mail.impl.imap.ImapSessionMonitor#replaceMonitor removed}
     * from the ImapSessionMonitor and replaced with an AUTHENTICATETokMon.
     *
     */
    protected final void swapBackAUTHENTICATEHandler() {
        getSessionMonitor().replaceMonitor(this, new AUTHENTICATETokMon(
                                                                        getSessionMonitor(), this));
    }

    /**
     * Handle a client message.  These bytes are in pure SASL
     * form, without the base64 decoding and continuation-stuff
     * from the IMAP/SASL profile.
     *
     * @param tokenizer the tokenizer (not really needed).
     * @param tokenizer the buffer (not really needed).
     * @param message the bytes of the message
     *
     * @return true if the ImapSessionMonitor should declare this
     *         session unparsable, and abandon scanning (i.e. if
     *         encryption is encountered).
     */
    protected boolean clientMessage(IMAPTokenizer tokenizer,
                                    ByteBuffer buf,
                                    byte[] message) {

        if(m_observer.clientData(ByteBuffer.wrap(message))) {

            if(m_observer.exchangeAuthIDFound() == SASLObserver.FeatureStatus.YES) {
                getSessionMonitor().setUserName(m_observer.getAuthID());
            }
        }
        boolean ret = isChannelSecured();
        if(ret) {
            m_logger.debug("[clientMessage()] Returning true (channel encrypted) upon advice of SASLObserver");
        }
        return ret;
    }

    /**
     * Handle a server message.  These bytes are in pure SASL
     * form, without the base64 decoding and continuation-stuff
     * from the IMAP/SASL profile.
     *
     * @param tokenizer the tokenizer (not really needed).
     * @param tokenizer the buffer (not really needed).
     * @param message the bytes of the message
     *
     * @return true if the ImapSessionMonitor should declare this
     *         session unparsable, and abandon scanning (i.e. if
     *         encryption is encountered).
     */
    protected boolean serverMessage(IMAPTokenizer tokenizer,
                                    ByteBuffer buf,
                                    byte[] message) {

        if(m_observer.serverData(ByteBuffer.wrap(message))) {
            if(m_observer.exchangeAuthIDFound() == SASLObserver.FeatureStatus.YES) {
                getSessionMonitor().setUserName(m_observer.getAuthID());
            }
        }
        boolean ret = isChannelSecured();
        if(ret) {
            m_logger.debug("[serverMessage()] Returning true (channel encrypted) upon advice of SASLObserver");
        }
        return isChannelSecured();
    }

    /**
     * Called after a client issues a SASL cancel ("*").
     *
     * Default implementation calls {@link #swapBackAUTHENTICATEHandler swapBackAUTHENTICATEHandler}.
     */
    protected void clientCanceled() {
        swapBackAUTHENTICATEHandler();
    }

    /**
     * Called when the server's final disposition of the command
     * is determined (i.e. the non-continuation-request line corresponding
     * to the opening client TAG).
     *
     * Default implementation calls {@link #swapBackAUTHENTICATEHandler swapBackAUTHENTICATEHandler}.
     *
     * @param compl the server's response to the client's SASL command.
     */
    protected boolean transactionComplete(SASLCompletion compl) {
        swapBackAUTHENTICATEHandler();
        boolean ret = isChannelUnsecure()?false:true;
        if(ret) {
            m_logger.debug("[transactionComplete()] Returning true (channel maybe encrypted) upon advice of SASLObserver");
        }
        return ret;
    }

    private boolean isChannelSecured() {
        return
            m_observer.exchangeUsingPrivacy() == SASLObserver.FeatureStatus.YES ||
            m_observer.exchangeUsingIntegrity() == SASLObserver.FeatureStatus.YES;
    }
    private boolean isChannelUnsecure() {
        return
            m_observer.exchangeUsingPrivacy() == SASLObserver.FeatureStatus.NO &&
            m_observer.exchangeUsingIntegrity() == SASLObserver.FeatureStatus.NO;
    }


}

