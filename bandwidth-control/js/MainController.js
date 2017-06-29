Ext.define('Ung.apps.bandwidthcontrol.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-bandwidth-control',

    control: {
        '#': {
            afterrender: 'afterRender'
        }
    },

    afterRender: function () {
        this.getSettings();
    },

    // use a callback function needed for config wizard
    getSettings: function (cb) {
        var v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);
        v.appManager.getSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            vm.set({
                settings: result,
                isConfigured: result.configured,
                qosEnabled: rpc.networkManager.getNetworkSettings().qosSettings.qosEnabled
            });
            if (cb) { cb(result.configured); }
        });
    },

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
        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
        }, vm.get('settings'));
    },

    runWizard: function (btn) {
        var me = this;
        me.wizard = me.getView().add({
            xtype: 'app-bandwidth-control-wizard',
            appManager: me.getView().appManager,
            listeners: {
                // when wizard is finished, reload settings and try to start the app
                finish: function () {
                    me.getSettings(function (configured) {
                        if (configured && me.getView().appManager.getRunState() !== 'RUNNING') {
                            me.getView().down('appstate > button').click();
                        }
                    });
                }
            }
        });
        me.wizard.show();
    }
});
