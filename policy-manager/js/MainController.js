Ext.define('Ung.apps.policymanager.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-policy-manager',

    control: {
        '#': {
            afterrender: 'getSettings',
            deactivate: 'onDeactivate'
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

    onDeactivate: function () {
        this.lookup('tree').getSelectionModel().deselectAll();
        this.getViewModel().set('appsData', []);
    },

    getSettings: function () {
        var v = this.getView(), vm = this.getViewModel(), policies, selNode;

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'getSettings')
        .then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }

            vm.set('settings', result);

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });

    },

    // this saves only the rules as the policies are saved on the fly
    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();
        v.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            if (store.getModifiedRecords().length > 0 ||
                store.getNewRecords().length > 0 ||
                store.getRemovedRecords().length > 0 ||
                store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
            }
        });

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'setSettings', vm.get('settings'))
        .then(function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            Util.successToast('Settings saved');
            vm.set('panel.saveDisabled', false);
            v.setLoading(false);

            me.getSettings();
            Ext.fireEvent('resetfields', v);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    onRootChange: function (newRoot) {
        var me = this, selNode;

        if (me.selectedPolicyId) {
            selNode = newRoot.findChild('policyId', me.selectedPolicyId, true);
            if (selNode) {
                // short delay for node selection
                // todo: fix some issue when removing PolicyManager but tries to select a path
                setTimeout(function() {
                    me.lookup('tree').selectPath(selNode.getPath('policyId'), 'policyId');
                }, 100);
            }
        }else{
            var tree = me.lookup('tree');
            if( tree != null){
                tree.getSelectionModel().select(0);
            }
        }
    },

    onPolicySelect: function (rowModel, selectedNode) {
        var me = this;
        me.selectedPolicyId = selectedNode.get('policyId');
        me.selectedPolicyName = selectedNode.get('name');

        Rpc.asyncData('rpc.appManager.getAppsView', selectedNode.get('policyId'))
        .then(function (result) {
            if(Util.isDestroyed(me)){
                return;
            }
            me.buildApps(result);
        }, function(ex) {
            Util.handleException(ex);
        });
    },

    buildApps: function (policy) {
        var me = this, appsList = [], instance, status = null, parentPolicy = null;

        // fix NGFW-10894 - after creating new policy and installing apps within policy manager
        Rpc.asyncData('rpc.appManager.getAppsViews')
            .then(function (policies) {
                Ext.getStore('policies').loadData(policies);
            });
        // also get the current policy apps if were installed/removed
        Ung.app.getGlobalController().getAppsView().getController().getApps();

        Ext.Array.each(policy.appProperties.list, function (app) {

            if (app.type !== 'FILTER') { return; }

            instance = Ext.Array.findBy(policy.instances.list, function(instance) { return instance.appName === app.name; });
            parentPolicy = null;

            if (instance) {
                if (instance.policyId && policy.policyId !== instance.policyId) {
                    parentPolicy = me.lookup('tree').getStore().findNode('policyId', instance.policyId).get('name');
                }

                instance.runState = policy.runStates.map[instance.id];

                Ext.apply(app, {
                    state: Ext.create('Ung.model.AppState',{instance: instance}),
                    instanceId: instance.id || null,
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
            appManager = Rpc.directData('rpc.appManager.app', rec.get('instanceId'));

        if (appManager) {
            Ext.Msg.wait('Starting ' + rec.get('displayName'), me.selectedPolicyName, { interval: 500, text: '' });
            Ext.Deferred.sequence([
                Rpc.directPromise(appManager, 'start'),
                Rpc.directPromise('rpc.appManager.getAppsView', me.selectedPolicyId)
            ]).then(function(result){
                if(Util.isDestroyed(me)){
                    return;
                }
                me.buildApps(result[1]);
                Ext.Msg.close();

            }, function(ex){
                Ext.Msg.close();
                Util.handleException(ex);
            });
        }
    },

    onStop: function (btn) {
        var me = this, rec = btn.getViewModel().get('record'),
            appManager = rpc.appManager.app(rec.get('instanceId'));

        if (appManager) {
            Ext.Msg.wait('Stopping ' + rec.get('displayName'), me.selectedPolicyName, { interval: 500, text: '' });
            Ext.Deferred.sequence([
                Rpc.directPromise(appManager, 'stop'),
                Rpc.directPromise('rpc.appManager.getAppsView', me.selectedPolicyId)
            ]).then(function(result){
                if(Util.isDestroyed(me)){
                    return;
                }
                me.buildApps(result[1]);
                Ext.Msg.close();

            }, function(ex){
                Ext.Msg.close();
                Util.handleException(ex);
            });
        }
    },

    removePolicy: function (view, rowIndex, colIndex, item, e, record) {
        var me = this, vm = me.getViewModel(), idx;

        Ext.Array.each(vm.get('settings.policies.list'), function (p, index) {
            if (p.policyId === record.get('policyId')) {
                idx = index;
            }
        });

        Ext.Array.removeAt(vm.get('settings.policies.list'), idx);

        view.setLoading(true);
        Rpc.asyncData(me.getView().appManager, 'setSettings', vm.get('settings'))
        .then( function(result){
            if(Util.isDestroyed(view, vm, record)){
                return;
            }
            view.setLoading(false);
            if (me.selectedPolicyId === record.get('policyId')) {
                me.selectedPolicyId = null;
                vm.set('appsData', []);
            }
            Ext.getStore('policiestree').build();
        }, function(ex) {
            if(Util.isDestroyed(view)){
                view.setLoading(true);
            }
            Util.handleException(ex);
        });
    },

    editPolicy: function (view, rowIndex, colIndex, item, e, record) {
        this.showEditor(record);
    },

    addPolicy: function () {
        this.showEditor();
    },

    showEditor: function (rec) {
        var me = this, 
            vm = this.getViewModel(),
            policiesStore = [[0, 'None'.t()]], 
            initialName;

        if (!me.lookup('tree')) { return; }
        me.lookup('tree').getRootNode().cascadeBy(function (node) {
            if (node.isRoot()) { return; }
            if (rec) {
                if (node.get('policyId') !== rec.get('policyId') && !node.isAncestor(rec)) {
                    policiesStore.push([node.get('policyId'), node.get('name')]);
                }
            } else {
                policiesStore.push([node.get('policyId'), node.get('name')]);
            }
        });

        if (rec) { initialName = rec.get('name'); }

        me.editWin = me.getView().add({
            xtype: 'window',
            width: 300,
            modal: true,
            title: rec ? 'Edit Policy' : 'New Policy',
            items: [{
                xtype: 'form',
                layout: 'anchor',
                bodyPadding: 10,
                border: false,
                defaults: {
                    anchor: '100%'
                },
                items: [{
                    xtype: 'hidden',
                    name: 'policyId',
                    value: rec ? rec.get('policyId') : vm.get('settings.nextPolicyId')
                }, {
                    xtype: 'hidden',
                    name: 'javaClass',
                    value: 'com.untangle.app.policy_manager.PolicySettings'
                }, {
                    xtype:  'textfield',
                    name: 'name',
                    fieldLabel: '<strong>' + 'Name'.t() + '</strong>',
                    emptyText: 'enter policy name',
                    allowBlank: false,
                    labelAlign: 'top',
                    value: rec ? rec.get('name') : '',
                    msgTarget: 'side',
                    validator: function (val) {
                        if (val === initialName) { return true; } // if name not modified it can be saved
                        if (Ext.getStore('policiestree').find('name', val, 0, false, false, true) >= 0) {
                            return 'This policy name already exists'.t();
                        }
                        return true;
                    }
                }, {
                    xtype:  'textarea',
                    name: 'description',
                    fieldLabel: '<strong>' + 'Description'.t() + '</strong>',
                    emptyText: 'enter policy description',
                    labelAlign: 'top',
                    value: rec ? rec.get('description') : ''
                }, {
                    xtype: 'combo',
                    name: 'parentId',
                    fieldLabel: '<strong>' + 'Parent'.t() + '</strong>',
                    reference: 'policiesCombo',
                    labelAlign: 'top',
                    value: rec ? rec.get('parentPolicyId') : 0,
                    store: policiesStore,
                    emptyText: 'Select parent',
                    allowBlank: false,
                    hidden: rec && rec.get('policyId') === 1,
                    queryMode: 'local',
                    editable: false
                }],
                buttons: [{
                    text: 'Cancel'.t(),
                    iconCls: 'fa fa-ban',
                    handler: function (btn) {
                        btn.up('window').close();
                    }
                }, {
                    text: rec ? 'Save'.t() : 'Add'.t(),
                    action: rec ? 'save' : 'add',
                    formBind: true,
                    iconCls: 'fa fa-floppy-o',
                    handler: 'savePolicy'
                }]
            }]
        });
        me.editWin.show();
    },

    savePolicy: function (btn) {
        var me = this, 
            vm = this.getViewModel(),
            win = btn.up('window'), 
            values = btn.up('form').getValues();

        values.policyId = parseInt(values.policyId, 10);
        values.parentId = parseInt(values.parentId, 10);

        if (btn.action === 'save') {
            var editPolicy = Ext.Array.findBy(vm.get('settings.policies.list'), function (policy) {
                return policy.policyId === values.policyId;
            });
            Ext.apply(editPolicy, values);
        } else {
            vm.get('settings.policies.list').push(values);
        }

        // Save just this policy, not all other changes.
        win.setLoading(true);
        Rpc.asyncData(me.getView().appManager, 'setSettings', vm.get('settings'))
        .then(function(result){
            if(Util.isDestroyed(vm)){
                // win and btn cclaim to be destroyed at this point but not really?
                return;
            }
            win.setLoading(false);

            if (btn.action === 'add') {
                me.selectedPolicyId = values.policyId;
            }
            Ext.getStore('policiestree').build();
            me.getSettings();
        }, function(ex) {
            win.setLoading(true);
            Util.handleException(ex);
        });

        win.close();
    }

});
