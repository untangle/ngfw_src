/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: EmailTransform.java,v 1.1 2005/01/22 05:34:24 jdi Exp $
 */

package com.metavize.tran.email;

import com.metavize.mvvm.tran.Transform;

public interface EmailTransform extends Transform
{
    EmailSettings getEmailSettings();
    void setEmailSettings(EmailSettings settings);
}
