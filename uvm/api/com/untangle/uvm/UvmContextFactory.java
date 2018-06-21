/**
 * $Id$
 */
package com.untangle.uvm;

import java.lang.reflect.Method;

/**
 * Factory to get the UvmContext for the UVM instance.
 */
public class UvmContextFactory
{
    private static UvmContext UVM_CONTEXT = null;

    /**
     * Gets the current state of the UVM.  This provides a way to get
     * the state without creating the UvmContext in case we're
     * calling this at a very early stage.
     *
     * @return a <code>UvmState</code> enumerated value
     */
    public static UvmState state()
    {
        if (UVM_CONTEXT == null) {
            return UvmState.LOADED;
        } else {
            return UVM_CONTEXT.state();
        }
    }

    /**
     * Get the <code>UvmContext</code> from this classloader.
     * used by Apps to get the context internally.
     *
     * @return the <code>UvmContext</code>.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static UvmContext context()
    {
        if (null == UVM_CONTEXT) {
            synchronized (UvmContextFactory.class) {
                if (null == UVM_CONTEXT) {
                    try {
                        Class c = Class.forName("com.untangle.uvm.UvmContextImpl");
                        Method m = c.getMethod("context");
                        UVM_CONTEXT = (UvmContext)m.invoke(null);
                    } catch ( Exception e ) {
                        System.err.println( "No class or method for the UVM context" );
                        e.printStackTrace();
                    }
                }
            }
        }
        return UVM_CONTEXT;
    }
}
