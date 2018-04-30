/**
 * $Id$
 */
package com.untangle.app.smtp;

import static com.untangle.uvm.util.Ascii.COLON;
import static com.untangle.uvm.util.Ascii.CR;
import static com.untangle.uvm.util.Ascii.CRLF_BA;
import static com.untangle.uvm.util.Ascii.DOT;
import static com.untangle.uvm.util.Ascii.HTAB;
import static com.untangle.uvm.util.Ascii.LF;
import static com.untangle.uvm.util.BufferUtil.findCrLf;
import static com.untangle.uvm.util.BufferUtil.findPattern;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

/**
 * Specialized little class to process ByteBuffers which contain RFC821 Messages (MIME). <br>
 * Instances are stateful and not threadsafe.
 */
public class MessageBoundaryScanner
{
    // CR
    final byte[] CR_BA = new byte[] { CR };
    final byte[] CRLF = CRLF_BA;
    final byte[] CRLF_DOT = new byte[] { (byte) CR, (byte) LF, (byte) DOT };
    final byte[] CRLF_DOT_CR = new byte[] { (byte) CR, (byte) LF, (byte) DOT, (byte) CR };
    final byte[] CRLF_DOT_CRLF = new byte[] { (byte) CR, (byte) LF, (byte) DOT, (byte) CR, (byte) LF };
    final byte[] CRLF_DOT_DOT = new byte[] { (byte) CR, (byte) LF, (byte) DOT, (byte) DOT };
    final byte[] CRLF_DOT_DOT_CR = new byte[] { (byte) CR, (byte) LF, (byte) DOT, (byte) DOT, (byte) CR };
    final byte[] CRLF_DOT_DOT_CRLF = new byte[] { (byte) CR, (byte) LF, (byte) DOT, (byte) DOT, (byte) CR, (byte) LF };
    final byte[] CRLF_CR = new byte[] { (byte) CR, (byte) LF, (byte) CR };
    final byte[] CRLF_CRLF = new byte[] { (byte) CR, (byte) LF, (byte) CR, (byte) LF };

    public enum ScanningState
    {
        INIT, LOOKING_FOR_HEADERS_END, INIT_BODY, LOOKING_FOR_BODY_END, DONE
    };

    private final Logger m_logger = Logger.getLogger(MessageBoundaryScanner.class);

    private boolean m_headersBlank = false;
    private boolean m_isEmptyMessage = false;
    private ScanningState m_state = ScanningState.INIT;

    /**
     * Initialize MessageBoundaryScanner instance.
     * @return Instance of MessageBoundaryScanner.
     */
    public MessageBoundaryScanner() {
    }

    /**
     * Reset this Object for reuse
     */
    public void reset()
    {
        m_headersBlank = false;
        m_isEmptyMessage = false;
        m_state = ScanningState.INIT;
    }

    /**
     * Get current state (see the enum on this class).
     *
     * @return ScanningState value.
     */
    public ScanningState getState()
    {
        return m_state;
    }

    /**
     * Returns true if the headers were blank (the message started with "CRLF"). This method should only be called after
     * the state has progressed beyond INIT
     *
     * @return true if header is blank, false otherwise.
     */
    public boolean isHeadersBlank()
    {
        return m_headersBlank;
    }

    /**
     * Returns true if the entire message was a "CRLF.CRLF". This method should only be called after the state has
     * progressed beyond INIT
     *
     * @return true if message is empty, false otherwise.
     */
    public boolean isEmptyMessage()
    {
        return m_isEmptyMessage;
    }

    /**
     * Process the headers, returning true when the end of the headers has been found (a CR on a line by itself). The
     * terminating CR <b>is consumed</b> (i.e. the buffer is advanced past the terminating CR). <br>
     * There are three boundary cases to be detected. <br>
     * The first case involves the {@link #isEmptyMessage empty message} It happens when the message starts with
     * "CRLF.CRLF". In such a case, the state will be advanced to DONE. <br>
     * The second case is blank headers. This is when the message starts with "CRLFX" where "X" is not the start of
     * "CRLF.CRLF". <br>
     * The third case is an opening line which does not have a colon. We'll treat this as part of the body, declaring
     * the headers blank. <br>
     * When this method concludes, it may leave bytes in the buffer which are intended to be passed-back on the next
     * call at the head.
     * 
     * @param buf
     *            the buffer to scan
     * @param maxHeaderLineSz
     *            the max header line size
     * 
     * @return true if end of headers encountered
     * 
     */
    public boolean processHeaders(ByteBuffer buf, int maxHeaderLineSz)
    {

        // Note there is a special case we must guard against. A mail body
        // with only a CRLF.CRLF (i.e. no headers or body) is used in pipelining as a retroactive RSET.
        //
        // We can only detect this if we have at least 5 bytes, so make sure we have at least that many.

        if (m_state == ScanningState.INIT) {
            // Special case. We cannot know what is going on without 5 bytes, to catch the blank-message case
            if (buf.remaining() < 5) {
                return false;
            }
            // We're guaranteed at least 5 bytes

            // Check for the "blank" message case
            if ((buf.get(buf.position()) == CR) && (buf.get(buf.position() + 1) == LF)
                    && (buf.get(buf.position() + 2) == DOT) && (buf.get(buf.position() + 3) == CR)
                    && (buf.get(buf.position() + 4) == LF)) {

                m_isEmptyMessage = true;
                m_headersBlank = true;
                m_state = ScanningState.DONE;
                // Advance buffer past the blank stuff
                buf.position(buf.position() + 5);
                return true;
            }
            m_isEmptyMessage = false;

            // Check for the no-headers case
            if ((buf.get(buf.position()) == CR) && (buf.get(buf.position() + 1) == LF)) {

                // Blank headers
                buf.position(buf.position() + 2);
                m_state = ScanningState.INIT_BODY;
                m_headersBlank = true;
                return true;
            }

            // Check for a first line which isn't a header,
            // and should be treated as body
            int crlfIndex = findCrLf(buf);
            if (crlfIndex > 0) {// Cannot be equal to zero, as wel aready tested
                                // for this
                boolean foundColon = false;
                ByteBuffer dup = buf.duplicate();
                while (dup.hasRemaining()) {
                    if (dup.get() == COLON) {
                        foundColon = true;
                        break;
                    }
                }
                // If we found a line yet not a colon, headers are blank
                if (!foundColon) {
                    m_headersBlank = true;
                    m_state = ScanningState.INIT_BODY;
                    return true;
                } else {
                    // Fall through
                }
            } else {
                // Still cannot determine if this is a valid header line, or part of the body from a crap message
                if (buf.remaining() > maxHeaderLineSz) {
                    m_logger.warn("Exceeded max candidate header line length of \"" + maxHeaderLineSz
                            + "\".  Assume no headers");
                    m_headersBlank = true;
                    m_state = ScanningState.INIT_BODY;
                    return true;
                }
                return false;
            }

            m_state = ScanningState.LOOKING_FOR_HEADERS_END;
        }

        // handle amread's special syntactically incorrect message
        // - last header field/value does not end with <CRLF>
        // and DATA section is immediately terminated by <CRLF.CRLF>
        // or
        // last header field/value ends with <CRLF>
        // and DATA section is only terminated by <.CRLF>
        if (buf.remaining() == 5) {
            if ((buf.get(buf.position()) == CR) && (buf.get(buf.position() + 1) == LF)
                    && (buf.get(buf.position() + 2) == DOT) && (buf.get(buf.position() + 3) == CR)
                    && (buf.get(buf.position() + 4) == LF)) {

                m_state = ScanningState.INIT_BODY;
                // Advance buffer past 1st CRLF pair
                // - last header field/value ends with <CRLF>
                // - processBody will handle the rest of buffer <.CRLF>
                // as a case of a blank message body
                buf.position(buf.position() + 2);
                return true;
            }
        }

        // Scan for the end of headers
        int headersEnd = findPattern(buf, CRLF_CRLF, 0, CRLF_CRLF.length);

        if (headersEnd >= 0) {
            // Found the end of the headers
            buf.position(headersEnd + 4);
            m_headersBlank = false;
            m_state = ScanningState.INIT_BODY;
            return true;
        } else {
            // Figure out if we should hold-back some bytes
            int headerTermBackset = searchForStart(buf, CRLF_CRLF);
            int msgEndBackset = searchForStart(buf, CRLF_DOT_CRLF);
            buf.position(buf.limit() - Math.max(headerTermBackset, msgEndBackset));
            return false;
        }
    }

    /**
     * Process a body chunk, moving bytes from source to sink. Look for lines to "dot escape" as well as the terminator. <br>
     * <br>
     * If this method returns false, data may be left in the buffer. This is a simplification, in case the last few
     * bytes were candidates for a significant sequence (please use some magic to cause them to come back to this object
     * on the next read). <br>
     * <br>
     * A return of true indicates that we found the end. If true is returned, the source is positioned just after the
     * "CRLF.CRLF" sequence. Sink should be as big as the source, although it may be underfilled by "dot escaped" lines
     * or the terminator. <br>
     * <br>
     * The sink is <b>not</b>flipped when this method returns. <br>
     * <br>
     * <b>Do not call this method again after the end was found (i.e. when state is DONE).</b>
     * 
     * 
     * @param source
     *            the source
     * @param sink
     *            the sink
     * 
     * @return true if end of body encountered.
     */
    public boolean processBody(ByteBuffer source, ByteBuffer sink)
    {
        if (m_state == ScanningState.DONE) {
            return true;
        }

        // Special case. If the body is blank, then
        // the CRLF which terminated the headers
        // may also start the body termination.
        // For this case, we look for ".CRLF"
        if (m_state == ScanningState.INIT_BODY) {
            // Not enough bytes to determine if this is the end
            if (source.remaining() < 3) {
                return false;
            }
            // Check for no body case, knowing what we
            // last saw was the CRLF terminating the headers
            if ((source.get(source.position()) == DOT) && (source.get(source.position() + 1) == CR)
                    && (source.get(source.position() + 2) == LF)) {

                m_isEmptyMessage = true;
                source.position(source.position() + 3);
                m_state = ScanningState.DONE;
                return true;
            }

            m_state = ScanningState.LOOKING_FOR_BODY_END;
        }

        // Scan for lines we need to escape
        int index = findPattern(source, CRLF_DOT_DOT, 0, CRLF_DOT_DOT.length);

        while (index >= 0 && source.hasRemaining()) {
            // Copy before the ".." (if there is any)
            if (source.position() < index) {
                ByteBuffer dup = source.duplicate();
                dup.position(source.position());
                dup.limit(index);
                sink.put(dup);
            }
            // Write "CRLF."
            sink.put(CRLF_DOT);

            // Position just after the "CRLF.."
            source.position(index + CRLF_DOT_DOT.length);
            index = findPattern(source, CRLF_DOT_DOT, 0, CRLF_DOT_DOT.length);
        }

        // No more CRLF..s. Look for end of message
        if (source.hasRemaining()) {// BEGIN More Bytes after dot escape
                                    // scanning
            // Look for "CRLF.CRLF"
            index = findPattern(source, CRLF_DOT_CRLF, 0, CRLF_DOT_CRLF.length);
            if (index != -1) {
                // Copy bytes without CRLF.CRLF to sink
                if (source.position() < index) {
                    ByteBuffer dup = source.duplicate();
                    dup.position(source.position());
                    dup.limit(index);
                    sink.put(dup);
                }

                // Position source just after CRLF.CRLF
                source.position(index + CRLF_DOT_CRLF.length);
                m_state = ScanningState.DONE;
                return true;
            } else {// BEGIN Copy remaining Bytes
                    // Amount *not* to copy
                    // to sink and reserve for next
                    // read
                    // Check for out four end patterns
                    // "CR", "CRLF", "CRLF.", "CRLF.CR"
                    // There should not be any "CRLF.."
                    // If they are found, cause them to *stay* in the buffer
                int holdBackMsgEnd = searchForStart(source, CRLF_DOT_CRLF);

                // Copy remaining bytes, holding back
                // what may be the start of
                source.limit(source.limit() - holdBackMsgEnd);
                sink.put(source);
                source.limit(source.limit() + holdBackMsgEnd);
            }// ENDOF Copy remaining Bytes
        }// ENDOF More Bytes after dot escape scanning
        return false;
    }

    /**
     * Searches the tail of the buffer to see if it could have been the start of the given pattern.
     *
     * @param buf ButeBuffer to search.
     * @param bytes Array of bytes pattern to match.
     * @return the number of bytes from the tail which could be the start of the pattern.
     */
    protected static final int searchForStart(ByteBuffer buf, byte[] bytes)
    {
        if (!buf.hasRemaining()) {
            return 0;
        }
        int startScan = buf.remaining() > bytes.length ? bytes.length : buf.remaining();

        for (int i = startScan; i > 0; i--) {
            boolean found = true;
            for (int j = 0; j < i; j++) {
                if (buf.get(buf.limit() - i + j) != bytes[j]) {
                    found = false;
                }
            }
            if (found) {
                return i;
            }
        }
        return 0;
    }

    /************** Tests ******************/

    /**
     * Run test on class.
     * @param  args      Unused.
     * @return           String of results.
     * @throws Exception If any issues.
     */
    public static String runTest(String[] args) throws Exception
    {
        String result = "";
        ByteBuffer b1 = null;
        ByteBuffer b2 = null;

        b1 = ByteBuffer.wrap("\r\n..X".getBytes());
        result += "Expect true: " + findPattern(b1, "\r\n..".getBytes(), 0, 4) + "\n";

        b1 = ByteBuffer.wrap("abcd".getBytes());
        result += "Expect 2, " + MessageBoundaryScanner.searchForStart(b1, "cd".getBytes()) + "\n";

        b1 = ByteBuffer.wrap("abcd".getBytes());
        result += "Expect 0, " + MessageBoundaryScanner.searchForStart(b1, "xy".getBytes()) + "\n";

        b1 = ByteBuffer.wrap("abxycd".getBytes());
        result += "Expect 0, " + MessageBoundaryScanner.searchForStart(b1, "xy".getBytes()) + "\n";

        b1 = ByteBuffer.wrap("abcd".getBytes());
        result += "Expect 1, " + MessageBoundaryScanner.searchForStart(b1, "dx".getBytes()) + "\n";

        b1 = ByteBuffer.wrap("abc".getBytes());
        result += "Expect 3, " + MessageBoundaryScanner.searchForStart(b1, "abcdefg".getBytes()) + "\n";

        b1 = ByteBuffer.wrap("efg".getBytes());
        result += "Expect 0, " + MessageBoundaryScanner.searchForStart(b1, "xyzpdq".getBytes()) + "\n";

        // Good case 1 chunk
        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\nBody\r\nbody2\r\n.\r\n".getBytes());
        result += test("Good, all in one", b1);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\n".getBytes());
        b2 = ByteBuffer.wrap("Body\r\nbody2\r\n.\r\n".getBytes());
        result += test("Headers, Body", b1, b2);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n".getBytes());
        b2 = ByteBuffer.wrap("\r\nBody\r\nbody2\r\n.\r\n".getBytes());
        result += test("HeadersCRLF, CRLFBody", b1, b2);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r".getBytes());
        b2 = ByteBuffer.wrap("\nBody\r\nbody2\r\n.\r\n".getBytes());
        result += test("HeadersCRLFCR, LFBody", b1, b2);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r".getBytes());
        b2 = ByteBuffer.wrap("\n\r\nBody\r\nbody2\r\n.\r\n".getBytes());
        result += test("HeadersCR, LFCRLFBody", b1, b2);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo".getBytes());
        b2 = ByteBuffer.wrap("\r\n\r\nBody\r\nbody2\r\n.\r\n".getBytes());
        result += test("Headers, CRLFCRLFBody", b1, b2);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\nBody\r\nbody".getBytes());
        b2 = ByteBuffer.wrap("2\r\n.\r\n".getBytes());
        result += test("HeadersBody, BodyCRLF.CRLF", b1, b2);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\nBody\r\nbody2".getBytes());
        b2 = ByteBuffer.wrap("\r\n.\r\n".getBytes());
        result += test("HeadersBody, CRLF.CRLF", b1, b2);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\nBody\r\nbody2\r".getBytes());
        b2 = ByteBuffer.wrap("\n.\r\n".getBytes());
        result += test("HeadersBodyCR, LF.CRLF", b1, b2);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\nBody\r\nbody2\r\n".getBytes());
        b2 = ByteBuffer.wrap(".\r\n".getBytes());
        result += test("HeadersBodyCRLF, .CRLF", b1, b2);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\nBody\r\nbody2\r\n.".getBytes());
        b2 = ByteBuffer.wrap("\r\n".getBytes());
        result += test("HeadersBodyCRLF., CRLF", b1, b2);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\nBody\r\nbody2\r\n.\r".getBytes());
        b2 = ByteBuffer.wrap("\n".getBytes());
        result += test("HeadersBodyCRLF.CR, LF", b1, b2);

        // Remainder
        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\nBody\r\nbody2\r\n.\r\nRCPT TO:<foo@moo>\r\n".getBytes());
        result += test("Good, all in one", b1);

        // No headers case
        b1 = ByteBuffer.wrap("\r\nBody\r\nbody2\r\n.\r\n".getBytes());
        result += test("No Headers", b1);

        // no body case
        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\n.\r\n".getBytes());
        result += test("No Body", b1);

        // blank message case
        b1 = ByteBuffer.wrap("\r\n.\r\n".getBytes());
        result += test("Blank Msg", b1);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\nBody\r\nbody2\r\n..\r\n\r\n..foo\r\n.\r\n".getBytes());
        result += test("All in one, dot escaping", b1);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\nBody\r\nbody2\r".getBytes());
        b2 = ByteBuffer.wrap("\n..\r\n\r\n..foo\r\n.\r\n".getBytes());
        result += test("HeaderBodyCR, LF..CRLFBody", b1, b2);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\nBody\r\nbody2\r\n".getBytes());
        b2 = ByteBuffer.wrap("..\r\n\r\n..foo\r\n.\r\n".getBytes());
        result += test("HeaderBodyCRLF, ..CRLFBody", b1, b2);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\nBody\r\nbody2\r\n.".getBytes());
        b2 = ByteBuffer.wrap(".\r\n\r\n..foo\r\n.\r\n".getBytes());
        result += test("HeaderBodyCRLF., .CRLFBody", b1, b2);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\nBody\r\nbody2\r\n..".getBytes());
        b2 = ByteBuffer.wrap("\r\n\r\n..foo\r\n.\r\n".getBytes());
        result += test("HeaderBodyCRLF., .CRLFBody", b1, b2);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\nBody\r\nbody2\r".getBytes());
        b2 = ByteBuffer.wrap("\n..XX\r\n..foo\r\n.\r\n".getBytes());
        result += test("HeaderBodyCR, LF..XXBody", b1, b2);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\nBody\r\nbody2\r\n".getBytes());
        b2 = ByteBuffer.wrap("..XX\r\n..foo\r\n.\r\n".getBytes());
        result += test("HeaderBodyCRLF, ..XXBody", b1, b2);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\nBody\r\nbody2\r\n.".getBytes());
        b2 = ByteBuffer.wrap(".XX\r\n..foo\r\n.\r\n".getBytes());
        result += test("HeaderBodyCRLF., .XXBody", b1, b2);

        b1 = ByteBuffer.wrap("FOO: moo\r\nDOO: goo\r\n\r\nBody\r\nbody2\r\n..".getBytes());
        b2 = ByteBuffer.wrap("XX\r\n..foo\r\n.\r\n".getBytes());
        result += test("HeaderBodyCRLF.., XXBody", b1, b2);

        b1 = ByteBuffer.wrap("BODY\r\n.\r\n".getBytes());
        result += test("No Headers (bleed into body)", b1);

        return result;
    }

    /**
     * Run test
     * @param  desc      description of test.
     * @param  bufs      Buffers to test with.
     * @return           String of results.
     * @throws Exception If issues.
     */
    private static String test(String desc, ByteBuffer... bufs) throws Exception
    {
        String result = "\n\n\n==============================================\nBEGIN TEST " + desc + "\n";

        for (int i = 0; i < bufs.length; i++) {
            printBuffer(bufs[i], "Source " + i);
        }
        result += "Begin calling our scanner\n";
        MessageBoundaryScanner scanner = new MessageBoundaryScanner();

        for (int i = 0; i < bufs.length; i++) {
            result += "Iterate on buffer " + i + "\n";
            switch (scanner.getState()) {
                case INIT:
                case LOOKING_FOR_HEADERS_END:
                    if (scanner.processHeaders(bufs[i], 1024)) {
                        result += "Consumed Header on buffer: " + i + "\n";
                        if (bufs[i].hasRemaining()) {
                            i--;
                            result += "Decrement i (" + i + ") to revisit this buffer\n";
                            break;
                        }
                    } else {
                        result += "Not enough bytes in buffer: " + i + " for header\n";
                        if (i + 1 >= bufs.length) {
                            result += "\n\n*************\n***ERROR*** Wants more bytes "
                                    + "(there are not any)\n*************\n";
                            return result;
                        }
                        if (bufs[i].hasRemaining()) {
                            int rem = bufs[i].remaining();
                            ByteBuffer newBuf = ByteBuffer.allocate(rem + bufs[i + 1].remaining());
                            newBuf.put(bufs[i]);
                            newBuf.put(bufs[i + 1]);
                            bufs[i + 1] = newBuf;
                            bufs[i + 1].flip();
                            result += "Moved remainder (" + rem + ") into next buffer\n";
                        }
                    }
                    break;
                case INIT_BODY:
                case LOOKING_FOR_BODY_END:
                    ByteBuffer sink = ByteBuffer.allocate(bufs[i].remaining());
                    if (scanner.processBody(bufs[i], sink)) {
                        sink.flip();
                        result += "Read end of message on buffer: " + i + "\n";
                        printBuffer(sink, "(last) Body ChunkToken");
                        printBuffer(bufs[i], "Remaining");
                    } else {
                        sink.flip();
                        result += "Not enough bytes in buffer: " + i + " for body\n";
                        printBuffer(sink, "BodyChunkToken");
                        printBuffer(bufs[i], "Remaining");
                        if (i + 1 >= bufs.length) {
                            result += "\n\n*************\n***ERROR*** Wants more bytes "
                                    + "(there are not any)\n*************\n";
                            return result;
                        }
                        if (bufs[i].hasRemaining()) {
                            int rem = bufs[i].remaining();
                            ByteBuffer newBuf = ByteBuffer.allocate(rem + bufs[i + 1].remaining());
                            newBuf.put(bufs[i]);
                            newBuf.put(bufs[i + 1]);
                            bufs[i + 1] = newBuf;
                            bufs[i + 1].flip();
                            result += "Moved remainder (" + rem + ") into next buffer\n";
                        }
                    }
                    break;
                case DONE:
                    result += "\n\n*************\n***ERROR*** Should not hit DONE state " + "\n*************\n";
                    break;
            }
        }

        return result + "Summary:  MsgEmpty? " + scanner.isEmptyMessage() + ", headers empty? "
                + scanner.isHeadersBlank() + "\n";
    }

    /**
     * Display the buffer.
     * @param  pBuf ByteBuffer to display.
     * @param  txt  Identifier used in result.
     * @return      String of result.
     */
    private static String printBuffer(ByteBuffer pBuf, String txt)
    {
        String result = "";
        int bufLen = pBuf.remaining();
        ByteBuffer buf = pBuf.duplicate();
        ByteBuffer dup = buf.duplicate();
        boolean nonASCII = false;
        while (dup.hasRemaining()) {
            byte b = dup.get();
            if (!isPrintableASCII(b)) {
                nonASCII = true;
                break;
            }
        }
        StringBuilder sb = new StringBuilder();
        if (nonASCII) {
            while (buf.hasRemaining()) {
                byte b = buf.get();
                if (isPrintableASCII(b)) {
                    sb.append("\"" + (char) b + "\"");
                } else {
                    sb.append("UNPRINT: " + (int) b);
                }
                sb.append('\n');
            }
        } else {
            while (buf.hasRemaining()) {
                byte b = buf.get();
                if (b == CR) {
                    if (buf.hasRemaining()) {
                        if (buf.get(buf.position()) == LF) {
                            buf.get();
                        }
                    }
                    sb.append(LF);
                } else {
                    sb.append((char) b);
                }
            }
        }
        result += "\t\t----BEGIN " + txt + " (" + bufLen + " bytes)-----\n";
        result += sb.toString() + "\n";
        return result + "\t\t----ENDOF " + txt + "-----\n";
    }

    /**
     * Determine if bye is printable ASCII.
     * @param  b Byte to test.
     * @return   true if printable ASCII, false otherwise.
     */
    private static boolean isPrintableASCII(byte b)
    {
        return (b > 31 && b < 127) || (b == CR || b == LF || b == HTAB);
    }

}
