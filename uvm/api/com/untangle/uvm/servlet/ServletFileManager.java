/**
 * $Id: ServletFileManager.java 34990 2013-06-12 23:07:00Z dmorris $
 */
package com.untangle.uvm.servlet;

import java.util.Map;

public interface ServletFileManager
{
    public void registerUploadHandler( UploadHandler handler );
    public void unregisterUploadHandler( String name );

    public UploadHandler getUploadHandler( String name );
    public Map<String,UploadHandler> getUploadHandlers();

    public void registerDownloadHandler( DownloadHandler handler );
    public void unregisterDownloadHandler( String name );
    
    public DownloadHandler getDownloadHandler( String name );
    public Map<String,DownloadHandler> getDownloadHandlers();
}
