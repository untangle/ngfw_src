Ext.define('Ung.apps.wireguard-vpn.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-wireguard-vpn',
    controller: 'app-wireguard-vpn',

    viewModel: {
        stores: {
            tunnelStatusList: {
                data: '{tunnelStatusData}',
                fields: [{
                    name: 'interface',
                },{
                    name :'tunnel-description',
                },{
                    name :'public-key',
                },{
                    name: 'private-key',
                },{
                    name :'peer-key',
                },{
                    name: 'listen-port',
                },{
                    name: 'fwmark'
                },{
                    name: 'peer-key'
                },{
                    name: 'preshared-key'
                },{
                    name: 'endpoint'
                },{
                    name: 'allowed-ips'
                },{
                    name: 'latest-handshake'
                },{
                    name: 'transfer-rx'
                },{
                    name: 'transfer-txt'
                },{
                    name: 'persistent-keepalive'
                }]
            },
            tunnels: {
                data:'{settings.tunnels.list}'
            }
        },

        data: {
            tunnelStatusData: []
        },

        formulas: {
            getSiteUrl: {
                get: function(get) {
                    var publicUrl = Rpc.directData('rpc.networkManager.getPublicUrl');
                    return(publicUrl.split(":")[0] + ":" + get('settings.listenPort'));
                }
            },
            peerAddress: {
                get: function(get){
                    var address = get('settings.addressPool');
                    if(!address){
                        return address;
                    }
                    return address.split('/')[0];
                }
            }
        }
    },

    items: [
        { xtype: 'app-wireguard-vpn-status' },
        { xtype: 'app-wireguard-vpn-settings' },
        { xtype: 'app-wireguard-vpn-tunnels' }
    ],
    statics: {
        dynamicEndpointRenderer: function(value, cell, record, rowIndex, columnIndex, store, table){
            var dataIndex = table.getColumnManager().columns[columnIndex].dataIndex;
            if(dataIndex == 'endpointDynamic'){
                return value ? 'Roaming'.t() : 'Static'.t();
            }else if(record.get('endpointDynamic')){
                return '&mdash;';
            }
            return value;
        },

        statusHandshakeRenderer: function(value){
            if(value == 0){
                return 'No recent activity'.t();
            }
            return Renderer.timestamp(value);
        },

        hostDisplayFields: function(collapsible, collapsed, recordEditor){
            return {
                    xtype: 'fieldset',
                    title: 'Local Service Information'.t(),
                    collapsible: collapsible ? true : false,
                    collapsed: collapsible && collapsed ? true : false,
                    layout: {
                        type: 'vbox'
                    },
                    defaults: {
                        labelWidth: 170,
                        labelAlign: recordEditor ? 'right' : 'left',
                        defaults: {
                            labelWidth: 170,
                            labelAlign: recordEditor ? 'right' : 'left',
                            defaults: {
                                labelWidth: 170,
                                labelAlign: recordEditor ? 'right' : 'left',
                            }
                        }
                    },
                    items:[{
                        xtype: 'copytoclipboard',
                        key: {
                            key: 'itemId'
                        },
                        dataType: 'javascript',
                        items: [{
                            xtype: 'fieldset',
                            border: false,
                            width: '100%',
                            items: [{
                                xtype: 'displayfield',
                                itemId: 'hostname',
                                fieldLabel: 'Hostname'.t(),
                                cls: 'x-selectable',
                                bind: {
                                    value: '{hostname}'
                                }
                            }, {
                                xtype: 'displayfield',
                                itemId: 'publicKey',
                                fieldLabel: 'Public Key'.t(),
                                cls: 'x-selectable',
                                bind: {
                                    value: '{settings.publicKey}'
                                }
                            }, {
                                fieldLabel: 'Local Endpoint IP Address'.t(),
                                itemId: 'endpointAddress',
                                xtype: 'displayfield',
                                cls: 'x-selectable',
                                bind: {
                                    value: Rpc.directData('rpc.networkManager.getPublicUrl').split(":")[0],
                                }
                            },{
                                fieldLabel: 'Local Endpoint Port'.t(),
                                itemId: 'endpointPort',
                                xtype: 'displayfield',
                                cls: 'x-selectable',
                                bind: {
                                    value: '{settings.listenPort}',
                                }
                            }, {
                                xtype: 'displayfield',
                                itemId: 'peerAddress',
                                fieldLabel: 'Peer IP Address'.t(),
                                cls: 'x-selectable',
                                bind: {
                                    value: '{peerAddress}',
                                }
                            }, {
                                xtype: 'displayfield',
                                itemId: 'networks',
                                fieldLabel: 'Local Networks'.t(),
                                cls: 'x-selectable',
                                bind: {
                                    value: '{settings.networks}',
                                }
                            }]
                        }]
                    }]
            };
        }
    }

});
