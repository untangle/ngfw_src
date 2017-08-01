Ext.define('Ung.apps.tunnel-vpn.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-tunnel-vpn',

    control: {
        '#': {
            afterrender: 'getSettings'
        }
    },

    getSettings: function () {
        var v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);
        v.appManager.getSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            console.log(result);
            vm.set('settings', result);

            var destinationTunnelData = [];
            destinationTunnelData.push([-1, 'Any Available Tunnel'.t()]);
            destinationTunnelData.push([0, 'Route Normally'.t()]);
            if ( result.tunnels && result.tunnels.list ) {
                for (var i = 0 ; i < result.tunnels.list.length ; i++) {
                    var tunnel = result.tunnels.list[i];
                    destinationTunnelData.push([tunnel.tunnelId, tunnel.name]);
                }
            }
            vm.set('destinationTunnelData', destinationTunnelData);
        });
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (me.validateSettings() != true) return;

        v.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
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
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
            }
        });

        v.setLoading(true);
        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
            Ext.fireEvent('resetfields', v);
        }, vm.get('settings'));
    },

    validateSettings: function() {
        return(true);
    },

    runWizard: function (btn) {
        var me = this;
        me.wizard = me.getView().add({
            xtype: 'app-tunnel-vpn-wizard',
            appManager: me.getView().appManager,
            listeners: {
                // when wizard is finished, reload settings and try to start the app
                finish: function () {
                    me.getSettings(function (configured) {
                        if (configured && me.getView().appManager.getRunState() !== 'RUNNING') {
                            me.getView().down('appstate > button').click();
                        }
                    });
                }
            }
        });
        me.wizard.show();
    },

    refreshTextArea: function(cmp)
    {
        var tunnelVpnApp = rpc.appManager.app('tunnel-vpn');
        var target;

        switch(cmp.target) {
            case "tunnelLog":
                target = this.getView().down('#tunnelLog');
                target.setValue(tunnelVpnApp.getLogFile());
                break;
        }
    },
    
});

