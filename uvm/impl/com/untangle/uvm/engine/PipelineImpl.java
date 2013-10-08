/**
 * $Id: PipelineImpl.java 34529 2013-04-11 19:04:52Z dmorris $
 */
package com.untangle.uvm.engine;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.Pipeline;

/**
 * Implementation of <code>Pipeline</code>.
 */
public class PipelineImpl implements Pipeline
{
    // This does not need to be concurrent since there is only one thread per pipeline.
    private final Map<Long,Object> attachments = new HashMap<Long,Object>();

    // next unused attachment Id
    private int attachId = 0;
        
    // constructors -----------------------------------------------------------

    PipelineImpl(long sessionId, List<PipelineConnectorImpl> pipelineConnectors)
    {
    }

    // object registry methods ------------------------------------------------

    /**
     * Add object to registry. The object will remain in the token
     * manager as long as the key is held onto.
     *
     * @param object object to add.
     * @return the key.
     */
    public Long attach(Object o)
    {
        Long key;
        synchronized (attachments) {
            key = new Long(++attachId);
        }
        attachments.put(key, o);
        return key;
    }

    /**
     * Get object, by key..
     *
     * @param key object's key.
     * @return the object.
     */
    public Object getAttachment(Long key)
    {
        return attachments.get(key);
    }

    /**
     * Retrieve and remove an object from the pipeline.
     *
     * @param key key of the object.
     * @return the object.
     */
    public Object detach(Long key)
    {
        return attachments.remove(key);
    }

    public Fitting getClientFitting(PipelineConnector pipelineConnector)
    {
        return pipelineConnector.getInputFitting();
    }

    public Fitting getServerFitting(PipelineConnector pipelineConnector)
    {
        return pipelineConnector.getOutputFitting();
    }
}
