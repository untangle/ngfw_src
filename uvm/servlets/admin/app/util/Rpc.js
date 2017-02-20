/**
 * rpc connectivity brain
 */
Ext.define('Ung.util.Rpc', {
    alternateClassName: 'Rpc',
    singleton: true,

    asyncData: function(expression /*, args */) {
        var args = [].slice.call(arguments).splice(1),
            ns = expression.split('.'),
            method = ns.pop(),
            context = window,
            dfrd = new Ext.Deferred();

        ns.forEach(function(part) { context = context[part]; });

        if (!context.hasOwnProperty(method) || !Ext.isFunction(context[method])) {
            console.error('Error: No such RPC method: \'' + expression + '\'');
            Util.exceptionToast('No such RPC method: \'' + expression + '\'');
            return;
        }

        args.unshift(function (result, ex) {
            if (ex) {
                console.error('Error: ' + ex);
                Util.exceptionToast(ex);
                dfrd.reject(ex);
            }
            console.info(expression + ' (async data) ... OK');
            dfrd.resolve(result);
        });

        context[method].apply(null, args);
        return dfrd.promise;
    },

    asyncPromise: function(expression /*, args */) {
        var args = [].slice.call(arguments).splice(1),
            ns = expression.split('.'),
            method = ns.pop(),
            context = window;

        ns.forEach(function(part) { context = context[part]; });

        if (!context.hasOwnProperty(method) || !Ext.isFunction(context[method])) {
            console.error('Error: No such RPC method: \'' + expression + '\'');
            Util.exceptionToast('No such RPC method: \'' + expression + '\'');
            return;
        }

        return function() {
            var dfrd = new Ext.Deferred();
            args.unshift(function (result, ex) {
                if (ex) { dfrd.reject(ex); }
                console.info(expression + ' (async promise) ... OK');
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

        ns.forEach(function(part) { context = context[part]; });

        if (!context.hasOwnProperty(method) || !Ext.isFunction(context[method])) {
            console.error('Error: No such RPC method: \'' + expression + '\'');
            Util.exceptionToast('No such RPC method: \'' + expression + '\'');
            return;
        }

        return function() {
            var dfrd = new Ext.Deferred();
            try {
                console.info(expression + ' (direct promise) ... OK');
                dfrd.resolve(context[method].call());
            } catch (ex) {
                dfrd.reject(ex);
            }
            return dfrd.promise;
        };
    },

});
