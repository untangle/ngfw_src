/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.email;

import com.metavize.mvvm.tapi.*;

public class EmailTransformImpl extends AbstractTransform
{
    protected PipeSpec[] getPipeSpecs()
    {
        return new PipeSpec[0];
    }

    public Object getSettings()
    {
        return null;
    }

    public void setSettings(Object settings) throws Exception
    {
        return;
    }
    
}
