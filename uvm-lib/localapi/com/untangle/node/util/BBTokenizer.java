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

package com.untangle.node.util;
import static com.untangle.node.util.Ascii.CR_B;
import static com.untangle.node.util.Ascii.HT_B;
import static com.untangle.node.util.Ascii.LF_B;
import static com.untangle.node.util.Ascii.SP_B;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


/**
 * Simple tokenizer for logical streams seen as
 * a chain of ByteBuffers.
 * <br><br>
 * It tokenizes a stream into words and delimiters.
 * delimiters bound words.  Some (not all) delimiters
 * are significant, and can be returned (see docs
 * on {@link #setReturnDelims} and {@link #setSkipDelims}).
 * <br><br>
 * It is used by passing a ByteBuffer to the {@link #next next}
 * method until either a {@link #NextResult NextResult} of
 * NEED_MORE_DATA or EXCEEDED_LONGEST_WORD is found.  If the
 * return is HAVE_WORD or HAVE_DELIM, then the {@link getTokenType getTokenType},
 * {@link #tokenStart} and {@link #tokenLength tokenLength} methods contain
 * data about the token just encountered.  Note that we call the token
 * encountered during a call to {@link #next next} as the "current"
 * token.  Initialy, there is no current token, as is also the case
 * if the tokenizer needs more data.
 * <br><br>
 * This class is a bit lazy.  It assumes the caller can buffer data.  If
 * a portion of a word is encountered, calls to {@link #next next} will return
 * NEED_MORE_DATA.  It is the caller's responsibility to pass-back in these
 * bytes as well as new data.  In other words, this class only maintains
 * state about the current <i>full</i> token, and relies on revisiting
 * data for partial tokens.
 * <br><br>
 * Obviously not threadsafe
 */
public class BBTokenizer {

    private static final byte[] BLANK_BYTES = new byte[0];
    private static final byte[] LWS = new byte[] {HT_B, SP_B};

    /**
     * Enum of the types of tokens.
     */
    public enum TT {
        WORD,
        DELIM,
        NONE
    };

    public enum NextResult {
        HAVE_WORD,
        HAVE_DELIM,
        NEED_MORE_DATA,
        EXCEEDED_LONGEST_WORD
    }

    private BAHolder m_delimHolder = new BAHolder(LWS);
    private BAHolder m_delimsToSkipHolder = new BAHolder(LWS);
    private int m_longestWord = 1024;
    private int m_start = -1;
    private int m_len = -1;
    private TT m_tt = TT.NONE;
    private int m_toSkip = 0;

    public BBTokenizer() {
    }

    /**
     * Call valid after a successful call to {@link #next next},
     * indicating the type of Token encountered.
     */
    public TT getTokenType() {
        return m_tt;
    }

    /**
     * Get the offset within the ByteBuffer of the
     * current token
     */
    public int tokenStart() {
        return m_start;
    }

    /**
     * Get the length from {@link #tokenStart tokenStart}
     * of the current token
     */
    public int tokenLength() {
        return m_len;
    }

    public int getLongestWord() {
        return m_longestWord;
    }

    /**
     * Set the maximum length of a word, before the
     * tokenizer gives-up and returns EXCEEDED_LONGEST_WORD
     * from a call to {@link #next next}.
     */
    public void setLongestWord(int len) {
        m_longestWord = len;
    }

    /**
     * Set the delimiters.  These are the superset
     * of delimiters.  If some of these delimiters are
     * not of interest to the caller, you may pass
     * some of these to {@link #setSkipDelims setSkipDelims}.
     */
    public void setDelims(byte[] delims) {
        m_delimHolder = new BAHolder(delims);
    }

    /**
     * Note that any delims in this list which are <b>
     * not</b> in the {@link #setDelims delimiter list}
     * are ignored.  In other words, anything passed to
     * this method should have already been declared a delimiter.
     */
    public void setSkipDelims(byte[] delims) {
        m_delimsToSkipHolder = new BAHolder(delims);
    }

    /**
     * Set the delimiters
     *
     * @param delims the delimiters
     * @param toSkip a subset ot <code>delims</code> which
     *        bound words, but are not of interest to be
     *        returned as tokens.
     */
    public void setDelims(byte[] delims, byte[] toSkip) {
        m_delimHolder = new BAHolder(delims);
        m_delimsToSkipHolder = new BAHolder(toSkip==null?BLANK_BYTES:toSkip);
    }

    /**
     * Instruct the tokenizer to skip-past the next
     * <code>quantity</code> bytes.  Skipping begins
     * on the next call to {@link #next next}.  This may result
     * in some calls to {@link #next next}
     * returning NEED_MORE_DATA.
     */
    public void skip(int quantity) {
        m_toSkip = quantity;
    }


    /**
     * Increment the buffer to the next token.  If the
     * return is HAVE_WORD or HAVE_WORD, the ByteBuffer
     * is advanced past the bytes of the encountered token.  To
     * recover the token for investigation, the {@link #tokenStart tokenStart}
     * and {@link #tokenLength tokenLength} methods describe the
     * token's location within the ByteBuffer.
     * <br><br>
     * If "EXCEEDED_LONGEST_WORD" or "NEED_MORE_DATA"
     * are returned, the buffer still contains the bytes which
     * were candidates for the next word
     *
     * @param bb the ByteBuffer to scan
     *
     * @return the result
     */
    public NextResult next(final ByteBuffer bb) {
        //Reset token type and values
        m_tt = TT.NONE;
        m_start = -1;
        m_len = -1;

        if(m_toSkip > 0) {
            int q = m_toSkip>bb.remaining()?
                bb.remaining():m_toSkip;
            bb.position(bb.position() + q);
            m_toSkip-=q;
            if(!bb.hasRemaining()) {
                return NextResult.NEED_MORE_DATA;
            }
        }


        NextResult ret = null;
        do {
            ret = nextImpl(bb);
        }
        while(ret == NextResult.HAVE_DELIM
              && m_delimsToSkipHolder.contains(bb.get(tokenStart())));
        return ret;
    }


    private NextResult nextImpl(final ByteBuffer bb) {
        m_start = bb.position();
        while(bb.hasRemaining()) {
            if(m_delimHolder.contains(bb.get())) {
                if(m_start + 1 == bb.position()) {
                    m_len = 1;
                    m_tt = TT.DELIM;
                    return NextResult.HAVE_DELIM;
                }
                else {
                    //We have a word.  Re-wind the buffer by one to get the delim next time
                    bb.position(bb.position()-1);
                    m_len = bb.position() - m_start;
                    m_tt = TT.WORD;
                    return NextResult.HAVE_WORD;
                }
            }
            if((bb.position() - m_start) > m_longestWord) {
                bb.position(m_start);
                m_tt = TT.NONE;
                return NextResult.EXCEEDED_LONGEST_WORD;
            }
        }
        //Go back to the begining of the potential word
        bb.position(m_start);
        m_start = -1;
        m_len = -1;
        m_tt = TT.NONE;
        return NextResult.NEED_MORE_DATA;
    }



    //I'm assuming the the byte[] will always be small
    //enough that sorting and searching are not worth it.  However,
    //it has been broken-off for later optimization.
    private class BAHolder {
        final byte[] a;
        final int len;

        BAHolder(byte[] bytes) {
            if(bytes == null) {
                bytes = new byte[0];
            }
            this.a = bytes;
            this.len = a.length;
        }
        boolean contains(final byte b) {
            for(int i = 0; i<len; i++) {
                if(a[i] == b) {
                    return true;
                }
            }
            return false;
        }
    }


    //===============================
    // Test Code from here below
    //===============================


    public static void main(String[] args)
        throws Exception {

        //Test exceeding the longest word

        //Test "normal" stuff
        byte[] delims = new byte[] {
            SP_B,
            HT_B,
            CR_B,
            LF_B,
            (byte) '|',
            (byte) ','
        };
        byte[] delimsToSkip = new byte[] {
            SP_B,
            HT_B
        };

        test("FOO".getBytes(),
             delims,
             delimsToSkip,
             "FOO");

        test("  FOO".getBytes(),
             delims,
             delimsToSkip,
             "FOO");

        test("  FOO \t".getBytes(),
             delims,
             delimsToSkip,
             "FOO");

        test("FOO|".getBytes(),
             delims,
             delimsToSkip,
             "FOO", "|");

        test("FOO |".getBytes(),
             delims,
             delimsToSkip,
             "FOO", "|");

        test("\t FOO |".getBytes(),
             delims,
             delimsToSkip,
             "FOO", "|");

        test("\t FOO |moo".getBytes(),
             delims,
             delimsToSkip,
             "FOO", "|", "moo");

    }

    private static void test(byte[] bytes,
                             byte[] delims,
                             byte[] delimsToSkip,
                             String...tokens) throws Exception {

        System.out.println("----------Test------------");

        BBTokenizer tokenizer = new BBTokenizer();
        tokenizer.setDelims(delims, delimsToSkip);

        for(int i = 1; i<bytes.length-1; i++) {
            ArrayTester at = new ArrayTester(bytes, i);
            while(at.hasNext()) {
                ByteBuffer[] bufs = at.nextBuffers();
                String lens = bbLensToString(bufs);
                List<String> out = tokenize(tokenizer, bufs);
                if(!compare(tokens, out)) {
                    errorReport(bytes, i, tokens, out, lens);
                }
                else {
                    System.out.println("Passed (" + lens + ")");
                }
            }
        }
    }


    private static void errorReport(byte[] bytes,
                                    int numBuffers,
                                    String[] expected,
                                    List<String> outcome,
                                    String lens) {
        System.err.print("ERROR");
        System.out.println("******************* ERROR REPORT *****************");
        System.out.println("Lengths: " + lens);
        System.out.println("===BEGIN Input===");
        for(int i = 0; i<bytes.length; i++) {
            System.out.println(i + " " + ASCIIUtil.asciiByteToString(bytes[i]));
        }
        System.out.println("===ENDOF Input===");
        System.out.println("===BEGIN Arrays===");
        System.out.println("Expected        Received");
        int longest = Math.max(expected.length, outcome.size());

        for(int i = 0; i<longest; i++) {
            System.out.print(
                             i>=expected.length?"<null>":expected[i]);
            System.out.print("    ");
            System.out.println(
                               i>=outcome.size()?"<null>":outcome.get(i));

        }
        System.out.println("===ENDOF Arrays===");
    }

    private static String bbLensToString(ByteBuffer[] bufs) {
        String bufsLen = "";
        for(int j = 0; j<bufs.length; j++) {
            if(j != 0) {
                bufsLen+=", ";
            }
            bufsLen+=Integer.toString(bufs[j].remaining());
        }
        return bufsLen;
    }

    private static boolean compare(String[] in,
                                   List<String> out) {
        if(in.length != out.size()) {
            System.out.println("Length Wrong.  " +
                               in.length + " != " + out.size());
            return false;
        }

        for(int i = 0; i<in.length; i++) {
            if(!in[i].equals(out.get(i))) {
                System.out.println("\"" + in[i] + "\" != \"" + out.get(i) + "\"");
                return false;
            }
        }
        return true;
    }

    private static List<String> tokenize(BBTokenizer tokenizer,
                                         ByteBuffer[] bufs) throws Exception {

        List<String> ret = new ArrayList<String>();

        for(int i = 0; i<bufs.length; i++) {
            tokenizeInto(ret, bufs[i], tokenizer);
            if(bufs[i].hasRemaining()) {
                if(i+1 == bufs.length) {
                    //Treat remainder as plain token (EOF is delim)
                    ret.add(ASCIIUtil.bbToString(bufs[i]));
                }
                else {
                    bufs[i+1] = joinBuffers(bufs[i], bufs[i+1]);
                }
            }
        }
        return ret;
    }

    private static void tokenizeInto(List<String> list,
                                     ByteBuffer buf,
                                     BBTokenizer tokenizer) throws Exception{

        while(true) {
            BBTokenizer.NextResult result = tokenizer.next(buf);
            switch(result) {
            case HAVE_WORD:
                ByteBuffer dup = buf.duplicate();
                dup.position(tokenizer.tokenStart());
                dup.limit(dup.position() + tokenizer.tokenLength());
                list.add(ASCIIUtil.bbToString(dup));
                break;
            case HAVE_DELIM:
                list.add(new StringBuilder().append((char) buf.get(tokenizer.tokenStart())).toString());
                break;
            case NEED_MORE_DATA:
                return;
            case EXCEEDED_LONGEST_WORD:
                throw new Exception("EXCEEDED_LONGEST_WORD.  Unexpected");
            }
        }
    }

    private static ByteBuffer joinBuffers(ByteBuffer b1, ByteBuffer b2) {
        ByteBuffer ret = ByteBuffer.allocate(b1.remaining() + b2.remaining());
        ret.put(b1);
        ret.put(b2);
        ret.flip();
        return ret;
    }


}
