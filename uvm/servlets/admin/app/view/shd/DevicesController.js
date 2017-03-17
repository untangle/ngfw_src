Ext.define('Ung.view.shd.DevicesController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.devices',

    control: {
        '#': {
            afterrender: 'getDevices',
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
            me.getDevices();
            this.refreshInterval = setInterval(function () {
                me.getDevices();
            }, 5000);
        } else {
            clearInterval(this.refreshInterval);
        }

    },

    getDevices: function () {
        var me = this,
            grid = me.getView().down('#devicesgrid');
        grid.getView().setLoading(true);
        Rpc.asyncData('rpc.deviceTable.getDevices')
            .then(function(result) {
                grid.getView().setLoading(false);
                Ext.getStore('devices').loadData(result.list);
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
