Ext.define('Ung.apps.openvpn.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-openvpn',

    control: {
        '#': {
            afterrender: 'getSettings'
        },
        '#status': {
            beforerender: 'onStatusBeforeRender'
        }
    },

    onStatusBeforeRender: function () {
        var me = this,
            vm = this.getViewModel();
        vm.bind('{instance.targetState}', function (state) {
            if (state === 'RUNNING') {
                me.getActiveClients();
                me.getRemoteServersStatus();
            }
        });
    },

    getActiveClients: function () {
        var grid = this.getView().down('#activeClients'),
            vm = this.getViewModel();
        grid.setLoading(true);
        this.getView().appManager.getActiveClients(function (result, ex) {
            grid.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('activeClients', result.list);
        });
    },

    getRemoteServersStatus: function () {
        var grid = this.getView().down('#remoteServers'),
            vm = this.getViewModel();
        grid.setLoading(true);
        this.getView().appManager.getRemoteServersStatus(function (result, ex) {
            grid.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('remoteServers', result.list);
            console.log(result);
        });
    },

    getSettings: function () {
        var v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);
        v.appManager.getSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('settings', result);
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
            if (ex) { Util.exceptionToast(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
        }, vm.get('settings'));
    }

});
