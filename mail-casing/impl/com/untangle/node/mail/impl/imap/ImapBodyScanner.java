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

import static com.untangle.node.util.Ascii.CLOSE_BRACKET_B;
import static com.untangle.node.util.Ascii.LT_B;
import static com.untangle.node.util.Ascii.OPEN_BRACKET_B;
import static com.untangle.node.util.Ascii.OPEN_PAREN_B;
import static com.untangle.node.util.Ascii.PERIOD_B;

import java.nio.ByteBuffer;

import com.untangle.node.mail.papi.imap.IMAPTokenizer;
import com.untangle.node.util.UtLogger;

/**
 * Logicaly part of the "ImapServerParser", but broken out so we
 * can test parsing logic from independent programs.
 * <br><br>
 * Responsible for finding message boundaries.
 */
public class ImapBodyScanner {

    private static final int S01= 0;//Skipping Literal
    private static final int S02= 1;//Draining Body
    private static final int S03= 2;//Scanning New Line
    private static final int S04= 3;//Saw "FETCH"
    private static final int S05= 4;//Skipping to end of line
    private static final int S06= 5;//Saw "FETCH .. ("
    private static final int S07= 6;//Saw "FETCH...(...BODY"
    private static final int S08= 7;//Saw "FETCH...(...RFC822"
    private static final int S09= 8;//Saw "FETCH .. (...BODY["
    private static final int S10= 9;//Saw "FETCH .. (...BODY[]"
    private static final int S11=10;//Checking for "BODY.PEEK"
    private static final int S12=11;//Skipping current Token, then going to s5 or s6

    private static final String[] STATE_STRINGS = {
        "Skipping Literal",
        "Draining Body",
        "Scanning New Line",
        "Saw \"FETCH\"",
        "Skipping to end of line",
        "Saw \"FETCH .. (\"",
        "Saw \"FETCH...(...BODY\"",
        "Saw \"FETCH...(...RFC822\"",
        "Saw \"FETCH .. (...BODY[\"",
        "Saw \"FETCH .. (...BODY[]\"",
        "Checking for \"BODY.PEEK\"",
        "Skipping current Token, then going to s6"
    };


    private static final int T01 =  0;//EOL
    private static final int T02 =  1;//"FETCH"
    private static final int T03 =  2;//"BODY"
    private static final int T04 =  3;//"RFC822"
    private static final int T05 =  4;//"PEEK"
    private static final int T06 =  5;//(word)
    private static final int T07 =  6;//(qstr)
    private static final int T08 =  7;//(literal)
    private static final int T09 =  8;//OB "["
    private static final int T10 =  9;//CB "]"
    private static final int T11 = 10;//LT "<"
    private static final int T12 = 11;//Paren "("
    private static final int T13 = 12;//Dot "."
    private static final int T14 = 13;//(delim)
    private static final int T15 = 14;//SYNTHETIC TOKEN FOR LOGGING (meaning, previous was a literal)
    private static final int T16 = 15;//SYNTHETIC TOKEN FOR LOGGING (meaning, previous was a msg)
    private static final int T17 = 16;//SYNTHETIC TOKEN FOR LOGGING (meaning, initial state)

    private static final String[] TOKEN_STRINGS = {
        "EOL",
        "\"FETCH\"",
        "\"BODY\"",
        "\"RFC822\"",
        "\"PEEK\"",
        "(word)",
        "(qstr)",
        "(literal)",
        "OB \"[\"",
        "CB \"]\"",
        "LT \"<\"",
        "Paren \"(\"",
        "Dot \".\"",
        "(delim)",
        "previous was a literal",
        "previous was a msg",
        "initial state"
    };

    private static final int A00 =  0;
    private static final int A01 =  1;
    private static final int A02 =  2;
    private static final int A03 =  3;
    private static final int A04 =  4;
    private static final int A05 =  5;
    private static final int A06 =  6;
    private static final int A07 =  7;
    private static final int A08 =  8;
    private static final int A09 =  9;
    private static final int A10 = 10;
    private static final int A11 = 11;
    private static final int A12 = 12;
    private static final int A13 = 13;
    private static final int A14 = 14;
    private static final int A15 = 15;

    private static final int[][] TRAN_TBL = {
        /*            T01  T02  T03  T04  T05  T06  T07  T08  T09  T10  T11  T12  T13  T14
                      /* S01 */    {A00, A00, A00, A00, A00, A00, A00, A00, A00, A00, A00, A00, A00, A00},
                      /* S02 */    {A00, A00, A00, A00, A00, A00, A00, A00, A00, A00, A00, A00, A00, A00},
                      /* S03 */    {A04, A02, A01, A01, A01, A01, A01, A03, A04, A04, A04, A04, A04, A04},
                      /* S04 */    {A06, A04, A04, A04, A04, A04, A04, A09, A04, A04, A04, A05, A04, A04},
                      /* S05 */    {A06, A04, A04, A04, A04, A04, A04, A09, A04, A04, A04, A04, A04, A04},
                      /* S06 */    {A06, A04, A07, A08, A04, A04, A04, A09, A04, A04, A04, A04, A04, A04},
                      /* S07 */    {A06, A05, A04, A08, A05, A05, A05, A10, A11, A05, A05, A05, A12, A05},
                      /* S08 */    {A06, A05, A07, A04, A05, A05, A05, A13, A05, A05, A05, A05, A14, A05},
                      /* S09 */    {A06, A14, A14, A14, A14, A14, A14, A10, A05, A15, A05, A05, A14, A05},
                      /* S10 */    {A06, A06, A07, A08, A05, A05, A05, A13, A05, A05, A05, A05, A05, A05},
                      /* S11 */    {A06, A05, A05, A05, A07, A05, A05, A10, A05, A05, A05, A05, A05, A05},
                      /* S12 */    {A06, A05, A05, A05, A05, A05, A05, A05, A05, A05, A05, A05, A05, A05}
    };

    private static final int MAX_TOKENS_BEFORE_FETCH = 8;
    private static final byte[] FETCH_BYTES = "fetch".getBytes();
    private static final byte[] BODY_BYTES = "body".getBytes();
    private static final byte[] RFC822_BYTES = "rfc822".getBytes();
    private static final byte[] PEEK_BYTES = "peek".getBytes();

    private int m_lineWordCount;
    private int m_toSkipLiteral;
    private int m_state = S03;
    private int m_msgLength = -1;
    private int m_pushedStateForLiteral = -1;
    private UtLogger m_logger =
        new UtLogger(ImapBodyScanner.class);
    private final IMAPTokenizer m_tokenizer;


    ImapBodyScanner() {
        m_tokenizer = new IMAPTokenizer();
        changeState(S03, T17);
    }

    /**
     * Get the longest word for this scanner.
     */
    int getLongestWord() {
        return m_tokenizer.getLongestWord();
    }

    private void changeState(int newState,
                             int tokenClass) {
        if(newState != m_state) {
            m_logger.debug("Change state from \"",
                           STATE_STRINGS[m_state],
                           "\" to \"",
                           STATE_STRINGS[newState],
                           "\" on token \"",
                           TOKEN_STRINGS[tokenClass],
                           "\"");
            m_state = newState;
        }
    }

    int getMessageOctetCount() {
        return m_msgLength;
    }

    /**
     * If true is returned, then the caller <b>must</b> make sure
     * to rewind the buffer such that the literal declaration is
     * not sent to the client as part of the stuff we were
     * scanning before we found the message.
     */
    boolean scanForMsgState(ByteBuffer buf) {

        //Reset the message length, as it never caries
        //over
        if(m_state == S02) {
            m_msgLength = -1;
            changeState(S05, T16);
            m_lineWordCount = 0;
        }

        while(buf.hasRemaining()) {
            //Before we tokenize into a literal by-accident,
            //handle literal draining first
            if(m_state == S01) {
                //Skipping literal
                int thisSkip = buf.remaining()>m_toSkipLiteral?
                    m_toSkipLiteral:buf.remaining();
                m_logger.debug("Continuing to skip next: ", thisSkip, " bytes");
                buf.position(buf.position() + thisSkip);
                m_toSkipLiteral-=thisSkip;
                if(m_toSkipLiteral == 0) {
                    if(m_pushedStateForLiteral == -1) {
                        throw new RuntimeException("Draining literal without next state");
                    }
                    changeState(m_pushedStateForLiteral, T15);
                    m_pushedStateForLiteral = -1;
                }
                else {
                    m_logger.debug(m_toSkipLiteral + " bytes remain to be skipped");
                }
                continue;
            }

            //From here, the states "S01" and "S02" are illegal

            //Now, get the next result
            switch(m_tokenizer.next(buf)) {
            case EXCEEDED_LONGEST_WORD:
                m_logger.debug("Exceeded Longest Word.  Skip past whole buffer");
                buf.position(buf.limit());
                return false;
            case NEED_MORE_DATA:
                m_logger.debug("Need more data");
                return false;
            }

            //Falling-out of that switch is equivilant
            //to the "HAVE_TOKEN:" case.  Now classify the token
            int tokenClass = -1;
            switch(m_tokenizer.getTokenType()) {
            case WORD:
                if(m_tokenizer.compareWordAgainst(buf, FETCH_BYTES, true)) {
                    tokenClass = T02;
                }
                else if(m_tokenizer.compareWordAgainst(buf, BODY_BYTES, true)) {
                    tokenClass = T03;
                }
                else if(m_tokenizer.compareWordAgainst(buf, RFC822_BYTES, true)) {
                    tokenClass = T04;
                }
                else if(m_tokenizer.compareWordAgainst(buf, PEEK_BYTES, true)) {
                    tokenClass = T05;
                }
                else {
                    tokenClass = T06;
                }
                break;
            case QSTRING:
                tokenClass = T07;
                break;
            case LITERAL:
                tokenClass = T08;
                break;
            case CONTROL_CHAR:
                if(buf.get(m_tokenizer.getTokenStart()) == OPEN_BRACKET_B) {
                    tokenClass = T09;
                }
                else if(buf.get(m_tokenizer.getTokenStart()) == CLOSE_BRACKET_B) {
                    tokenClass = T10;
                }
                else if(buf.get(m_tokenizer.getTokenStart()) == LT_B) {
                    tokenClass = T11;
                }
                else if(buf.get(m_tokenizer.getTokenStart()) == OPEN_PAREN_B) {
                    tokenClass = T12;
                }
                else if(buf.get(m_tokenizer.getTokenStart()) == PERIOD_B) {
                    tokenClass = T13;
                }
                else {
                    tokenClass = T14;
                }
                break;
            case NEW_LINE:
                tokenClass = T01;
                break;
            default:
                throw new RuntimeException("Unexpected token type: " + m_tokenizer.getTokenType());
            }


            //Now, index into our function table for what to do based
            //on current state and the token class
            switch(TRAN_TBL[m_state][tokenClass]) {
                //================================
            case A00:
                //Assert (illegal)
                throw new RuntimeException("Illegal state right now (" + m_state + ")");
                //================================
            case A01:
                //If line_word_count < MAX, no change in state.
                //Otherwise, change state to s5 ("Look for new line") and reset line_word_count
                if(++m_lineWordCount > MAX_TOKENS_BEFORE_FETCH) {
                    changeState(S05, tokenClass);
                    m_lineWordCount = 0;
                }
                break;
                //================================
            case A02:
                //Change to s4 ("Saw FETCH")
                changeState(S04, tokenClass);
                break;
                //================================
            case A03:
                //Push current state, change to "Skipping Literal" (s1).  Increment line_word_count
                m_pushedStateForLiteral = m_state;
                changeState(S01, tokenClass);
                m_toSkipLiteral = m_tokenizer.getLiteralOctetCount();
                m_lineWordCount++;
                break;
                //================================
            case A04:
                //No change in state
                break;
                //================================
            case A05:
                //Reset line_word_count, change state to s6 ("Saw 'FETCH' (")
                m_lineWordCount = 0;
                changeState(S06, tokenClass);
                break;
                //================================
            case A06:
                //Change state to s3 ("new line").  Reset line_word_count
                m_lineWordCount = 0;
                changeState(S03, tokenClass);
                break;
                //================================
            case A07:
                //Change state to s7 ("saw 'FETCH...(...BODY'")
                changeState(S07, tokenClass);
                break;
                //================================
            case A08:
                //Change state to s8 ("saw 'FETCH...(...RFC822")
                changeState(S08, tokenClass);
                break;
                //================================
            case A09:
                //Push Current state, change state to s1 ("Skipping Literal")
                m_pushedStateForLiteral = m_state;
                changeState(S01, tokenClass);
                m_toSkipLiteral = m_tokenizer.getLiteralOctetCount();
                break;
                //================================
            case A10:
                //Push s6 state as next, change to s1 (skipping literal).
                m_pushedStateForLiteral = S06;
                changeState(S01, tokenClass);
                m_toSkipLiteral = m_tokenizer.getLiteralOctetCount();
                break;
                //================================
            case A11:
                //Change state to s9 ("saw 'FETCH...(...BODY[")
                changeState(S09, tokenClass);
                break;
                //================================
            case A12:
                //Change state to s11 (If next token is "PEEK",
                //change state to s7, otherwise assume failed "BODY")
                changeState(S11, tokenClass);
                break;
                //================================
            case A13:
                //Found Message (change state to s2)
                m_logger.debug("Found body declaration");
                m_msgLength = m_tokenizer.getLiteralOctetCount();
                changeState(S02, tokenClass);
                return true;
                //================================
            case A14:
                //Change state to s12 ("Skip current
                //token, then change state to s5 or s6")
                changeState(S12, tokenClass);
                break;
                //================================
            case A15:
                //Change state to s10 ("saw 'FETCH...(...BODY[]'")
                changeState(S10, tokenClass);
                break;
            default:
                throw new RuntimeException("Unknown action");
            }
        }
        return false;
    }

    /*
    //Test for Bug 961

    private static final String TEST_STRING =
    "* 3250 FETCH (UID 21723 BODY[1]<0> {10240}\r\n" +
    "\r\n" +
    "<html xmlns=\"http://www.w3.org/1999/xhtml\">\r\n" +
    "\r\n" +
    "  <!-- HEADING -->\r\n" +
    "  <head>\r\n" +
    "    <title>Untangle Reports</title>\r\n" +
    "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\"/>\r\n" +
    "\r\n" +
    "\r\n" +
    "<tr bgcolor=\" foo  \r\n";

    public static void main(String[] args) {
    try {
    ImapBodyScanner scanner = new ImapBodyScanner();
    ByteBuffer buf = ByteBuffer.wrap(
    TEST_STRING.getBytes());
    System.out.println("***DEBUG*** (pre) Pos: " +
    buf.position() +
    ", limit: " +
    buf.limit());
    System.out.println("***DEBUG*** Result: " + scanner.scanForMsgState(buf));
    System.out.println("***DEBUG*** (post) Pos: " +
    buf.position() +
    ", limit: " +
    buf.limit());
    }
    catch(Exception ex) {
    ex.printStackTrace();
    }
    }
    */
}

