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

    setAutoRefresh: function (btn) {
        var me = this,
            vm = this.getViewModel();
        vm.set('autoRefresh', btn.pressed);

        if (btn.pressed) {
            me.getUsers();
            this.refreshInterval = setInterval(function () {
                me.getUsers();
            }, 5000);
        } else {
            clearInterval(this.refreshInterval);
        }

    },

    resetView: function( btn ){
        var grid = this.getView().down('#usersgrid');
        Ext.state.Manager.clear(grid.stateId);
        grid.reconfigure(null, grid.initialConfig.columns);
    },

    getUsers: function () {
        var me = this, vm = this.getViewModel(),
            grid = me.getView().down('#usersgrid');
        grid.getView().setLoading(true);
        Rpc.asyncData('rpc.userTable.getUsers')
            .then(function(result) {
                grid.getView().setLoading(false);
                vm.set('usersData', result.list);
            });
    }
});
