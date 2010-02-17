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

package com.untangle.node.mail.papi;

import static com.untangle.node.util.Ascii.CR;
import static com.untangle.node.util.Ascii.DOT;
import static com.untangle.node.util.Ascii.LF;

import java.nio.ByteBuffer;

/**
 * Comical name.  Stateful class which
 * transfers bytes from a source to a
 * sink buffer, performing "byte stuffing",
 * making sure lines starting with "." are
 * converted to "..".
 * <br>
 * Not threadsafe.
 */
public class ByteBufferByteStuffer {

    private final byte[] CRLF_DOT_CRLF = new byte[] {
        (byte) CR, (byte) LF, (byte) DOT, (byte) CR, (byte) LF
    };

    //---------------------------------
    //Symbols for interesting data.
    //Squished into a zero-based form
    //to make transition table easier
    private static final int R_CR = 0;//Read a CR
    private static final int R_LF = 1;//Read an LF
    private static final int R_DT = 2;//Read a dot
    private static final int R_NO = 3;//Read nothing

    //------------------------------------
    //Symbols for state, indicative of
    //the pattern just observed
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

    //------------------------------------
    // Symbols for actions.  I cannot think
    // of easy names, so they are a bit
    // cryptic
    private static final int A_00 = 0;//Unused, 'cause my initial table on paper started at 1
    private static final int A_01 = 1;//Change state to HDR_INIT_CR
    private static final int A_02 = 2;//Change state to BDY_NONE
    private static final int A_03 = 3;//Change state to HDR_NONE
    private static final int A_04 = 4;//Change state to BDY_CRLF
    private static final int A_05 = 5;//Change state to HDR_CR
    private static final int A_06 = 6;//Change state to HDR_LF
    private static final int A_07 = 7;//Change state to HDR_CRLF
    private static final int A_08 = 8;//Change state to BDY_CR
    private static final int A_09 = 9;//Change state to BDY_LF
    private static final int A_10 = 10;//Change state to HDR_CRLFCR
    private static final int A_11 = 11;//Change state to BDY_CRLF
    private static final int A_12 = 12;//Write dot, change state to BDY_NONE

    //==========================================
    // Transition Table
    private static final int[][] ACTION_TBL = {
        //R_CR  R_LF  R_DT  R_NO
        {A_01, A_09, A_03, A_03},//HDR_INIT
        {A_08, A_04, A_12, A_02},//HDR_INIT_CR
        {A_05, A_06, A_03, A_03},//HDR_NONE
        {A_08, A_07, A_03, A_03},//HDR_CR
        {A_05, A_09, A_03, A_03},//HDR_LF
        {A_10, A_06, A_03, A_03},//HDR_CRLF
        {A_05, A_11, A_03, A_03},//HDR_CRLFCR
        {A_08, A_09, A_02, A_02},//BDY_NONE
        {A_08, A_11, A_12, A_02},//BDY_CR
        {A_08, A_09, A_12, A_02},//BDY_LF
        {A_08, A_09, A_12, A_02} //BDY_CRLF
    };

    private int m_state = HDR_INIT;

    private DynBB m_leftover;

    public ByteBufferByteStuffer() {
        m_leftover = new DynBB();
    }

    /**
     * Returns true if this Object has
     * remaining bytes.
     */
    public boolean hasLeftover() {
        return !m_leftover.isEmpty();
    }

    /**
     * Number of bytes left over
     */
    public int getLeftoverCount() {
        return m_leftover.available();
    }

    /**
     * Get any queued bytes.  Optionaly,
     * include the CRLF.CRLF in the
     * returned buffer.
     * <br>
     * Returned buffer is ready for reading
     */
    public ByteBuffer getLast(boolean includeBodyTerm) {
        ByteBuffer ret = ByteBuffer.allocate(m_leftover.available() +
                                             (includeBodyTerm?5:0));
        m_leftover.transferTo(ret);
        if(includeBodyTerm) {
            ret.put(CRLF_DOT_CRLF);
        }
        ret.flip();
        return ret;
    }

    /**
     * Tell this stuffer that it it should consider the
     * headers already passed.
     */
    public void advancePastHeaders() {
        m_state = BDY_CRLF;
    }

    /**
     * Transfer bytes from source to sink.  All
     * available bytes from source will be consumed,
     * even if there is no room in the sink.  Instead,
     * any extra bytes will be buffered by this class.
     * As such, one must also end the use of this
     * Object with {@link #getLast getLast()} which
     * transfers the remaining bytes.
     * <br><br>
     * The returned buffer is ready for reading (already flipped).
     *
     * @return the number of bytes queued
     */
    public int transfer(ByteBuffer source,
                        ByteBuffer sink) {

        if(!m_leftover.isEmpty()) {
            m_leftover.transferTo(sink);
        }

        while(source.hasRemaining()) {
            byte b = source.get();
            if(sink.hasRemaining()) {
                sink.put(b);
            }
            else {
                m_leftover.put(b);
            }

            int action = A_00;//Illegal, but at least we'll catch it on assert

            //Access transition table to determine
            //the correct action based on the byte
            if(b == CR) {
                action = ACTION_TBL[m_state][R_CR];
            }
            else if(b == LF) {
                action = ACTION_TBL[m_state][R_LF];
            }
            else if(b == DOT) {
                action = ACTION_TBL[m_state][R_DT];
            }
            else {
                action = ACTION_TBL[m_state][R_NO];
            }

            //Execute the action
            switch(action) {
            case A_00:
                throw new RuntimeException("Illegal action 0.");//Bug in the software
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
                if(sink.hasRemaining()) {
                    sink.put((byte) DOT);
                }
                else {
                    m_leftover.put((byte) DOT);
                }
                m_state = BDY_NONE;
                break;
            default:
                throw new RuntimeException("Illegal action" + action);//Bug in the software
            }
        }
        sink.flip();
        return m_leftover.available();
    }


    private class DynBB {
        private byte[] m_buf;
        private int m_pos;

        DynBB() {
            m_buf = new byte[1024];
            m_pos = 0;
        }
        boolean isEmpty() {
            return m_pos == 0;
        }
        int available() {
            return m_pos;
        }
        void transferTo(ByteBuffer buf) {
            int toTransfer = available() > buf.remaining()?
                buf.remaining():available();

            for(int i = 0; i<toTransfer; i++) {
                buf.put(m_buf[i]);
            }

            //Check if we need to recompress this buffer
            if(toTransfer < available()) {
                System.arraycopy(m_buf, toTransfer, m_buf, 0, m_pos - toTransfer);
                m_pos = m_pos - toTransfer;
            }
            else {
                m_pos = 0;
            }

        }
        void put(byte b) {
            if(m_pos >= m_buf.length) {
                byte[] newBuf = new byte[m_buf.length + 1024];
                System.arraycopy(m_buf, 0, newBuf, 0, m_buf.length);
                m_buf = newBuf;
            }
            m_buf[m_pos++] = b;
        }
        
        @SuppressWarnings("unused")
        /* Added for my tests */
        void put(byte[] bytes, int start, int len) {
            for(int i = 0; i<len; i++) {
                put(bytes[start+i]);
            }
        }
    }

    /*
    //=========== BEGIN TEST CODE ======================

    public static void main(String[] args)
    throws Exception {

    String crlf = new String(CRLF_BA);
    String cr = new String(new byte[] {CR});
    String lf = new String(new byte[] {LF});
    String dot = new String(new byte[] {DOT});
    String term = crlf+dot+crlf;

    //
    String inputStr;
    String outputStr;

    //Basic
    inputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + "Body";
    outputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + "Body" + term;
    doTest("Terminator only", inputStr.getBytes(), outputStr.getBytes());

    inputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + "Body1" + crlf + dot + "Body2";
    outputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + "Body1" + crlf + dot + dot + "Body2" + term;
    doTest("Escaped line", inputStr.getBytes(), outputStr.getBytes());

    //Body ends in CRLF
    inputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + "Body" + crlf;
    outputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + "Body" + crlf + term;
    doTest("Terminator only, body ends in crlf", inputStr.getBytes(), outputStr.getBytes());

    inputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + "Body1" + crlf + dot + "Body2" + crlf;
    outputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + "Body1" + crlf + dot + dot + "Body2" + crlf + term;
    doTest("Escaped line, body ends in crlf", inputStr.getBytes(), outputStr.getBytes());

    //CR lines
    inputStr = "foo:goo" + cr + "doo:foo" + cr + cr + "Body" + cr;
    outputStr = "foo:goo" + cr + "doo:foo" + cr + cr + "Body" + cr + term;
    doTest("CR lines, terminator only", inputStr.getBytes(), outputStr.getBytes());

    inputStr = "foo:goo" + cr + "doo:foo" + cr + cr + "Body1" + cr + dot + "Body2";
    outputStr = "foo:goo" + cr + "doo:foo" + cr + cr + "Body1" + cr + dot + dot + "Body2" + term;
    doTest("CR lines, escaped line", inputStr.getBytes(), outputStr.getBytes());

    //LF lines
    inputStr = "foo:goo" + lf + "doo:foo" + lf + lf + "Body" + lf;
    outputStr = "foo:goo" + lf + "doo:foo" + lf + lf + "Body" + lf + term;
    doTest("LF lines, terminator only", inputStr.getBytes(), outputStr.getBytes());

    inputStr = "foo:goo" + lf + "doo:foo" + lf + lf + "Body1" + lf + dot + "Body2";
    outputStr = "foo:goo" + lf + "doo:foo" + lf + lf + "Body1" + lf + dot + dot + "Body2" + term;
    doTest("LF lines, escaped line", inputStr.getBytes(), outputStr.getBytes());

    //No header
    inputStr = crlf + "Body";
    outputStr = crlf + "Body" + term;
    doTest("No header", inputStr.getBytes(), outputStr.getBytes());

    inputStr = cr + "Body";
    outputStr = cr + "Body" + term;
    doTest("No header (CR lines)", inputStr.getBytes(), outputStr.getBytes());

    inputStr = lf + "Body";
    outputStr = lf + "Body" + term;
    doTest("No header (LF lines)", inputStr.getBytes(), outputStr.getBytes());

    inputStr = crlf + dot + "Body";
    outputStr = crlf + dot + dot + "Body" + term;
    doTest("No header (first line dot)", inputStr.getBytes(), outputStr.getBytes());

    inputStr = cr + dot + "Body";
    outputStr = cr + dot + dot + "Body" + term;
    doTest("No header (first line dot, CR lines)", inputStr.getBytes(), outputStr.getBytes());

    inputStr = lf + dot + "Body";
    outputStr = lf + dot + dot + "Body" + term;
    doTest("No header (first line dot, LF lines)", inputStr.getBytes(), outputStr.getBytes());

    //Dot in header
    inputStr = "foo:goo" + crlf + dot + "doo:foo" + crlf + crlf + "BODY";
    outputStr = "foo:goo" + crlf + dot + "doo:foo" + crlf + crlf + "BODY" + term;
    doTest("Dot line in header", inputStr.getBytes(), outputStr.getBytes());

    inputStr = "foo:goo" + cr + dot + "doo:foo" + cr + cr + "BODY";
    outputStr = "foo:goo" + cr + dot + "doo:foo" + cr + cr + "BODY" + term;
    doTest("Dot line in header (CR lines)", inputStr.getBytes(), outputStr.getBytes());

    inputStr = "foo:goo" + lf + dot + "doo:foo" + lf + lf + "BODY";
    outputStr = "foo:goo" + lf + dot + "doo:foo" + lf + lf + "BODY" + term;
    doTest("Dot line in header (LF lines)", inputStr.getBytes(), outputStr.getBytes());

    //No body
    inputStr = "foo:goo" + crlf + "doo:foo" + crlf;
    outputStr = "foo:goo" + crlf + "doo:foo" + crlf + term;
    doTest("No body, header not terminated", inputStr.getBytes(), outputStr.getBytes());

    inputStr = "foo:goo" + cr + "doo:foo" + cr;
    outputStr = "foo:goo" + cr + "doo:foo" + cr + term;
    doTest("No body, header not terminated (CR lines)", inputStr.getBytes(), outputStr.getBytes());

    inputStr = "foo:goo" + lf + "doo:foo" + lf;
    outputStr = "foo:goo" + lf + "doo:foo" + lf + term;
    doTest("No body, header not terminated (LF lines)", inputStr.getBytes(), outputStr.getBytes());

    inputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf;
    outputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + term;
    doTest("No body, header terminated", inputStr.getBytes(), outputStr.getBytes());

    inputStr = "foo:goo" + cr + "doo:foo" + cr + cr;
    outputStr = "foo:goo" + cr + "doo:foo" + cr + cr + term;
    doTest("No body, header terminated (CR lines)", inputStr.getBytes(), outputStr.getBytes());

    inputStr = "foo:goo" + lf + "doo:foo" + lf + lf;
    outputStr = "foo:goo" + lf + "doo:foo" + lf + lf + term;
    doTest("No body, header terminated (LF lines)", inputStr.getBytes(), outputStr.getBytes());


    //Blank
    inputStr = "";
    outputStr = term;
    doTest("No header or body", inputStr.getBytes(), outputStr.getBytes());


    }

    private static void doTest(String name,
    byte[] input,
    byte[] expectedOut) {
    int len = Math.min(input.length, 5);
    for(int i = 1; i<len; i++) {
    test(name, input, expectedOut, i);
    }
    }

    //Performs test on the given input/output,
    //by trying all combinations of array sizes
    //for the given number of buffers.
    private static void test(String name,
    byte[] input,
    byte[] expectedOut,
    int numBuffers) {

    CaseHolder ch = new CaseHolder(numBuffers, input.length);

    while(ch.hasNext()) {
    int[] arraySizes = ch.next();
    String subName = name + arraySizesToString(arraySizes);
    List<ByteBuffer> bufs = new ArrayList<ByteBuffer>();
    int lenSoFar = 0;
    for(int i = 0; i<arraySizes.length; i++) {
    bufs.add(ByteBuffer.wrap(input, lenSoFar, arraySizes[i]));
    lenSoFar+=arraySizes[i];
    }
    byte[] ret = test(subName, bufs);
    if(!arrayCompare(ret, expectedOut)) {
    System.out.println("\n=======================================");
    System.out.println("Failure on " + subName);
    System.err.println("Failure on " + subName);
    printArraysSBS(expectedOut, ret);
    System.out.println("=======================================\n");
    }
    else {
    System.out.println("\n=======================================");
    System.out.println(subName + " passed");
    System.out.println("=======================================\n");
    }
    }
    }

    //Performs the test.  Returns the output
    //from the byte stuffer (including terminator)
    private static byte[] test(String name,
    List<ByteBuffer> bufs) {

    List<ByteBuffer> outputBufs = new ArrayList<ByteBuffer>();

    ByteBufferByteStuffer bbs = new ByteBufferByteStuffer();
    int sinkSz = 0;
    for(ByteBuffer buf : bufs) {
    ByteBuffer sink = ByteBuffer.allocate(buf.remaining());
    bbs.transfer(buf, sink);
    sink.flip();
    sinkSz+=sink.remaining();
    outputBufs.add(sink);
    if(buf.hasRemaining()) {
    System.out.println(name + " error.  Source had remaining");
    }
    }
    ByteBuffer remainder = bbs.getLast(true);
    sinkSz+=remainder.remaining();
    outputBufs.add(remainder);

    byte[] ret = new byte[sinkSz];
    int i = 0;
    for(ByteBuffer buf : outputBufs) {
    while(buf.hasRemaining()) {
    ret[i++] = buf.get();
    }
    }
    return ret;
    }

    //Pretty-prints the size of the array
    private static String arraySizesToString(int[] a) {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    for(int i : a) {
    sb.append(" " + i + " ");
    }
    sb.append(")");
    return sb.toString();
    }

    //Prints to arrays side-by-side for comparison
    private static void printArraysSBS(byte[] expected, byte[] found) {
    int len = Math.max(expected.length, found.length);

    System.out.println(" expected     output");

    for(int i = 0; i<len; i++) {
    String str = null;
    if(i < expected.length) {
    byte b = expected[i];
    if(b > 31 && b < 127) {
    str = new String(new byte[] {b, SP, SP});
    }
    else {
    str = "-?-";
    }
    System.out.print(btoiPad(expected[i]) + " (" + str + ")   ");
    }
    else {
    System.out.print("  <EOF>     ");
    }
    if(i < found.length) {
    byte b = found[i];
    if(b > 31 && b < 127) {
    str = new String(new byte[] {b, SP, SP});
    }
    else {
    str = "-?-";
    }
    System.out.print(btoiPad(found[i]) + " (" + str + ") ");
    }
    else {
    System.out.print("  <EOF>  ");
    }
    System.out.println();
    }
    }
    //Pads a byte to 4 characters
    private static String btoiPad(byte b) {
    String ret = "" + (int) b;
    while(ret.length() < 4) {
    ret+=" ";
    }
    return ret;
    }


    private static boolean arrayCompare(byte[] a, byte[] b) {
    if(a.length != b.length) {
    return false;
    }
    for(int i = 0; i<a.length; i++) {
    if(a[i] != b[i]) {
    return false;
    }
    }
    return true;
    }

    //Stateful class which acts as a factory
    //for all combinations of array sizes.
    //Works by ensuring that any given
    //value in the array is always greater
    //than zero, and the sum of the array
    //is constant.
    private static class CaseHolder {
    private int m_ptr;
    private int[] m_vals;
    private boolean m_hasNext;

    CaseHolder(int len, int sum) {
    m_vals = new int[len];
    for(int i = 0; i<len; i++) {
    m_vals[i] = 1;
    }
    m_vals[len-1] = sum - len + 1;
    m_hasNext = true;
    }

    boolean hasNext() {
    return m_hasNext;
    }

    int[] next() {
    int[] ret = new int[m_vals.length];
    System.arraycopy(m_vals, 0, ret, 0, ret.length);
    m_hasNext = makeNext(0);
    return ret;
    }
    boolean makeNext(int ptr) {
    if(ptr+1 == m_vals.length) {
    return false;
    }
    if(m_vals[ptr+1] > 1) {
    //Pivot the value down
    m_vals[ptr]++;
    m_vals[ptr+1]--;
    return true;
    }
    else {
    //Move current quantity up
    //one and attempt to pivot again
    //on the next slot
    m_vals[ptr+1]+=(m_vals[ptr] - 1);
    m_vals[ptr]=1;
    return makeNext(ptr+1);
    }
    }

    }

    //=========== ENDOF TEST CODE ======================
    */
}
