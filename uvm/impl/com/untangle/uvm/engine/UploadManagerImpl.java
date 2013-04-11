/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.servlet.UploadManager;


/**
 * Managers upload of files
 *
 */
public class UploadManagerImpl implements UploadManager
{
    Map<String,UploadHandler> uploadHandlers = new HashMap<String,UploadHandler>();
    
    /* 
     * @see com.untangle.uvm.servlet.UploadManager#getUploadHandler(java.lang.String)
     */
    @Override
    public UploadHandler getUploadHandler(String name)
    {
        return this.uploadHandlers.get(name);
    }

    /* (non-Javadoc)
     * @see com.untangle.uvm.servlet.UploadManager#getUploadHandlers()
     */
    @Override
    public Map<String,UploadHandler> getUploadHandlers()
    {
        return Collections.unmodifiableMap(this.uploadHandlers);
    }

    /* (non-Javadoc)
     * @see com.untangle.uvm.servlet.UploadManager#registerHandler(com.untangle.uvm.servlet.UploadHandler)
     */
    @Override
    public void registerHandler(UploadHandler handler)
    {
        this.uploadHandlers.put(handler.getName(), handler);
    }

    /* (non-Javadoc)
     * @see com.untangle.uvm.servlet.UploadManager#unregisterHandler(java.lang.String)
     */
    @Override
    public void unregisterHandler(String name)
    {
        this.uploadHandlers.remove(name);
    }

}
