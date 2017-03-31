Ext.define('Ung.apps.policymanager.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-policy-manager',

    control: {
        '#': {
            afterrender: 'getSettings',
        }
    },

    tree: [],
    policiesMap: {},

    getSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);
        v.appManager.getSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            console.log(result);
            me.policiesMap = Ext.Array.toValueMap(result.policies.list, 'policyId');
            // vm.set('settings', result);

            console.log(me.policiesMap);

            me.lookup('tree').setRoot({
                name: 'Policies',
                expanded: true,
                children: me.buildPoliciesTree(Ext.clone(result.policies.list))
            });
        });
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);
        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
        }, vm.get('settings'));
    },



    buildPoliciesTree: function (array, parent, tree) {
        var me = this;
        tree = typeof tree !== undefined ? tree : [];
        parent = parent || { policyId: null };


        var children = Ext.Array.filter(array, function (child) {
            return child.parentId === parent.policyId;
        });

        if (!Ext.isEmpty(children)) {
            parent.iconCls = 'fa fa-file-text-o';
            if (parent.policyId === null) {
                tree = children;
            } else {
                parent.children = children;
            }
            Ext.Array.each(children, function (child) {
                me.buildPoliciesTree(array, child);
            });
        } else {
            parent.iconCls = 'fa fa-file-text-o';
            parent.leaf = true;
        }

        console.log('here');

        return tree;
    },

    onPolicySelect: function (rowModel, node) {
        var me = this;
        Rpc.asyncData('rpc.appManager.getAppsView', node.get('policyId'))
            .then(function (result) {
                me.buildApps(result);
                // console.log(result);
            });
    },

    buildApps: function (policy) {
        console.log(policy);
        var me = this, appsList = [], instance, status = null, parentPolicy = null;

        Ext.Array.each(policy.appProperties.list, function (app) {
            instance = Ext.Array.findBy(policy.instances.list, function(instance) { return instance.appName === app.name; });

            if (instance) {
                status = instance.targetState;
                if (instance.policyId && policy.policyId !== instance.policyId) {
                    parentPolicy = me.policiesMap[instance.policyId].name;
                }
                // console.log(inheritedApp);
            }
            appsList.push({
                displayName: app.displayName,
                name: app.name,
                type: app.type,
                status: status || null,
                parentPolicy: parentPolicy || null
            });
        });
        Ext.Array.each(policy.installable.list, function (app) {
            if (!Ext.Array.findBy(appsList, function(instance) { return instance.name === app.name; })) {
                appsList.push({
                    displayName: app.displayName,
                    name: app.name,
                    type: app.type,
                    status: null,
                    parentPolicy: null
                });
            }
        });

        // console.log(appsList);
        this.getViewModel().set('appsData', appsList);
        // this.lookup('apps')
    }


});
