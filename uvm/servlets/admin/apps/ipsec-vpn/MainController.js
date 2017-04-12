Ext.define('Ung.apps.ipsecvpn.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-ipsec-vpn',

    control: {
        '#': {
            afterrender: 'getSettings'
        },
        '#status': {
            beforerender: 'onStatusBeforeRender'
        },
        '#ipsectunnels': {
            beforerender: 'calculateNetworks'
        }
    },

    onStatusBeforeRender: function() {
        var me = this,
            vm = this.getViewModel();
        vm.bind('{instance.targetState}', function(state) {
            if (state === 'RUNNING') {
                me.getVirtualUsers();
                me.getTunnelStatus();
            }
        });
    },

    getVirtualUsers: function() {
        var grid = this.getView().down('#virtualUsers'),
            vm = this.getViewModel();
        grid.setLoading(true);
        this.getView().appManager.getVirtualUsers(function(result, ex) {
            grid.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('virtualUserData', result.list);
        });
    },

    getTunnelStatus: function() {
        var grid = this.getView().down('#tunnelStatus'),
            vm = this.getViewModel();
        grid.setLoading(true);
        this.getView().appManager.getTunnelStatus(function(result, ex) {
            grid.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('tunnelStatusData', result.list);
        });
    },

    getSettings: function() {
        var v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);
        v.appManager.getSettings(function(result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('settings', result);
        });
    },

    setSettings: function() {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.query('ungrid').forEach(function(grid) {
            var store = grid.getStore();
            if (store.getModifiedRecords().length > 0 ||
                store.getNewRecords().length > 0 ||
                store.getRemovedRecords().length > 0 ||
                store.isReordered) {
                store.each(function(record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
            }
        });

        v.setLoading(true);
        v.appManager.setSettings(function(result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
        }, vm.get('settings'));
    },

    calculateNetworks: function() {
        var vm = this.getViewModel();
        var leftDefault = '0.0.0.0';
        var leftSubnetDefault = '0.0.0.0/0';
        var wanListData = [];
        var x,y;

        // we need the interface list and the status list so we can get the IP address of active interfaces
        var netSettings = rpc.networkManager.getNetworkSettings();
        var intStatus = rpc.networkManager.getInterfaceStatus();

        wanListData.push(['', '- ' + 'Custom'.t() + ' -']);

        // build the list of active WAN networks for the interface combo box and set the defaults for left and leftSubnet
        for( x = 0 ; x <  netSettings.interfaces.list.length ; x++ )
        {
            var device = netSettings.interfaces.list[x];

            if (! device.interfaceId) { continue; }
            if (device.disabled) { continue; }

            for( y = 0 ; y < intStatus.list.length ; y++ )
            {
                var status = intStatus.list[y];

                if (! status.v4Address) { continue; }
                if (! status.interfaceId) { continue; }
                if (device.interfaceId !== status.interfaceId) { continue; }

                // found a WAN device
                if (device.isWan)
                {
                    // add the address and name to the WAN list
                    wanListData.push([ status.v4Address, device.name]);

                    // save the first WAN address to use as the default for new tunnels
                    if (leftDefault === '0.0.0.0') { leftDefault = status.v4Address; }
                }

                // found a LAN devices
                else
                {
                    // save the first LAN address to use as the default for new tunnels
                    if (leftSubnetDefault === '0.0.0.0/0') { leftSubnetDefault = (status.v4Address + '/' + status.v4PrefixLength); }
                }
            }
        }

        vm.set('leftDefault', leftDefault);
        vm.set('leftSubnetDefault', leftSubnetDefault);
        vm.set('wanListData', wanListData);
    },

    configureAuthTarget: function(btn)
    {
        var vm = this.getViewModel(),
            policyId = vm.get('policyId');

        switch (this.getViewModel().get('settings.authenticationType')) {
        case 'LOCAL_DIRECTORY': Ung.app.redirectTo('#config/localdirectory'); break;
        case 'RADIUS': Ung.app.redirectTo('#apps/' + policyId + '/directory-connector/radius'); break;
        default: return;
        }
    },

    refreshTextArea: function(cmp)
    {
        var ipsecApp = rpc.appManager.app('ipsec-vpn');
        var target;

        switch(cmp.target) {
            case "ipsecStateInfo":
                target = this.getView().down('#ipsecStateInfo');
                target.setValue(ipsecApp.getStateInfo());
                break;
            case "ipsecPolicyInfo":
                target = this.getView().down('#ipsecPolicyInfo');
                target.setValue(ipsecApp.getPolicyInfo());
                break;
            case "ipsecSystemLog":
                target = this.getView().down('#ipsecSystemLog');
                target.setValue(ipsecApp.getLogFile());
                break;
            case "ipsecVirtualLog":
                target = this.getView().down('#ipsecVirtualLog');
                target.setValue(ipsecApp.getVirtualLogFile());
                break;
        }
    },

    disconnectUser: function(view, row, colIndex, item, e, record) {
        var me = this, v = this.getView(), vm = this.getViewModel();
        var ipsecApp = rpc.appManager.app('ipsec-vpn');

        v.setLoading('Disconnecting...'.t());
        ipsecApp.virtualUserDisconnect(Ext.bind(function(result, exception) {
            // this gives the app a couple seconds to process the disconnect before we refresh the list
            var timer = setTimeout(function() {
                me.getVirtualUsers();
                v.setLoading(false);
            },2000);
        }, this), record.get("clientAddress"), record.get("clientUsername"));
    }
});
