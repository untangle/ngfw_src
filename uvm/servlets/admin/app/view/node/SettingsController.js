Ext.define('Ung.view.node.SettingsController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.nodesettings',

    config: {
        nodeManager: null
    },

    control: {
        '#': { // main settings view listeners
            beforeactivate: 'onBeforeActivate',
            beforedeactivate: 'onBeforeDeactivate'
        }
    },

    listen: {
        store: {
            '#metrics': {
                datachanged: 'updateMetrics'
            }
        }
    },

    onBeforeActivate: function () {
        var me = this,
            vm = this.getViewModel();

        var policy = Ext.getStore('policies').findRecord('policyId', vm.get('policyId'));

        // get node instance based on policy and node name

        var nodeInstance = policy.get('instances').list.filter(function (node) {
            return node.nodeName === vm.get('nodeName');
        })[0];
        // get node properties based on policy and node instance
        var nodeProps = vm.get('policy.nodeProperties.list').filter(function (prop) {
            return nodeInstance.nodeName === prop.name;
        })[0];

        vm.set('nodeInstance', nodeInstance);
        vm.set('nodeProps', nodeProps);

        var mask = new Ext.LoadMask({
            msg: 'Loading...'.t(),
            target: this.getView()
        }).show();

        // dynamic require node class
        Ext.require(Util.nodeClassMapping[nodeInstance.nodeName], function () {
            // get node manager
            console.log('hererere');
            rpc.nodeManager.node(function (nodeManager, ex) {
                if (ex) { Util.exceptionToast(ex); return false; }
                me.setNodeManager(nodeManager);
                // get node settings
                nodeManager.getSettings(function (settings) {
                    // add the node settings view, based on node type and instance
                    me.getView().add({
                        xtype: 'ung.' + nodeInstance.nodeName,
                        region: 'center',
                        itemId: 'settings'
                        //manager: nodeManager
                    });
                    console.log(settings);
                    vm.set('settings', settings);
                    mask.hide();
                });
            }, nodeInstance.id);
        });
    },

    /**
     * Updates the node metrics everytime the global metrics store is changed
     */
    updateMetrics: function () {
        //console.log('update metrics');
        var vm = this.getViewModel();
        var nodeMetrics = Ext.getStore('metrics').findRecord('nodeId', vm.get('nodeInstance.id'));
        if (nodeMetrics) {
            vm.set('metrics', nodeMetrics.get('metrics').list);
        }

        if (this.getView().down('nodechart')) {
            this.getView().down('nodechart').fireEvent('addPoint');
        }

    },

    /**
     * methos called when starting/stopping the node
     */
    onPower: function (btn) {
        var nodeManager = this.getNodeManager(),
            vm = this.getViewModel();

        btn.setDisabled(true);
        if (vm.get('nodeInstance.targetState') === 'RUNNING') {
            // stop node
            nodeManager.stop(function (result, ex) {
                if (ex) { Util.exceptionToast(ex); return false; }
                nodeManager.getRunState(function (result2, ex2) {
                    if (ex2) { Util.exceptionToast(ex2); return false; }
                    vm.set('nodeInstance.targetState', result2);
                    vm.notify();
                    btn.setDisabled(false);

                    if (nodeManager.getNodeProperties().name === 'untangle-node-reports') {
                        vm.getParent().set('reportsRunning', false);
                    }

                    Util.successToast(vm.get('powerMessage'));

                    Ext.GlobalEvents.fireEvent('nodestatechange', result2, vm.get('nodeInstance'));
                });
            });
        } else {
            // start node
            nodeManager.start(function (result, ex) {
                if (ex) {
                    Ext.Msg.alert('Error', ex.message);
                    btn.setDisabled(false);
                    return false;
                }
                nodeManager.getRunState(function (result2, ex2) {
                    if (ex2) { Util.exceptionToast(ex2); return false; }
                    vm.set('nodeInstance.targetState', result2);
                    vm.notify();
                    btn.setDisabled(false);

                    if (nodeManager.getNodeProperties().name === 'untangle-node-reports') {
                        vm.getParent().set('reportsRunning', true);
                    }

                    Util.successToast(vm.get('powerMessage'));
                    Ext.GlobalEvents.fireEvent('nodestatechange', result2, vm.get('nodeInstance'));
                });
            });
        }
    },

    onBeforeDeactivate: function (view) {
        console.log('on deactivate');
        view.remove('settings', {destroy: true});
    },

    /**
     * Saves the whole node settings object sending the new settings to backend via RPC
     */
    saveSettings: function () {
        var me = this,
            vm = this.getViewModel(),
            nodeManager = this.getNodeManager();
            //newSettings = me.getViewModel().get('settings');

        // all outstanding changes made on grids are commited and transferred to the node settings
        this.getView().query('grid').forEach(function(grid) {
            grid.fireEvent('save');
        });

        var myMask = new Ext.LoadMask({
            msg    : 'Saving ...',
            target : this.getView()
        }).show();

        // send settings to backend
        nodeManager.setSettings(function (result, ex) {
            myMask.hide();
            if (ex) { Util.exceptionToast(ex); return false; }

            Util.Util.successToast('Settings saved!');

            // retreive again settings from backend as pushed changes might have extra effects on the data
            nodeManager.getSettings(function (settings, ex) {
                if (ex) { Util.exceptionToast(ex); return false; }
                vm.set('settings', settings); // apply fresh settings on the viewmodel
                me.getView().query('grid').forEach(function(grid) {
                    grid.fireEvent('reloaded');
                });
            });
        }, vm.get('settings'));
    },

    removeNode: function () {
        var vm = this.getViewModel(), settingsView = this.getView();
        var message = Ext.String.format('{0} will be uninstalled from this policy.'.t(), 'display name') + '<br/>' +
            'All of its settings will be lost.'.t() + '\n' + '<br/>' + '<br/>' +
            'Would you like to continue?'.t();

        Ext.Msg.confirm('Warning:'.t(), message, function(btn) {
            if (btn === 'yes') {
                var nodeItem = settingsView.up('#main').down('#apps').down('#' + vm.get('nodeInstance.nodeName'));
                //nodeItem.setDisabled(true);
                nodeItem.addCls('remove');
                Ung.app.redirectTo('#apps/' + vm.get('policyId'));

                rpc.nodeManager.destroy(function (result, ex) {
                    if (ex) { Util.exceptionToast(ex); return false; }

                    rpc.nodeManager.getAppsViews(function (result2, ex2) {
                        if (ex2) { Util.exceptionToast(ex2); return; }
                        Ext.getStore('policies').loadData(result2);
                    });

                    if (nodeItem.node.name === 'untangle-node-reports') {
                        delete rpc.reportsManager;
                        vm.getParent().set('reportsInstalled', false);
                        vm.getParent().set('reportsRunning', false);
                        vm.getParent().notify();
                        Ext.GlobalEvents.fireEvent('reportsinstall');
                    }

                    if (rpc.reportsManager) {
                        rpc.reportsManager.getUnavailableApplicationsMap(function (result3, ex3) {
                            if (ex3) { Util.exceptionToast(ex3); return; }
                            Ext.getStore('unavailableApps').loadRawData(result3.map);

                            // fire nodeinstall event to update widgets on dashboard
                            Ext.GlobalEvents.fireEvent('nodeinstall', 'remove', nodeItem.node);
                        });
                    }
                }, vm.get('nodeInstance.id'));
            }
        });
    }
});
