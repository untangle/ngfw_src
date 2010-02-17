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
package com.untangle.node.mail.papi.imap;
import static com.untangle.node.util.ASCIIUtil.asciiByteToString;
import static com.untangle.node.util.ASCIIUtil.bbToString;
import static com.untangle.node.util.ASCIIUtil.eatWhitespace;
import static com.untangle.node.util.ASCIIUtil.equalsIgnoreCase;
import static com.untangle.node.util.ASCIIUtil.isLWS;
import static com.untangle.node.util.ASCIIUtil.isNumber;
import static com.untangle.node.util.Ascii.BACK_SLASH_B;
import static com.untangle.node.util.Ascii.CLOSE_BRACE;
import static com.untangle.node.util.Ascii.CLOSE_BRACE_B;
import static com.untangle.node.util.Ascii.CLOSE_BRACKET_B;
import static com.untangle.node.util.Ascii.CLOSE_PAREN_B;
import static com.untangle.node.util.Ascii.CR_B;
import static com.untangle.node.util.Ascii.GT_B;
import static com.untangle.node.util.Ascii.HT_B;
import static com.untangle.node.util.Ascii.LF_B;
import static com.untangle.node.util.Ascii.LT_B;
import static com.untangle.node.util.Ascii.OPEN_BRACE_B;
import static com.untangle.node.util.Ascii.OPEN_BRACKET_B;
import static com.untangle.node.util.Ascii.OPEN_PAREN_B;
import static com.untangle.node.util.Ascii.PERIOD_B;
import static com.untangle.node.util.Ascii.PLUS_B;
import static com.untangle.node.util.Ascii.QUOTE_B;
import static com.untangle.node.util.Ascii.SP_B;
import static com.untangle.node.util.Ascii.STAR_B;

import java.nio.ByteBuffer;

import com.untangle.node.util.BBTokenizer;

/**
 * Class to tokenize a chain of ByteBuffers.  Does
 * not modify the Buffers it visits.
 * <br><br>
 * This class works by calling {@link #next next}
 * with each ByteBuffer of data until {@link #IMAPNextResult NEED_MORE_DATA}
 * is returned.  Note that the IMAPTokenizer is designed
 * to work with the TAPI, in that it can return NEED_MORE_DATA
 * with data remaining in the ByteBuffer.  It is expected that this
 * data will be retuned (along with more data) on any subsquent call
 * to {@link #next next}.  This is a form of pushback.  Since all that
 * is pushed-back are incomplete tokens, the maximum amount of data which
 * is pushed-back is defined as the {@link #getLongestWord longest word},
 * which defaults to 2048.
 * <br><br>
 * Said another way, instances of IMAPTokenizer maintain state only about
 * the current token, not candidate tokens.  This is handy in that callers
 * may choose to perform their own parsing of the ByteBuffer once a given
 * token is encountered.  The Tokenizer when then "pick-up" where ever
 * the ByteBuffer is positioned (note that it is then implicit that
 * the character at the buffer's position -1 was a delimiter).
 * <br><br>
 * Instances of IMAPTokenizer are stateful (obviously not
 * threadsafe).  After each call to {@link #next next}, the
 * {@link #getTokenType token type}, its
 * {@link #getTokenStart starting position} within the buffer,
 * and its {@link #getTokenLength length within the buffer}
 * are set.  Since each call to {@link #next next} advances the
 * position of the buffer (provided the result of {@link #next next}
 * is not NEED_MORE_DATA), the start/length are used to derefference
 * the just-encountered token.  This design is a bit harder to use
 * than returning the actual tokens from each call to {@link #next next},
 * but avoids creating zillions of little objects 99% of which will be
 * ignored.
 * <br><br>
 * There are two more specialized methods for accessing information about
 * the just-encountered token.
 * <ul>
 * <li>{@link getLiteralOctetCount getLiteralOctetCount}
 * returns the number of octets from an IMAP LITERAL (see RFC 3501 Section 4.3).
 * RFC 3501 defines a "LITERAL" in the form <code>{nnn}CRLF</code> where
 * <code>nnn</code> is the number of octets following the CRLF of the octet
 * declaration.  The EOL (CRLF, CR, or LF in this implementation) is <b>not</b>
 * included in the count.  Note that when a {@link #IMAPTT LITERAL} token
 * is encountered, the buffer is advanced past the EOL to the first byte
 * of the octet sequence.   Since LITERALS can contain any characters (including
 * control characters and EOL), callers must "advance" the ByteBuffer chain
 * the number of octets declared by the literal before calling
 * {@link #next next} again.
 * </li>
 * <li>{@link #getQStringToken getQStringToken} returns a QString token
 * (see RFC 3501 section 4.3).  The QString token is stripped of its
 * leading/trailing quotes (&#34;), and any internally escaped quotes
 * (&#92;&#34;) are replaced with quotes.  Reading RFC3501 (page 88) it seems
 * that escapes for quotes within QStrings are illegal, but I'm assuming
 * <i>someone</i> out there assumed it was legal and likely does it.
 * </li>
 * </ul>
 */
public class IMAPTokenizer {

    private static final int DEF_LONGEST_WORD = 1024*8;

    static final byte[] DELIMS = new byte[] {
        HT_B,
        SP_B,
        CR_B,
        LF_B,
        OPEN_BRACKET_B,//[
        CLOSE_BRACKET_B,//]
        OPEN_BRACE_B,//{
        CLOSE_BRACE_B,//}
        OPEN_PAREN_B,//(
        CLOSE_PAREN_B,//)
        QUOTE_B,
        BACK_SLASH_B,
        PLUS_B,
        PERIOD_B,
        LT_B,
        GT_B,
        STAR_B
    };

    static final byte[] EXCLUDE_DELIMS = new byte[] {
        HT_B,
        SP_B,
    };

    static final byte[] QUOTE_DELIMS = new byte[] {
        QUOTE_B,
        BACK_SLASH_B
    };

    /**
     * Enumeration of the various TokenTypes
     */
    public enum IMAPTT {
        /**
         * A simple Word, defined as a sequence of
         * characters bounded by delimiters and not taking
         * the form of a QSTRING or LITERAL definition
         */
        WORD,
        /**
         * A quoted String (see RFC 3501 Section 4.3)
         */
        QSTRING,
        /**
         * A literal declaration (see RFC 3501 Section 4.3)
         */
        LITERAL,
        /**
         * A control delimiter, which also has significance.  These
         * are as follows:
         * <ul>
         * <li>[</li>
         * <li>]</li>
         * <li>{</li>
         * <li>}</li>
         * <li>(</li>
         * <li>)</li>
         * <li>&#34;</li>
         * <li>&#92;</li>
         * <li>*</li>
         * <li>+</li>
         * <li>.</li>
         * <li>&#60;</li>
         * <li>></li>
         * </ul>
         * Other delimiters such as spaces are not significant.  EOL charaters
         * are significant, but returned via their own token (NEW_LINE).
         */
        CONTROL_CHAR,
        /**
         * A new line (CR, LF, CRLF)
         */
        NEW_LINE,
        /**
         * Placeholder for when there is no token type
         */
        NONE
    };

    /**
     * Enum of the results from
     * a call to next
     */
    public enum IMAPNextResult {
        HAVE_TOKEN,
        EXCEEDED_LONGEST_WORD,
        NEED_MORE_DATA
    };


    private BBTokenizer m_tokenizer;
    private IMAPTT m_tt = IMAPTT.NONE;
    private int m_start = -1;
    private int m_len = -1;

    private int m_literalOctetCount = 0;

    public IMAPTokenizer() {
        m_tokenizer = new BBTokenizer();
        m_tokenizer.setLongestWord(DEF_LONGEST_WORD);
        m_tokenizer.setDelims(DELIMS, EXCLUDE_DELIMS);
    }


    //==================================
    // Properties
    //==================================

    /**
     * Set the longest word.  If, while tokenizing,
     * a word is being scanned and is found to be longer
     * than this value, the return of
     * {@link #IMAPNextResult EXCEEDED_LONGEST_WORD}
     * will be returned.
     *
     * @param longestWord the longest word (in bytes)
     */
    public void setLongestWord(int longestWord) {
        m_tokenizer.setLongestWord(longestWord);
    }
    public int getLongestWord() {
        return m_tokenizer.getLongestWord();
    }

    //=====================================
    // Stateful Method about current token
    //=====================================

    /**
     * Helper method, shortcut for
     * <code>m_tokenizer.getTokenType() == IMAPTokenizer.IMAPTT.WORD</code>
     *
     * @return true if current token is a word
     */
    public boolean isTokenWord() {
        return getTokenType() == IMAPTokenizer.IMAPTT.WORD;
    }

    /**
     * Helper method, shortcut for
     * <code>m_tokenizer.getTokenType() == IMAPTokenizer.IMAPTT.LITERAL</code>
     *
     * @return true if current token is a LITERAL
     */
    public boolean isTokenLiteral() {
        return getTokenType() == IMAPTokenizer.IMAPTT.LITERAL;
    }

    /**
     * Helper method, shortcut for
     * <code>m_tokenizer.getTokenType() == IMAPTokenizer.IMAPTT.CONTROL_CHAR</code>
     *
     * @return true if current token is a CONTROL_CHAR
     */
    public boolean isTokenCtl() {
        return getTokenType() == IMAPTokenizer.IMAPTT.CONTROL_CHAR;
    }

    /**
     * Helper method, shortcut for
     * <code>m_tokenizer.getTokenType() == IMAPTokenizer.IMAPTT.NEW_LINE</code>
     *
     * @return true if current token is a NEW_LINE
     */
    public boolean isTokenEOL() {
        return getTokenType() == IMAPTokenizer.IMAPTT.NEW_LINE;
    }
    /**
     * Helper method, shortcut for
     * <code>m_tokenizer.getTokenType() == IMAPTokenizer.IMAPTT.QSTRING</code>
     *
     * @return true if current token is a QSTRING
     */
    public boolean isTokenQSTRING() {
        return getTokenType() == IMAPTokenizer.IMAPTT.QSTRING;
    }


    /**
     * Compare the current CONTROL_CHAR token against
     * the given byte.  Returns true if the
     * current token is a CONTROL_CHAR, and the byte matches.
     */
    public boolean compareCtlAgainstByte(ByteBuffer buf,
                                         byte compare) {
        return isTokenCtl()?
            (buf.get(getTokenStart()) == compare):false;
    }

    public boolean compareWordAgainst(ByteBuffer buf,
                                      byte[] bytes,
                                      boolean ignoreCase) {
        return compareWordAgainst(buf, bytes, 0, bytes.length, ignoreCase);
    }

    /**
     * Convienence method to test the current WORD for equivilance
     * against a reference pattern.
     * <br><br>
     * This will always return false if the {@link #getTokenType current token}
     * is not a WORD
     */
    public boolean compareWordAgainst(ByteBuffer buf,
                                      byte[] bytes,
                                      int start,
                                      int len,
                                      boolean ignoreCase) {

        if(!isTokenWord() || getTokenLength() != len) {
            return false;
        }
        for(int i = 0; i<len; i++) {
            if(ignoreCase) {
                if(!equalsIgnoreCase(bytes[i], buf.get(getTokenStart() + i))) {
                    return false;
                }
            }
            else {
                if(bytes[i] != buf.get(getTokenStart() + i)) {
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * If the type is QSTRING, the begin/end quotes
     * <b>are</b> included in the count (i.e.
     * {@link #getTokenStart the token start index is a quote}
     * as is the {@link #getTokenLength token length}).
     * <br><br>
     * Also, the tokenizer did skip-over slash-escaped quotes,
     * but did <b>not</b> modify the buffer (i.e. if the QString is
     * to be used, it must have its leading/trailing quotes stripped
     * as well as any embedded escaped quotes).  To get the "proper"
     * QString token, use {@link #getQStringToken getQStringToken}.
     * <br><br>
     * Technically, there should not be any forward slashes, CR of LF
     * in a QStrig but we interpret the fwd-slash-quote sequence as
     * an escaped quote, and include CRLFs.
     */
    public IMAPTT getTokenType() {
        return m_tt;
    }

    /**
     * Get the offset within the {@link #next ByteBuffer just scanned}
     * of the current token.  This is inclusive (i.e. if this
     * value is 5 and the {@link #getTokenLength length} is 2, then
     * the token occupies indecies 5 and 6 of the ByteBuffer).
     *
     * @return the token start, or undefined if there is
     *         no current token.
     */
    public int getTokenStart() {
        return m_start;
    }

    /**
     * Get the length of the current token.  This is
     * 1 when the {@link #getTokenType type}
     * is CONTROL_CHAR, zero for a LITERAL token,
     * and the length of the String for a WORD.  If the type
     * is QSTRING, this is <b>inclusive</b> of the open/close
     * quote ({@link #getQStringToken see getQStringToken}).
     * <br><br>
     * If there is no current token, this value is undefined.
     *
     * @return the length of the current token
     */
    public int getTokenLength() {
        return m_len;
    }

    /**
     * Debugging method which returns a reasonable String
     * representation for the current token.
     */
    public String tokenToStringDebug(ByteBuffer buf) {
        switch(m_tt) {
        case WORD:
            return getWordAsString(buf);
        case QSTRING:
            return new String(getQStringToken(buf));
        case LITERAL:
            return new String("<literal> " + getLiteralOctetCount());
        case CONTROL_CHAR:
            return asciiByteToString(buf.get(getTokenStart()));
        case NEW_LINE:
            return "<EOL>";
        case NONE:
            return "<NONE>";
        }
        throw new RuntimeException("Unknown Token type: " + m_tt);
    }

    /**
     * Converts the current WORD to a String.  If the current
     * token type is not WORD, then this returns null.
     *
     * @param buf the buffer which was just passed to {@link #next next}
     */
    public String getWordAsString(ByteBuffer buf) {
        if(m_tt != IMAPTT.WORD) {
            return null;
        }
        ByteBuffer dup = buf.duplicate();
        dup.position(getTokenStart());
        dup.limit(dup.position() + getTokenLength());
        return bbToString(dup);
    }

    /**
     * Only if {@link getTokenType the token type} is LITERAL, this defines
     * the number of octets (bytes) of that literal.  Note also that the
     * position of the buffer has been moved beyond the CRLF which defined
     * the literal in the form <code>{NNN}CRLF</code>
     *
     * @return the literal octet count, provided the current
     *         {@link #getTokenType token type} is {@link #IMAPTT LITERAL}.
     *
     */
    public int getLiteralOctetCount() {
        return m_literalOctetCount;
    }

    /**
     * If the current token is of type QSTRING, this is the String without
     * the leading/trailing quotes as well as any internal
     * escaped quoted fixed.
     *
     * @param bb the Buffer just passed to {@link #next next}
     *
     * @return QString token without quotes.
     */
    public byte[] getQStringToken(ByteBuffer bb) {
        int next = 0;
        byte[] ret = new byte[getTokenLength()-2];
        for(int i = 1; i<getTokenLength()-1; i++) {
            if(bb.get(getTokenStart() + i) == BACK_SLASH_B &&
               bb.limit() < bb.get(getTokenStart() + i + 1) &&
               bb.get(getTokenStart() + i + 1) == QUOTE_B) {
                i++;
            }
            ret[next++] = bb.get(getTokenStart() + i);
        }
        if(next < ret.length) {
            byte[] newRet = new byte[next];
            System.arraycopy(ret, 0, newRet, 0, next);
            ret = newRet;
        }
        return ret;
    }

    /**
     * If {@link getTokenType the token type} is LITERAL, this method
     * instructs the Tokenizer to skip-past the number of octets
     * defined by the literal.  Subsequent calls to {@link #next next}
     * may return NEED_MORE_DATA for several buffers until the literal
     * is consumed.
     * <br><br>
     * If the current token is not a literal, this has no effect.
     */
    public void skipCurrentLiteral() {
        if(isTokenLiteral()) {
            m_tokenizer.skip(getLiteralOctetCount());
        }
    }



    //=======================================
    // Token Consumption methods
    //=======================================



    /**
     * Advance this ByteBuffer to the next token.  If
     * {@link #IMAPNextResult HAVE_TOKEN} is returned,
     * then the {@link #getTokenType getTokenType},
     * {@link #getTokenStart getTokenStart}, and
     * {@link #getTokenLength getTokenLength} methods will return
     * information about the just-encountered token.  The ByteBuffer
     * is advanced just-past the token.
     * <br><br>
     * If the return is {@link #IMAPNextResult NEED_MORE_DATA}, then
     * up to {@link getLongestWord the longest word}-1 number of bytes may
     * be left in the buffer.  Any incomplete tokens are left in the buffer,
     * and duplicate scanning repeats in the subsequent call to
     * {@link #next next}.
     * <br><br>
     * If the return is {@link #IMAPNextResult EXCEEDED_LONGEST_WORD}, the
     * caller must either {@link #setLongestWord increase the longest word},
     * or abandon tokenizing as this will always be returned.
     * <br><br>
     * Values for the token start, type, and length are undefined
     * for the NEED_MORE_DATA and EXCEEDED_LONGEST_WORD returns.
     *
     * @param bb the ByteBuffer to be tokenized
     *
     * @return the result
     */
    public IMAPNextResult next(ByteBuffer bb) {
        //Reset token type
        reset();

        //Get next token
        BBTokenizer.NextResult nextResult = m_tokenizer.next(bb);

        switch(nextResult) {
        case NEED_MORE_DATA:
            m_tt = IMAPTT.NONE;
            return IMAPNextResult.NEED_MORE_DATA;
        case EXCEEDED_LONGEST_WORD:
            m_tt = IMAPTT.NONE;
            return IMAPNextResult.EXCEEDED_LONGEST_WORD;
        case HAVE_DELIM:
            //The delim can mean a few things
            return processDelim(bb);
        case HAVE_WORD:
            m_tt = IMAPTT.WORD;
            m_start = m_tokenizer.tokenStart();
            m_len = m_tokenizer.tokenLength();
            return IMAPNextResult.HAVE_TOKEN;
        }
        throw new RuntimeException(
                                   "Fell from a switch which should have been inclusive");
    }

    /**
     * Resets the Token properties to invalid values
     */
    private void reset() {
        m_tt = IMAPTT.NONE;
        m_start = -1;
        m_len = -1;
    }

    /**
     * Helper method to process a delimiter.
     */
    private IMAPNextResult processDelim(ByteBuffer bb) {
        switch(bb.get(m_tokenizer.tokenStart())) {
            //The simple delim-as-tokens.  Note that SP and HT are never returned
        case OPEN_BRACKET_B:
        case CLOSE_BRACKET_B:
        case OPEN_PAREN_B:
        case CLOSE_PAREN_B:
        case PLUS_B:
        case STAR_B:
        case PERIOD_B:
        case LT_B:
        case GT_B:
        case BACK_SLASH_B:
            //I'm very unclear from the lame IMAP spec if
            //  \" encountered bare should indicate an
            //escape of a QString.  I'll assume "no"
            //
            //UPDATE - Found on page 88 of RFC 3501
        case CLOSE_BRACE_B:
            m_tt = IMAPTT.CONTROL_CHAR;
            m_start = m_tokenizer.tokenStart();
            m_len = m_tokenizer.tokenLength();
            return IMAPNextResult.HAVE_TOKEN;
        case OPEN_BRACE_B:
            return processOpenBrace(bb);
        case CR_B:
            if(!bb.hasRemaining()) {
                bb.position(bb.position()-1);
                m_tt = IMAPTT.NONE;
                return IMAPNextResult.NEED_MORE_DATA;
            }
            if(bb.get() == LF_B) {
                m_tt = IMAPTT.NEW_LINE;
                m_start = bb.position()-2;
                m_len = 2;
                return IMAPNextResult.HAVE_TOKEN;
            }
            else {
                //Rewind the get
                bb.position(bb.position()-1);
                m_tt = IMAPTT.NEW_LINE;
                m_start = bb.position()-1;
                m_len = 1;
                return IMAPNextResult.HAVE_TOKEN;
            }
        case LF_B:
            m_tt = IMAPTT.NEW_LINE;
            m_start = m_tokenizer.tokenStart();
            m_len = 1;
            return IMAPNextResult.HAVE_TOKEN;
        case QUOTE_B:
            int quoteStart = m_tokenizer.tokenStart();
            bb.position(quoteStart+1);
            //Search for the quote end, or the end of the buffer.
            //TODO Should new line determine the implicit end of a broken quote?
            while(bb.hasRemaining()) {
                byte b = bb.get();
                //Check for too long
                if((bb.position() - quoteStart) > m_tokenizer.getLongestWord()) {
                    bb.position(quoteStart);
                    m_tt = IMAPTT.NONE;
                    return IMAPNextResult.EXCEEDED_LONGEST_WORD;
                }
                if(b == BACK_SLASH_B) {
                    //Either grab the escaped character, or let
                    //us fall through to request more bytes
                    if(bb.hasRemaining()) {
                        bb.get();
                    }
                    continue;
                }
                if(b == QUOTE_B) {
                    //we're done
                    m_tt = IMAPTT.QSTRING;
                    m_start = quoteStart;
                    m_len = bb.position()-m_start;
                    return IMAPNextResult.HAVE_TOKEN;
                }
            }
            //Fell through.  Reset and request more bytes
            bb.position(quoteStart);
            m_tt = IMAPTT.NONE;
            return IMAPNextResult.NEED_MORE_DATA;
        }
        throw new RuntimeException(
                                   "Fell from a swithc which should have been inclusive");
    }

    /**
     * Helper method to process an open brace delimiter ("{"),
     * which may begin a LITERAL declaration.
     */
    private IMAPNextResult processOpenBrace(ByteBuffer bb) {

        //Test:
        //{{
        //{EOF
        //{nEOF
        //{n EOF
        //{ nEOF
        //{nn}EOF
        //{nn}CREOF
        //{nn}LFEOF
        //{nn}CRLFEOF
        //{nn}CRLFxx
        //{xEOF

        //Note the start.  We may have to re-wind to this
        //punt if we cannot complete this literal
        int braceStart = m_tokenizer.tokenStart();

        BBTokenizer.NextResult nextResult = m_tokenizer.next(bb);
        switch(nextResult) {

        case NEED_MORE_DATA:
            //Re-wind the buffer so next time we re-encounter the brace
            //Test {EOF
            bb.position(braceStart);
            m_tt = IMAPTT.NONE;
            return IMAPNextResult.NEED_MORE_DATA;

        case EXCEEDED_LONGEST_WORD:
            //Leave { consumed and rewind this "too long" word
            //so we encounter the "longest-word" thing next
            bb.position(braceStart);
            m_tt = IMAPTT.CONTROL_CHAR;
            return IMAPNextResult.HAVE_TOKEN;

        case HAVE_DELIM:
            //Re-wind to we re-encounter the token
            //we just consumed next time.  Test "{{"
            bb.position(m_tokenizer.tokenStart());
            m_start = braceStart;
            m_len = 1;
            m_tt = IMAPTT.CONTROL_CHAR;
            return IMAPNextResult.HAVE_TOKEN;
        }

        //If we're here, then we have a word.  Check
        //if the word is all numbers (permit LWS)
        for(int i = 0; i<m_tokenizer.tokenLength(); i++) {
            if(!(isNumber(bb.get(m_tokenizer.tokenStart() + i)) ||
                 isLWS(bb.get(m_tokenizer.tokenStart() + i)))) {
                //Not a number.  Rewind it and return later
                //as a simple WORD
                bb.position(m_tokenizer.tokenStart());
                m_start = braceStart;
                m_len = 1;
                m_tt = IMAPTT.CONTROL_CHAR;
                return IMAPNextResult.HAVE_TOKEN;
            }
        }

        //Parse the number
        int octetCount = 0;
        try {
            ByteBuffer dup = bb.duplicate();
            dup.position(m_tokenizer.tokenStart());
            dup.limit(dup.position() + m_tokenizer.tokenLength());
            String octetString = bbToString(dup);
            octetString.trim();
            octetCount = Integer.parseInt(octetString);
        }
        catch(Exception ex) {
            ex.printStackTrace(System.out);//TODO bscott removeme
            //TODO bscott log this
            bb.position(m_tokenizer.tokenStart());
            m_start = braceStart;
            m_len = 1;
            m_tt = IMAPTT.CONTROL_CHAR;
            return IMAPNextResult.HAVE_TOKEN;
        }

        //Consume any whitespace
        eatWhitespace(bb, false);

        //We have a {NNN   Check for
        //a } then CRLF
        if(bb.remaining() < 2) {
            //Rewind such that we re-encounter the "{NNN" next time
            bb.position(braceStart);
            m_tt = IMAPTT.NONE;
            return IMAPNextResult.NEED_MORE_DATA;
        }
        if(bb.get() == CLOSE_BRACE) {
            //Check for the CRLF
            if(bb.get(bb.position()) == CR_B) {
                bb.get();
                if(bb.remaining() < 1) {
                    //Need more bytes to check for new line
                    bb.position(braceStart);
                    m_tt = IMAPTT.NONE;
                    return IMAPNextResult.NEED_MORE_DATA;
                }
                if(bb.get() != LF_B) {
                    bb.position(bb.position()-1);
                }
                m_start = bb.position();
                m_len = 0;
                m_literalOctetCount = octetCount;
                m_tt = IMAPTT.LITERAL;
                return IMAPNextResult.HAVE_TOKEN;
            }
            else if(bb.get(bb.position()) == LF_B) {
                //Bare LF.  Naughty, naughty, naughty
                bb.position(bb.position()+1);
                m_start = bb.position();
                m_len = 0;
                m_literalOctetCount = octetCount;
                m_tt = IMAPTT.LITERAL;
                return IMAPNextResult.HAVE_TOKEN;
            }
            else {
                //Odd.  Did not comply
                bb.position(braceStart+1);
                m_start = braceStart;
                m_len = 1;
                m_tt = IMAPTT.CONTROL_CHAR;
                return IMAPNextResult.HAVE_TOKEN;
            }
        }
        else {
            //Not a candidate
            bb.position(braceStart+1);
            m_start = braceStart;
            m_len = 1;
            m_tt = IMAPTT.CONTROL_CHAR;
            return IMAPNextResult.HAVE_TOKEN;
        }
    }



}
