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
package com.untangle.node.util;

import java.io.File;
import java.io.IOException;

/**
 * Interface which can create Files.  This provides
 * an abstraction, such that classes which need to create
 * Files can so so in a way that the larger application
 * can control their name and lifetime.
 */
public interface FileFactory {

    /**
     * Create a file based on the given name.  Implementations
     * are permitted to create a File with a name which is
     * a function of <code>name</code>, or ignore the
     * name alltogether.  In other words, the name
     * is a hint.
     */
    public File createFile(String name)
        throws IOException;

    /**
     * Create an anonymous file.
     */
    public File createFile()
        throws IOException;

}
