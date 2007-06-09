/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.airgap;

import junit.framework.*;
import java.util.*;
import com.untangle.mvvm.tran.TransformStats;

public class TestFakeTransformStats extends TestCase {

    private TransformStats stats;

    /**
     * Constructs a TestBlacklist with the specified name.
     *
     * @param name Test case name.
     */
    public TestFakeTransformStats(String name) {
        super(name);
    }

    /**
     * Sets up the test fixture.
     *
     * Called before every test case method.
     */
    protected void setUp() {
        stats = new TransformStats();
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
        assertEquals(stats.c2tBytes(), 0);
        assertEquals(stats.s2tBytes(), 0);
        assertEquals(stats.t2cBytes(), 0);
        assertEquals(stats.t2sBytes(), 0);
        assertEquals(stats.c2tChunks(), 0);
        assertEquals(stats.s2tChunks(), 0);
        assertEquals(stats.t2cChunks(), 0);
        assertEquals(stats.t2sChunks(), 0);

        FakeTransformStats.update(stats);

        // After update, at least eth0 > 0
        assertTrue(stats.s2tBytes() > 0);
        assertTrue(stats.t2sBytes() > 0);
        assertTrue(stats.s2tChunks() > 0);
        assertTrue(stats.t2sChunks() > 0);

        System.out.println("Inside bytes: " + stats.c2tBytes() + ", " + stats.t2cBytes());
        System.out.println("Outside bytes: " + stats.s2tBytes() + ", " + stats.t2sBytes());
        System.out.println("Inside chunks: " + stats.c2tChunks() + ", " + stats.t2cChunks());
        System.out.println("Outside chunks: " + stats.s2tChunks() + ", " + stats.t2sChunks());
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
        TestSuite suite = new TestSuite(TestFakeTransformStats.class);

        return suite;
    }

    /**
     * Runs the test case.
     */
    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
}
