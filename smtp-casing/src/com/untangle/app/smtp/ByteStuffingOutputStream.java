/**
 * $Id$
 */
package com.untangle.app.smtp;

import static com.untangle.uvm.util.Ascii.CR;
import static com.untangle.uvm.util.Ascii.CRLF_BA;
import static com.untangle.uvm.util.Ascii.DOT;
import static com.untangle.uvm.util.Ascii.LF;
import static com.untangle.uvm.util.Ascii.SP;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Output stream which performs "byte stuffing", the process whereby RFC822 messages with body lines starting with "."
 * are apped to ".."
 */
public class ByteStuffingOutputStream extends FilterOutputStream
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
    private static final int A_00 = 0;// Unused, 'cause my initial table on
                                      // paper started at 1
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
    private long m_count;

    /**
     * Initialzie ByteStuffingOutputStream.
     * 
     * @param  out OutputStream to initialize with.
     * @return     Instance of ByteStuffingOutputStream.
     */
    public ByteStuffingOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * Get the current number of bytes written. This may be more than the bytes read, as we escape dotted lines. Note
     * also that the {@link #terminateBody termination} is not part of this count.
     *
     * @return Long of bytes written.
     */
    public long count()
    {
        return m_count;
    }

    /**
     * Cause a CRLF.CRLF to be written to the wrapped stream. Note that this is <b>not</b> counted in the overall
     * {@link #count byte count}.
     * @throws IOException
     */
    public void terminateBody() throws IOException
    {
        // Note: Should we be "smart" about
        // the last chars being CRLF? I don't
        // think so, as they would have belonged
        // to the body.
        out.write(CRLF_DOT_CRLF);
    }

    /**
     * Write to buffer.
     *
     * @param  b           Integer value to write.
     * @throws IOException If IO problem occurs.
     */
    @Override
    public void write(final int b) throws IOException
    {

        // Always write-out the byte
        out.write(b);
        m_count++;

        int action = A_00;// Illegal, but at least we'll catch it on assert

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
            case A_00:
                throw new IOException("Illegal action 0.");// Bug in the
                                                           // software
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
                throw new IOException("Illegal action" + action);// Bug in the
                                                                 // software
        }
    }

    /************** Tests ******************/

    /**
     * Run tests
     * 
     * @param  args      String to run with.
     * @return           String of result of test.
     * @throws Exception If unable to pass.
     */
    public static String runTest(String[] args) throws Exception
    {
        String result = "";

        String crlf = new String(CRLF_BA);
        String cr = new String(new byte[] { CR });
        String lf = new String(new byte[] { LF });
        String dot = new String(new byte[] { DOT });
        String term = crlf + dot + crlf;

        //
        String inputStr;
        String outputStr;

        // Basic
        inputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + "Body";
        outputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + "Body" + term;
        result += doTest("Terminator only", inputStr.getBytes(), outputStr.getBytes());

        inputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + "Body1" + crlf + dot + "Body2";
        outputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + "Body1" + crlf + dot + dot + "Body2" + term;
        result += doTest("Escaped line", inputStr.getBytes(), outputStr.getBytes());

        // Body ends in CRLF
        inputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + "Body" + crlf;
        outputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + "Body" + crlf + term;
        result += doTest("Terminator only, body ends in crlf", inputStr.getBytes(), outputStr.getBytes());

        inputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + "Body1" + crlf + dot + "Body2" + crlf;
        outputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + "Body1" + crlf + dot + dot + "Body2" + crlf + term;
        result += doTest("Escaped line, body ends in crlf", inputStr.getBytes(), outputStr.getBytes());

        // CR lines
        inputStr = "foo:goo" + cr + "doo:foo" + cr + cr + "Body" + cr;
        outputStr = "foo:goo" + cr + "doo:foo" + cr + cr + "Body" + cr + term;
        result += doTest("CR lines, terminator only", inputStr.getBytes(), outputStr.getBytes());

        inputStr = "foo:goo" + cr + "doo:foo" + cr + cr + "Body1" + cr + dot + "Body2";
        outputStr = "foo:goo" + cr + "doo:foo" + cr + cr + "Body1" + cr + dot + dot + "Body2" + term;
        result += doTest("CR lines, escaped line", inputStr.getBytes(), outputStr.getBytes());

        // LF lines
        inputStr = "foo:goo" + lf + "doo:foo" + lf + lf + "Body" + lf;
        outputStr = "foo:goo" + lf + "doo:foo" + lf + lf + "Body" + lf + term;
        result += doTest("LF lines, terminator only", inputStr.getBytes(), outputStr.getBytes());

        inputStr = "foo:goo" + lf + "doo:foo" + lf + lf + "Body1" + lf + dot + "Body2";
        outputStr = "foo:goo" + lf + "doo:foo" + lf + lf + "Body1" + lf + dot + dot + "Body2" + term;
        result += doTest("LF lines, escaped line", inputStr.getBytes(), outputStr.getBytes());

        // No header
        inputStr = crlf + "Body";
        outputStr = crlf + "Body" + term;
        result += doTest("No header", inputStr.getBytes(), outputStr.getBytes());

        inputStr = cr + "Body";
        outputStr = cr + "Body" + term;
        result += doTest("No header (CR lines)", inputStr.getBytes(), outputStr.getBytes());

        inputStr = lf + "Body";
        outputStr = lf + "Body" + term;
        result += doTest("No header (LF lines)", inputStr.getBytes(), outputStr.getBytes());

        inputStr = crlf + dot + "Body";
        outputStr = crlf + dot + dot + "Body" + term;
        result += doTest("No header (first line dot)", inputStr.getBytes(), outputStr.getBytes());

        inputStr = cr + dot + "Body";
        outputStr = cr + dot + dot + "Body" + term;
        result += doTest("No header (first line dot, CR lines)", inputStr.getBytes(), outputStr.getBytes());

        inputStr = lf + dot + "Body";
        outputStr = lf + dot + dot + "Body" + term;
        result += doTest("No header (first line dot, LF lines)", inputStr.getBytes(), outputStr.getBytes());

        // Dot in header
        inputStr = "foo:goo" + crlf + dot + "doo:foo" + crlf + crlf + "BODY";
        outputStr = "foo:goo" + crlf + dot + "doo:foo" + crlf + crlf + "BODY" + term;
        result += doTest("Dot line in header", inputStr.getBytes(), outputStr.getBytes());

        inputStr = "foo:goo" + cr + dot + "doo:foo" + cr + cr + "BODY";
        outputStr = "foo:goo" + cr + dot + "doo:foo" + cr + cr + "BODY" + term;
        result += doTest("Dot line in header (CR lines)", inputStr.getBytes(), outputStr.getBytes());

        inputStr = "foo:goo" + lf + dot + "doo:foo" + lf + lf + "BODY";
        outputStr = "foo:goo" + lf + dot + "doo:foo" + lf + lf + "BODY" + term;
        result += doTest("Dot line in header (LF lines)", inputStr.getBytes(), outputStr.getBytes());

        // No body
        inputStr = "foo:goo" + crlf + "doo:foo" + crlf;
        outputStr = "foo:goo" + crlf + "doo:foo" + crlf + term;
        result += doTest("No body, header not terminated", inputStr.getBytes(), outputStr.getBytes());

        inputStr = "foo:goo" + cr + "doo:foo" + cr;
        outputStr = "foo:goo" + cr + "doo:foo" + cr + term;
        result += doTest("No body, header not terminated (CR lines)", inputStr.getBytes(), outputStr.getBytes());

        inputStr = "foo:goo" + lf + "doo:foo" + lf;
        outputStr = "foo:goo" + lf + "doo:foo" + lf + term;
        result += doTest("No body, header not terminated (LF lines)", inputStr.getBytes(), outputStr.getBytes());

        inputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf;
        outputStr = "foo:goo" + crlf + "doo:foo" + crlf + crlf + term;
        result += doTest("No body, header terminated", inputStr.getBytes(), outputStr.getBytes());

        inputStr = "foo:goo" + cr + "doo:foo" + cr + cr;
        outputStr = "foo:goo" + cr + "doo:foo" + cr + cr + term;
        result += doTest("No body, header terminated (CR lines)", inputStr.getBytes(), outputStr.getBytes());

        inputStr = "foo:goo" + lf + "doo:foo" + lf + lf;
        outputStr = "foo:goo" + lf + "doo:foo" + lf + lf + term;
        result += doTest("No body, header terminated (LF lines)", inputStr.getBytes(), outputStr.getBytes());

        // Blank
        inputStr = "";
        outputStr = term;
        result += doTest("No header or body", inputStr.getBytes(), outputStr.getBytes());
        return result;

    }

    /**
     * Perform the test.
     *
     * @param  name           String of name of test.
     * @param  inputPattern   Array of byte to test.
     * @param  expectedOutput Array of expected bytes.
     * @return                String result
     * @throws IOException    If IO error occurs.
     */
    private static String doTest(String name, byte[] inputPattern, byte[] expectedOutput) throws IOException
    {
        String result = "\n\n===============================================\n";
        result += "**** BEGIN " + name + " ****\n";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteStuffingOutputStream bsos = new ByteStuffingOutputStream(baos);
        bsos.write(inputPattern);
        bsos.terminateBody();
        bsos.flush();
        byte[] output = baos.toByteArray();

        if (arrayCompare(expectedOutput, output)) {
            result += "Passed\n";
        } else {
            result += "!!!!!!!!FAIL!!!!!!!!!!\n";
            result += "Print arrays\n";
            result += printArraysSBS(expectedOutput, output);
        }

        result += "**** ENDOF " + name + " ****\n";
        result += "\n\n===============================================\n";
        return result;
    }

    /**
     * Prints to arrays side-by-side for comparison
     *
     * @param  expected Array of expected bytes.
     * @param  found    Array of found bytes
     * @return          String of expected output.
     */
    public static String printArraysSBS(byte[] expected, byte[] found)
    {
        int len = Math.max(expected.length, found.length);

        String result = " expected     output\n";

        for (int i = 0; i < len; i++) {
            String str = null;
            if (i < expected.length) {
                byte b = expected[i];
                if (b > 31 && b < 127) {
                    str = new String(new byte[] { b, SP, SP });
                } else {
                    str = "-?-";
                }
                result += btoiPad(expected[i]) + " (" + str + ")   ";
            } else {
                result += "  <EOF>     ";
            }
            if (i < found.length) {
                byte b = found[i];
                if (b > 31 && b < 127) {
                    str = new String(new byte[] { b, SP, SP });
                } else {
                    str = "-?-";
                }
                result += btoiPad(found[i]) + " (" + str + ") ";
            } else {
                result += "  <EOF>  ";
            }
            result += "\n";
        }
        return result;
    }
    
    /**
     * Pads a byte to 4 characters
     *
     * @param  b Byte to padd
     * @return   Padded String length of 4.
     */
    private static String btoiPad(byte b)
    {
        String ret = "" + (int) b;
        while (ret.length() < 4) {
            ret += " ";
        }
        return ret;
    }
    
    /**
     * Compare two arrays of bytes.
     *
     * @param  a  Byte array to compare.
     * @param  b  Byte array to compare.
     * @return   true if match, false otherwise.
     */
    private static boolean arrayCompare(byte[] a, byte[] b)
    {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

}
