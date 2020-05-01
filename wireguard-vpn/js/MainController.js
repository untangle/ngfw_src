Ext.define('Ung.apps.wireguard-vpn.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-wireguard-vpn',

    control: {
        '#': {
            afterrender: 'getSettings'
        }
    },

    // General settings that are checked and if different, force
    // a full restart of wireguard.
    fullRestartSettingsKeys: [
        'keepaliveInterval',
        'listenPort',
        'mtu',
        'addressPool'
    ],

    getSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise(v.appManager, 'getSettings'),
            Rpc.asyncPromise('rpc.networkManager.getNetworkSettings')
        ], this).then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            vm.set('originalSettings', JSON.parse(JSON.stringify(result[0])));
            vm.set('settings', result[0]);
            vm.set('panel.saveDisabled', false);

            var warning = '';
            var listenPort = vm.get('settings.listenPort');
            if(me.isUDPAccessAllowedForPort(result[1], listenPort) == false) {
                warning = '<i class="fa fa-exclamation-triangle fa-red fa-lg"></i> <strong>' + 'There are no enabled access rules to allow traffic on UDP port '.t() + listenPort + '</strong>';
            }
            vm.set('warning', warning);

            v.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });

        // trigger active clients/servers fetching when instance run state changes
        vm.bind('{state.on}', function (stateon) {
            if (stateon) {
                me.getTunnelStatus();
            } else {
                vm.set({
                    tunnelStatusData: []
                });
            }
        });
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (!Util.validateForms(v)) {
            return;
        }

        if (me.validateSettings() != true) return;

        var tunnelsAdded = [],
            tunnelsDeleted = [],
            settingsChanged = false;

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
                if(grid.listProperty == 'settings.tunnels.list'){
                    store.getModifiedRecords().forEach(function(record){
                        var previousPublicKey = record.getPrevious('publicKey');
                        if(previousPublicKey == undefined ){
                            previousPublicKey = record.get('publicKey');
                        }
                        if(previousPublicKey != ""){
                            if(tunnelsDeleted.indexOf(previousPublicKey) == -1){
                                tunnelsDeleted.push(previousPublicKey);
                            }
                        }
                        if( record.get("enabled") &&
                            tunnelsAdded.indexOf(record.get('publicKey')) == -1){
                            tunnelsAdded.push(record.get('publicKey'));
                        }
                    });
                    store.getNewRecords().forEach(function(record){
                        if( record.get("enabled") &&
                            tunnelsAdded.indexOf(record.get('publicKey')) == -1){
                            tunnelsAdded.push(record.get('publicKey'));
                        }
                    });
                    store.getRemovedRecords().forEach(function(record){
                        if(tunnelsDeleted.indexOf(record.get('publicKey')) == -1){
                            tunnelsDeleted.push(record.get('publicKey'));
                        }
                    });
                }
            }
        });

        // Determine if settings changed, requiring a full restart.
        me.fullRestartSettingsKeys.forEach( function(key){
            if(vm.get('originalSettings')[key] != vm.get('settings')[key]){
                settingsChanged = true;
            }
        });
        if(tunnelsDeleted.length == 0 && tunnelsAdded == 0){
            settingsChanged = true;
        }

        v.setLoading(true);
        vm.set('panel.saveDisabled', true);
        Rpc.asyncData(v.appManager, 'setSettings', vm.get('settings'), settingsChanged)
        .then(function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            Util.successToast('Settings saved');

            if(settingsChanged == false){
                me.updateTunnels(tunnelsDeleted, tunnelsAdded);
            }

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);

            me.getSettings();
            Ext.fireEvent('resetfields', v);
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', false);
                v.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    updateTunnels: function( deleted, added ){
        var me = this, v = this.getView(), vm = this.getViewModel();
        var rpcSequence = [];

        deleted.forEach(function(publicKey){
            rpcSequence.push(Rpc.asyncPromise(v.appManager, 'deleteTunnel', publicKey));
        });
        added.forEach(function(publicKey){
            rpcSequence.push(Rpc.asyncPromise(v.appManager, 'addTunnel', publicKey));
        });
        Ext.Deferred.sequence(rpcSequence, this)
        .then(function(result){
            if(Util.isDestroyed(vm)){
                return;
            }
        }, function(ex) {
            if(!Util.isDestroyed(v, vm)){
                vm.set('panel.saveDisabled', true);
                v.setLoading(false);
            }
        });
    },

    validateSettings: function() {
        return(true);
    },

    getTunnelStatus: function () {
        var me = this,
            grid = this.getView().down('#tunnelStatus'),
            vm = this.getViewModel();

        grid.setLoading(true);
        Rpc.asyncData(this.getView().appManager, 'getTunnelStatus')
        .then( function(result){
            if(Util.isDestroyed(grid, vm)){
                return;
            }
            var status = Ext.JSON.decode(result);

            var delay = 100;
            var updateStatusTask = new Ext.util.DelayedTask( Ext.bind(function(){
                if(Util.isDestroyed(vm, status)){
                    return;
                }
                var tunnels = vm.get('tunnels');
                if(!tunnels){
                    updateStatusTask.delay(delay);
                    return;
                }
                tunnels.each(function(tunnel){
                    status.wireguard.forEach(function(status){
                        if(tunnel.get('publicKey') == status['peer-key']){
                            status['tunnel-description'] = tunnel.get('description');
                        }
                    });
                });
                vm.set('tunnelStatusData', status.wireguard);
            }, me) );
            updateStatusTask.delay( delay );

            grid.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(grid)){
                grid.setLoading(false);
            }
            Util.handleException(ex);
        });
    },

    isUDPAccessAllowedForPort: function(networkSettings, listenPort) {
        if(networkSettings.accessRules && networkSettings.accessRules.list) {
            for(var i=0; i<networkSettings.accessRules.list.length ; i++) {
                var rule = networkSettings.accessRules.list[i];
                if(rule.enabled == true && rule.blocked == false) {
                    if(rule.conditions && rule.conditions.list) {
                        var isUDP = false;
                        var isPort = false;
                        for(var j=0; j<rule.conditions.list.length ; j++) {
                            var condition = rule.conditions.list[j];
                            if(condition.invert == false) {
                                if(condition.conditionType == 'PROTOCOL' && condition.value == 'UDP') {
                                    isUDP = true;
                                }
                                if(condition.conditionType == 'DST_PORT' && parseInt(condition.value, 10) == listenPort) {
                                    isPort = true;
                                }
                            }
                        }

                        if(isUDP == true && isPort == true) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    },

    getNewAddressSpace: function() {
        var me = this,
        vm = this.getViewModel();
        Rpc.asyncData(this.getView().appManager, 'getNewAddressPool')
        .then( function(result){
            if(Util.isDestroyed(me, vm)){
                return;
            }

            vm.set('settings.addressPool', result);
        },function(ex){
            Util.handleException(ex);
        });
    },
});

Ext.define('Ung.apps.wireguard-vpn.cmp.WireguardVpnTunnelRecordEditor', {
    extend: 'Ung.cmp.RecordEditor',
    xtype: 'ung.cmp.unwireguardvpntunnelrecordeditor',

    controller: 'unwireguardvpntunnelrecordeditorcontroller'
});

Ext.define('Ung.apps.wireguard-vpn.cmp.WireguardVpnTunnelRecordEditorController', {
    extend: 'Ung.cmp.RecordEditorController',
    alias: 'controller.unwireguardvpntunnelrecordeditorcontroller',

    // Loop through the stored records to see if the passed ip
    // address is already used
    isAddrUsed: function(ip, store) {
        var ret = false;
        store.each(function(record,idx) {
            if( record.get('peerAddress') == ip ){
                ret = true;
            }
        });
        return ret;
    },

    // Override onAfterRender so we can prepopulate the peerAddress field with the next
    // available address from the wireguard tunnel address pool.  We loop through the
    // existing tunnels to make sure we select an address that isn't already used.  After
    // setting the peerAddress, we then call the default onAfterRender.
    onAfterRender: function (view) {
        var grid = this.mainGrid, vm = this.getViewModel();
        var addressPool = grid.up('panel').up('apppanel').getViewModel().get('settings.addressPool');
        var pool = addressPool.split("/")[0];

        // Assume the first pool address is used by the wg interface
        var nextPoolAddr = Util.incrementIpAddr(pool, 2);

        var store = grid.getStore();
        while (this.isAddrUsed(nextPoolAddr, store)) {
            nextPoolAddr = Util.incrementIpAddr(nextPoolAddr, 1);
        }

        var record = vm.get('record');
        if(record.get('markedForNew')) {
            record.set('peerAddress', nextPoolAddr);
        }

        this.callParent([view]);
    }
});
