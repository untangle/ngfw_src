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
        Rpc.asyncData(v.appManager, 'getSettings')
        .then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            vm.set('originalSettings', JSON.parse(JSON.stringify(result)));
            vm.set('settings', result);
            vm.set('panel.saveDisabled', false);
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

        // Determine if settings changed, requiring a full restart.
        me.fullRestartSettingsKeys.forEach( function(key){
            if(vm.get('originalSettings')[key] != vm.get('settings')[key]){
                settingsChanged = true;
            }
        });

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

        v.setLoading(true);
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
                vm.set('panel.saveDisabled', true);
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
        var grid = this.getView().down('#tunnelStatus'),
            vm = this.getViewModel();

        grid.setLoading(true);
        Rpc.asyncData(this.getView().appManager, 'getTunnelStatus')
        .then( function(result){
            if(Util.isDestroyed(grid, vm)){
                return;
            }
            var status = Ext.JSON.decode(result);

            // Match tunnel public key with configuration tunnel descriptions.
            vm.get('tunnels').each(function(tunnel){
                status.wireguard.forEach(function(status){
                    if(tunnel.get('publicKey') == status['peer-key']){
                        status['tunnel-description'] = tunnel.get('description');
                    }
                });
            });

            vm.set('tunnelStatusData', status.wireguard);
            grid.setLoading(false);
        },function(ex){
            if(!Util.isDestroyed(grid)){
                grid.setLoading(false);
            }
            Util.handleException(ex);
        });
    },
});
