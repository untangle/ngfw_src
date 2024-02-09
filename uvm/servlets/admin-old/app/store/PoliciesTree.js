Ext.define('Ung.store.PoliciesTree', {
    extend: 'Ext.data.TreeStore',
    alias: 'store.policiestree',
    storeId: 'policiestree',
    // filterer: 'bottomup',


    build: function () {
        var me = this, policies;

        if(!Rpc.directData('rpc.appManager.app', 'policy-manager')){
            me.setRoot({
                name: 'Policies',
                policyId: 'root', // used for path
                expanded: true,
                children: {
                    policyId: 1,
                    name: 'Default'
                }
            });
        }else{
            Rpc.asyncData('rpc.appManager.app("policy-manager").getSettings')
            .then( function(result){
                if(Util.isDestroyed(me)){
                    return;
                }

                policies = result.policies.list;

                Ext.Array.each(policies, function (policy) {
                    /**
                     * parentId can be 0 or null (if no parent policy), so it is normalized to be just 0 (None)
                     * parentId is also used by the treestore internals so it will be used "parentPolicyId" instead
                     */
                    policy.parentId = policy.parentId || 0;
                    policy.parentPolicyId = policy.parentId;
                });

                var tree = me.recursiveTree(Ext.clone(policies));

                me.setRoot({
                    name: 'Policies',
                    policyId: 'root', // used for path
                    expanded: true,
                    children: tree
                });
            },function(ex){
                Util.handleException(ex);
            });
        }

    },

    recursiveTree: function (array, parent, tree) {
        var me = this;
        tree = typeof tree != undefined ? tree : [];
        parent = parent || { policyId: 0 };


        var children = Ext.Array.filter(array, function (child) {
            return child.parentId === parent.policyId;
        });

        if (!Ext.isEmpty(children)) {
            parent.expanded = true;
            parent.iconCls = 'fa fa-file-text-o';
            if (parent.policyId === 0) {
                tree = children;
            } else {
                parent.children = children;
            }
            Ext.Array.each(children, function (child) {
                // child.parentPolicyId = child.parentId; // parentId is reserved for the tree store
                me.recursiveTree(array, child);
            });
        } else {
            parent.iconCls = 'fa fa-file-text-o';
            parent.leaf = true;
        }
        return tree;
    }

});
