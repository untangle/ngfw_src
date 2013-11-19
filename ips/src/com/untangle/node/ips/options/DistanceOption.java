/**
 * $Id$
 */
package com.untangle.node.ips.options;

import org.apache.log4j.Logger;

public class DistanceOption extends IpsOption
{

    private final Logger logger = Logger.getLogger(getClass());

    public DistanceOption(OptionArg arg)
    {
        super(arg);

        int distance = Integer.parseInt(arg.getParams());
        IpsOption option = signature.getOption("ContentOption",this);
        if(option == null) {
            logger.warn("Unable to find content option to set distance for sig: " + signature);
            return;
        }

        ContentOption content = (ContentOption) option;
        content.setDistance(distance);
    }
}
