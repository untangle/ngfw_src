/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: TestTransform.java,v 1.12 2005/01/29 06:19:36 amread Exp $
 */

package com.metavize.tran.test;

import com.metavize.mvvm.tran.Transform;

public interface TestTransform extends Transform
{
    public void setTestSettings(TestSettings settings);
    public TestSettings getTestSettings();
}
