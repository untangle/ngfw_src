Ext.define('Ung.view.extra.UsersController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.users',

    control: {
        '#': {
            deactivate: 'onDeactivate'
        },
        '#usersgrid': {
            afterrender: 'getUsers',
        }
    },

    refreshInterval: null,

    onDeactivate: function (view) {
        view.destroy();
    },

    resetView: function( btn ){
        var grid = this.getView().down('#usersgrid');
        Ext.state.Manager.clear(grid.stateId);
        grid.reconfigure(null, grid.initialConfig.columns);
    },

    getUsers: function () {
        var me = this,
            vm = this.getViewModel(),
            grid = me.getView().down('#usersgrid');

        grid.getView().setLoading(true);
        Rpc.asyncData('rpc.userTable.getUsers')
            .then(function(result) {
                grid.getView().setLoading(false);
                vm.set('usersData', result.list);
            });
    },

    saveUsers: function () {
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
        Rpc.asyncData('rpc.userTable.setUsers', {
            javaClass: 'java.util.LinkedList',
            list: list
        }).then(function(result, ex) {
             me.getUsers();
        }, function (ex) {
            Util.handleException(ex);
        }).always(function () {
            me.getView().setLoading(false);
        });
   }
});
