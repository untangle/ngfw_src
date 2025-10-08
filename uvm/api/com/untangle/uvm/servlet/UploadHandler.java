/**
 * $Id$
 */
package com.untangle.uvm.servlet;

import org.apache.commons.fileupload.FileItem;

import java.util.Map;

public interface UploadHandler
{
    public String getName();
    
    public Object handleFile(FileItem fileItem, String argument) throws Exception;

    default Object handleV2File(byte[] backupFileBytes, Map<String, String> arguments) throws Exception {
        return null;
    }
}
