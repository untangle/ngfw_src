Ext.define('Ung.apps.wireguard-vpn.view.Tunnels', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-wireguard-vpn-tunnels',
    itemId: 'tunnels',
    title: 'Tunnels'.t(),
    scrollable: true,

    withValidation: true,
    padding: '8 5',

    items: [{
        xtype: 'app-wireguard-vpn-server-tunnels-grid',
    }]
});

Ext.define('Ung.apps.wireguard-vpn.cmp.TunnelsGrid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-wireguard-vpn-server-tunnels-grid',
    itemId: 'server-tunnels-grid',
    viewModel: true,

    controller: 'unwireguardvpntunnelgrid',

    emptyText: 'No tunnels defined'.t(),

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: ['@add', '->', '@import', '@export']
    }],

    recordActions: ['edit', 'copy', 'delete'],
    copyAppendField: 'description',
    copyModify: [{
        key: 'publicKey',
        value: '',
    },{
        key: 'peerAddress',
        value: function() {
            var wirgrdVpnCmp = Ext.ComponentQuery.query('[alias=widget.app-wireguard-vpn]')[0];
            if(wirgrdVpnCmp)
                return wirgrdVpnCmp.getController().getNextUnusedPoolAddr();
            else return '';
        }
    }],

    listProperty: 'settings.tunnels.list',
    emptyRow: {
        'javaClass': 'com.untangle.app.wireguard_vpn.WireGuardVpnTunnel',
        'id': -1,
        'enabled': true,
        'description': '',
        'publicKey': '',
        'endpointDynamic': true,
        'endpointHostname': '',
        'endpointPort': 51820,
        'peerAddress': '',
        'networks': '',
        'pingInterval': 60,
        'pingConnectionEvents': true,
        'pingUnreachableEvents': false,
        'assignDnsServer': false
    },

    importValidationJavaClass: true,

    bind: '{tunnels}',

    columns: [{
        xtype: 'checkcolumn',
        header: 'Enabled'.t(),
        width: Renderer.booleanWidth,
        dataIndex: 'enabled',
        resizable: false
    }, {
        header: 'Description'.t(),
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'description',
    }, {
        header: 'Remote Public Key'.t(),
        width: 290,
        dataIndex: 'publicKey',
    }, {
        header: 'Remote Peer IP Address'.t(),
        width: Renderer.ipWidth,
        dataIndex: 'peerAddress',
        editor: {
            xtype: 'textfield',
            allowBlank: false,
            vtype: 'isSingleIpValid',
            emptyText: '[enter peer IP address]'.t(),
            validator: function(value) {
                return peerIpAddrValidator(value, 'peerAddress', this, 'app-wireguard-vpn-server-tunnels-grid');
            }
        }
    }, {
        header: 'Remote Networks'.t(),
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'networks'
    }, {
        header: 'Remote Endpoint'.t(),
        width: Renderer.messageWidth,
        dataIndex: 'endpointDynamic',
        renderer: Ung.apps['wireguard-vpn'].Main.dynamicEndpointRenderer
    }, {
        header: 'Hostname'.t(),
        width: Renderer.messageWidth,
        dataIndex: 'endpointHostname',
        renderer: Ung.apps['wireguard-vpn'].Main.dynamicEndpointRenderer
    }, {
        header: 'Port'.t(),
        width: Renderer.portWidth,
        dataIndex: 'endpointPort',
        renderer: Ung.apps['wireguard-vpn'].Main.dynamicEndpointRenderer
    }, {
        xtype: 'actioncolumn',
        header: 'Remote Client'.t(),
        width: Renderer.messageWidth,
        iconCls: 'fa fa-cog',
        align: 'center',
        handler: 'getRemoteConfig',
        isDisabled: 'remoteConfigDisabled'
    }],

    editorXtype: 'ung.cmp.unwireguardvpntunnelrecordeditor',
    editorFields: [{
        xtype: 'checkbox',
        fieldLabel: 'Enabled'.t(),
        bind: '{record.enabled}'
    }, {
        xtype: 'textfield',
        fieldLabel: 'Description'.t(),
        allowBlank: false,
        bind: {
            value: '{record.description}'
        },
        validator: function(value, component) {
            if(component === undefined)
                    component = this;
            return Util.isUnique(value, 'tunnel', 'description', component, 'app-wireguard-vpn-server-tunnels-grid');
        }
    }, {
        xtype: 'textfield',
        itemId: 'publicKey',
        vtype: 'wireguardPublicKey',
        fieldLabel: 'Remote Public Key'.t(),
        bind: {
            value: '{record.publicKey}'
        },
        validator: function(value, component) {
            if(component == undefined){
                component = this;
            }
            return Util.isUnique(value, 'tunnel', 'publicKey', component, 'app-wireguard-vpn-server-tunnels-grid');
        }
    }, {
        xtype: 'fieldset',
        title: 'Remote Endpoint'.t(),
        layout: {
            type: 'vbox'
        },
        defaults: {
            labelWidth: 170,
            labelAlign: 'right'
        },
        items:[{
            fieldLabel: 'Type'.t(),
            xtype: 'combobox',
            editable: false,
            bind: {
                value: '{record.endpointDynamic}'
            },
            queryMode: 'local',
            store: [
                [true, 'Roaming'.t()],
                [false, 'Static'.t()]
            ],
            forceSelection: true,
            listeners: {
                change: 'endpointTypeComboChange'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'Hostname'.t(),
            hidden: true,
            disabled: true,
            allowBlank: false,
            vtype: 'hostName',
            bind: {
                value: '{record.endpointHostname}',
                hidden: '{record.endpointDynamic}',
                disabled: '{record.endpointDynamic}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'Port'.t(),
            vtype: 'isSinglePortValid',
            hidden: true,
            disabled: true,
            allowBlank: false,
            bind: {
                value: '{record.endpointPort}',
                hidden: '{record.endpointDynamic}',
                disabled: '{record.endpointDynamic}'
            }
        }]
    }, {
        xtype: 'textfield',
        fieldLabel: 'Remote Peer IP Address'.t(),
        vtype: 'isSingleIpValid',
        allowBlank: false,
        bind: {
            value: '{record.peerAddress}'
        },
        validator: function(value, component) {
            if(component == undefined){
                component = this;
            }
            return peerIpAddrValidator(value, 'peerAddress', component, 'app-wireguard-vpn-server-tunnels-grid');
        }
    }, {
        xtype: 'textarea',
        fieldLabel: 'Remote Networks'.t(),
        vtype: 'cidrBlockArea',
        allowBlank: true,
        width: 250,
        height: 50,
        bind: {
            value: '{record.networks}'
        },
        validator: function(value, component) {
            try{
                var isValidVtypeField = Ext.form.field.VTypes[this.vtype](value);
                var peerNetworkIp ;
                if(!isValidVtypeField){
                    return true;
                }
                
                var remoteNetworks = value;
                
                if(remoteNetworks.trim().length<=0){
                    return true;
                }
                if(component === undefined){
                    peerNetworkIp  = this.up("app-wireguard-vpn").getViewModel().get("settings").addressPool;
                }else{
                    peerNetworkIp  = component.getView().up().up().getViewModel()._data.settings.addressPool;
                }
                var localNetworkStore = remoteNetworks.length > 0 ? Ext.Array.map(remoteNetworks.split("\n"),function (remoteIpAddr){
                    return remoteIpAddr.trim();
                }) : [];

                var res = null;
                for(var i=0;i<localNetworkStore.length;i++){
                    res = Util.networkValidator(localNetworkStore[i]);
                    if(res!=true){
                        break;
                    }
                }
                if(res != true){
                    return res;
                }
                
                return Util.findIpPoolConflict(peerNetworkIp, localNetworkStore, this, false);

            } catch(err) {
                console.log(err);
                return true;
            }                        
        }
    }, {
        xtype: 'checkbox',
        fieldLabel: 'Assign DNS Server'.t(),
        bind: {
            value: '{record.assignDnsServer}',
            hidden: '{!record.endpointDynamic}',
            disabled: '{!record.endpointDynamic}'
        }
    }, {
        xtype: 'fieldset',
        title: 'Monitor'.t(),
        padding: 10,
        layout: {
            type: 'vbox'
        },
        bind: {
            hidden: '{record.endpointDynamic}'
        },
        defaults: {
            labelWidth: 170,
            labelAlign: 'right'
        },
        items:[{
            xtype: 'textfield',
            fieldLabel: 'Ping IP Address'.t(),
            allowBlank: true,
            vtype: 'isSingleIpValid',
            bind: {
                value: '{record.pingAddress}',
            }
        }, {
            xtype: 'numberfield',
            fieldLabel: 'Ping Interval'.t(),
            allowBlank: false,
            allowDecimals: false,
            minValue: 0,
            maxValue: 300,
            bind: {
                value: '{record.pingInterval}',
                disabled: '{!record.pingAddress}'
            }
        },{
            xtype: 'checkbox',
            fieldLabel: 'Alert on Tunnel Up/Down'.t(),
            bind: {
                value: '{record.pingConnectionEvents}',
                disabled: '{!record.pingAddress}'
            }
        },{
            xtype: 'checkbox',
            fieldLabel: 'Alert on Ping Unreachable'.t(),
            bind: {
                value: '{record.pingUnreachableEvents}',
                disabled: '{!record.pingAddress}'
            }
        }]
    }]
});

function isUnique(value, field, component) {
    var currentRecord ;
    if(component.getId().indexOf('textfield') !== -1){
    if(component.up('window')!=undefined)
         currentRecord = component.up('window').getViewModel().data.record.get(field);
    if (value === currentRecord) {
        return true;
    }
    //Return true if editable field peerAddress in grid is not modified
    if(!component.dirty && field === 'peerAddress') {
        return true; 
    }
    }
    var grid = Ext.ComponentQuery.query('app-wireguard-vpn-server-tunnels-grid')[0];
    var store = grid.getStore();

    var isNameUnique = store.findBy(function(record) {
        return record.get(field) === value;
    }) === -1;
    
    return isNameUnique? true : Ext.String.format('A tunnel with this {0} already exists.'.t(), field);
}


function isIPAddressUnderNWRange(value, component) { 
    var wirgrdVpnCmp = Ext.ComponentQuery.query('[alias=widget.app-wireguard-vpn]')[0];
    var addrpool;
    if(wirgrdVpnCmp)
        unusedPoolAddr =  wirgrdVpnCmp.getController().getNextUnusedPoolAddr();
    if(component.getId().indexOf('textfield') !== -1){
         addrpool = component.up('tabpanel').getViewModel().data.originalSettings.addressPool;
    }else{
         addrpool = component.getView().up().up().getViewModel()._data.settings.addressPool;
    }
    var subnet = addrpool.split('/')[1];
    var pool = addrpool.split('/')[0];
    netMask = Util.getV4NetmaskMap()[subnet ? subnet: 32];
    network = Util.getNetwork(pool, netMask); 
    //Flag to check if IP address is reserved for WG Interface
    isReservedForWGInterface = Util.convertIPIntoDecimal(value) < Util.convertIPIntoDecimal(Util.incrementIpAddr(pool, 2));
    if (unusedPoolAddr === '' &&  value === '') {
        return 'No more pool addresses are available in the address pool.'.t();
    }else if(!Util.isIPInRange(value, network, netMask) ||  isReservedForWGInterface) {
        return 'Please ensure that the entered IP address is within the specified subnet IP range.'.t();
    } else{
        return true;
    }       
}

function peerIpAddrValidator(value, field, component, grid) {
    var uniqueError = Util.isUnique(value, 'tunnel', field, component, grid);
    if (uniqueError !== true) {
        return uniqueError;
    } else {
        return true;
    }
}