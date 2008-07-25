/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/spyware/impl/com/untangle/node/firewall/FirewallValidator.java $
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
package com.untangle.node.firewall;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.untangle.uvm.node.ValidationResult;
import com.untangle.uvm.node.Validator;
import com.untangle.uvm.node.firewall.ip.IPMatcherFactory;
import com.untangle.uvm.node.firewall.port.PortMatcherFactory;

public class FirewallValidator implements Validator {

	public ValidationResult validate(Object data) {

		try {
			if (data != null) {
                String type = (String) ((HashMap) data).get("type");
                if(type.equals("IP")) {
                    // validate IPMatcher data
                    List<String> values = (List<String>) ((HashMap) data).get("values");
                    for (Iterator iterator = values.iterator(); iterator.hasNext();) {
                        String val = (String) iterator.next();
                        try {
                            IPMatcherFactory.parse(val);
                        } catch (Exception e) {
                            return new ValidationResult(false,
                                    "Invalid address specified:", val);
                        }
                    }
                }
                else if(type.equals("Port")) {
                    // validate IPMatcher data
                    List<String> values = (List<String>) ((HashMap) data).get("values");
                    for (Iterator iterator = values.iterator(); iterator.hasNext();) {
                        String val = (String) iterator.next();
                        try {
                            PortMatcherFactory.parse(val);
                        } catch (Exception e) {
                            return new ValidationResult(false,
                                    "Invalid port specified:", val);
                        }
                    }
                }
			}
		} catch (Exception e) {
			return new ValidationResult(false, e.getMessage(), e);
		}

		return new ValidationResult(true);
	}

}
