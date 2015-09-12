/*
 * $Id$
 */
package com.untangle.node.directory_connector;

/**
 * This is the RadiusManager API (used by the GUI)
 */
public interface RadiusManager
{
    public String getRadiusStatusForSettings( DirectoryConnectorSettings newSettings, String username, String password );
}