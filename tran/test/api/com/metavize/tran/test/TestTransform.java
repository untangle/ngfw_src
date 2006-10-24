/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.test;

import com.metavize.mvvm.tran.Transform;

public interface TestTransform extends Transform
{
    public void setTestSettings(TestSettings settings);
    public TestSettings getTestSettings();
}
