/**
 * $Id: ExecManagerResult.java,v 1.00 2012/01/31 23:41:22 dmorris Exp $
 */
package com.untangle.uvm;

import org.apache.commons.codec.binary.Base64;

public class ExecManagerResult
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
