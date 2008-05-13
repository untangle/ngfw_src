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

package com.untangle.node.ips;

import com.untangle.node.ips.options.*;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.vnet.event.*;

public class IPSRuleSignature
{
    private final IPSRuleSignatureImpl impl;

    // constructors ------------------------------------------------------------

    private IPSRuleSignature(IPSRuleSignatureImpl impl)
    {
        this.impl = impl;
    }

    // public static methods ---------------------------------------------------

    public static IPSRuleSignature parseSignature(IPSNodeImpl ips,
                                                  String signatureString,
                                                  int action, IPSRule rule,
                                                  boolean initSettingsTime,
                                                  String string)
        throws ParseException
    {
        IPSRuleSignatureImpl impl = new IPSRuleSignatureImpl(action, rule,
                                                             string);

        String replaceChar = ""+0xff42;
        signatureString = signatureString.replaceAll("\\\\;",replaceChar);
        String options[] = signatureString.trim().split(";");
        for (int i = 0; i < options.length; i++) {
            options[i].trim();
            options[i] = options[i].replaceAll(replaceChar,"\\\\;");
            int delim = options[i].indexOf(':');
            if (delim < 0) {
                impl.addOption(ips.getEngine(), options[i].trim(),"No Params", initSettingsTime);
            } else {
                String opt = options[i].substring(0,delim).trim();
                impl.addOption(ips.getEngine(), opt, options[i].substring(delim+1).trim(), initSettingsTime);
            }

            if (impl.remove()) {
                // Early exit.  Don't bother with rest of options.
                break;
            }
        }
        return new IPSRuleSignature(impl);
    }

    public boolean remove()
    {
        return impl.remove();
    }

    public IPSRule rule()
    {
        return impl.rule();
    }

    public String getMessage()
    {
        return impl.getMessage();
    }

    public String getClassification()
    {
        return impl.getClassification();
    }

    public String getURL()
    {
        return impl.getURL();
    }

    public boolean execute(IPSNodeImpl ips, IPSSessionInfo info)
    {
        return impl.execute(ips, info);
    }

    public String toString()
    {
        return impl.toString();
    }

    static void dumpRuleTimes()
    {
        IPSRuleSignature.dumpRuleTimes();
    }
}
