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
        '#ipsec-state': { afterrender: 'refreshIpsecStateInfo' },
        '#ipsec-policy': { afterrender: 'refreshIpsecPolicyInfo' },
        '#ipsec-log': { afterrender: 'refreshIpsecSystemLog' },
        '#l2tp-log': { afterrender: 'refreshIpsecVirtualLog' },
    },

    onStatusBeforeRender: function() {
        var me = this,
            vm = this.getViewModel();
        vm.bind('{state}', function(state) {
            if (state.get('on') ){
                me.getVirtualUsers();
                me.getTunnelStatus();
            }
        });
    },

    getVirtualUsers: function() {
        var grid = this.getView().down('#virtualUsers'),
            vm = this.getViewModel();

        grid.setLoading(true);
        Rpc.asyncData(this.getView().appManager, 'getVirtualUsers')
        .then( function(result){
            if(Util.isDestroyed(grid, vm)){
                return;
            }

            vm.set('virtualUserData', result.list);

            grid.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(grid)){
                grid.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    getTunnelStatus: function() {
        var grid = this.getView().down('#tunnelStatus'),
            vm = this.getViewModel();

        grid.setLoading(true);
        Rpc.asyncData(this.getView().appManager, 'getTunnelStatus')
        .then( function(result){
            if(Util.isDestroyed(grid, vm)){
                return;
            }

            vm.set('tunnelStatusData', result.list);

            grid.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(grid)){
                grid.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    getSettings: function() {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise(v.appManager, 'getSettings'),
            Rpc.asyncPromise('rpc.networkManager.getNetworkSettings'),
            Rpc.asyncPromise('rpc.networkManager.getInterfaceStatus')
        ]).then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            var wanListData = me.calculateNetworks( result[1], result[2] );
            vm.set('wanListData', wanListData);

            var settings = result[0];
            var x,y;
            for( x = 0 ; x < settings.tunnels.list.length; x++ ) {
                settings.tunnels.list[x].localInterface = 0;
                for( y = 0 ; y < wanListData.length ; y++) {
                    if (settings.tunnels.list[x].left == wanListData[y][1]) {
                        settings.tunnels.list[x].localInterface = wanListData[y][0];
                    }
                }
            }

            for( x = 0 ; x < settings.networks.list.length; x++ ) {
                settings.networks.list[x].localInterface = 0;
                for( y = 0 ; y < wanListData.length ; y++) {
                    if (settings.networks.list[x].localAddress == wanListData[y][1]) {
                        settings.networks.list[x].localInterface = wanListData[y][0];
                    }
                }
            }

            vm.set('settings', settings);

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    setSettings: function() {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (!Util.validateForms(v)) {
            return;
        }

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
        Rpc.asyncData(v.appManager, 'setSettings', vm.get('settings'))
        .then(function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            Util.successToast('Settings saved');
            vm.set('panel.saveDisabled', false);
            v.setLoading(false);

            me.getSettings();
            Ext.fireEvent('resetfields', v);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    calculateNetworks: function(netSettings, intStatus) {
        var leftDefault = '0.0.0.0';
        var leftSubnetDefault = '0.0.0.0/0';
        var wanListData = [];
        var x,y;

        // we need the interface list and the status list so we can get the IP address of active interfaces
        var counter = 0;

        wanListData.push([ counter++ , '' , '- ' + 'Custom'.t() + ' -' ]);

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
                    wanListData.push([ counter++ , status.v4Address , device.name ]);

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

        Ung.util.Util.setAppStorageValue('ipsec.leftDefault', leftDefault);
        Ung.util.Util.setAppStorageValue('ipsec.leftSubnetDefault', leftSubnetDefault);

        return(wanListData);
    },

    configureAuthTarget: function(btn)
    {
        var vm = this.getViewModel(),
            policyId = vm.get('policyId'),
            authType = vm.get('settings.authenticationType');

        switch (authType) {
            case 'LOCAL_DIRECTORY': Ung.app.redirectTo('#config/local-directory'); break;
            case 'RADIUS_SERVER': Ung.app.redirectTo('#apps/' + policyId + '/directory-connector/radius'); break;
            default: return;
        }
    },

    refreshIpsecStateInfo: function (cmp) {
        var v = cmp.isXType('button') ? cmp.up('panel') : cmp;
        var target = v.down('textarea');

        target.setValue('');

        v.setLoading(true);
        Rpc.asyncData( v.up('app-ipsec-vpn').appManager, 'getStateInfo')
        .then(function(result){
            if(Util.isDestroyed(v, target)){
                return;
            }
            target.setValue(result);
            v.setLoading(false);
        });
    },

    refreshIpsecPolicyInfo: function (cmp) {
        var v = cmp.isXType('button') ? cmp.up('panel') : cmp;
        var target = v.down('textarea');

        target.setValue('');

        v.setLoading(true);
        Rpc.asyncData( v.up('app-ipsec-vpn').appManager, 'getPolicyInfo')
        .then(function(result){
            if(Util.isDestroyed(v, target)){
                return;
            }
            target.setValue(result);
            v.setLoading(false);
        });
    },

    refreshIpsecSystemLog: function (cmp) {
        var v = cmp.isXType('button') ? cmp.up('panel') : cmp;
        var target = v.down('textarea');

        target.setValue('');

        v.setLoading(true);
        Rpc.asyncData( v.up('app-ipsec-vpn').appManager, 'getLogFile')
        .then(function(result){
            if(Util.isDestroyed(v, target)){
                return;
            }
            target.setValue(result);
            v.setLoading(false);
        });
    },

    refreshIpsecVirtualLog: function (cmp) {
        var v = cmp.isXType('button') ? cmp.up('panel') : cmp;
        var target = v.down('textarea');

        target.setValue('');

        v.setLoading(true);
        Rpc.asyncData( v.up('app-ipsec-vpn').appManager, 'getVirtualLogFile')
        .then(function(result){
            if(Util.isDestroyed(v, target)){
                return;
            }
            target.setValue(result);
            v.setLoading(false);
        });
    },

    disconnectUser: function(view, row, colIndex, item, e, record) {
        var me = this, v = this.getView(), vm = this.getViewModel();

        var target = v.down('#virtualUsers');
        target.setLoading('Disconnecting...'.t());
        Rpc.asyncData( v.appManager, 'virtualUserDisconnect', record.get("clientAddress"), record.get("clientUsername"))
        .then( function(result){
            if(Util.isDestroyed(target, me)){
                return;
            }

            // this gives the app a couple seconds to process the disconnect before we refresh the list
            setTimeout(function() {
                if(Util.isDestroyed(target, me)){
                    return;
                }
                me.getVirtualUsers();
                target.setLoading(false);
            },2000);
        },function(ex){
            if(!Util.isDestroyed(v, vm)){
                target.setLoading(false);
            }
            Util.handleException(ex);
        });

    },

    statics:{
        modeRenderer: function(value){
            var showtxt = 'Inactive'.t(),
                showico = 'fa fa-circle fa-gray';
            if (value.toLowerCase() === 'active') {
                showtxt = 'Active'.t();
                showico = 'fa fa-circle fa-green';
            }
            if (value.toLowerCase() === 'unknown') {
                showtxt = 'Unknown'.t();
                showico = 'fa fa-exclamation-triangle fa-orange';
            }
            return '<i class="' + showico + '">&nbsp;&nbsp;</i>' + showtxt;
        }
    }
});
