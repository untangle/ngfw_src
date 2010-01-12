package com.untangle.uvm.servlet;

import org.apache.commons.fileupload.FileItem;

public interface UploadHandler {
    public String getName();
    
    public String handleFile(FileItem fileItem) throws Exception;
}
