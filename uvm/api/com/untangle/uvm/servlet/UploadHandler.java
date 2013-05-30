package com.untangle.uvm.servlet;

import org.apache.commons.fileupload.FileItem;

public interface UploadHandler
{
    public String getName();
    
    public Object handleFile(FileItem fileItem, String argument) throws Exception;
}
