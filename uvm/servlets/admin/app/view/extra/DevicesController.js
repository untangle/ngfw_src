Ext.define('Ung.view.extra.DevicesController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.devices',

    control: {
        '#': {
            deactivate: 'onDeactivate',
            refresh: 'getDevices'
        },
        '#devicesgrid': {
            afterrender: 'getDevices'
        }
    },

    onDeactivate: function (view) {
        view.destroy();
    },

    getDevices: function () {
        var me = this,
            v = me.getView(),
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
        Rpc.asyncData('rpc.deviceTable.getDevices')
            .then(function(result) {
                me.getView().setLoading(false);
                store.loadData(result.list);
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

    saveDevices: function () {
        var me = this, list = [];

        me.getView().query('ungrid').forEach(function (grid) {
            var store = grid.getStore();

            var filters = store.getFilters().clone();
            store.clearFilter();

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

            filters.each( function(filter){
                store.addFilter(filter);
            });
        });

        me.getView().setLoading(true);
        Rpc.asyncData('rpc.deviceTable.setDevices', {
            javaClass: 'java.util.LinkedList',
            list: list
        }).then(function() {
            me.getDevices();
        }, function (ex) {
            Util.handleException(ex);
        }).always(function () {
            me.getView().setLoading(false);
        });
    }
});
