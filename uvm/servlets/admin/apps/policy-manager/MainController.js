Ext.define('Ung.apps.policymanager.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-policy-manager',

    control: {
        '#': {
            afterrender: 'getSettings',
        }
    },

    listen: {
        store: {
            '#policiestree': {
                rootchange: 'onRootChange'
            }
        }
    },

    settings: null,
    tree: [],
    policiesMap: {},
    selectedPolicyId: null,


    getSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel(), policies, selNode;
        v.setLoading(true);
        v.appManager.getSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            me.settings = result;
        });
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel(), policies = [];
        // v.setLoading(true);
        vm.set('appsData', []);

        me.lookup('tree').getRootNode().cascadeBy(function (node) {
            if (node.isRoot()) { return; }
            policies.push({
                name: node.get('name'),
                description: node.get('description'),
                policyId: node.get('policyId'),
                parentId: node.get('parentPolicyId') || null,
                javaClass: 'com.untangle.app.policy_manager.PolicySettings'
            });
        });

        // console.log(policies);

        me.settings.policies.list = policies;

        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            // Util.successToast('Settings saved');
            // me.getSettings();
            Ext.getStore('policiestree').build();
        }, me.settings);
    },

    onRootChange: function (newRoot) {
        var me = this, selNode;
        if (me.selectedPolicyId) {
            selNode = newRoot.findChild('policyId', me.selectedPolicyId, true);
            if (selNode) {
                // short delay for node selection
                setTimeout(function() {
                    me.lookup('tree').selectPath(selNode.getPath('policyId'), 'policyId');
                }, 100);
            }
        }
    },

    onPolicySelect: function (rowModel, selectedNode) {
        var me = this, policiesStore = [[0, 'None'.t()]];
        me.selectedPolicyId = selectedNode.get('policyId');
        me.selectedPolicyName = selectedNode.get('name');
        me.setPoliciesCombo(selectedNode);
        Rpc.asyncData('rpc.appManager.getAppsView', selectedNode.get('policyId'))
            .then(function (result) {
                me.buildApps(result);
                // console.log(result);
            });
    },

    setPoliciesCombo: function (selectedNode) {
        var me = this, policiesStore = [[0, 'None'.t()]];

        me.lookup('tree').getRootNode().cascadeBy(function (node) {
            if (node.isRoot()) { return; }
            if (selectedNode) {
                if (node.get('policyId') !== selectedNode.get('policyId') && !node.isAncestor(selectedNode)) {
                    policiesStore.push([node.get('policyId'), node.get('name')]);
                }
            } else {
                policiesStore.push([node.get('policyId'), node.get('name')]);
            }

        });
        me.lookup('policiesCombo').setStore(policiesStore);
    },

    buildApps: function (policy) {
        var me = this, appsList = [], instance, status = null, parentPolicy = null;
        Ext.Array.each(policy.appProperties.list, function (app) {

            if (app.type !== 'FILTER') { return; }

            instance = Ext.Array.findBy(policy.instances.list, function(instance) { return instance.appName === app.name; });

            if (instance) {
                // status = instance.targetState;
                if (instance.policyId && policy.policyId !== instance.policyId) {
                    // parentPolicy = me.policiesMap[instance.policyId].name;
                    parentPolicy = me.lookup('tree').getStore().findNode('policyId', instance.policyId).get('name');
                }

                Ext.apply(app, {
                    status: instance.targetState || null,
                    instanceId: instance.id || null,
                    // status: status || null,
                    parentPolicy: parentPolicy || null,
                });
            }
            appsList.push(app);
        });
        Ext.Array.each(policy.installable.list, function (app) {
            if (app.type === 'FILTER' && !Ext.Array.findBy(appsList, function(instance) { return instance.name === app.name; })) {
                appsList.push(app);
            }
        });

        this.getViewModel().set('appsData', appsList);
    },

    newPolicy: function () {
        var me = this, vm = this.getViewModel();
        me.lookup('tree').getSelectionModel().deselectAll();
        vm.set('appsData', []);
        me.setPoliciesCombo();
    },

    addPolicy: function () {
        var me = this, v = me.getView(),
            values = me.getView().down('form').getValues();

        me.settings.policies.list.push({
            name: values.name,
            description: values.description,
            policyId: null,
            parentId: values.parentPolicyId || null,
            javaClass: 'com.untangle.app.policy_manager.PolicySettings'
        });

        me.selectedPolicyId = me.settings.nextPolicyId;
        v.setLoading(true);
        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            // Util.successToast('Settings saved');
            // me.getSettings();
            Ext.getStore('policiestree').build();
        }, me.settings);
    },

    onInstall: function (btn) {
        var me = this, rec = btn.getViewModel().get('record');

        Ext.Msg.wait('Installing ' + rec.get('displayName'), me.selectedPolicyName, { interval: 500, text: '' });

        Rpc.asyncData('rpc.appManager.instantiate', rec.get('name'), me.selectedPolicyId)
        .then(function (result1) {
            Rpc.asyncData('rpc.appManager.getAppsView', me.selectedPolicyId)
                .then(function (result2) {
                    me.buildApps(result2);
                    Ext.Msg.close();
                });
        });
    },

    onRemove: function (btn) {
        var me = this, rec = btn.getViewModel().get('record');

        Ext.Msg.wait('Removing ' + rec.get('displayName'), me.selectedPolicyName, { interval: 500, text: '' });

        Rpc.asyncData('rpc.appManager.destroy', rec.get('instanceId'))
            .then(function (result) {
                Rpc.asyncData('rpc.appManager.getAppsView', me.selectedPolicyId)
                    .then(function (result2) {
                        me.buildApps(result2);
                        Ext.Msg.close();
                    });
            });
    },

    onStart: function (btn) {
        var me = this, rec = btn.getViewModel().get('record'),
            appManager = rpc.appManager.app(rec.get('instanceId'));

        if (appManager) {
            Ext.Msg.wait('Starting ' + rec.get('displayName'), me.selectedPolicyName, { interval: 500, text: '' });
            appManager.start(function (result, ex) {
                if (ex) { Ext.Msg.alert('Error', ex.message); return false; }
                Rpc.asyncData('rpc.appManager.getAppsView', me.selectedPolicyId)
                    .then(function (result2) {
                        me.buildApps(result2);
                        Ext.Msg.close();
                    });
            });
        }
    },

    onStop: function (btn) {
        var me = this, rec = btn.getViewModel().get('record'),
            appManager = rpc.appManager.app(rec.get('instanceId'));

        if (appManager) {
            Ext.Msg.wait('Stopping ' + rec.get('displayName'), me.selectedPolicyName, { interval: 500, text: '' });
            appManager.stop(function (result, ex) {
                if (ex) { Ext.Msg.alert('Error', ex.message); return false; }
                Rpc.asyncData('rpc.appManager.getAppsView', me.selectedPolicyId)
                    .then(function (result2) {
                        me.buildApps(result2);
                        Ext.Msg.close();
                    });
            });
        }
    }

});
