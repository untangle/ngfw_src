/**
 * $Id$
 */
package com.untangle.node.ips.options;

import org.apache.log4j.Logger;

public class OffsetOption extends IpsOption
{
    private final Logger logger = Logger.getLogger(getClass());

    public OffsetOption(OptionArg arg)
    {
        super(arg);

        String params = arg.getParams();

        ContentOption option = (ContentOption) signature.getOption("ContentOption",this);
        if(option == null) {
            logger.warn("Unable to find content option to set offset for sig: " + signature);
            return;
        }

        int offset = 0;
        try {
            offset = Integer.parseInt(params);
        } catch (Exception e) {
            throw new RuntimeException("Not a valid Offset argument: " + params);
        }
        option.setOffset(offset);
    }
}
