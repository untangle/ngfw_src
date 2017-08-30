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
   },

    refillQuota: function (view, rowIndex, colIndex, item, e, record) {
        var me = this;
        Ext.MessageBox.wait('Refilling...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.userTable.refillQuota', record.get('username'))
            .then(function () {
                me.getUsers();
            }, function (ex) {
                Util.handleException(ex);
            }).always(function () {
                Ext.MessageBox.hide();
            });
    },

    dropQuota: function (view, rowIndex, colIndex, item, e, record) {
        var me = this;
        Ext.MessageBox.wait('Removing Quota...'.t(), 'Please wait'.t());
        Rpc.asyncData('rpc.userTable.removeQuota', record.get('username'))
            .then(function () {
                me.getUsers();
            }, function (ex) {
                Util.handleException(ex);
            }).always(function () {
                Ext.MessageBox.hide();
            });
    }
});
