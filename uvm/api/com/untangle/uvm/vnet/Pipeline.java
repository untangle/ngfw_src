/**
 * $Id: Pipeline.java 35575 2013-08-08 20:44:28Z dmorris $
 */
package com.untangle.uvm.vnet;

import java.io.File;
import java.io.IOException;

/**
 * A Pipeline is a chain of <code>PipelineConnector</code>s for one <code>Session</code>.
 */
public interface Pipeline
{
    Long attach(Object o);
    Object getAttachment(Long key);
    Object detach(Long key);
    Fitting getClientFitting(PipelineConnector pipelineConnector);
    Fitting getServerFitting(PipelineConnector pipelineConnector);
}
