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
package com.untangle.node.mime.test;

import java.io.*;

import com.untangle.node.mime.*;

/**
 * Little test which parses MIME then describes
 * its contents
 */
public class DescribeMIME {

    public static void main(final String[] args) throws Exception {

        File mimeFile = new File(args[0]);

        FileMIMESource source = new FileMIMESource(mimeFile, false);

        MIMEMessage mp = new MIMEMessage(source.getInputStream(),
                                         source,
                                         new MIMEPolicy(),
                                         null);

        System.out.println(mp.describe());
    }

}
