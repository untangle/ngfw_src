Ext.define('Ung.view.extra.DevicesController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.devices',

    control: {
        '#': {
            deactivate: 'onDeactivate',
            refresh: 'getDevicesSettings'
        },
        '#devicesgrid': {
            afterrender: 'getDevicesSettings'
        }
    },

    onDeactivate: function (view) {
        view.destroy();
    },

    getDevicesSettings: function () {
        var me = this,
            v = me.getView(),
            vm = me.getViewModel(),
            grid = me.getView().down('#devicesgrid'),
            filters = grid.getStore().getFilters(),
            store = Ext.getStore('devices');

        var existingRouteFilter = filters.findBy( function( filter ){
            if(filter.config.source == "route"){
                return true;
            }
        } );
        if( existingRouteFilter != null ){
            filters.remove(existingRouteFilter);
        }
        if( v.routeFilter ){
            filters.add(v.routeFilter);
        }

        if( !store.getFields() ){
            store.setFields(grid.fields);
        }

        me.getView().setLoading(true);
        Rpc.asyncData('rpc.deviceTable.getDevicesSettings')
            .then(function(result) {
                me.getView().setLoading(false);
                vm.set('settings', result);
                store.loadData(result.devices.list);
                if(store.getSorters().items.length == 0){
                    store.sort('macAddress', 'ASC');
                }
            });
    },

    resetView: function( btn ){
        var grid = this.getView().down('#devicesgrid'),
            store = grid.getStore();

        Ext.state.Manager.clear(grid.stateId);
        store.getSorters().removeAll();
        store.sort('macAddress', 'ASC');
        store.clearFilter();
        grid.reconfigure(null, grid.initialConfig.columns);
    },

    timestampColumns : [ "lastSessionTime" ],

    setDevicesSettings: function () {
        var me = this, list = [], v = me.getView(), vm = me.getViewModel();

        if (!Util.validateFields(v)) {
            return;
        }

        me.getView().query('ungrid').forEach(function (grid) {
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
                });
                store.isReordered = undefined;
            }
            list = Ext.Array.pluck(store.getRange(), 'data');

            list.forEach(function(record){
                me.timestampColumns.forEach(function(fieldName){
                    if(record[fieldName] && typeof(record[fieldName]) == "object"){
                        record[fieldName] = record[fieldName].getTime();
                    }
                });
            });

            filters.each( function(filter){
                store.addFilter(filter);
            });
        });

        vm.set('settings.devices.list', list);

        me.getView().setLoading(true);
        Rpc.asyncData('rpc.deviceTable.setDevicesSettings', vm.get('settings')).then(function() {
            me.getDevicesSettings();
        }, function (ex) {
            Util.handleException(ex);
        }).always(function () {
            me.getView().setLoading(false);
        });
    }
});
