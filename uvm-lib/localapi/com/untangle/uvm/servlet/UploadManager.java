package com.untangle.uvm.servlet;

import java.util.Map;

public interface UploadManager{
    
    public void registerHandler( UploadHandler handler );
    public void unregisterHandler( String name );
    
    public UploadHandler getUploadHandler( String name );
    public Map<String,UploadHandler> getUploadHandlers();
}
