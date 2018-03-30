/**
 * $Id$
 */
package com.untangle.app.smtp;

import static com.untangle.uvm.util.Ascii.CR;
import static com.untangle.uvm.util.Ascii.DOT;
import static com.untangle.uvm.util.Ascii.LF;

import java.nio.ByteBuffer;

/**
 * Comical name. Stateful class which transfers bytes from a source to a sink buffer, performing "byte stuffing", making
 * sure lines starting with "." are converted to "..". <br>
 * Not threadsafe.
 */
public class ByteBufferByteStuffer
{

    private final byte[] CRLF_DOT_CRLF = new byte[] { (byte) CR, (byte) LF, (byte) DOT, (byte) CR, (byte) LF };

    // ---------------------------------
    // Symbols for interesting data.
    // Squished into a zero-based form
    // to make transition table easier
    private static final int R_CR = 0;// Read a CR
    private static final int R_LF = 1;// Read an LF
    private static final int R_DT = 2;// Read a dot
    private static final int R_NO = 3;// Read nothing

    // ------------------------------------
    // Symbols for state, indicative of
    // the pattern just observed
    private static final int HDR_INIT = 0;
    private static final int HDR_INIT_CR = 1;
    private static final int HDR_NONE = 2;
    private static final int HDR_CR = 3;
    private static final int HDR_LF = 4;
    private static final int HDR_CRLF = 5;
    private static final int HDR_CRLFCR = 6;
    private static final int BDY_NONE = 7;
    private static final int BDY_CR = 8;
    private static final int BDY_LF = 9;
    private static final int BDY_CRLF = 10;

    // ------------------------------------
    // Symbols for actions. I cannot think
    // of easy names, so they are a bit
    // cryptic
    
    private static final int A_01 = 1;// Change state to HDR_INIT_CR
    private static final int A_02 = 2;// Change state to BDY_NONE
    private static final int A_03 = 3;// Change state to HDR_NONE
    private static final int A_04 = 4;// Change state to BDY_CRLF
    private static final int A_05 = 5;// Change state to HDR_CR
    private static final int A_06 = 6;// Change state to HDR_LF
    private static final int A_07 = 7;// Change state to HDR_CRLF
    private static final int A_08 = 8;// Change state to BDY_CR
    private static final int A_09 = 9;// Change state to BDY_LF
    private static final int A_10 = 10;// Change state to HDR_CRLFCR
    private static final int A_11 = 11;// Change state to BDY_CRLF
    private static final int A_12 = 12;// Write dot, change state to BDY_NONE

    // ==========================================
    // Transition Table
    private static final int[][] ACTION_TBL = {
            // R_CR R_LF R_DT R_NO
            { A_01, A_09, A_03, A_03 },// HDR_INIT
            { A_08, A_04, A_12, A_02 },// HDR_INIT_CR
            { A_05, A_06, A_03, A_03 },// HDR_NONE
            { A_08, A_07, A_03, A_03 },// HDR_CR
            { A_05, A_09, A_03, A_03 },// HDR_LF
            { A_10, A_06, A_03, A_03 },// HDR_CRLF
            { A_05, A_11, A_03, A_03 },// HDR_CRLFCR
            { A_08, A_09, A_02, A_02 },// BDY_NONE
            { A_08, A_11, A_12, A_02 },// BDY_CR
            { A_08, A_09, A_12, A_02 },// BDY_LF
            { A_08, A_09, A_12, A_02 } // BDY_CRLF
    };

    private int m_state = HDR_INIT;

    private DynBB m_leftover;

    /**
     * Initalize bute buffer stuffer object.
     * @return Instance of ByteBufferByteStuffer.
     */
    public ByteBufferByteStuffer() {
        m_leftover = new DynBB();
    }

    /**
     * Determine if buffer has leftover data.
     *
     * @return true if this Object has remaining bytes.
     */
    public boolean hasLeftover()
    {
        return !m_leftover.isEmpty();
    }

    /**
     * Retrive number of bytes remaining.
     * @return Number of bytes left over
     */
    public int getLeftoverCount()
    {
        return m_leftover.available();
    }

    /**
     * Get any queued bytes. Optionaly, include the CRLF.CRLF in the returned buffer. <br>
     * Returned buffer is ready for reading
     *
     * @param includeBodyTerm If true, add the trailing CRLF.CRLF
     * @return ByteBuffer.
     */
    public ByteBuffer getLast(boolean includeBodyTerm)
    {
        ByteBuffer ret = ByteBuffer.allocate(m_leftover.available() + (includeBodyTerm ? 5 : 0));
        m_leftover.transferTo(ret);
        if (includeBodyTerm) {
            ret.put(CRLF_DOT_CRLF);
        }
        ret.flip();
        return ret;
    }

    /**
     * Tell this stuffer that it it should consider the headers already passed.
     */
    public void advancePastHeaders()
    {
        m_state = BDY_CRLF;
    }

    /**
     * Transfer bytes from source to sink. All available bytes from source will be consumed, even if there is no room in
     * the sink. Instead, any extra bytes will be buffered by this class. As such, one must also end the use of this
     * Object with {@link #getLast getLast()} which transfers the remaining bytes. <br>
     * <br>
     * The returned buffer is ready for reading (already flipped).
     *
     * @param source ByteBuffer source.
     * @param sink ByteBuffer destination.
     * @return the number of bytes queued
     */
    public int transfer(ByteBuffer source, ByteBuffer sink)
    {

        if (!m_leftover.isEmpty()) {
            m_leftover.transferTo(sink);
        }

        while (source.hasRemaining()) {
            byte b = source.get();
            if (sink.hasRemaining()) {
                sink.put(b);
            } else {
                m_leftover.put(b);
            }

            int action = ACTION_TBL[m_state][R_NO];;// Illegal, but at least we'll catch it on assert

            // Access transition table to determine
            // the correct action based on the byte
            if (b == CR) {
                action = ACTION_TBL[m_state][R_CR];
            } else if (b == LF) {
                action = ACTION_TBL[m_state][R_LF];
            } else if (b == DOT) {
                action = ACTION_TBL[m_state][R_DT];
            } else {
                action = ACTION_TBL[m_state][R_NO];
            }

            // Execute the action
            switch (action) {
                case A_01:
                    m_state = HDR_INIT_CR;
                    break;
                case A_02:
                    m_state = BDY_NONE;
                    break;
                case A_03:
                    m_state = HDR_NONE;
                    break;
                case A_04:
                    m_state = BDY_CRLF;
                    break;
                case A_05:
                    m_state = HDR_CR;
                    break;
                case A_06:
                    m_state = HDR_LF;
                    break;
                case A_07:
                    m_state = HDR_CRLF;
                    break;
                case A_08:
                    m_state = BDY_CR;
                    break;
                case A_09:
                    m_state = BDY_LF;
                    break;
                case A_10:
                    m_state = HDR_CRLFCR;
                    break;
                case A_11:
                    m_state = BDY_CRLF;
                    break;
                case A_12:
                    if (sink.hasRemaining()) {
                        sink.put((byte) DOT);
                    } else {
                        m_leftover.put((byte) DOT);
                    }
                    m_state = BDY_NONE;
                    break;
                default:
                    throw new RuntimeException("Illegal action" + action);// Bug
                                                                          // in
                                                                          // the
                                                                          // software
            }
        }
        sink.flip();
        return m_leftover.available();
    }

    /**
     * Dynamic byte buffer.
     */
    private class DynBB
    {
        private byte[] m_buf;
        private int m_pos;

        /**
         * Initialize DynBB.
         * 
         * @return DynBB instance.
         */
        DynBB() {
            m_buf = new byte[1024];
            m_pos = 0;
        }

        /**
         * Determines if buffer is empty.
         *
         * @return true if empty, false otherwise.
         */
        boolean isEmpty()
        {
            return m_pos == 0;
        }

        /**
         * Determines if more is available in buffer.
         * @return If 0, no more available, available otherwise.
         */
        int available()
        {
            return m_pos;
        }

        /**
         * Transfer buffer into passed buffer.
         * @param buf ByteBuffer to transfer into.
         */
        void transferTo(ByteBuffer buf)
        {
            int toTransfer = available() > buf.remaining() ? buf.remaining() : available();

            for (int i = 0; i < toTransfer; i++) {
                buf.put(m_buf[i]);
            }

            // Check if we need to recompress this buffer
            if (toTransfer < available()) {
                System.arraycopy(m_buf, toTransfer, m_buf, 0, m_pos - toTransfer);
                m_pos = m_pos - toTransfer;
            } else {
                m_pos = 0;
            }

        }

        /**
         * Add byte to the buffer.
         *
         * @param b byte to add into buffer.
         */
        void put(byte b)
        {
            if (m_pos >= m_buf.length) {
                byte[] newBuf = new byte[m_buf.length + 1024];
                System.arraycopy(m_buf, 0, newBuf, 0, m_buf.length);
                m_buf = newBuf;
            }
            m_buf[m_pos++] = b;
        }

        /**
         * Added for tests 
         *
         * @param bytes Array of bytes to add.
         * @param start Starting position to add.
         * @param len Length to add.
         */
        @SuppressWarnings("unused")
        void put(byte[] bytes, int start, int len)
        {
            for (int i = 0; i < len; i++) {
                put(bytes[start + i]);
            }
        }
    }
}
