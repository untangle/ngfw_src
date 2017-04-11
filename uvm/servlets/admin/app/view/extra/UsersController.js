Ext.define('Ung.view.extra.UsersController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.users',

    control: {
        '#': {
            afterrender: 'getUsers',
            deactivate: 'onDeactivate'
        },
        // '#list': {
        //     select: 'onSelect'
        // },
        // 'toolbar textfield': {
        //     change: 'globalFilter'
        // }
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

    getUsers: function () {
        var me = this, vm = this.getViewModel(),
            grid = me.getView().down('#list');
        grid.getView().setLoading(true);
        Rpc.asyncData('rpc.userTable.getUsers')
            .then(function(result) {
                grid.getView().setLoading(false);
                vm.set('usersData', result.list);
            });
    }
});
