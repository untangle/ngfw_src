Ext.define('Ung.apps.adblocker.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-ad-blocker',

    control: {
        '#': {
            afterrender: 'getSettings'
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
            vm.set('lastUpdate', result.lastUpdate || 'Never'.t());

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

    updateFilters: function() {
        var me = this, v = this.getView(), vm = this.getViewModel();
        Ext.MessageBox.wait("Updating filters, this may take a few minutes...".t(), "Please wait".t());

        Rpc.asyncData(v.appManager, 'updateList')
        .then(function(result){
            if(Util.isDestroyed(me)){
                return;
            }
            Ext.MessageBox.hide();
            me.getSettings();
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                Ext.MessageBox.hide();
            }
            Util.handleException(ex);
        });
    },

    statics:{
        actionRenderer: function(value){
            return value ? 'Block'.t() : 'Pass'.t();
        },

        flaggedRenderer: function(value){
            return value ? 'Yes'.t() : '';
        }
    }

});
