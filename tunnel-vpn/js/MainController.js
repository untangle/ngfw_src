Ext.define('Ung.apps.tunnel-vpn.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-tunnel-vpn',

    control: {
        '#': {
            afterrender: 'getSettings'
        }
    },

    getSettings: function () {
        var me = this,
            v = me.getView(), 
            vm = me.getViewModel();
        v.setLoading(true);

        v.appManager.getSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
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

        vm.set('providers',{
            Untangle: {
                description: 'Untangle'.t(),
                userAuth: false,
                providerTitle: 'Upload the Untangle OpenVPN config zip',
                providerInstructions: '<li>' + 'Log in the main Untangle server'.t() + '<br/>' +
                    '<li>' + 'Inside "OpenVPN" app settings in Server > Remote Clients add new client and hit Save'.t() + '<br/>' +
                    '<li>' + 'Click Download for the new client and download the configuration zip file for remote Untangle OpenVPN clients'.t() + '<br/>' +
                    '<li>' + 'Upload the zip file below'.t() + '<br/>'
            },
            NordVPN: {
                description: 'NordVPN'.t(),
                userAuth: true,
                providerTitle: 'Upload the NordVPN OpenVPN config zip'.t(),
                providerInstructions: '<li>' + 'Log in to "My account" at nordvpn.com'.t() + '<br/>' +
                    '<li>' + 'Click on the "Download area"'.t() + '<br/>' +
                    '<li>' + 'Download the Linux ".OVPN configuration files" zip'.t() + '<br/>' +
                    '<li>' + 'Choose an ovpn file for a server (in your region)'.t() + '<br/>' +
                    '<li>' + 'Upload the chosen ovpn file below'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password of your NordVPN account'.t() + '<br/>'
            },
            ExpressVPN: {
                description: 'ExpressVPN'.t(),
                userAuth: true,
                providerTitle: 'Upload the ExpressVPN OpenVPN config zip'.t(),
                providerInstructions: '<li>' + 'Log in to "My account" at expressvpn.com'.t() + '<br/>' +
                    '<li>' + 'FIXME"'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password provided by ExpressVPN'.t() + '<br/>' + 
                    '<li>' + 'NOTE: This is not your ExpressVPN account username/password'.t() + '<br/>'
            },
            CustomZip: {
                description: 'Custom zip file'.t(),
                userAuth: false,
                providerTitle: 'Upload the Custom OpenVPN config zip'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .zip File'.t() + '<br/>'
            },
            CustomZipPass: {
                description: 'Custom zip file with username/password'.t(),
                userAuth: true,
                providerTitle: 'Upload the Custom OpenVPN config zip with username/password'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .zip File'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password'.t() + '<br/>'
            },
            CustomOvpn: {
                description: 'Custom ovpn file'.t(),
                userAuth: false,
                providerTitle: 'Upload the Custom OpenVPN .ovpn file'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .ovpn File'.t() + '<br/>'
            },
            CustomOvpnPass: {
                description: 'Custom ovpn file with username/password'.t(),
                userAuth: true,
                providerTitle: 'Upload the Custom OpenVPN .ovpn file with username/password'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .ovpn File'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password'.t() + '<br/>'
            },
            CustomConf: {
                description: 'Custom conf file'.t(),
                userAuth: false,
                providerTitle: 'Upload the Custom OpenVPN .conf file'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .conf File'.t() + '<br/>'
            },
            CustomConfPass: {
                description: 'Custom conf file with username/password'.t(),
                userAuth: true,
                providerTitle: 'Upload the Custom OpenVPN .conf file with username/password'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .conf File'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password'.t() + '<br/>'
            },
        });

        var providers = vm.get('providers');
        var providerComboListData = [['', 'Select'.t()]];
        for( var provider in providers ){
            providerComboListData.push([provider, providers[provider].description]);
        }

        vm.set('providersComboList', Ext.create('Ext.data.ArrayStore', {
            fields: [ 'name', 'description' ],
            data: providerComboListData
        }) );

    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        if (me.validateSettings() != true) return;

        var validSave = true;
        var tunnelsToImport = [];
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

                var items = Ext.Array.pluck(store.getRange(), 'data');
                if(grid.listProperty == 'settings.tunnels.list'){
                    items.forEach(function(tunnel){
                        if( tunnel.tunnelId == -1 ){
                            var tunnelId = me.getNextAvailableTunnelId(items);
                            if( tunnelId === false){
                                validSave = false;
                                Util.handleException("Unable to obtain a unique tunnel id, cannot add tunnel".t() + ": " + tunnel.name);
                                return;
                            }
                            tunnel.tunnelId = tunnelId;
                        }
                        if(tunnel.tempPath){
                            tunnelsToImport.push({
                                filename: tunnel.tempPath,
                                provider: tunnel.provider,
                                tunnelId: tunnel.tunnelId,
                                name: tunnel.name
                            });
                            delete tunnel.tempPath;
                        }
                    });
                }
                vm.set(grid.listProperty, items);
            }
        });

        if( validSave == false ){
            return;
        }

        v.setLoading(true);
        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.handleException(ex); return; }
            Util.successToast('Settings saved');

            tunnelsToImport.forEach( function(tunnel){
                v.appManager.importTunnelConfig(function (result, ex) {
                    v.setLoading(false);
                    if (ex) { Util.handleException(ex); return; }
                    Util.successToast('Configuration imported'.t() + ': ' + tunnel.name);
                }, tunnel.filename, tunnel.provider, tunnel.tunnelId);
            });

            me.getSettings();

            if(tunnelsToImport.length > 0){
                if( me.getView().appManager.getRunState() !== 'RUNNING'){
                    me.getView().down('appstate > button').click();
                }
            }
            Ext.fireEvent('resetfields', v);
        }, vm.get('settings'));
    },

    getNextAvailableTunnelId: function(current){
        var found = false;
        var tunnel;
        var tunnels = vm.get('settings.tunnels.list');
        var virtualInterfaces = rpc.networkManager.getNetworkSettings().virtualInterfaces.list;

        for( var i = 200; i < 240; i++ ){
            found = false;
            for(tunnel in tunnels){
                if ( tunnel.tunnelId != null && i == tunnel.tunnelId ) {
                    found = true;
                    break;
                }
            }
            if(current){
                current.forEach( function(tunnel){
                    if ( tunnel.tunnelId != null && i == tunnel.tunnelId ) {
                        found = true;
                        return;
                    }
                });
            }

            if(virtualInterfaces){
                virtualInterfaces.forEach( function( interface ){
                    if( i == interface.interfaceId){
                        found = true;
                        return;
                    }
                });
            }

            if (!found) {
                return i;
            }
        }
        return false;
    },

    validateSettings: function() {
        return(true);
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

    tunnelidRenderer: function(value){
        if(value == -1){
            return 'New'.t();
        }
        return 'tun' + value;
    }
});

Ext.define('Ung.apps.tunnel-vpn.TunnelGridController', {
    extend: 'Ung.cmp.GridController',
    alias: 'controller.untunnelgrid',

    deleteRecord: function (view, rowIndex, colIndex, item, e, record) {
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        var rulesInUse = [];
        var recordTunnelId = record.get('tunnelId');
        var rules = vm.get('rules');
        rules.each( function(rule){
            if( ( recordTunnelId != -1 ) && ( recordTunnelId == rule.get('tunnelId') ) ){
                rulesInUse.push( rule.get('description') );
            }
        });

        if(rulesInUse.length > 0){
            Util.handleException('Cannot delete tunnel.  It is used by one or more rules'.t() + ': ' + rulesInUse.join(','));
        }else{
            this.callParent(arguments);
        }
    }

 });

Ext.define('Ung.apps.tunnel-vpn.TunnelRecordEditor', {
    extend: 'Ung.cmp.RecordEditor',
    xtype: 'ung.cmp.untunnelrecordeditor',

    controller: 'untunnelrecordeditorcontroller',

});

Ext.define('Ung.apps.tunnel-vpn.TunnelRecordEditorController', {
    extend: 'Ung.cmp.RecordEditorController',
    alias: 'controller.untunnelrecordeditorcontroller',

    uploadFile: function(cmp) {
        var me = this,
            v = me.getView(),
            vm = me.getViewModel(),
            appManager = v.up('app-tunnel-vpn').appManager,
            component = cmp;

        var form = Ext.ComponentQuery.query('form[name=upload_form]')[0];
        Ext.MessageBox.wait('Uploading and validating...'.t(), 'Please wait'.t());

        component.setValidation(true);

        form.submit({
            url: "upload",
            success: Ext.bind(function( form, action ) {
                Ext.MessageBox.hide();
                var resultMsg = action.result.msg.split('&');
                vm.set('fileResult', resultMsg[1]);
                vm.set('record.tempPath', resultMsg[0]);
            }, this),
            failure: Ext.bind(function( form, action ) {
                Ext.MessageBox.hide();
                console.log(action);
                vm.set('fileResult', action.result.msg);
                component.setValidation('Must upload valid VPN config file'.t());
            }, this)
        });
    },
    providerChange: function(combo, newValue, oldValue){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        vm.set('fileResult', '');
        if( newValue == ''){
            vm.set('tunnelProviderSelected', false);
            vm.set('tunnelProviderTitle', '');
            vm.set('tunnelProviderInstructions', '');
            combo.setValidation('Provider must be selected');
        }else{
            vm.set('tunnelProviderSelected', true);

            var fileButton = Ext.ComponentQuery.query('[name=upload_file]')[0];
            if(vm.get('record.tunnelId') != -1){
                fileButton.setValidation(true);
            }

            var record = vm.get('record');
            if( ( oldValue != null ) &&
                ( newValue != oldValue ) && 
                ( ( typeof record.modified.provider == undefined ) || record.modified.provider != newValue ) ){
                fileButton.setValidation('Provider changed');
            }

            combo.setValidation(true);

            var providers = vm.get('providers');
            for( var provider in providers ){
                if(provider == newValue){
                    vm.set('tunnelProviderTitle', providers[provider].providerTitle);
                    vm.set('tunnelProviderInstructions', providers[provider].providerInstructions);

                    if(providers[provider].userAuth == true){
                        vm.set('tunnelUsernameHidden', false);
                        vm.set('tunnelPasswordHidden', false);
                    }else{
                        vm.set('tunnelUsernameHidden', true);
                        vm.set('tunnelPasswordHidden', true);
                    }
                }
            }
        }
        me.updateTunnelName();
    },

    defaultTunnelName: 'tunnel',
    updateTunnelName: function(){
        var me = this,
            v = me.getView(),
            tunnelName = v.down('[name=tunnelName]'),
            providerValue = v.down('[name=provider]').getValue();

        var newDefaultTunnelName = 'tunnel';
        if( providerValue != '' ){
            newDefaultTunnelName += '-' + providerValue;
        }
        if(tunnelName.getValue() == me.defaultTunnelName){
            tunnelName.setValue(newDefaultTunnelName);
        }
        me.defaultTunnelName = newDefaultTunnelName;
    }
});

