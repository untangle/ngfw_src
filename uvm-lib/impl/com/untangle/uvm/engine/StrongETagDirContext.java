/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/uvm-lib/impl/com/untangle/uvm/engine/StrongETagDirContext.java $
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
package com.untangle.uvm.engine;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.naming.resources.FileDirContext;
import org.apache.naming.resources.ResourceAttributes;

/**
 * This associates a strong ETag (instead of Tomcat's default Weak ETags) with
 * each static resource served up.
 * 
 * @author <a href="mailto:cmatei@untangle.com">Catalin Matei</a>
 * @version 1.0
 */
@SuppressWarnings("unchecked")
public class StrongETagDirContext extends FileDirContext
{
	
	@Override
	public Attributes getAttributes(String name, String[] attrIds) throws NamingException
    {
		ResourceAttributes r = (ResourceAttributes) super.getAttributes(name, attrIds);
		long cl = r.getContentLength();
		long lm = r.getLastModified();

		String strongETag = String.format("\"%s-%s\"", cl, lm);
		r.setETag(strongETag);
		
		return r;
	}
}
