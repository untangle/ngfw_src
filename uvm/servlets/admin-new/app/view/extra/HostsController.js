Ext.define('Ung.view.extra.HostsController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.hosts',

    control: {
        '#': {
            deactivate: 'onDeactivate',
            refresh: 'getHosts'
        },
        '#hostsgrid': {
            afterrender: 'getHosts'
        }
    },

    onDeactivate: function (view) {
        view.destroy();
    },

    setAutoRefresh: function (btn) {
        var me = this,
            vm = this.getViewModel();
        vm.set('autoRefresh', btn.pressed);

        if (btn.pressed) {
            me.getHosts();
            this.refreshInterval = setInterval(function () {
                me.getHosts();
            }, 5000);
        } else {
            clearInterval(this.refreshInterval);
        }

    },

    resetView: function( btn ){
        var grid = this.getView().down('#hostsgrid'),
            store = grid.getStore();

        Ext.state.Manager.clear(grid.stateId);
        store.getSorters().removeAll();
        store.sort('address', 'ASC');
        store.clearFilter();
        grid.reconfigure(null, grid.initialConfig.columns);
    },

    getHosts: function () {
        var me = this,
            v = me.getView(),
            grid = me.getView().down('#hostsgrid'),
            filters = grid.getStore().getFilters(),
            store = grid.getStore('hosts');

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

        grid.getView().setLoading(true);
        Rpc.asyncData('rpc.hostTable.getHosts')
            .then(function(result) {
                grid.getView().setLoading(false);
                store.loadData(result.list);

                if(store.getSorters().items.length == 0){
                    store.sort('address', 'ASC');
                }

                grid.getSelectionModel().select(0);
            });
    },

    refillQuota: function (view, rowIndex, colIndex, item, e, record) {
        var me = this;
        Ext.MessageBox.wait('Refilling...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.hostTable.refillQuota', record.get('address'))
            .then(function () {
                me.getHosts();
            }, function (ex) {
                Util.handleException(ex);
            }).always(function () {
                Ext.MessageBox.hide();
            });
    },

    dropQuota: function (view, rowIndex, colIndex, item, e, record) {
        var me = this;
        Ext.MessageBox.wait('Removing Quota...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.hostTable.removeQuota', record.get('address'))
            .then(function () {
                me.getHosts();
            }, function (ex) {
                Util.handleException(ex);
            }).always(function () {
                Ext.MessageBox.hide();
            });
    },

    saveHosts: function () {
        var me = this, list = [];

        me.getView().query('ungrid').forEach(function (grid) {
            var store = grid.getStore();

            var filters = store.getFilters().clone();
            store.clearFilter(true);

            if (store.getModifiedRecords().length > 0 ||
                store.getNewRecords().length > 0 ||
                store.getRemovedRecords().length > 0 ||
                store.isReordered) {
                store.isReordered = undefined;
            }
            list = Ext.Array.pluck(store.getRange(), 'data');

            filters.each( function(filter){
                store.addFilter(filter);
            });
        });

        me.getView().setLoading(true);
        Rpc.asyncData('rpc.hostTable.setHosts', {
            javaClass: 'java.util.LinkedList',
            list: list
        }, true).then(function() {
            me.getHosts();
        }, function (ex) {
            Util.handleException(ex);
        }).always(function () {
            me.getView().setLoading(false);
        });
    }

});
