/**
 * $Id$
 */
package com.untangle.node.ips.options;

import org.apache.log4j.Logger;

public class WithinOption extends IpsOption {
    private final Logger logger = Logger.getLogger(getClass());

    public WithinOption(OptionArg arg) {
        super(arg);

        String params = arg.getParams();

        int within = Integer.parseInt(params);
        IpsOption option = signature.getOption("ContentOption",this);
        if(option == null) {
            logger.warn("Unable to find content option to set within for sig: " + signature);
            return;
        }

        ContentOption content = (ContentOption) option;
        content.setWithin(within);
    }
}
