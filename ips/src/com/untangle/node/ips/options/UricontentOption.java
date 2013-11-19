/**
 * $Id$
 */
package com.untangle.node.ips.options;

import org.apache.xerces.impl.xpath.regex.BMPattern;
import com.untangle.node.ips.IpsSessionInfo;

public class UricontentOption extends IpsOption
{
    private BMPattern uriPattern;
    private String stringPattern;
    private boolean nocase = false;

    public UricontentOption(OptionArg arg)
    {
        super(arg);

        stringPattern = arg.getParams();
        uriPattern = new BMPattern(stringPattern, nocase);
    }

    public void setNoCase()
    {
        nocase = true;
        uriPattern = new BMPattern(stringPattern, nocase);
    }

    public boolean runnable()
    {
        return true;
    }

    public boolean run(IpsSessionInfo sessionInfo)
    {
        String path = sessionInfo.getUriPath();
        if(path != null) {
            int result = uriPattern.matches(path, 0, path.length());
            return negationFlag ^ (result >= 0);
        }
        return false;
    }

    public boolean optEquals(Object o)
    {
        if (!(o instanceof UricontentOption)) {
            return false;
        }

        UricontentOption uo = (UricontentOption)o;

        if (!super.optEquals(uo)) {
            return false;
        }

        return stringPattern.equals(uo.stringPattern); /* && nocase == nocase; */
    }

    public int optHashCode()
    {
        int result = 17;
        result = result * 37 + super.optHashCode();
        result = result * 37 + stringPattern.hashCode();
        result = result * 37 + (nocase ? 1 : 0);
        return result;
    }
}
