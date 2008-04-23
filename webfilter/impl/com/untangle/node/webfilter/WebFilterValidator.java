package com.untangle.node.webfilter;

import java.util.Iterator;
import java.util.List;

import com.untangle.uvm.node.IPMaddr;
import com.untangle.uvm.node.ValidationResult;
import com.untangle.uvm.node.Validator;

public class WebFilterValidator implements Validator {

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
