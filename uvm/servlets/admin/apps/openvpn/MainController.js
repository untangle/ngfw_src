Ext.define('Ung.apps.openvpn.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-openvpn',

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
                me.getActiveClients();
                me.getRemoteServersStatus();
            }
        });
    },

    getActiveClients: function () {
        var grid = this.getView().down('#activeClients'),
            vm = this.getViewModel();
        grid.setLoading(true);
        this.getView().appManager.getActiveClients(function (result, ex) {
            grid.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('activeClients', result.list);
        });
    },

    getRemoteServersStatus: function () {
        var grid = this.getView().down('#remoteServers'),
            vm = this.getViewModel();
        grid.setLoading(true);
        this.getView().appManager.getRemoteServersStatus(function (result, ex) {
            grid.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('remoteServers', result.list);
            console.log(result);
        });
    }

});
