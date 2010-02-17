/*
 * $HeadURL: svn://chef/work/src/uvm-lib/localapi/com/untangle/uvm/LocalBrandingManager.java $
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
 * This manager provides information about control of upstream services,
 * normally those running on the local machine.  Each service is described
 * in <code>UpstreamService</code>
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public interface RemoteUpstreamManager
{
    // Built-in service names are statically defined here.  User could still
    // remove them from the upstream-services definition file, so don't skip
    // the null test.
    static String SUPPORT_SERVICE_NAME = "support";
    static String EXIM_SERVICE_NAME = "exim4";
    static String SNMPD_SERVICE_NAME = "snmpd";
    static String AUTO_UPGRADE_SERVICE_NAME = "auto-upgrade";

    /**
     * <code>getUpstreamService</code> returns the <code>UpstreamService</code>
     * with the given name. Null is returned if no matching upstream service is
     * found.
     *
     * @param name a <code>String</code> giving the name of the service
     * @return the corresponding <code>UpstreamService</code>, or null if none
     */
    UpstreamService getService(String name);
    
    /**
     * <code>allUpstreamServiceNames</code> returns the names of all known
     * upstream services.
     *
     * @return a <code>String[]</code> giving all service names
     */
    String[] allServiceNames();

    /**
     * <code>enableService</code> enables the service with the given name.
     * If the name does not name a service, an <code>IllegalArgumentException</code>
     * is thrown.  If the service is already enabled or there is no
     * configPackage for the service, this is a no-op.
     *
     * Once the function returns without throwing, the service is enabled.
     *
     * @param name a <code>String</code> naming the service to enable.
     * @exception IllegalArgumentException if the name does not name a service
     * @exception MackageInstallException if an error occurs while installing the configPackage
     */
    void enableService(String name)
        throws IllegalArgumentException, MackageInstallException;

    /**
     * <code>disableService</code> disables the service with the given name.
     * If the name does not name a service, or if the service cannot be disabled
     * because it does not have a configPackage, an <code>IllegalArgumentException</code>
     * is thrown.  If the service is already disabled this is a no-op.
     *
     * Once the function returns without throwing, the service is disabled.
     *
     * @param name a <code>String</code> naming the service to disable.
     * @exception IllegalArgumentException if the name does not name a service or if the service has no configPackage
     * @exception MackageUninstallException if an exception occurs while uninstalling the configPackage
     */
    void disableService(String name)
        throws IllegalArgumentException, MackageUninstallException;
}
