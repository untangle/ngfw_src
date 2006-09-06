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
package com.metavize.tran.util;
import com.metavize.mvvm.tapi.Pipeline;
import java.io.File;
import java.io.IOException;



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
