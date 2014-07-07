/**
 * $Id$
 */
package com.untangle.node.ips.options;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.node.ips.IpsSessionInfo;

public class DsizeOption extends IpsOption
{
    private final Logger log = Logger.getLogger(getClass());

    private int min;
    private int max;

    public DsizeOption(OptionArg arg)
    {
        super(arg);

        String params = arg.getParams();

        char ch = params.charAt(0);
        String range[] = params.split("<>");
        try {
            if(range.length == 2) {
                min = Integer.parseInt(range[0].trim());
                max = Integer.parseInt(range[1].trim());
            }
            else if(ch == '<') {
                min = 0;
                max = Integer.parseInt(params.substring(1).trim());
            }
            else if(ch == '>') {
                min = Integer.parseInt(params.substring(1).trim());
                max = Integer.MAX_VALUE;
            }
            else
                min = max = Integer.parseInt(params.trim());
        }
        catch(NumberFormatException e) {
            log.error("Invalid Dsize param: " + params);
            min = 0;
            max = Integer.MAX_VALUE;
        }
    }

    public boolean runnable()
    {
        return true;
    }

    //XXX - check negation flag?
    public boolean run(IpsSessionInfo sessionInfo)
    {
        ByteBuffer data = sessionInfo.getData();
        int size = data.remaining();
        if(min <= size && max >= size)
            return true;
        return false;
    }

    public boolean optEquals(Object o)
    {
        if (!(o instanceof DsizeOption)) {
            return false;
        }

        DsizeOption dso = (DsizeOption)o;

        if (!super.optEquals(dso)) {
            return false;
        }

        return min == dso.min
            && max == dso.max;
    }

    public int optHashCode()
    {
        int result = 17;
        result = result * 37 + super.optHashCode();
        result = result * 37 + min;
        result = result * 37 + max;
        return result;
    }
}
