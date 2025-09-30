Ext.define('Ung.apps.dynamic-blocklists.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-dynamic-blocklists',
    control: {
        '#': {
            afterrender: 'getSettings'
        }
    },
    getSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();
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
        var me = this, v = this.getView(), vm = this.getViewModel(), settingsChanged = false;
        if (!Util.validateForms(v)) {
            return;
        }
        if (me.validateSettings() != true) return;
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
        Rpc.asyncData(v.appManager, 'setSettings', vm.get('settings'), settingsChanged)
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
    validateSettings: function() {
        return(true);
    }
});