Ext.define('Ung.apps.ipsecvpn.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-ipsec-vpn',

    control: {
        '#status': {
            beforerender: 'onStatusBeforeRender'
        }
    },

    onStatusBeforeRender: function () {
        var me = this,
            vm = this.getViewModel();
        vm.bind('{instance.targetState}', function (state) {
            if (state === 'RUNNING') {
                me.getVirtualUsers();
                me.getTunnelStatus();
            }
        });
    },

    getVirtualUsers: function () {
        var grid = this.getView().down('#virtualUsers'),
            vm = this.getViewModel();
        grid.setLoading(true);
        this.getView().appManager.getVirtualUsers(function (result, ex) {
            grid.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('virtualUsers', result.list);
        });
    },

    getTunnelStatus: function () {
        var grid = this.getView().down('#tunnelStatus'),
            vm = this.getViewModel();
        grid.setLoading(true);
        this.getView().appManager.getTunnelStatus(function (result, ex) {
            grid.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('tunnelStatus', result.list);
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
