/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail;

import java.io.File;

import com.metavize.tran.token.MetadataToken;

public class MessageFile extends MetadataToken
{
    private final File file;

    public MessageFile(File file)
    {
        this.file = file;
    }

    public File getFile()
    {
        return file;
    }
}
