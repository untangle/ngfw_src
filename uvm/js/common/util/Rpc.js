/**
 * rpc connectivity brain
 */
Ext.define('Ung.util.Rpc', {
    alternateClassName: 'Rpc',
    singleton: true,

    evalExp: function(ns, context, expression) {
        if ( ns == null || context == null || expression == null ) {
            console.error('Error: Invalid RPC expression: \'' + expression + '\'.');
            Util.handleException('Invalid RPC expression: \'' + expression + '\'.');
            return null;
        }

        var lastPart = null;
        var len = ns.length;

        for (var i = 0; i < len ; i++) {
            if (context == null )
                break;
            var part = ns[i];
            context = context[part];
            lastPart = part;
        }
        if (context == null ) {
            console.error('Error: Invalid RPC expression: \'' + expression + '\'. Attribute \'' + lastPart + '\' is null');
            Util.handleException('Invalid RPC expression: \'' + expression + '\'. Attribute \'' + lastPart + '\' is null');
            return null;
        }

        return context;
    },

    asyncData: function(expression /*, args */) {
        var args = [].slice.call(arguments).splice(1),
            ns = expression.split('.'),
            method = ns.pop(),
            context = window,
            dfrd = new Ext.Deferred();

        context = Ung.util.Rpc.evalExp(ns, context, expression);
        if (!context) return null;

        if (!context.hasOwnProperty(method) || !Ext.isFunction(context[method])) {
            console.error('Error: No such RPC method: \'' + expression + '\'');
            Util.handleException('No such RPC method: \'' + expression + '\'');
            return null;
        }

        args.unshift(function (result, ex) {
            if (ex) {
                console.error('Error: ' + ex);
                Util.handleException(ex);
                dfrd.reject(ex);
            }
            // console.info(expression + ' (async data) ... OK');
            dfrd.resolve(result);
        });

        context[method].apply(null, args);
        return dfrd.promise;
    },

    directData: function(expression /*, args */) {
        var ns = expression.split('.'),
            method = ns.pop(),
            context = window,
            lastPart = null;

        context = Ung.util.Rpc.evalExp(ns, context, expression);
        if (!context) return null;

        if (!context.hasOwnProperty(method) || !Ext.isFunction(context[method])) {
            console.error('Error: No such RPC method: \'' + expression + '\' on attribute \'' + lastPart + '\'');
            Util.handleException('No such RPC method: \'' + expression + '\' on attribute \'' + lastPart + '\'');
            return null;
        }

        try {
            return context[method].call();
        } catch (ex) {
            Util.handleException(ex);
        }
        return null;
    },

    asyncPromise: function(expression /*, args */) {
        var args = [].slice.call(arguments).splice(1),
            ns = expression.split('.'),
            method = ns.pop(),
            context = window;

        context = Ung.util.Rpc.evalExp(ns, context, expression);
        if (!context) return null;

        if (!context.hasOwnProperty(method) || !Ext.isFunction(context[method])) {
            console.error('Error: No such RPC method: \'' + expression + '\'');
            Util.handleException('No such RPC method: \'' + expression + '\'');
            return null;
        }

        return function() {
            var dfrd = new Ext.Deferred();
            args.unshift(function (result, ex) {
                if (ex) { dfrd.reject(ex); }
                // console.info(expression + ' (async promise) ... OK');
                dfrd.resolve(result);
            });
            context[method].apply(null, args);
            return dfrd.promise;
        };
    },

    directPromise: function(expression /*, args */) {
        var ns = expression.split('.'),
            method = ns.pop(),
            context = window;

        context = Ung.util.Rpc.evalExp(ns, context, expression);
        if (!context) return null;

        if (!context.hasOwnProperty(method) || !Ext.isFunction(context[method])) {
            console.error('Error: No such RPC method: \'' + expression + '\'');
            Util.handleException('No such RPC method: \'' + expression + '\'');
            return null;
        }

        return function() {
            var dfrd = new Ext.Deferred();
            try {
                // console.info(expression + ' (direct promise) ... OK');
                dfrd.resolve(context[method].call());
            } catch (ex) {
                dfrd.reject(ex);
            }
            return dfrd.promise;
        };
    },

});
