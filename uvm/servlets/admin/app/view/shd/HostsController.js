Ext.define('Ung.view.shd.HostsController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.hosts',

    control: {
        '#': {
            afterrender: 'getHosts',
            deactivate: 'onDeactivate'
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

    getHosts: function () {
        var me = this,
            grid = me.getView().down('#hostsgrid');
        grid.getView().setLoading(true);
        Rpc.asyncData('rpc.hostTable.getHosts')
            .then(function(result) {
                grid.getView().setLoading(false);
                Ext.getStore('hosts').loadData(result.list);
                grid.getSelectionModel().select(0);
                // grid.getStore().setData(result.list);
            });
    },

    timestampRenderer: function (timestamp) {
        if (!timestamp) {
            return '<i class="fa fa-minus"></i>';
        }
        return Ext.util.Format.date(new Date(timestamp), 'timestamp_fmt'.t());
    },

    boolRenderer: function (value) {
        return '<i class="fa ' + (value ? 'fa-check' : 'fa-minus') + '"></i>';
    }

});
