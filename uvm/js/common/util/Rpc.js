/**
 * Rpc connectivity brain
 */
Ext.define('Ung.util.Rpc', {
    alternateClassName: 'Rpc',
    singleton: true,

    /**
     * With the full rpc path and arguments, verify that each node and the target method exists.
     *
     * If path does not exist, return null.
     * Otherwise, return an object containing:
     *  context The object to execute.
     *  args    Arguments to execute in the context.
     */
    resolve: function() {
        var args = [].slice.call(arguments).splice(1),
            path = arguments[0],
            nodes = path.split('.'),
            method = nodes.pop(),
            context = window;

        if ( nodes === null || context === null || arguments[0] === null ) {
            Util.handleException('Invalid RPC path: \'' + path + '\'.');
            return null;
        }

        var len = nodes.length;
        for (var i = 0; i < len ; i++) {
            if (context === null ){
                break;
            }
            var node = nodes[i];

            if(node.indexOf('(') > -1 && node.indexOf(')') > -1){
                // Handle this context with arguments, such as finding an app by its name.

                // Extract argument list from within parens.
                cargsList=node.substring(node.indexOf('(') + 1, node.indexOf(')') ).split(',');
                var cargs = [];
                cargsList.forEach(function(carg){
                    if(carg[0] == '"' && carg[carg.length-1] == '"'){
                        // Strip quotes around argument.
                        carg=carg.substring(1,carg.length-1);
                    }
                    if(carg.trim().length > 0){
                        cargs.push(carg);
                    }
                });

                // Pull the method.
                node = node.substring(0, node.indexOf('('));
                if(context[node] == null){
                    break;
                }
                context = context[node].apply(null,cargs);
            }else{
                if(Ext.isFunction(context[node])){
                    context = context[node].apply(null,null);
                }else{
                    context = context[node];
                }
            }
            if(context == null){
                Util.handleException('Invalid RPC path: \'' + path + '\'. Attribute \'' + node + '\' is null');
                return null;
            }

        }

        if (!context.hasOwnProperty(method) ){
            Util.handleException('No such RPC property or method: \'' + path + '\'');
            return null;
        }
        if(!Ext.isFunction(context[method]) && args.length > 0){
            Util.handleException('RPC property requsted but arguments : \'' + path + '\'');
            return null;
        }

        return {
            context: context[method],
            args: args
        };
    },

    /**
     * Make RPC call as a deferred function and return promise.
     */
    asyncData: function() {
        var resolveResults = this.resolve.apply(null, arguments);
        if(resolveResults == null){
            return null;
        }
        if(!Ext.isFunction(resolveResults.context)){
            // Asynchronously getting a property doesn't make any sense without writing
            // an anonymoys function to handle it.  Don't allow it.
            Util.handleException('Path \'' + arguments[0] + '\' is not a function, use a direct method instead');
            return null;
        }

        var dfrd = new Ext.Deferred();

        resolveResults.args.unshift(function (result, ex) {
            if (ex) {
                console.error('Error: ' + ex);
                Util.handleException(ex);
                dfrd.reject(ex);
            }
            dfrd.resolve(result);
        });

        resolveResults.context.apply(null, resolveResults.args);
        return dfrd.promise;
    },

    /**
     * Make RPC call and return the results.
     */
    directData: function() {
        var resolveResults = this.resolve.apply(null, arguments);
        if(resolveResults == null){
            return null;
        }

        try {
            return  Ext.isFunction(resolveResults.context) ? resolveResults.context.apply(null, resolveResults.args) : resolveResults.context;
        } catch (ex) {
            Util.handleException(ex);
        }
        return null;
    },

    /**
     * Return a function containing the RPC call in a deferred function that will return a promise.
     * Suitable for calling in a sequence.
     */
    asyncPromise: function() {
        var resolveResults = this.resolve.apply(null, arguments);
        if( resolveResults == null){
            return null;
        }
        if(!Ext.isFunction(resolveResults.context)){
            // Asynchronously getting a property doesn't make any sense without writing
            // an anonymoys function to handle it.  Don't allow it.
            Util.handleException('Path \'' + arguments[0] + '\' is not a function, use a direct method instead');
            return null;
        }

        return function() {
            var dfrd = new Ext.Deferred();
            resolveResults.args.unshift(function (result, ex) {
                if (ex) { dfrd.reject(ex); }
                dfrd.resolve(result);
            });
            resolveResults.context.apply(null, resolveResults.args);
            return dfrd.promise;
        };
    },

    /**
     * Return a function containing the RPC call in a deferred function that will return a promise.
     * Suitable for calling in a sequence.
     */
    directPromise: function() {
        var resolveResults = this.resolve.apply(null, arguments);
        if(resolveResults == null){
            return null;
        }

        return function() {
            var dfrd = new Ext.Deferred();
            try {
                dfrd.resolve( Ext.isFunction(resolveResults.context) ? resolveResults.context.apply(null, resolveResults.args) : resolveResults.context );
            } catch (ex) {
                dfrd.reject(ex);
            }
            return dfrd.promise;
        };
    }
});
