/**
 * $Id$
 */
package com.untangle.node.ips.options;

public class MsgOption extends IpsOption
{
    private static final String BLEEDING_PREFIX = "BLEEDING-EDGE";

    public MsgOption(OptionArg arg)
    {
        super(arg);

        String params = arg.getParams();

        // reomve useless 'BLEEDING-EDGE' prefix
        if (params.length() > BLEEDING_PREFIX.length()) {
            String beginParams = params.substring(0, BLEEDING_PREFIX.length());
            if (beginParams.equalsIgnoreCase(BLEEDING_PREFIX))
                params = params.substring(BLEEDING_PREFIX.length()).trim();
        }
        signature.setMessage(params);
    }
}
