/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id:$
 */

package com.metavize.mvvm.portal;

import java.io.Serializable;

/**
 * Class for portal applications
 */
public final class Application implements Serializable {

    public interface Validator {
        boolean isValid(String target);
        // String whyNotValid(String target);
    }
        
    private String name;
    private String description;
    private boolean isHostService;
    private Validator validator;

    public Application(String name, String description, boolean isHostService, Validator validator) {
        this.name = name;
        this.description = description;
        this.isHostService = isHostService;
        this.validator = validator;
    }

    /**
     * Name of the application.  This is also its key.  Example: "SSH"
     *
     * @return a <code>String</code> giving the name of the application
     */
    public String getName() {
        return name;
    }

    /**
     * Short description of the application.  Example "Secure Shell"
     *
     * @return a <code>String</code> giving a short description of the application
     */
    public String getDescription() {
        return description;
    }

    /**
     * True if the application is a host-based service.  For example, CIFS and Email
     * are not host-based services, SSH and HTTP are.
     *
     * @return a <code>boolean</code> true if the application is a host-based service
     */
    public boolean isHostService() {
        return isHostService;
    }


    /**
     * Validates the given target, returning true if the target is syntactically valie
     * for the application
     *
     * @param target a <code>String</code> value
     * @return a <code>boolean</code> true if the target is valid
     */
    public boolean isValid(String target) {
        return validator.isValid(target);
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof Application)) {
            return false;
        }

        Application a = (Application)o;
        return name.equals(a.name);
    }

    public int hashCode()
    {
        return name.hashCode();
    }
}
