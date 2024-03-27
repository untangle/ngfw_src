Ext.define('Ung.apps.wireguard-vpn.view.Settings', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-wireguard-vpn-settings',
    itemId: 'settings',
    title: 'Settings'.t(),
    scrollable: true,

    withValidation: true,
    padding: '8 5',

    defaults: {
        labelWidth: 175
    },

    items: [{
        xtype: 'fieldcontainer',
        layout: 'hbox',
        items: [{
            fieldLabel: 'Listen port'.t(),
            xtype: 'textfield',
            vtype: 'isSinglePortValid',
            maxLength: 5,
            enforceMaxLength :true,
            bind: {
                value: '{settings.listenPort}'
            },
            allowBlank: false,
            labelWidth: 175,
            padding: '8 5 5 0',
            listeners: {
                change: function(field, newValue, oldValue) {
                    var app = Rpc.directData('rpc.UvmContext.appManager').app('wireguard-vpn'),
                        currentListenPort = app.getSettings().listenPort;

                    var warningLabel = field.nextSibling('label[cls=warningLabel]'),
                        metaData = warningLabel.getEl();

                    if (newValue != currentListenPort) {
                        warningLabel.setHtml('<span class="fa fa-exclamation-triangle" style="color: orange;"></span>');
                        metaData.set({
                            'data-qtip': 'Clients will need to configure their connections'.t(),
                        });
                        warningLabel.show();
                    } else {
                        warningLabel.hide();
                    }
                }
            }
        },{
            xtype: 'label',
            cls: 'warningLabel',
            hidden: true,
            margin: '18 0 5 5'
        }]
    },{
        fieldLabel: 'Keepalive interval'.t(),
        xtype: 'textfield',
        maxLength: 5,
        bind: {
            value: '{settings.keepaliveInterval}'
        },
        regex: RegExp("^[1-9]\\d*$"),
        regexText: 'Only accepts valid positive integer.'.t(),
        allowBlank: false
    },{
        xtype: 'fieldcontainer',
        layout: 'hbox',
        items: [{
            fieldLabel: 'MTU'.t(),
            xtype: 'textfield',
            vtype: 'mtu',
            bind: {
                value: '{settings.mtu}'
            },
            allowBlank: false,
            labelWidth: 175,
            padding: '8 5 5 0',
            listeners: {
                change: function(field, newValue, oldValue) {
                    var app = Rpc.directData('rpc.UvmContext.appManager').app('wireguard-vpn'),
                        currentMtu = app.getSettings().mtu;

                    var warningLabel = field.nextSibling('label[cls=warningLabel]'),
                        metaData = warningLabel.getEl();

                    if (newValue != currentMtu) {
                        warningLabel.setHtml('<span class="fa fa-exclamation-triangle" style="color: orange;"></span>');
                        metaData.set({
                            'data-qtip': 'Clients will need to configure their connections'.t(),
                        });
                        warningLabel.show();
                    } else {
                        warningLabel.hide();
                    }
                }
            }
        },{
            xtype: 'label',
            cls: 'warningLabel',
            hidden: true,
            margin: '18 0 5 5'
        }],
    }, {
        xtype: 'fieldset',
        title: 'Remote Client Configuration'.t(),
        layout: {
            type: 'vbox'
        },
        defaults: {
            labelWidth: 165,
            padding: "0 0 10 0"
        },
        items:[{
            xtype: 'textfield',
            fieldLabel: 'DNS Server'.t(),
            vtype: 'ipMatcher',
            bind: {
                value: '{settings.dnsServer}'
            }
        },{
            xtype: 'fieldcontainer',
            layout: 'hbox',
            items: [{
                xtype: 'label',
                text: 'Local Networks:'.t(),
                width: 170
            },{
                xtype: 'ungrid',
                itemId: 'localNetworkGrid',
                tbar: ['@addInline'],
                recordActions: ['delete'],
                listProperty: 'settings.networks.list',
                width: 300,
                bind: '{networks}',
                emptyRow: {
                    javaClass: 'com.untangle.app.wireguard_vpn.WireGuardVpnNetwork',
                    address: '10.0.0.0/24'
                },
                columns: [{
                    dataIndex: 'address',
                    header: 'Network Address',
                    width: 200,
                    flex: 1,
                    editor:{
                        xtype: 'textfield',
                        vtype: 'cidrBlock',
                        allowBlank: false,
                        emptyText: '[enter address]'.t(),
                        blankText: 'Invalid address specified'.t(),
                        validator: function(value) {
                            try{
                                var isValidVtypeField = Ext.form.field.VTypes[this.vtype](value);
                                if(!isValidVtypeField){
                                    return true;
                                }
                                var res = Util.networkValidator(value);
                                if(res != true){
                                    return res;
                                }
                                var me = this,
                                    defaultNewRowAddress = me.up('#settings').down("#localNetworkGrid").initialConfig.emptyRow.address;
                                
                                var localNetworkStoreFn = this.up('#settings').down('#localNetworkGrid').getStore();
                                var peerNetworkIp = this.up('#settings').down('#peerNetworkIp').getValue();

                                var localNetworkStore = [];
                  
                                localNetworkStoreFn.each(function (item){
                                    if(item.get("address") && !(me.originalValue == "" && item.get("address") == defaultNewRowAddress) && (item.get("address") !== me.originalValue)){
                                        localNetworkStore.push(item.get("address"));
                                    }
                                });

                                if(peerNetworkIp){
                                    localNetworkStore.push(peerNetworkIp);
                                }
                                
                                return Util.findIpPoolConflict(value, localNetworkStore, this, true);

                            } catch(err) {
                                console.log(err);
                                return true;
                            }                        
                        }
                    },
                }]
            }]
        }]
    }, {
        xtype: 'fieldset',
        title: 'Peer IP Address Pool'.t(),
        layout: {
            type: 'vbox'
        },
        defaults: {
            labelWidth: 165
        },
        items:[{
            fieldLabel: 'Assignment'.t(),
            xtype: 'combobox',
            bind: {
                value: '{settings.autoAddressAssignment}'
            },
            editable: false,
            queryMode: 'local',
            store: [
                [true, 'Automatic'.t()],
                [false, 'Self-assigned'.t()]
            ],
            forceSelection: true
        },{
            xtype: 'fieldcontainer',
            layout: 'hbox',
            defaults: {
                labelWidth: 165
            },
            items: [{
                    fieldLabel: 'Network Space'.t(),
                    xtype: 'textfield',
                    vtype: 'cidrAddr',
                    itemId: 'peerNetworkIp',
                    bind: {
                        value: '{settings.addressPool}',
                        disabled: '{settings.autoAddressAssignment}',
                        editable: '{!settings.autoAddressAssignment}'
                    },
                    listeners: {
                        change: function(field, newValue, oldValue) {
                            var app = Rpc.directData('rpc.UvmContext.appManager').app('wireguard-vpn'),
                                currentNetSpace = app.getSettings().addressPool;

                            var warningLabel = field.nextSibling('label[cls=warningLabel]'),
                                metaData = warningLabel.getEl();

                            if (newValue != currentNetSpace) {
                                warningLabel.setHtml('<span class="fa fa-exclamation-triangle" style="color: orange;"></span>');
                                metaData.set({
                                    'data-qtip': 'Clients will need to configure their connections'.t(),
                                });
                                warningLabel.show();
                            } else {
                                warningLabel.hide();
                            }
                        }
                    },
                    validator: function(value) {
                        try{
                            var isValidVtypeField = Ext.form.field.VTypes[this.vtype](value);
                            if(!isValidVtypeField){
                                return true;
                            }
                            var localNetworkStoreFn = this.up('#settings').down('#localNetworkGrid').getStore();

                            var localNetworkStore = [];
                            
                            localNetworkStoreFn.each(function (item){
                                if(item.get("address")){
                                    localNetworkStore.push(item.get("address"));
                                }
                            });

                            return Util.findIpPoolConflict(value, localNetworkStore, this, true);

                        } catch(err) {
                            console.log(err);
                            return true;
                        }                        
                    }
                },
                {
                    xtype:'button',
                    text: 'New Network Space'.t(),
                    bind:{
                        disabled: '{!settings.autoAddressAssignment}'
                    },
                    listeners: {
                        click: 'getNewAddressSpace'
                    }
                },{
                    xtype: 'label',
                    cls: 'warningLabel',
                    hidden: true,
                    margin: '10 0 5 10'
                }
            ]
        }]
    }]
});
