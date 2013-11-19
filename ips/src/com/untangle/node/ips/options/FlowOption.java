/**
 * $Id$
 */
package com.untangle.node.ips.options;

import java.util.regex.Pattern;

import com.untangle.node.ips.IpsSessionInfo;

public class FlowOption extends IpsOption
{
    /**
     * The options "only_stream" and "established" would  have *no* effect.
     * So I ignore them.
     */
    private static final Pattern noStream = Pattern.compile("no_stream",Pattern.CASE_INSENSITIVE);
    private static final Pattern[] validParams = {
        Pattern.compile("from_server", Pattern.CASE_INSENSITIVE),
        Pattern.compile("from_client", Pattern.CASE_INSENSITIVE),
        Pattern.compile("to_client", Pattern.CASE_INSENSITIVE),
        Pattern.compile("to_server", Pattern.CASE_INSENSITIVE)
    };

    private boolean matchFromServer = false;

    public FlowOption(OptionArg arg)
    {
        super(arg);

        String params = arg.getParams();

        if(noStream.matcher(params).find()) {
            signature.remove(true);
        }

        for(int i=0; i < validParams.length; i++) {
            if(validParams[i].matcher(params).find())
                matchFromServer = (i%2 == 0);
        }
    }

    public boolean runnable()
    {
        return true;
    }

    public boolean run(IpsSessionInfo sessionInfo)
    {
        boolean fromServer = sessionInfo.isServer();
        boolean returnValue = !(fromServer ^ matchFromServer);
        return (negationFlag ^ returnValue);
    }

    public boolean optEquals(Object o)
    {
        if (!(o instanceof FlowOption)) {
            return false;
        }

        FlowOption fo = (FlowOption)o;

        if (!super.optEquals(fo)) {
            return false;
        }

        return matchFromServer == fo.matchFromServer;
    }

    public int optHashCode()
    {
        int result = 17;
        result = result * 37 + super.optHashCode();
        result = result * 37 + (matchFromServer ? 1 : 0);
        return result;
    }
}
