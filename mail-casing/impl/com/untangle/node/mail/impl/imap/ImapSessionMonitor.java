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

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.imap.IMAPTokenizer;
import com.untangle.node.sasl.SASLObserver;
import com.untangle.node.sasl.SASLObserverFactory;

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
 * <br><br>
 * An ImapSessionMonitor works by passing tokens and literals
 * to an internal collection of {@link com.untangle.node.mail.impl.ima.TokMon TokMon}s.
 * The design of the TokMon API was to prevent having to duplicate and have
 * each "independent area of interest" re-tokenize the buffers.  Instead,
 * the ImapSessionMonitor performs tokenizing, and passes each token to
 * its consisuent TokMons.
 */
class ImapSessionMonitor {

    private final Logger m_logger =
        Logger.getLogger(ImapSessionMonitor.class);

    private String m_userName;
    private IMAPTokenizer m_fromServerTokenizer;
    private IMAPTokenizer m_fromClientTokenizer;
    private IntHolder m_literalFromServerCount;
    private IntHolder m_literalFromClientCount;

    private TokMon[] m_tokMons;

    ImapSessionMonitor() {
        m_fromServerTokenizer = new IMAPTokenizer();
        m_fromClientTokenizer = new IMAPTokenizer();
        m_literalFromServerCount = new IntHolder();
        m_literalFromClientCount = new IntHolder();
        m_tokMons = new TokMon[] {
            new AUTHENTICATETokMon(this),
            new LOGINTokMon(this),
            new STARTTLSTokMon(this)
        };
    }

    boolean hasUserName() {
        return m_userName != null;
    }

    /**
     * Get the UserName, as observed by one of the TokMons.
     * This may be null for the entire duration of the
     * session
     *
     * @return the username, or null.
     */
    String getUserName() {
        return m_userName;
    }

    /**
     * Call to set the UserName.  This is intended for use
     * by the various {@link com.untangle.node.mail.impl.imap.SASLTransactionTokMon SASL monitors}
     * or the vanilla {@link com.untangle.node.mail.impl.imap.LOGINTokMon LOGIN command TokMon}
     *
     * @param userName the userName
     */
    void setUserName(String userName) {
        m_logger.debug("Setting UserName to \"" + userName + "\"");
        m_userName = userName;
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
     * Replace one TokMon with another.  This is used for
     * the situation like SASL, where one monitor detects the start
     * of a SASL negotiation, and delegates to the specfic mechanism's
     * Monitor.  When complete, the reverse replacement can be made.
     */
    void replaceMonitor(TokMon old, TokMon replacement) {
        for(int i = 0; i<m_tokMons.length; i++) {
            if(m_tokMons[i] == old) {
                //        m_logger.debug("Replacing \"" + old + "\" monitor with \"" +
                //          replacement + "\"");
                m_tokMons[i] = replacement;
                break;
            }
        }
    }

    SASLExchangeTokMon getSASLMonitor(TokMon currentMonitor,
                                      String mechanismName) {

        if(mechanismName == null) {
            m_logger.debug("Null SASL mechanism.  Return null");
            return null;
        }

        SASLObserver observer = SASLObserverFactory.createObserverForMechanism(mechanismName);

        if(observer != null) {
            m_logger.debug("Found SASLObserver for mechanism \"" + mechanismName + "\"");
            return new SASLExchangeTokMon(this, currentMonitor, observer);
        }

        m_logger.warn("Unable to provide SASL handler for mechanism \"" +
                      mechanismName + "\".  Punt");
        //TODO bscott we *could* at least see if server rejects *then*
        //punt, but that it likely overkill
        return null;
    }

    private boolean handleBytes(final ByteBuffer buf,
                                final boolean fromClient) {

        TokMon[] tokMons = m_tokMons;

        final IMAPTokenizer tokenizer = fromClient?
            m_fromClientTokenizer:m_fromServerTokenizer;

        final IntHolder intHolder = fromClient?
            m_literalFromClientCount:m_literalFromServerCount;

        while(buf.hasRemaining()) {
            if(intHolder.val > 0) {
                int toSkip = intHolder.val > buf.remaining()?
                    buf.remaining():intHolder.val;
                for(TokMon tm : tokMons) {
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
                return false;
            }
            if(result == IMAPTokenizer.IMAPNextResult.EXCEEDED_LONGEST_WORD) {
                m_logger.warn("Exceeded longest WORD.  Assume some encryption and enter passthru");
                return true;
            }

            for(TokMon tm : tokMons) {
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
}






class STARTTLSTokMon
    extends TokMon {

    private static final byte[] STARTTLS_BYTES = "starttls".getBytes();

    private final Logger m_logger =
        Logger.getLogger(STARTTLSTokMon.class);

    STARTTLSTokMon(ImapSessionMonitor sesMon) {
        super(sesMon);
        m_logger.debug("Created");
    }

    protected boolean handleTokenFromClient(IMAPTokenizer tokenizer,
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



class LOGINTokMon
    extends TokMon {

    private static final byte[] LOGIN_BYTES = "login".getBytes();
    private static final int MAX_REASONABLE_UID_AS_LITERAL = 1024*2;

    private enum LTMState {
        NONE,
        TAGGED_SUSPECT,
        LOGIN_FOUND
    };

    private final Logger m_logger =
        Logger.getLogger(LOGINTokMon.class);

    private LTMState m_state = LTMState.NONE;
    private byte[] m_literalUID;
    private int m_nextLiteralPos;

    LOGINTokMon(ImapSessionMonitor sesMon) {
        super(sesMon);
        m_logger.debug("Created");
    }



    protected boolean handleLiteralFromClient(ByteBuffer buf, int bytesFromPosAsLiteral) {
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
            setSessionUserName(new String(m_literalUID));
        }
        return false;
    }

    private void setSessionUserName(String userName) {
        if(userName != null) {
            m_logger.debug("Found username \"" + userName + "\" in LOGIN authentication");
            getSessionMonitor().setUserName(userName);
        }
    }

    protected boolean handleTokenFromClient(IMAPTokenizer tokenizer,
                                            ByteBuffer buf) {

        //Quick bypass for impossible lines
        if(getClientRequestTokenCount() > 3) {
            m_state = LTMState.NONE;
            return false;
        }

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
                setSessionUserName(tokenizer.getWordAsString(buf));
                break;
            case QSTRING:
                setSessionUserName(new String(tokenizer.getQStringToken(buf)));
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
                String ctlUserName = new String(new byte[] {buf.get(tokenizer.getTokenStart())});
                m_logger.warn("Username is also a control character \"" + ctlUserName + "\" (?!?)");
                setSessionUserName(ctlUserName);
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


/**
 * Looks for the AUTHENTICATE command,
 * then replaces itself with an appropriate
 * SASL TokMon
 */
class AUTHENTICATETokMon
    extends CommandTokMon {

    private static final byte[] AUTHENTICATE_BYTES =
        "authenticate".getBytes();

    private final Logger m_logger =
        Logger.getLogger(AUTHENTICATETokMon.class);

    private StringBuilder m_mechNameSB;

    AUTHENTICATETokMon(ImapSessionMonitor sesMon) {
        super(sesMon);
        m_logger.debug("Created");
    }
    AUTHENTICATETokMon(ImapSessionMonitor sesMon,
                       TokMon tokMon) {
        super(sesMon, tokMon);
        m_logger.debug("Created");
    }

    @Override
    protected final boolean testCommand(IMAPTokenizer tokenizer,
                                        ByteBuffer buf) {
        return tokenizer.compareWordAgainst(buf, AUTHENTICATE_BYTES, true);
    }


    @Override
    protected boolean handleTokenFromServer(IMAPTokenizer tokenizer,
                                            ByteBuffer buf) {
        //We never care about this
        return false;
    }

    protected boolean handleTokenFromClient(IMAPTokenizer tokenizer,
                                            ByteBuffer buf) {
        //Instances of this class will only care about the "OPEN"
        //state, as "NONE" means we're not in an AUTHENTICATE
        //command, and "CLOSING" means we've been swapped-back
        //
        if(getCommandState() == CommandState.OPEN) {
            //Make sure not to grab the "AUTHENTICATE"
            //word itself
            if(getClientRequestTokenCount() <= 2) {
                return false;
            }
            //Things are tricky/nasty, as I'm not sure
            //if our IMAP tokens can apear in a mechanism
            //name.  As-such, we do our old "trick" to
            //accumulate a ByteByffer
            if(tokenizer.isTokenEOL()) {
                if(m_mechNameSB == null || m_mechNameSB.length() == 0) {
                    m_logger.warn("Unable to determine AUTHENTICATE mechanism.  Assume " +
                                  "worst case that this channel will become encrypted and " +
                                  "punt");
                    return true;
                }
                String mechName = m_mechNameSB.toString();
                m_logger.debug("SASL Mechanism name: \"" + mechName + "\"");

                SASLExchangeTokMon newMon = getSessionMonitor().getSASLMonitor(
                                                                               this,
                                                                               mechName);

                if(newMon != null) {
                    getSessionMonitor().replaceMonitor(this, newMon);
                    return false;
                }
                else {
                    m_logger.warn("Unknown SASL mechanism \"" +
                                  mechName + "\".  Give up on this session (passthru)");
                    return true;
                }
            }
            else {
                if(m_mechNameSB == null) {
                    m_mechNameSB = new StringBuilder();
                }
                m_mechNameSB.append(tokenizer.tokenToStringDebug(buf));
            }
        }
        return false;
    }
}









