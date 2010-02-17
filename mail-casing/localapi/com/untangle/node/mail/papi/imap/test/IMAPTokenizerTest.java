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

package com.untangle.node.mail.papi.imap.test;

import static com.untangle.node.util.Ascii.CLOSE_BRACE_B;
import static com.untangle.node.util.Ascii.CLOSE_BRACKET_B;
import static com.untangle.node.util.Ascii.CLOSE_PAREN_B;
import static com.untangle.node.util.Ascii.CR_B;
import static com.untangle.node.util.Ascii.LF_B;
import static com.untangle.node.util.Ascii.OPEN_BRACE_B;
import static com.untangle.node.util.Ascii.OPEN_BRACKET_B;
import static com.untangle.node.util.Ascii.OPEN_PAREN_B;
import static com.untangle.node.util.Ascii.PERIOD_B;
import static com.untangle.node.util.Ascii.STAR_B;

import java.nio.ByteBuffer;

import com.untangle.node.mail.papi.imap.IMAPTokenizer;
import com.untangle.node.util.ASCIIUtil;
import com.untangle.node.util.ArrayTester;

public class IMAPTokenizerTest {

    public static void main(String[] args) {
        new IMAPTokenizerTest();
    }

    private int m_errCount = 0;
    private final int m_maxErrors = 2;
    private int m_testNum = 0;

    IMAPTokenizerTest() {

        String cr = "\r";
        String lf = "\n";
        String slash = "\\";
        String quote = "\"";
        String crlf = cr + lf;

        testOrPunt(("\nX\n").getBytes(),
                   new EOLConsumer(),
                   new WordConsumer("X"),
                   new EOLConsumer());

        testOrPunt(("\nX[\n").getBytes(),
                   new EOLConsumer(),
                   new WordConsumer("X"),
                   new CtlConsumer(OPEN_BRACKET_B),
                   new EOLConsumer());

        testOrPunt(("\nX [\n").getBytes(),
                   new EOLConsumer(),
                   new WordConsumer("X"),
                   new CtlConsumer(OPEN_BRACKET_B),
                   new EOLConsumer());

        testOrPunt(("\nX           [\n").getBytes(),
                   new EOLConsumer(),
                   new WordConsumer("X"),
                   new CtlConsumer(OPEN_BRACKET_B),
                   new EOLConsumer());

        testOrPunt(("\n\nX\n").getBytes(),
                   new EOLConsumer(),
                   new EOLConsumer(),
                   new WordConsumer("X"),
                   new EOLConsumer());

        testOrPunt(("\n\"X\"\n").getBytes(),
                   new EOLConsumer(),
                   new QStringConsumer("X"),
                   new EOLConsumer());

        testOrPunt(("\n\"XX\"\n").getBytes(),
                   new EOLConsumer(),
                   new QStringConsumer("XX"),
                   new EOLConsumer());

        testOrPunt(("\n\"X {2} X\"\n").getBytes(),
                   new EOLConsumer(),
                   new QStringConsumer("X {2} X"),
                   new EOLConsumer());

        testOrPunt(("\n\"X {2}\r\n X\"\n").getBytes(),
                   new EOLConsumer(),
                   new QStringConsumer("X {2}\r\n X"),
                   new EOLConsumer());

        testOrPunt(("\n\"X {2}\r" + slash + quote + "\n X\"\n").getBytes(),
                   new EOLConsumer(),
                   new QStringConsumer("X {2}\r\"\n X"),
                   new EOLConsumer());


        testOrPunt(("\n\"X" + slash + quote + "X\"\n").getBytes(),
                   new EOLConsumer(),
                   new QStringConsumer("X\"X"),
                   new EOLConsumer());

        testOrPunt(("{2}\r\nAAX Y").getBytes(),
                   new LiteralConsumer(2),
                   new WordConsumer("X"),
                   new WordConsumer("Y"));

        testOrPunt(("{3 }\r\nAAAXX Y").getBytes(),
                   new LiteralConsumer(3),
                   new WordConsumer("XX"),
                   new WordConsumer("Y"));

        testOrPunt(("{{2}\r\nAAX Y").getBytes(),
                   new CtlConsumer(OPEN_BRACE_B),
                   new LiteralConsumer(2),
                   new WordConsumer("X"),
                   new WordConsumer("Y"));

        testOrPunt(("{{2x}\r\nAAX Y").getBytes(),
                   new CtlConsumer(OPEN_BRACE_B),
                   new CtlConsumer(OPEN_BRACE_B),
                   new WordConsumer("2x"),
                   new CtlConsumer(CLOSE_BRACE_B),
                   new EOLConsumer(),
                   new WordConsumer("AAX"),
                   new WordConsumer("Y"));

        testOrPunt(("{{2x}AAX Y").getBytes(),
                   new CtlConsumer(OPEN_BRACE_B),
                   new CtlConsumer(OPEN_BRACE_B),
                   new WordConsumer("2x"),
                   new CtlConsumer(CLOSE_BRACE_B),
                   new WordConsumer("AAX"),
                   new WordConsumer("Y"));

        testOrPunt(("{{}AAX Y").getBytes(),
                   new CtlConsumer(OPEN_BRACE_B),
                   new CtlConsumer(OPEN_BRACE_B),
                   new CtlConsumer(CLOSE_BRACE_B),
                   new WordConsumer("AAX"),
                   new WordConsumer("Y"));

        testOrPunt(("{2}\r\nAAX").getBytes(),
                   new LiteralConsumer(2),
                   new WordConsumer("X"));

        testOrPunt(("{2}\rAAX").getBytes(),
                   new LiteralConsumer(2),
                   new WordConsumer("X"));

        testOrPunt(("{0}\n\rAAX").getBytes(),
                   new LiteralConsumer(0),
                   new EOLConsumer(),
                   new WordConsumer("AAX"));

        testOrPunt(("{2}\nAAX").getBytes(),
                   new LiteralConsumer(2),
                   new WordConsumer("X"));

        testOrPunt(("* 1 FETCH (UID BODY[HEADER.FIELDS (\"REFERENCES\"" + crlf +
                    "\"NEWSGROUPS\")] {2}" + crlf + "XXFOO ").getBytes(),
                   new CtlConsumer(STAR_B),
                   new WordConsumer("1"),
                   new WordConsumer("FETCH"),
                   new CtlConsumer(OPEN_PAREN_B),
                   new WordConsumer("UID"),
                   new WordConsumer("BODY"),
                   new CtlConsumer(OPEN_BRACKET_B),
                   new WordConsumer("HEADER"),
                   new CtlConsumer(PERIOD_B),
                   new WordConsumer("FIELDS"),
                   new CtlConsumer(OPEN_PAREN_B),
                   new QStringConsumer("REFERENCES"),
                   new EOLConsumer(),
                   new QStringConsumer("NEWSGROUPS"),
                   new CtlConsumer(CLOSE_PAREN_B),
                   new CtlConsumer(CLOSE_BRACKET_B),
                   new LiteralConsumer(2),
                   new WordConsumer("FOO"));
    }


    private boolean incrementErrorCount() {
        return m_errCount++ > m_maxErrors;
    }

    private void incrementTest() {
        if(++m_testNum%1000 == 0) {
            System.out.println("Executing Test #" + m_testNum);
        }
    }

    private void testOrPunt(byte[] bytes,
                            TokenConsumer...tokens) {
        System.out.println("=================== NEW TEST (" + m_testNum + ")===================");
        System.out.println("--- BEGIN Test Bytes---");
        System.out.println(new String(bytes));
        System.out.println("--- ENDOF Test Bytes---");
        if(!test(bytes, tokens)) {
            System.out.println("**** Exceeded Max Errors ****");
            System.exit(100);
        }
    }


    private boolean test(byte[] bytes,
                         TokenConsumer...tokens) {

        int errCount = 0;

        //    System.out.println("----------Test------------");

        for(int i = 1; i<bytes.length-1; i++) {
            ArrayTester at = new ArrayTester(bytes, i);
            while(at.hasNext()) {
                if(!testWithBuffers(bytes, at.nextBuffers(), tokens)) {
                    if(incrementErrorCount()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean testWithBuffers(byte[] origBytes,
                                    ByteBuffer[] bufs,
                                    TokenConsumer[] tokenConsumers) {

        incrementTest();

        String bufLensStr = bbLensToString(bufs);

        IMAPTokenizer it = new IMAPTokenizer();

        int consumerIndex = 0;
        try {

            int toSkip = 0;
            for(int i = 0; i<bufs.length; i++) {
                while(toSkip > 0 && bufs[i].hasRemaining()) {
                    int amt = bufs[i].remaining()>toSkip?
                        toSkip:bufs[i].remaining();
                    //          System.out.println("##Skipping " + amt +
                    //            ", pos: " + bufs[i].position() + ", remaining: " + bufs[i].remaining());
                    bufs[i].position(bufs[i].position() + amt);
                    toSkip-=amt;
                }
                if(!bufs[i].hasRemaining()) {
                    //          System.out.println("##Buf has no more.  Go to next buf");
                    continue;
                }
                if(consumerIndex+1 >tokenConsumers.length &&
                   bufs[i].hasRemaining()) {
                    if(it.next(bufs[i]) != IMAPTokenizer.IMAPNextResult.NEED_MORE_DATA) {
                        throw new Exception("Remaining bytes (" +
                                            bufs[i].remaining() + ") - no more consumers");
                    }
                    continue;
                }
                int result = tokenConsumers[consumerIndex].consumeToken(it,
                                                                        bufs[i]);
                //        System.out.println("##Result from " + consumerIndex + " " + result);
                if(result < 0) {
                    //Need more bytes
                    if(i+1 >= bufs.length) {
                        //            System.out.println("##Result < 0 on last buffer.  Join buffer to new line");
                        ByteBuffer newBuf = ByteBuffer.wrap(new byte[] {CR_B, LF_B});
                        bufs[i] = joinBuffers(bufs[i], newBuf);
                        TokenConsumer[] newConsumers = new TokenConsumer[tokenConsumers.length + 1];
                        System.arraycopy(tokenConsumers, 0, newConsumers, 0, tokenConsumers.length);
                        newConsumers[tokenConsumers.length] = new EOLConsumer();
                        tokenConsumers = newConsumers;
                        i--;
                        continue;
                    }
                    //          System.out.println("##Result < 0 on buffer.  Join with next");
                    bufs[i+1] = joinBuffers(bufs[i], bufs[i+1]);
                    i--;
                    continue;
                }
                else if(result > 0) {
                    //          System.out.println("##Result requires skip (" + result + ")");
                    i--;
                    consumerIndex++;
                    toSkip = result;
                    continue;
                }
                //        System.out.println("##OK result.  Go to next consumer");
                consumerIndex++;
                i--;
            }

            if(consumerIndex+1<tokenConsumers.length) {
                throw new Exception("Not enough bytes for all consumers");
            }

            return true;
        }
        catch(Exception ex) {
            System.out.println("---------------ERROR--------------");
            ex.printStackTrace(System.out);
            System.out.println("");
            System.out.println("Buf Lengths: " + bufLensStr);
            System.out.println("Tests: ");
            for(int k = 0; k<tokenConsumers.length; k++) {
                System.out.println((
                                       k == consumerIndex?
                                       "(failed)":k<consumerIndex?
                                       "(pass)":"(not run)") + " " + tokenConsumers[k]);
            }
            System.out.println("--- BEGIN BYTES");
            for(int l = 0; l<origBytes.length; l++) {
                System.out.println(l + " " + ASCIIUtil.asciiByteToString(origBytes[l]));
            }
            System.out.println("--- ENDOF BYTES");
            return false;
        }
    }


    private static ByteBuffer joinBuffers(ByteBuffer b1, ByteBuffer b2) {
        ByteBuffer ret = ByteBuffer.allocate(b1.remaining() + b2.remaining());
        ret.put(b1);
        ret.put(b2);
        ret.flip();
        return ret;
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


    private abstract class TokenConsumer {

        /**
         * -1 means "need more data".>0 means consume
         * that many bytes.  Exception on error
         */
        final int consumeToken(IMAPTokenizer tokenizer,
                               ByteBuffer buf) throws Exception {
            //      System.out.println("###consumeToken() PRE " +
            //        "start: " + tokenizer.getTokenStart() +
            //        ", len: " + tokenizer.getTokenLength());

            IMAPTokenizer.IMAPNextResult result = tokenizer.next(buf);
            switch(result) {
            case HAVE_TOKEN:
                return consumeTokenImpl(tokenizer, buf, result);
            case EXCEEDED_LONGEST_WORD:
                throw new Exception("Exceeded Longest Word");
            case NEED_MORE_DATA:
                return -1;
            }
            throw new Exception("Unhandled Case");
        }


        abstract int consumeTokenImpl(IMAPTokenizer tokenizer,
                                      ByteBuffer buf,
                                      IMAPTokenizer.IMAPNextResult result) throws Exception;

        void assertTokenType(IMAPTokenizer tokenizer,
                             IMAPTokenizer.IMAPTT expectType,
                             ByteBuffer buf) throws Exception {

            if(tokenizer.getTokenType() != expectType) {
                throw new Exception("Expecting Token of type \"" +
                                    expectType + "\", got \"" + tokenizer.getTokenType() + "\"" +
                                    " (\"" + tokenizer.tokenToStringDebug(buf) + "\"");
            }
        }
        String details(IMAPTokenizer tokenizer,
                       ByteBuffer buf) {
            return "(Type: " + tokenizer.getTokenType() +
                ", start: " + tokenizer.getTokenStart() +
                ", len: " + tokenizer.getTokenLength() +
                ", value:\"" + tokenizer.tokenToStringDebug(buf) + "\")";
        }
    }

    private class EOLConsumer
        extends TokenConsumer {
        int consumeTokenImpl(IMAPTokenizer tokenizer,
                             ByteBuffer buf,
                             IMAPTokenizer.IMAPNextResult result) throws Exception {
            assertTokenType(tokenizer, IMAPTokenizer.IMAPTT.NEW_LINE, buf);
            return 0;
        }

        public String toString() {
            return "Expect EOL";
        }
    }

    private class CtlConsumer
        extends TokenConsumer {
        private byte m_ctl;
        CtlConsumer(byte ctl) {
            m_ctl = ctl;
        }
        int consumeTokenImpl(IMAPTokenizer tokenizer,
                             ByteBuffer buf,
                             IMAPTokenizer.IMAPNextResult result) throws Exception {
            assertTokenType(tokenizer, IMAPTokenizer.IMAPTT.CONTROL_CHAR, buf);
            if(buf.get(tokenizer.getTokenStart()) != m_ctl) {
                throw new Exception("Expecting control delim \"" + ASCIIUtil.asciiByteToString(m_ctl) + "\"" +
                                    ", got \"" + ASCIIUtil.asciiByteToString(buf.get(tokenizer.getTokenStart())) + "\"" +
                                    details(tokenizer, buf));
            }
            return 0;
        }
        public String toString() {
            return "Expect CTL \"" + ASCIIUtil.asciiByteToString(m_ctl) + "\"";
        }
    }

    private class LiteralConsumer
        extends TokenConsumer {
        private int m_len;
        LiteralConsumer(int len) {
            m_len = len;
        }
        int consumeTokenImpl(IMAPTokenizer tokenizer,
                             ByteBuffer buf,
                             IMAPTokenizer.IMAPNextResult result) throws Exception {
            assertTokenType(tokenizer, IMAPTokenizer.IMAPTT.LITERAL, buf);
            return tokenizer.getLiteralOctetCount();
        }
        public String toString() {
            return "Expect Literal {" + m_len + "}";
        }
    }

    private class QStringConsumer
        extends TokenConsumer {
        private String m_unquotedString;
        QStringConsumer(String s) {
            m_unquotedString = s;
        }
        int consumeTokenImpl(IMAPTokenizer tokenizer,
                             ByteBuffer buf,
                             IMAPTokenizer.IMAPNextResult result) throws Exception {
            assertTokenType(tokenizer, IMAPTokenizer.IMAPTT.QSTRING, buf);
            String str = new String(tokenizer.getQStringToken(buf));
            if(!m_unquotedString.equals(str)) {
                throw new Exception("Expecting qstring \"" + m_unquotedString + "\", got \"" + str + "\"" +
                                    details(tokenizer, buf));
            }
            return 0;
        }
        public String toString() {
            return "Expect QString \"" + m_unquotedString + "\"";
        }

    }

    private class WordConsumer
        extends TokenConsumer {
        private String m_word;
        WordConsumer(String word) {
            m_word = word;
        }
        int consumeTokenImpl(IMAPTokenizer tokenizer,
                             ByteBuffer buf,
                             IMAPTokenizer.IMAPNextResult result) throws Exception {
            assertTokenType(tokenizer, IMAPTokenizer.IMAPTT.WORD, buf);
            ByteBuffer dup = buf.duplicate();
            dup.position(tokenizer.getTokenStart());
            dup.limit(tokenizer.getTokenLength() + dup.position());
            String theWord = ASCIIUtil.bbToString(dup);
            if(!m_word.equals(theWord)) {
                throw new Exception("Expecting word \"" + m_word + "\", got \"" + theWord + "\""
                                    + details(tokenizer, buf));
            }
            return 0;
        }
        public String toString() {
            return "Expect WORD \"" + m_word + "\"";
        }
    }



}
