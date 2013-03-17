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

package com.untangle.node.smtp;
import static com.untangle.node.util.Ascii.CR;
import static com.untangle.node.util.Ascii.DOT;
import static com.untangle.node.util.Ascii.LF;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Output stream which performs "byte stuffing", the
 * process whereby RFC822 messages with body lines
 * starting with "." are nodeed to ".."
 */
public class ByteStuffingOutputStream
    extends FilterOutputStream {

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
    private long m_count;

    public ByteStuffingOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * Get the current number of bytes written.  This
     * may be more than the bytes read, as we escape
     * dotted lines.  Note also that the
     * {@link #terminateBody termination} is not part
     * of this count.
     */
    public long count() {
        return m_count;
    }

    /**
     * Cause a CRLF.CRLF to be written to the
     * wrapped stream.  Note that this
     * is <b>not</b> counted in the overall
     * {@link #count byte count}.
     */
    public void terminateBody()
        throws IOException {
        //     Note: Should we be "smart" about
        //     the last chars being CRLF?  I don't
        //     think so, as they would have belonged
        //     to the body.
        out.write(CRLF_DOT_CRLF);
    }

    @Override
    public void write(final int b)
        throws IOException {

        //Always write-out the byte
        out.write(b);
        m_count++;

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
            throw new IOException("Illegal action 0.");//Bug in the software
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
            out.write(DOT);
            m_count++;
            m_state = BDY_NONE;
            break;
        default:
            throw new IOException("Illegal action" + action);//Bug in the software
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
    byte[] inputPattern,
    byte[] expectedOutput)
    throws IOException {
    System.out.println("\n\n===============================================");
    System.out.println("**** BEGIN " + name + " ****");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ByteStuffingOutputStream bsos = new ByteStuffingOutputStream(baos);
    bsos.write(inputPattern);
    bsos.terminateBody();
    bsos.flush();
    byte[] output = baos.toByteArray();

    if(arrayCompare(expectedOutput, output)) {
    System.out.println("Passed");
    }
    else {
    System.out.println("!!!!!!!!FAIL!!!!!!!!!!");
    System.out.println("Print arrays");
    printArraysSBS(expectedOutput, output);
    }

    System.out.println("**** ENDOF " + name + " ****");
    System.out.println("\n\n===============================================");

    }

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

    //=========== ENDOF TEST CODE ======================
    */
}
