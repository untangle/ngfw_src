Ext.define('Ung.view.apps.install.InstallController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.appsinstall',

    control: {
        '#': {
            beforeactivate: 'onPolicy'
        }
    },

    listen: {
        store: {
            '#policies': {
                //datachanged: 'onPolicy'
            }
        }
    },

    init: function (view) {
        view.getViewModel().bind({
            bindTo: '{policyId}'
        }, this.onPolicy, this);
    },

    onPolicy: function () {
        var policy = Ext.getStore('policies').findRecord('policyId', this.getViewModel().get('policyId')),
            installable = policy.get('installable').list, i, node, filters = [], services = [];

        for (i = 0; i < installable.length; i += 1) {
            node = installable[i];
            if (node.type === 'FILTER') {
                filters.push({
                    xtype: 'ung.appinstallitem',
                    node: node
                });
            }
            if (node.type === 'SERVICE') {
                services.push({
                    xtype: 'ung.appinstallitem',
                    node: node
                });
            }
        }

        this.getView().lookupReference('filters').removeAll(true);
        this.getView().lookupReference('filters').add(filters);
        this.getView().lookupReference('services').removeAll(true);
        this.getView().lookupReference('services').add(services);
    },

    setPolicy: function (combo, newValue, oldValue) {
        if (oldValue !== null) {
            this.redirectTo('#apps/' + newValue + '/install', true);
        }
    },

    installNode: function (nodeItem) {
        var vm = this.getViewModel();
        var policyId = this.getViewModel().get('policyId'),
            nodeName = nodeItem.node.name;

        if (!nodeItem.hasCls('installed')) {
            nodeItem.setDisabled(true);
            nodeItem.addCls('progress');

            rpc.nodeManager.instantiate(function (result, ex) {
                if (ex) { Ung.Util.exceptionToast(ex); return; }

                try {
                    nodeItem.removeCls('progress');
                    nodeItem.addCls('installed');
                    nodeItem.setDisabled(false);
                    //nodeItem.removeListener('click', this.installnoi);
                    //nodeItem.setHref('#apps/' + policyId + '/' + nodeName);
                } catch (exception) {
                    console.log(exception);
                }

                Ung.Util.successToast(nodeItem.node.displayName + ' installed successfully!');

                // update policies
                rpc.nodeManager.getAppsViews(function (result, ex) {
                    if (ex) { Ung.Util.exceptionToast(ex); return; }
                    Ext.getStore('policies').loadData(result);
                });

                // update unavailable apps if reports are enabled

                //console.log(vm.getParent());

                if (nodeItem.node.name === 'untangle-node-reports') {
                    rpc.reportsManager = rpc.nodeManager.node('untangle-node-reports').getReportsManager();
                    Rpc.getReports().then(function (reports) {
                        Ext.getStore('reports').loadData(reports.list);
                        vm.getParent().set({
                            reportsInstalled: true,
                            reportsRunning: rpc.nodeManager.node('untangle-node-reports').getRunState() === 'RUNNING'
                        });
                        vm.getParent().notify();
                        Ext.GlobalEvents.fireEvent('reportsinstall');
                    });
                }

                if (rpc.reportsManager) {
                    rpc.reportsManager.getUnavailableApplicationsMap(function (result, ex) {
                        if (ex) { Ung.Util.exceptionToast(ex); return; }
                        Ext.getStore('unavailableApps').loadRawData(result.map);

                        // fire nodeinstall event to update widgets on dashboard
                        Ext.GlobalEvents.fireEvent('nodeinstall', 'install', nodeItem.node);
                    });
                }

            }, nodeName, policyId);
        } else {
            Ung.app.redirectTo('#apps/' + policyId + '/' + nodeName);
        }
    }

});
