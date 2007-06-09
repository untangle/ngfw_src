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

import com.untangle.uvm.tapi.Pipeline;

/**
 * Implementation of FileFactory which creates temp files.
 */
public class TempFileFactory
    implements FileFactory {

    private Pipeline pipeline;

    public TempFileFactory(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    public File createFile(String name)
        throws IOException {
        return pipeline.mktemp(name);
    }

    /**
     * Create an anonymous file.
     */
    public File createFile()
        throws IOException {
        return pipeline.mktemp();
    }
}
