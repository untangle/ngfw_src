Ext.define('Ung.apps.applicationcontrol.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-application-control',

    control: {
        '#': {
            afterrender: 'onAfterRender'
        }
    },

    onAfterRender: function () {
        var me = this, v = me.getView(), vm = me.getViewModel();

        v.setLoading(true);
        Rpc.asyncData(v.appManager, 'getStatistics')
        .then( function(result){
            if(Util.isDestroyed(v)){
                return;
            }

            vm.set('statistics', result);
            v.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(v)){
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
        me.getSettings();
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

            var filters = store.getFilters().clone();
            store.clearFilter(true);

            if (store.getModifiedRecords().length > 0 ||
                store.getNewRecords().length > 0 ||
                store.getRemovedRecords().length > 0 ||
                store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                }, this, true);
                store.isReordered = undefined;
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
            }

            filters.each( function(filter){
                store.addFilter(filter);
            });
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

    statics: {
        actionRenderer: function(action){
            switch(action.actionType) {
                case 'ALLOW': return 'Allow'.t();
                case 'BLOCK': return 'Block'.t();
                case 'TARPIT': return 'Tarpit'.t();
                default: return 'Unknown Action'.t() + ': ' + act;
            }
        }
    }

});
