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
                return value ? 'Dynamic'.t() : 'Static'.t();
            }else if(record.get('endpointDynamic')){
                return '&mdash;';
            }
            return value;
        },

        statusHandshakeRenderer: function(value){
            if(value == 0){
                return 'Never'.t();
            }
            return Renderer.timestamp(value);
        },

        hostDisplayFields: function(collapsible, collapsed, recordEditor){
            return {
                xtype: 'fieldset',
                title: 'Service Information'.t(),
                collapsible: collapsible ? true : false,
                collapsed: collapsible && collapsed ? true : false,
                layout: {
                    type: 'vbox'
                },
                defaults: {
                    labelWidth: 170,
                    labelAlign: recordEditor ? 'right' : 'left'
                },
                items:[{
                    xtype: 'displayfield',
                    fieldLabel: 'Public Key'.t(),
                    cls: 'x-selectable',
                    bind: {
                        value: '{settings.publicKey}',
                    }
                }, {
                    fieldLabel: 'Endpoint IP Address'.t(),
                    xtype: 'displayfield',
                    cls: 'x-selectable',
                    bind: {
                        value: Rpc.directData('rpc.networkManager.getPublicUrl').split(":")[0],
                    }
                },{
                    fieldLabel: 'Endpoint Port'.t(),
                    xtype: 'displayfield',
                    cls: 'x-selectable',
                    bind: {
                        value: '{settings.listenPort}',
                    }
                }, {
                    xtype: 'displayfield',
                    fieldLabel: 'Peer IP Address'.t(),
                    cls: 'x-selectable',
                    bind: {
                        value: '{peerAddress}',
                    }
                }]
            };
        }
    }

});
