Ext.define('Ung.apps.wireguard-vpn.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-wireguard-vpn',

    control: {
        '#': {
            afterrender: 'getSettings'
        }
    },

    // When comparing settings for conditional restarts, ignore these values.
    conditionalRestartIgnoreKeys: [
        'tunnels'
    ],

    getSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise(v.appManager, 'getSettings'),
            Rpc.asyncPromise('rpc.networkManager.getNetworkSettings'),
            Rpc.asyncPromise('rpc.networkManager.getPublicUrl')
        ], this).then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            vm.set('originalSettings', JSON.parse(JSON.stringify(result[0])));
            vm.set('settings', result[0]);
            vm.set('panel.saveDisabled', false);
            vm.set('localHostname', result[2].split(":")[0]);

            var networkSettings = result[1];
            var warning = '';
            var listenPort = vm.get('settings.listenPort');
            if(me.isUDPAccessAllowedForPort(networkSettings, listenPort) == false) {
                warning = '<i class="fa fa-exclamation-triangle fa-red fa-lg"></i> <strong>' + 'There are no enabled access rules to allow traffic on UDP port '.t() + listenPort + '</strong>';
            }
            vm.set('warning', warning);
            vm.set('hostname', networkSettings['hostName']);

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

        var changes = Util.updateListStoresToSettings(v.query('ungrid'), vm);

        // Determine if non-tunnel changes have been made.
        // NOTE: This code attempts to not restart the wireguard interface
        // if only the tunnels have changed.  Under previous circumstances, wireguard will pick up
        // the tunnel changes from configuration and allow connections.
        // HOWEVER, by adding routes to route table namespaces using the Table=
        // and NOT restarting causes new connections to go into the main table and
        // not the target table.  As a result, we'll comment this out until WireGuard can fix this bug.
        var settingsChanged = true;
        // var settingsChanged = Util.isSettingsChanged(vm.get('originalSettings'), vm.get('settings'), me.conditionalRestartIgnoreKeys);
        // if(settingsChanged == false && Ext.Object.isEmpty(changes)){
        //     // If no changes but no tunnel changes either, consider this a call to perform full restart.
        //     settingsChanged = true;
        // }

        // Build list of sequential commands to run
        var sequence = [
            Rpc.directPromise(v.appManager, 'setSettings', vm.get('settings'), settingsChanged)
        ];

        if('settings.tunnels.list' in changes){
            changes['settings.tunnels.list'].deleted.forEach(function(jsonRecord){
                // Delete tunnel commands inserted before settings commit.
                var recordObj = JSON.parse(jsonRecord);
                sequence.unshift(Rpc.directPromise(v.appManager, 'deleteTunnel', recordObj['publicKey']));
            });

            // Format routedNetworkProfiles checkbox list value to java compatible object
            vm.get('settings.tunnels.list').forEach(function(tunnel) {
                if(tunnel.routedNetworkProfiles && !tunnel.routedNetworkProfiles.javaClass) {
                    tunnel.routedNetworkProfiles.javaClass = 'java.util.LinkedList';
                    if(!tunnel.routedNetworkProfiles.list)
                        tunnel.routedNetworkProfiles.list = [];
                    if(typeof tunnel.routedNetworkProfiles.list === 'string')
                        tunnel.routedNetworkProfiles.list = [ tunnel.routedNetworkProfiles.list ];
                }
            });
        }

        v.setLoading(true);
        Ext.Deferred.sequence(sequence).then( function(result){
            if(Util.isDestroyed(v, vm)){
                return;
            }
            Util.successToast('Settings saved');

            vm.set('panel.saveDisabled', false);
            v.setLoading(false);

            if('settings.tunnels.list' in changes){
                me.addTunnels(changes['settings.tunnels.list']);
            }
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

    /**
     * From Util.storeGetChangedRecords result, add tunnels by waiting
     * for tunnel store to be reloaded then pulling publicKeys.
     *
     * This is neccessary because on new tunnels, the keypair is added when
     * the records are written and not known until after we reload.
     *
     * @param {*} changes Util.storeGetChangedRecords result with added field.
     */
    addTunnels: function( changes ){
        if(changes == undefined || !changes.added.length){
            // No adds.
            return;
        }
        var me = this, v = this.getView(), vm = this.getViewModel();

        var tunnelsLoadCount = vm.get('tunnels').loadCount;

        var tunnelLoadTaskDelay = 100;
        var tunnelLoadTaskCountMax = 500;
        var tunnelLoadTaskCount = 0;
        var tunnelLoadTask = new Ext.util.DelayedTask( Ext.bind(function(){
            if(Util.isDestroyed(vm, changes)){
                return;
            }
            tunnelLoadTaskCount++;
            if(tunnelLoadTaskCount > tunnelLoadTaskCountMax){
                // Too many attempts
                return;
            }
            var tunnels = vm.get('tunnels');
            if(tunnels.loadCount == tunnelsLoadCount){
                /** Store has not been updated yet, so delay */
                tunnelLoadTask.delay( tunnelLoadTaskDelay );
                return;
            }
            var sequence = [];
            changes.added.forEach(function(jsonRecord){
                var recordObj = JSON.parse(jsonRecord);

                // Attempt to find based on publicKey and if not that, the peerAddress.
                var recordIndex = tunnels.find('publicKey', recordObj['publicKey']);
                if(recordIndex == -1){
                    recordIndex = tunnels.find('peerAddress', recordObj['peerAddress']);
                }
                if(recordIndex != -1){
                    // With updated record, get its publicKey for addTunnel call.
                    var record = tunnels.getAt(recordIndex);
                    if(record != null){
                        var publicKey = record.get('publicKey');
                        if(publicKey != ""){
                            sequence.push(Rpc.asyncPromise(v.appManager, 'addTunnel', publicKey));
                        }
                    }
                }
            });

            if(sequence.length == 0){
                // Did not find any matches.  Try again.
                tunnelLoadTask.delay( tunnelLoadTaskDelay );
                return;
            }

            // Perform addTunnel operations.
            if(sequence.length){
                Ext.Deferred.sequence(sequence, this)
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

            }
        }, me) );
        tunnelLoadTask.delay( tunnelLoadTaskDelay );
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
                            status['configured-endpoint'] = tunnel.get('endpointHostname');
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

    getNextUnusedPoolAddr: function() {
        var me = this,
        vm = me.getViewModel(),
        addressPool = vm.get('settings.addressPool'),
        store = vm.getStore('tunnels');
        return Util.getUnusedPoolAddr(addressPool, store, 'peerAddress');
    },

    settingsChangeListener: function(field, newValue, settingField) {
        var app = Rpc.directData('rpc.UvmContext.appManager').app('wireguard-vpn'),
            currentFieldValue = app.getSettings()[settingField];

        var warningLabel = field.nextSibling('label[cls=warningLabel]'),
            metaData = warningLabel.getEl();

        if (newValue != currentFieldValue) {
            warningLabel.setHtml('<span class="fa fa-exclamation-triangle" style="color: orange;"></span>');
            metaData.set({
                'data-qtip': 'Clients will need to configure their connections'.t(),
            });
            warningLabel.show();
        } else {
            warningLabel.hide();
        }
    }
});

Ext.define('Ung.apps.reports.cmp.Ung.apps.wireguard-vpn.cmp.WireGuardVpnTunnelGridController', {
    extend: 'Ung.cmp.GridController',

    alias: 'controller.unwireguardvpntunnelgrid',

    remoteConfigDisabled: function(view, rowIndex, colIndex, item, record){
        if(record.get('id') == "" || record.get('id') == -1){
            return true;
        }
        return false;
    },

    getRemoteConfig: function(unk1, unk2, unk3, event, unk5, record){
        var v = this.getView();
        var dialog = v.add({
            xtype: 'app-wireguard-vpn-remote-config',
            title: 'Remote Configuration'.t(),
            record: record
        });
        // NGFW-13550 - show dialog centered on screen
        // dialog.setPosition(event.getXY());
        dialog.show();
    }
});

Ext.define('Ung.apps.wireguard-vpn.cmp.WireGuardVpnTunnelRecordEditor', {
    extend: 'Ung.cmp.RecordEditor',
    xtype: 'ung.cmp.unwireguardvpntunnelrecordeditor',
    alias: 'widget.unwireguardvpntunnelrecordeditor',
    itemId: 'unwireguardvpntunnelrecordeditor',

    controller: 'unwireguardvpntunnelrecordeditorcontroller'
});

Ext.define('Ung.apps.wireguard-vpn.cmp.WireGuardVpnTunnelRecordEditorController', {
    extend: 'Ung.cmp.RecordEditorController',
    alias: 'controller.unwireguardvpntunnelrecordeditorcontroller',

    control: {
        '#unwireguardvpntunnelrecordeditor': {
            afterrender: 'afterTunnelsEditorRender'
        }
    },

    pasteTunnel: function(component){
        if(!component.target ||
           !component.target.dataset ||
           !component.target.dataset.componentid){
            return;
        }
        var el = Ext.getCmp(component.target.dataset.componentid);
        if(!el){
            return;
        }
        var view = el.up('unwireguardvpntunnelrecordeditor'),
            controller = view.getController(),
            record = view.record;
        if(record.get('id') != -1){
            // Only on a new record.
            return;
        }

        var remote = {};
        try{
            remote = JSON.parse(component.event.clipboardData.getData("text/plain"));
        }catch(e){
            return;
        }

        var remoteToRecordTask = new Ext.util.DelayedTask( Ext.bind(function(){
            if(Util.isDestroyed(remote, record)){
                return;
            }
            record.set('description', remote['hostname']);
            record.set('endpointDynamic', false);
            Ext.Object.each(remote, function(key, value){
                /* Backwards compatibility from 16.3 */
                if (key == 'endpointAddress') {
                    key = 'endpointHostname';
                }
                if(key in record.data){
                    record.set(key, value);
                }
            });
            record.set('networks',record.get('networks').split(/\s*,\s*/).join("\r\n"));
        }, view) );
        remoteToRecordTask.delay( 150 );
    },

    // get next pool address
    getNextUnusedPoolAddr: function(){
        var me = this,
        grid = this.mainGrid,
        store = grid.getStore(),
        addressPool = grid.up('panel').up('apppanel').getViewModel().get('settings.addressPool');
        return Util.getUnusedPoolAddr(addressPool, store, 'peerAddress');
    },

    // Override onAfterRender so we can prepopulate the peerAddress field with the next
    // available address from the wireguard tunnel address pool.  We loop through the
    // existing tunnels to make sure we select an address that isn't already used.  After
    // setting the peerAddress, we then call the default onAfterRender.
    onAfterRender: function (view) {
        var me = this,
            grid = this.mainGrid, vm = this.getViewModel(),
            record = vm.get('record');

        this.callParent([view]);

        // Set routed networks from the profiles selected for a tunnel dynamically
        var rnProfileList;
        if(record.get('routedNetworkProfiles')) {
            rnProfileList = record.get('routedNetworkProfiles').list;
        }
        me.setRoutedNetworksFromProfiles(rnProfileList, vm, record);
        view.down('form').add(
            Ung.apps['wireguard-vpn'].Main.hostDisplayFields(true, !record.get('markedForNew'), true)
        );

        view.getEl().on('paste', me.pasteTunnel);
    },

    endpointTypeComboChange: function(combo, newValue, oldValue){
        var me = this,
            record = me.getViewModel().get('record');
            form = combo.up('form');

        form.down('[itemId=publicKey]').allowBlank = newValue;
        form.down('[itemId=publicKey]').validate();

        var peerAddress = record.get('peerAddress');
        if(newValue && !peerAddress){
            // Dynamic
            record.set('peerAddress', me.getNextUnusedPoolAddr());
        } else if(!newValue && peerAddress){
            // Static
            if(record.get('markedForNew')){
                if(peerAddress == me.getNextUnusedPoolAddr()){
                    record.set('peerAddress', '');
                }
            }
        }
    },

    afterTunnelsEditorRender: function() {
        var v = this.getView(),
            vm = this.getViewModel(),
            items = [],
            routedNetworks = v.down('#routednetworkscbgroup'),
            localNetProfiles = vm.get('settings.networkProfiles.list');

        // Add all the Local Networks Profiles from settings page as checkboxes
        // to be selected as Routed Network Profiles on tunnel add/edit window
        localNetProfiles.forEach(function(profile) {
            items.push({
                boxLabel: profile.profileName,
                name: 'list', 
                inputValue: profile.profileName,
                autoEl: {
                    tag: 'div',
                    'data-qtip': profile.subnetsAsString
                }
            });
        });
        routedNetworks.add(items);
    },

    /**
     * listener for Routed Network Profiles change 
     */
    onRoutednetworkscbgroupChange: function(checkboxgroup, newValue, oldValue, eOpts) {
        var editor = checkboxgroup.up('unwireguardvpntunnelrecordeditor'),
            record = editor.record,
            form = editor.down('form'),
            localServiceInfo = form && form.down('#localserviceinfo');

        if (localServiceInfo) {
            form.remove(localServiceInfo, true);  // autoDestroy = true
        }

        this.setRoutedNetworksFromProfiles(newValue.list, record);
        var newCmp = Ung.apps['wireguard-vpn'].Main.hostDisplayFields(true, false, true );

        if (form) form.add(newCmp);

    },

    /**
     * Sets the routedNetworks from selected profile names in tunnel store
     */
    setRoutedNetworksFromProfiles: function(profiles, record) {
        var vm = this.getViewModel(),
            localNetProfiles = vm.get('settings.networkProfiles.list'),
            routedNetworks = "",
            networksList = [];

        if(profiles) {
            if(typeof profiles === 'string') {
                profiles = [ profiles ];
            }

            localNetProfiles.forEach(function(profile) {
                if(profiles.indexOf(profile.profileName) !== -1) {
                    var newList = profile.subnetsAsString.split(',');
                    for (var i = 0; i < newList.length; i++) {
                        var net = newList[i].trim();
                        if (net && networksList.indexOf(net) === -1) {
                            networksList.push(net);
                        }
                    }
                }
            });
            routedNetworks = networksList.join(',');
        }   

        vm.set('localNetworkList', routedNetworks);
        record.set('routedNetworks', routedNetworks);
    }
});
