/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ExecManagerResult implements Serializable
{
    Integer result;
    String output;
        
    public ExecManagerResult() {};
    public ExecManagerResult(Integer result, String output)
    {
        this.result = result;
        this.output = output;
    };

    public Integer getResult() { return this.result; }
    public void setResult(Integer result) { this.result = result; }

    public String getOutput() { return this.output; }
    public void setOutput(String output) { this.output = output; }
}
