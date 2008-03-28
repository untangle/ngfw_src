/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/spyware/impl/com/untangle/node/spyware/SpywareValidator.java $
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
package com.untangle.node.spyware;

import java.util.Iterator;
import java.util.List;

import com.untangle.uvm.node.IPMaddr;
import com.untangle.uvm.node.ValidationResult;
import com.untangle.uvm.node.Validator;

public class SpywareValidator implements Validator {

	public ValidationResult validate(Object data) {

		try {
			if (data != null) {
				// for now we only validate IPMaddr data
				List<String> subnets = (List<String>) data;
				for (Iterator iterator = subnets.iterator(); iterator.hasNext();) {
					String val = (String) iterator.next();
					try {
						IPMaddr.parse(val);
					} catch (Exception e) {
						return new ValidationResult(false,
								"Invalid subnet specified", val);
					}
				}

			}
		} catch (Exception e) {
			return new ValidationResult(false, e.getMessage(), e);
		}

		return new ValidationResult(true);
	}

}
