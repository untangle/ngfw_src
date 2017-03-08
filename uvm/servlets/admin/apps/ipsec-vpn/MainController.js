Ext.define('Ung.apps.ipsecvpn.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-ipsec-vpn',

    control: {
        '#status': {
            beforerender: 'onStatusBeforeRender'
        }
    },

    onStatusBeforeRender: function () {
        var me = this,
            vm = this.getViewModel();
        vm.bind('{instance.targetState}', function (state) {
            if (state === 'RUNNING') {
                me.getVirtualUsers();
                me.getTunnelStatus();
            }
        });
    },

    getVirtualUsers: function () {
        var grid = this.getView().down('#virtualUsers'),
            vm = this.getViewModel();
        grid.setLoading(true);
        this.getView().appManager.getVirtualUsers(function (result, ex) {
            grid.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('virtualUsers', result.list);
        });
    },

    getTunnelStatus: function () {
        var grid = this.getView().down('#tunnelStatus'),
            vm = this.getViewModel();
        grid.setLoading(true);
        this.getView().appManager.getTunnelStatus(function (result, ex) {
            grid.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('tunnelStatus', result.list);
        });
    }

});
