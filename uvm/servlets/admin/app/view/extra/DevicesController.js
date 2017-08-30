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
                store.sort('macAddress', 'DSC');

                v.down('ungridstatus').fireEvent('update');
            });
    },

    resetView: function( btn ){
        var grid = this.getView().down('#devicesgrid');
        Ext.state.Manager.clear(grid.stateId);

        var cols = grid.initialConfig.columns;
        /**
         * add Edit/Delete columns needed for reconfigure, as they are missing from initialConfig
         */
        if (!Ext.Array.findBy(cols, function(col) {
            return col.handler === 'editRecord';
        })) {
            cols.push({
                xtype: 'actioncolumn',
                width: 60,
                header: 'Edit'.t(),
                align: 'center',
                resizable: false,
                tdCls: 'action-cell',
                iconCls: 'fa fa-pencil',
                handler: 'editRecord',
                menuDisabled: true,
                hideable: false
            });
        }

        if (!Ext.Array.findBy(cols, function(col) {
            return col.handler === 'deleteRecord';
        })) {
            cols.push({
                xtype: 'actioncolumn',
                width: 60,
                header: 'Delete'.t(),
                align: 'center',
                resizable: false,
                tdCls: 'action-cell',
                iconCls: 'fa fa-trash-o fa-red',
                handler: 'deleteRecord',
                menuDisabled: true,
                hideable: false
            });
        }

        grid.reconfigure(null, cols);
    },


    saveDevices: function () {
        var me = this,
            store = me.getView().down('ungrid').getStore(),
            list = [];

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
                list = Ext.Array.pluck(store.getRange(), 'data');
            }

            filters.each( function(filter){
                store.addFilter(filter);
            });
        });

        me.getView().setLoading(true);
        Rpc.asyncData('rpc.deviceTable.setDevices', {
            javaClass: 'java.util.LinkedList',
            list: list
        }).then(function(result, ex) {
             me.getDevices();
        }, function (ex) {
            Util.handleException(ex);
        }).always(function () {
            me.getView().setLoading(false);
        });
   }
});
