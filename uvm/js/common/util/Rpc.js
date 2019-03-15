/**
 * Rpc connectivity brain
 */
Ext.define('Ung.util.Rpc', {
    alternateClassName: 'Rpc',
    singleton: true,

    /**
     * With the full rpc path and arguments, verify that each node and the target method exists.
     *
     * The first argument can be an already resolved RPC path.  If this is the case, then the
     * second argument is the string path and subsequent arguments are arguments to that path mthod.
     *
     * If path does not exist, return null.
     * Otherwise, return an object containing:
     *  context The object to execute.
     *  args    Arguments to execute in the context.
     */
    getCommand: function() {
        var path = arguments[0],
            args, nodes,
            method = null, context,
            result = {
                context: null,
                args: null,
                error: null
            };

        if(path === undefined){
            result.error = "Null path or rpc object";
            return result;
        }else if(typeof(path) === 'object'){
            args = [].slice.call(arguments).splice(2);
            context = arguments[0];
            path = arguments[1];
        }else{
            args = [].slice.call(arguments).splice(1);
            context = window;
        }
        nodes = path.split('.');
        method = nodes.pop();

        if ( context == null || arguments[0] == null ) {
            result.error = "Invalid RPC path: '" + path + "'";
            return result;
        }

        var len = nodes.length;
        for (var i = 0; i < len ; i++) {
            if (context == null ){
                break;
            }
            var node = nodes[i];

            if(node.indexOf('(') > -1 && node.indexOf(')') > -1){
                // Handle this context with arguments, such as finding an app by its name.

                // Extract argument list from within parens.
                var cargsList=node.substring(node.indexOf('(') + 1, node.indexOf(')') ).split(',');
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
                result.error = "Invalid RPC path: '" + path + "' Attribute '" + node + "' is null";
                return result;
            }
        }

        if (method && !context.hasOwnProperty(method) ){
            result.error = "No such RPC property or method: '" + path + "'";
            return result;
        }else if(method && !Ext.isFunction(context[method]) && args.length > 0){
            result.error = "RPC property requsted but arguments : '" + path + "'";
            return result;
        }else if(method){
            result.context = context[method];
            result.args = args;
        }else{
            result.context = context;
            result.args = args;
        }

        return result;
    },

    /**
     * If we get an exception from within a deferred, we need to handlee it manually
     * here.
     *
     * Also look for certain known JSONRpc exceptions that we just cand do from getCommand
     * and try to clarify them:
     *     -    "method not found" almost certainly means that we have an argument mismatch.
     *
     * @param  dfrd Deferred object.
     * @param  ex   Exception.
     */
    processException: function(dfrd, ex, handle){
        if( typeof(ex) == 'object'){
            if(ex.message && ex.message.indexOf("method not found") > -1){
                ex.message = "possible argument count mis-match: " + ex.message;
            }
        }
        if(dfrd){
            dfrd.reject(ex);
        }
        if(handle){
            console.log(ex);
            Util.handleException(ex.toString());
        }
    },

    exists: function(){
        var commandResult = this.getCommand.apply(null, arguments);
        if(commandResult.context == null || commandResult.context.error){
            return false;
        }
        return true;
    },

    /**
     * Make RPC call as a deferred function and return promise.
     */
    asyncData: function() {
        var commandResult = this.getCommand.apply(null, arguments);
        if(commandResult.context != null && !Ext.isFunction(commandResult.context)){
            // Asynchronously getting a property doesn't make any sense without writing
            // an anonymoys function to handle it.  Don't allow it.
            return Ext.Deferred.rejected("Path '" + arguments[0] + "' is not a function, use a direct method instead");
        }

        if(commandResult.context == null){
            return Ext.Deferred.rejected(commandResult.error);
        }else{
            var dfrd = new Ext.Deferred();
            commandResult.args.unshift(function (result, ex) {
                if (ex) {
                    Rpc.processException(dfrd, ex, true);
                }
                dfrd.resolve(result);
            });

            commandResult.context.apply(null, commandResult.args);
            return dfrd.promise;
        }
    },

    /**
     * Make RPC call and return the results.
     */
    directData: function() {
        var commandResult = this.getCommand.apply(null, arguments);
        if(commandResult.context == null){
            Util.handleException(commandResult.error.toString());
            throw(commandResult.error);
        }

        try {
            return  Ext.isFunction(commandResult.context) ? commandResult.context.apply(null, commandResult.args) : commandResult.context;
        } catch (ex) {
            Rpc.processException(null, ex, true);
        }
        return null;
    },

    /**
     * Return a function containing the RPC call in a deferred function that will return a promise.
     * Suitable for calling in a sequence.
     */
    asyncPromise: function() {
        var commandResult = this.getCommand.apply(null, arguments);

        if(commandResult.context != null && !Ext.isFunction(commandResult.context)){
            // Asynchronously getting a property doesn't make any sense without writing
            // an anonymous function to handle it.  Don't allow it.
            return Ext.Deferred.rejected("Path '" + arguments[0] + "' is not a function, use a direct method instead");
        }

        if(commandResult.context == null){
            return Ext.Deferred.rejected(commandResult.error);
        }else{
            return function() {
                var dfrd = new Ext.Deferred();
                commandResult.args.unshift(function (result, ex) {
                    if (ex) {
                        Rpc.processException(dfrd, ex, true);
                    }
                    dfrd.resolve(result);
                });
                commandResult.context.apply(null, commandResult.args);
                return dfrd.promise;
            };
        }
    },

    /**
     * Return a function containing the RPC call in a deferred function that will return a promise.
     * Suitable for calling in a sequence.
     */
    directPromise: function() {
        var commandResult = this.getCommand.apply(null, arguments);
        if(commandResult.context == null){
            return Ext.Deferred.rejected(commandResult.error);
        }else{
            return function() {
                var dfrd = new Ext.Deferred();
                try {
                    dfrd.resolve( Ext.isFunction(commandResult.context) ? commandResult.context.apply(null, commandResult.args) : commandResult.context );
                } catch (ex) {
                    Rpc.processException(dfrd, ex, false);
                }
                return dfrd.promise;
            };
        }
    }
});
