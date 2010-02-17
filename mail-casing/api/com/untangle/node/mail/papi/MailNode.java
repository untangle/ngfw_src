/*
 * $HeadURL$
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

package com.untangle.node.mail.papi;

import com.untangle.node.mail.papi.quarantine.QuarantineMaintenenceView;
import com.untangle.node.mail.papi.quarantine.QuarantineUserView;
import com.untangle.node.mail.papi.safelist.SafelistAdminView;
import com.untangle.node.mail.papi.safelist.SafelistEndUserView;

public interface MailNode
{
    MailNodeSettings getMailNodeSettings();
    void setMailNodeSettings(MailNodeSettings settings);
    void setMailNodeSettingsWithoutSafelists(MailNodeSettings settings);

    /**
     * Get the interface to the Quarantine used for end-user
     * interaction
     *
     * @return the QuarantineUserView
     */
    QuarantineUserView getQuarantineUserView();


    /**
     * Get the interface to the Quarantine for Administrative
     * interaction (other than {@link #setMailNodeSettings property}
     * manipulation.
     *
     * @return the QuarantineMaintenenceView
     */
    QuarantineMaintenenceView getQuarantineMaintenenceView();

    /**
     * Get the interface to the Safelist facility for end-user
     * interaction
     *
     * @return the SafelistEndUserView
     */
    SafelistEndUserView getSafelistEndUserView();


    /**
     * Get the interface to the Safelist subsystem for Administrative
     * interaction (other than {@link #setMailNodeSettings property}
     * manipulation.
     *
     * @return the SafelistAdminView
     */
    SafelistAdminView getSafelistAdminView();

    /**
     * minimum size of the space allocated for the entire store (in B or GB)
     * (e.g., minimum space allocated on HDD that store can use) 
     * -> in GB if inGB == true or in B if inGB == false
     */
    public long getMinAllocatedStoreSize(boolean inGB);

    /**
     * maximum size of the space allocated for the entire store (in B or GB)
     * (e.g., maximum space allocated on HDD that store can use) 
     * -> in GB if inGB == true or in B if inGB == false
     */
    public long getMaxAllocatedStoreSize(boolean inGB);

    /**
     * Retrieve an authentication token for an email address.
     */
    public String createAuthToken(String account);
}
