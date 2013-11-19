/**
 * $Id$
 */
package com.untangle.node.ips.options;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class matches the reference option found in snort based rule
 * signatures.
 *
 * @Author Nick Childers
 */
public class ReferenceOption extends IpsOption
{
    private static final Pattern URLP = Pattern.compile("url,", Pattern.CASE_INSENSITIVE);

    public ReferenceOption(OptionArg arg)
    {
        super(arg);

        String params = arg.getParams();

        Matcher urlm = URLP.matcher(params);
        if (true == urlm.find()) {
            String url = "http://" + params.substring(urlm.end()).trim();
            signature.setURL(url);
        }
    }
}
