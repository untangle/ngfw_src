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

package com.untangle.uvm.logging;

/**
 * Factory for retrieving the current logging context. Allows for
 * access to the {@link UvmLoggingContext} on demand. Returns the
 * {@link UvmLoggingContext} in effect at the time the {@link #get()}
 * method is called. Thus, it is only guaranteed to work if called
 * syncronously from the thread that accesses {@link #get()}.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface UvmLoggingContextFactory
{
    /**
     * Returns the {@link UvmLoggingContext} in effect for this
     * thread at the moment the get() method is called.
     *
     * @return the effective {@link UvmLoggingContext}.
     */
    UvmLoggingContext get();
}
