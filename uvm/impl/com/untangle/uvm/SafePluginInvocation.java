package com.untangle.uvm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Forwards the fixed CMD operations absent from the common Plugin interface.
 */
final class SafePluginInvocation
{
    private SafePluginInvocation()
    {
    }

    static Object invoke(Plugin delegate, String name, Class<?>[] parameters, Object[] arguments)
        throws Exception
    {
        Method method = delegate.getClass().getMethod(name, parameters);
        try {
            return method.invoke(delegate, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException("Plugin invocation failed", cause);
        }
    }
}
