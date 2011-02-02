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
import java.nio.ByteBuffer;


/**
 * Little class useful for testing stateful parsers.  Takes
 * an array and splits it into sub-arrays of varying length.  The
 * goal is to test a stateful parser through all cases of input
 * data.
 * <br><br>
 * This class is not efficent, and should not be used for
 * production code.
 */
public class ArrayTester {
    private int[] m_vals;
    private boolean m_hasNext;
    private byte[] m_sourceBytes;

    public ArrayTester(byte[] bytes,
                       int numChunks) {

        m_vals = new int[numChunks];
        for(int i = 0; i<numChunks; i++) {
            m_vals[i] = 1;
        }
        m_vals[numChunks-1] = bytes.length - numChunks + 1;
        m_hasNext = true;
        m_sourceBytes = bytes;
    }

    /**
     * Is there a next iteration available
     */
    public boolean hasNext() {
        return m_hasNext;
    }

    /**
     * Get the next ByteBuffer[]
     */
    public ByteBuffer[] nextBuffers() {
        byte[][] bytes = nextArrays();
        ByteBuffer[] ret = new ByteBuffer[bytes.length];
        for(int i = 0; i<bytes.length; i++) {
            ret[i] = ByteBuffer.wrap(bytes[i]);
        }
        return ret;
    }

    public byte[][] nextArrays() {
        int[] lens = nextArraySizes();
        byte[][] ret = new byte[lens.length][];
        int start = 0;
        for(int i = 0; i<lens.length; i++) {
            ret[i] = new byte[lens[i]];
            System.arraycopy(m_sourceBytes, start, ret[i], 0, lens[i]);
            start+=lens[i];
        }
        return ret;
    }

    public int[] nextArraySizes() {
        int[] ret = new int[m_vals.length];
        System.arraycopy(m_vals, 0, ret, 0, ret.length);
        m_hasNext = makeNext(0);
        return ret;
    }

    private boolean makeNext(int ptr) {
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

    /**
     * For a given number of chunks and the total size
     * of the input array ("num"), compute the number
     * of times {@link #hasNext hasNext} will return
     * true.
     *
     * @param chunks the number of chunks (arrays)
     * @param num the length of the array to be broken into
     *         <code>chunks</code> chunks.
     *
     * @return the number of iterations
     */
    public static long computeIterations(int chunks, int num) {

        //I'm sure this is some old, dead guy's theorm, but
        //here is the approach taken to computing the
        //number of iterations a given chunk/num pair combo
        //will result in.
        //
        //If you examine a sequence of arrays such as:
        //
        // 4 1 1
        // 3 2 1
        // 3 1 2
        // 2 3 1
        // 2 2 2
        // 2 1 3
        // 1 4 1
        // 1 3 2
        // 1 2 3
        // 1 1 4
        //
        //An interesting pattern comes out.  Looking at a lot of
        //such sequences (I did - I'll spare you) the occurances of
        //the first value follows a pattern.  Interestingly, the
        //pattern is independent of the number of items to
        //be put into the chunks.  It is only a function of the
        //number of chunks.  This pattern actualy nests, but that
        //is likely part of the same dead-guy's theorm.
        //
        //The pattern defines the sequence of occurances of
        //a given value in the first slot.  It begins with
        //a chunk size of 2 (a chunk size of 1 always totals to
        //1).  For a chunk size of 2, the pattern is:
        //
        //1
        //1
        //1
        //1
        //...
        //Always 1.  You use this as follows.  If there are 2 chunks and
        //5 items, the total will be 4.  This is the sum of the sequence
        //from index 0 -> #items-#chunks.  This magical sequence is "incremented"
        //to the next chunk size via adding index[n] + index[n-1] and
        //assigning it it index[n].  This is a "something" sequence like
        //Fibonacci I recall from math class, but have forgotten.  Here
        //is an example for 5 chunks
        //
        //1
        //4
        //10
        //20
        //35
        //56
        //84
        //
        //So the overall approach is as follows.  Compute the magical sequence,
        //by applying the "index[n]=index[n]+index[n-1]" thing to a sequence
        //of length "#items-#chunks+1" "#chunks-2" times.  Then, sum up
        //the values in index.  Here is an example for the same 5 chunk case
        //
        //   2    3    4     5
        //======================
        //   1    1    1     1   (always 1)
        //   1    2    3     4   (((1+1)+1)+1)
        //   1    3    6     10  (((2+1)+3)+4)
        //   1    4    10    20
        //   1    5    15    35
        //   1    6    21    56  (((5+1)+15)+35)
        //----------------------
        //   6    21   56    126
        //
        //I just noticed that the total of column n is the
        //last value in column n+1, and seems to always be
        //a factor of 7.  I'll think about that one...

        int seqLength = num-chunks+1;
        //Compute the sequence
        long[] seq = new long[seqLength];
        java.util.Arrays.fill(seq, 1);
        for(int i = 0; i<(chunks-2); i++) {
            for(int j = 1; j<seqLength; j++) {
                seq[j] = seq[j-1] + seq[j];
            }
        }

        long ret = 0;
        for(long l : seq) {
            ret+=l;
        }
        return ret;

    }

    /**
     * Compare two arrays byte-for-byte.
     */
    public static boolean compare(final byte[] source, final byte[] sink) {
        if(source == null || sink == null) {
            //lame
            return false;
        }
        if(source.length != sink.length) {
            return false;
        }
        for(int i = 0; i<source.length; i++) {
            if(source[i] != sink[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Helper method to merge n arrays into a single array
     */
    public static byte[] mergeArrays(byte[]...arrays) {
        int total = 0;
        for(byte[] a : arrays) {
            total+=a.length;
        }
        byte[] sink = new byte[total];

        int start = 0;
        for(int i = 0; i<arrays.length; i++) {
            System.arraycopy(arrays[i], 0, sink, start, arrays[i].length);
            start+=arrays[i].length;
        }
        return sink;
    }


    /**
     * Produces a debug String with the arrays compared
     * side-by-side.
     */
    public static String arraysSBS(byte[] expected, byte[] found) {

        StringBuilder sb = new StringBuilder();

        String newLine = System.getProperty("line.separator", "\n");

        int len = Math.max(expected.length, found.length);

        sb.append(" expected     output").append(newLine);

        for(int i = 0; i<len; i++) {
            String str = null;
            if(i < expected.length) {
                byte b = expected[i];
                if(b > 31 && b < 127) {
                    str = new String(new byte[] {b, ' ', ' '});
                }
                else {
                    str = "-?-";
                }
                sb.append(btoiPad(expected[i]) + " (" + str + ")   ").append(newLine);
            }
            else {
                sb.append("  <EOF>     ").append(newLine);
            }
            if(i < found.length) {
                byte b = found[i];
                if(b > 31 && b < 127) {
                    str = new String(new byte[] {b, ' ', ' '});
                }
                else {
                    str = "-?-";
                }
                sb.append(btoiPad(found[i]) + " (" + str + ") ").append(newLine);
            }
            else {
                sb.append("  <EOF>  ").append(newLine);
            }
            sb.append(newLine);
        }
        return sb.toString();
    }


    //Pads a byte to 4 characters
    private static String btoiPad(byte b) {
        String ret = "" + (int) b;
        while(ret.length() < 4) {
            ret+=" ";
        }
        return ret;
    }


}
