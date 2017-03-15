/*
 * $Id$
 */
package com.untangle.uvm;

import java.net.URL;
import java.net.URLClassLoader;

public class UvmClassLoader extends URLClassLoader
{
    public UvmClassLoader( URL[] urls, ClassLoader parent )
    {
        super(urls, parent);
    }
}
