Ext.define('Ung.store.PoliciesTree', {
    extend: 'Ext.data.TreeStore',
    alias: 'store.policiestree',
    storeId: 'policiestree',
    // filterer: 'bottomup',


    build: function () {
        var me = this, policies;

        var policyManager = rpc.appManager.app('policy-manager');

        if (!policyManager) { return; }

        policyManager.getSettings(function (result, ex) {
            if (ex) { Util.exceptionToast(ex); return; }
            policies = result.policies.list;

            Ext.Array.each(policies, function (policy) {
                policy.parentPolicyId = policy.parentId || 0;
                // policy.text = policy.name;
            });

            var tree = me.recursiveTree(Ext.clone(policies));

            me.setRoot({
                name: 'Policies',
                policyId: 'root', // used for path
                expanded: true,
                children: tree
            });

        });
    },

    recursiveTree: function (array, parent, tree) {
        var me = this;
        tree = typeof tree !== undefined ? tree : [];
        parent = parent || { policyId: null };


        var children = Ext.Array.filter(array, function (child) {
            return child.parentId === parent.policyId;
        });

        if (!Ext.isEmpty(children)) {
            parent.expanded = true;
            parent.iconCls = 'fa fa-file-text-o';
            if (parent.policyId === null) {
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
