/**
 * $Id$
 */
package com.untangle.uvm.reports.jabsorb;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.jabsorb.JSONRPCBridge;
import org.jabsorb.callback.CallbackController;

/**
 * Untangle Callback controller.
 */
@SuppressWarnings("serial")
public class UtCallbackController extends CallbackController
{
    private final JSONRPCBridge bridge;

    /**
     * Set the bridge.
     *
     * @param bridge
     *  Bridge to set.
     */
    UtCallbackController(JSONRPCBridge bridge)
    {
        this.bridge = bridge;
    }
    
    /**
     * Do nothing before callback is invoked.
     *
     * @param context
     *  Context.
     * @param instance
     *  Instance.
     * @param method
     *  Method to call.
     * @param arguments
     *  Arguments to method.
     */
    @Override
    public void preInvokeCallback(Object context, Object instance, Method method,
                                  Object[] arguments)
    {
    }

    /**
     * Do nothing after callback is invoked.
     *
     * @param context
     *  Context.
     * @param instance
     *  Instance.
     * @param method
     *  Method to call.
     * @param result
     *  Result of call.
     * @throws Exception
     *  If exception encountered.
     */
    @Override
    public void postInvokeCallback(Object context, Object instance,
                                   Method method, Object result)
        throws Exception
    {
        if (result!= null && !(result instanceof Serializable)) {
            bridge.registerCallableReference(result.getClass());
        }
    }
}
