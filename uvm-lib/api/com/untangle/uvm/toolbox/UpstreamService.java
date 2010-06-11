/*
 * $HeadURL: svn://chef/work/src/uvm-lib/api/com/untangle/uvm/toolbox/UpstreamService.java $
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.toolbox;

/**
 * Records information about upstream service, including package that manages it (if any)
 * and its current state.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public class UpstreamService
{

    private String name;
    private boolean enabled;
    private String configPackage;

    public UpstreamService(String name, boolean enabled, String configPackage) {
        this.name = name;
        this.enabled = enabled;
        this.configPackage = configPackage;
    }

    /**
     * Returns the name of the upstream service.
     *
     * @return a <code>String</code> giving the name of the upstream service
     */
    public String name()
    {
        return name;
    }

    /**
     * Returns true if automatic management is enabled for this service
     *
     * @return a <code>boolean</code> true if automatic management enabled
     */
    public boolean enabled()
    {
        return enabled;
    }

    /**
     * Gives the deb package that autoconfigures this service. May be null, in
     * which case the service is either:
     *   configured purely inside the UVM, or
     *   that the service configuration isn't optional.
     *
     * @return a <code>String</code> value
     */
    public String configPackage()
    {
        return configPackage;
    }
}
