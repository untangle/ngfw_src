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

    statusAfterRender: function () {
        var me = this, v = me.getView(), vm = me.getViewModel();

        Rpc.asyncData(v.appManager, 'getStatistics' )
        .then(function(result){
            if(Util.isDestroyed(vm)){
                return;
            }
            me.getViewModel().set('statistics', result);
        }, function(ex) {
            Util.handleException(ex);
        });
    },

    clearCache: function (btn) {
        var me = this, v = me.getView();
        Ext.MessageBox.wait('Clearing Cache...'.t(), 'Please wait'.t());

        Rpc.asyncData(v.appManager, 'clearSquidCache' )
        .then(function(result){
            Ext.MessageBox.hide();
            if(Util.isDestroyed(me)){
                return;
            }
            me.lookupReference('clearCacheConsent').setValue(false);
        }, function(ex) {
            Ext.MessageBox.hide();
            Util.handleException(ex);
        });
    }
});
