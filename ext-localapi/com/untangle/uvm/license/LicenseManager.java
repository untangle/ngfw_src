/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.uvm.license;

public interface LicenseManager extends LocalLicenseManager
{
    /**
     * Register a product with the license manager.
     *
     * @param product The product to register.
     */
    public void register( Product product );

    public interface Product
    {
        /**
         * Identifier for this product, this should be unique.
         */
        public String identifier();

        /**
         * Update the expiration date, this is executed regularly
         * whenever the product isn't expired.
         *
         * @param expirationDate This product is valid until
         * <code>expirationDate</code>.
         */
        public void updateExpirationDate( long expirationDate );

        /**
         * Expire a product right now.
         */
        public void expire();

        /**
         * Returns true if the premium version of the product is
         * current active.
         */
        public boolean isActivated();
    }
}
