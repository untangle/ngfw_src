/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.ips.options;

import java.nio.ByteBuffer;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.untangle.node.ips.IpsRule;
import com.untangle.node.ips.IpsSessionInfo;
import com.untangle.node.util.AsciiCharBuffer;

public class PcreOption extends IpsOption
{
    private final Logger logger = Logger.getLogger(getClass());

    private Pattern pcrePattern;

    public PcreOption(OptionArg arg)
    {
        super(arg);

        String params = arg.getParams();
        IpsRule rule = arg.getRule();

        int beginIndex = params.indexOf("/");
        int endIndex = params.lastIndexOf("/");

        if (endIndex < 0 || beginIndex < 0 || endIndex == beginIndex) {
            logger.warn("Malformed pcre: " + params + ", ignoring rule: " +
                        rule.getText());
            signature.remove(true);
        } else {
            try {
                String pattern = params.substring(beginIndex+1, endIndex);
                String options = params.substring(endIndex+1);
                int flag = 0;
                for (int i = 0; i < options.length(); i++) {
                    char c = options.charAt(i);
                    switch (c) {
                    case 'i':
                        flag = flag | Pattern.CASE_INSENSITIVE;
                        break;
                    case 's':
                        flag = flag | Pattern.DOTALL;
                        break;
                    case 'm':
                        flag = flag | Pattern.MULTILINE;
                        break;
                    case 'x':
                        flag = flag | Pattern.COMMENTS;
                        break;
                    default:
                        logger.info("Unable to handle pcre option: "
                                    + c + ", ignoring rule: " + rule.getText());
                        signature.remove(true);
                        break;
                    }
                }
                pcrePattern = Pattern.compile(pattern, flag);
            } catch(Exception e) {
                logger.warn("Unable to parse pcre: " + params + " ("
                            + e.getMessage() + "), ignoring rule: "
                            + rule.getText());
                signature.remove(true);
            }
        }
    }

    public boolean runnable()
    {
        return true;
    }

    public boolean run(IpsSessionInfo sessionInfo)
    {
        ByteBuffer eventData = sessionInfo.getEvent().data();

        //  if(pcrePattern == null) {
        //      System.out.println("pcrePattern is null\n\n"+getSignature());
        //      return false;
        //  }

        AsciiCharBuffer acb = AsciiCharBuffer.wrap(eventData);
        boolean patMatch = pcrePattern.matcher(acb).find();

        // if (logger.isDebugEnabled()) {
        // logger.debug("Match: " + patMatch + " for data of len " + eventData.remaining() + " on " +
        // signature.rule().getText());
        // }

        return negationFlag ^ patMatch;
    }

    public boolean optEquals(Object o)
    {
        if (!(o instanceof PcreOption)) {
            return false;
        }

        PcreOption po = (PcreOption)o;

        if (!super.optEquals(po)) {
            return false;
        }

        if (null == pcrePattern || null == po.pcrePattern) {
            return pcrePattern == po.pcrePattern;
        } else {
            return pcrePattern.pattern().equals(po.pcrePattern.pattern());
        }
    }

    public int optHashCode()
    {
        int result = 17;
        result = result * 37 + super.optHashCode();
        result = result * 37 + (null == pcrePattern ? 0 : pcrePattern.pattern().hashCode());
        return result;
    }
}
