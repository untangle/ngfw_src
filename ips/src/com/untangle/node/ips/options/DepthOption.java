/**
 * $Id$
 */
package com.untangle.node.ips.options;

import org.apache.log4j.Logger;

import com.untangle.uvm.node.ParseException;

public class DepthOption extends IpsOption
{
    private final Logger logger = Logger.getLogger(getClass());

    public DepthOption(OptionArg arg) throws ParseException
    {
        super(arg);

        String params = arg.getParams();

        ContentOption option = (ContentOption)signature.getOption("ContentOption",this);
        if (option == null) {
            logger.warn("Unable to find content option to set depth for sig: "
                        + arg.getRule().getText());
            signature.remove(true);
            return;
        }

        int depth = 0;
        try {
            depth = Integer.parseInt(params);
        } catch (Exception e) {
            throw new ParseException("Not a valid Offset argument: " + params);
        }
        option.setDepth(depth);
    }
}
