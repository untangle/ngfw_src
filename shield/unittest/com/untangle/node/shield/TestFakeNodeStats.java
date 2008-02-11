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

package com.untangle.node.shield;

import junit.framework.*;
import java.util.*;
import com.untangle.uvm.node.NodeStats;

public class TestFakeNodeStats extends TestCase {

    private NodeStats stats;

    /**
     * Constructs a TestBlacklist with the specified name.
     *
     * @param name Test case name.
     */
    public TestFakeNodeStats(String name) {
        super(name);
    }

    /**
     * Sets up the test fixture.
     *
     * Called before every test case method.
     */
    protected void setUp() {
        stats = new NodeStats();
    }

    /**
     * Tears down the test fixture.
     *
     * Called after every test case method.
     */
    protected void tearDown() {
    }

    public void test01() {
        // Before update, all zero.
        assertEquals(stats.getC2tBytes(), 0);
        assertEquals(stats.getS2tBytes(), 0);
        assertEquals(stats.getT2cBytes(), 0);
        assertEquals(stats.getT2sBytes(), 0);
        assertEquals(stats.getC2tChunks(), 0);
        assertEquals(stats.getS2tChunks(), 0);
        assertEquals(stats.getT2cChunks(), 0);
        assertEquals(stats.getT2sChunks(), 0);

        FakeNodeStats.update(stats);

        // After update, at least eth0 > 0
        assertTrue(stats.getS2tBytes() > 0);
        assertTrue(stats.getT2sBytes() > 0);
        assertTrue(stats.getS2tChunks() > 0);
        assertTrue(stats.getT2sChunks() > 0);

        System.out.println("Inside bytes: " + stats.getC2tBytes() + ", " + stats.getT2cBytes());
        System.out.println("Outside bytes: " + stats.getS2tBytes() + ", " + stats.getT2sBytes());
        System.out.println("Inside chunks: " + stats.getC2tChunks() + ", " + stats.getT2cChunks());
        System.out.println("Outside chunks: " + stats.getS2tChunks() + ", " + stats.getT2sChunks());
    }

    /**
     * Assembles and returns a test suite for
     * all the test methods of this test case.
     *
     * @return A non-null test suite.
     */
    public static Test suite() {

        //
        // Reflection is used here to add all
        // the testXXX() methods to the suite.
        //
        TestSuite suite = new TestSuite(TestFakeNodeStats.class);

        return suite;
    }

    /**
     * Runs the test case.
     */
    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
}
