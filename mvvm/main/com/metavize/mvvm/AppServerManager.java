/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: $
 */

package com.metavize.mvvm;


/**
 *
 * Instances of this interface are obtained via
 * {@link com.metavize.mvvm.MvvmLocalContext#appServerManager MvvmLocalContext}.
 */
public interface AppServerManager  {

  /**
   * Change the port to-which the external interface is bound
   */
  public void rebindExternalHttpsPort(int port) throws Exception;


  /**
    * Load a web app.  The web app's files are already assumed to be
    * unpackaged into the root web apps directory of the edgeguard
    * deployment
    *
    * @param urlBase the base URL (i.e. "http://edgeguard/<i>urlBase</i>/foo").
    * @param rootDir the name of the root directory under the "web" directory
    *        of edgeguard w/ the app.
    */
  public boolean loadWebApp(String urlBase,
    String rootDir);

  public boolean unloadWebApp(String urlBase);

}
