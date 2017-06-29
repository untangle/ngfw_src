Ext.define('Ung.apps.webcache.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-web-cache',

    control: {
        '#': {
            afterrender: 'getSettings',
        },
        '#status': {
            afterrender: 'statusAfterRender'
        }
    },

    getSettings: function () {
        var v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);
        v.appManager.getSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            console.log(result);
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
            if (ex) { Util.handleException(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
        }, vm.get('settings'));
    },

    statusAfterRender: function () {
        var me = this;
        me.getView().appManager.getStatistics(function (result, ex) {
            if (ex) { Util.handleException(ex); return; }
            me.getViewModel().set('statistics', result);
        });
    },

    clearCache: function (btn) {
        var me = this;
        Ext.MessageBox.wait('Clearing Cache...'.t(), 'Please wait'.t());
        me.getView().appManager.clearSquidCache(function (result, ex) {
            Ext.MessageBox.hide();
            if (ex) { Util.handleException(ex); return; }
            me.lookupReference('clearCacheConsent').setValue(false);
        });
    }
});
