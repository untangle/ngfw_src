Ext.define('Ung.config.network.Advanced', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.config.network.advanced',

    viewModel: true,

    title: 'Advanced'.t(),

    layout: 'fit',

    tbar: [{
        xtype: 'displayfield',
        padding: '0 10',
        value: '<i class="fa fa-exclamation-triangle" style="color: red;"></i> '  + 'Advanced settings require careful configuration. Misconfiguration can compromise the proper operation and security of your server.'.t()
    }],

    items: [{
        xtype: 'tabpanel',

        // tabPosition: 'left',
        // tabRotation: 0,
        // tabStretchMax: true,

        items: [{
            title: 'Options'.t(),
            padding: 10,
            defaults: {
                xtype: 'checkbox',
                // labelWidth: 250,
                // labelAlign: 'right'
            },
            items: [{
                boxLabel: 'Enable SIP NAT Helper'.t(),
                bind: '{settings.enableSipNatHelper}'
            }, {
                boxLabel: 'Send ICMP Redirects'.t(),
                bind: '{settings.sendIcmpRedirects}'
            }, {
                boxLabel: 'Enable STP (Spanning Tree) on Bridges'.t(),
                bind: '{settings.stpEnabled}'
            }, {
                boxLabel: 'Enable Strict ARP mode'.t(),
                bind: '{settings.strictArpMode}'
            }, {
                boxLabel: 'DHCP Authoritative'.t(),
                bind: '{settings.dhcpAuthoritative}'
            }, {
                boxLabel: 'Block new sessions during network configuration'.t(),
                bind: '{settings.blockDuringRestarts}'
            }, {
                boxLabel: 'Block replay packets'.t(),
                bind: '{settings.blockReplayPackets}'
            }, {
                boxLabel: 'Log bypassed sessions'.t(),
                bind: '{settings.logBypassedSessions}'
            }, {
                boxLabel: 'Log local outbound sessions'.t(),
                bind: '{settings.logLocalOutboundSessions}'
            }, {
                boxLabel: 'Log local inbound sessions'.t(),
                bind: '{settings.logLocalInboundSessions}'
            }, {
                boxLabel: 'Log blocked sessions'.t(),
                bind: '{settings.logBlockedSessions}'
            }, {
                boxLabel: 'Log intermediate session updates'.t(),
                bind: '{settings.logSessionUpdates}'
            }]
        }, {
            title: 'QoS'.t(),
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            defaults: {
                border: false
            },
            items: [{
                xtype: 'panel',
                title: 'QoS'.t(),
                bodyPadding: 10,
                defaults: {
                    labelAlign: 'right',
                    labelWidth: 120,
                },
                items: [{
                    xtype: 'checkbox',
                    fieldLabel: 'Enabled'.t(),
                    bind: '{settings.qosSettings.qosEnabled}'
                }, {
                    xtype: 'combo',
                    fieldLabel: 'Default Priority'.t(),
                    bind: '{settings.qosSettings.defaultPriority}',
                    queryMode: 'local',
                    editable: false,
                    store: [
                        [1, 'Very High'.t()],
                        [2, 'High'.t()],
                        [3, 'Medium'.t()],
                        [4, 'Low'.t()],
                        [5, 'Limited'.t()],
                        [6, 'Limited More'.t()],
                        [7, 'Limited Severely'.t()]
                    ]
                }]
            }, {
                xtype: 'panel',
                title: 'WAN Bandwidth'.t(),
                // bodyPadding: 10,
                tbar: [{
                    xtype: 'component',
                    padding: 5,
                    html: Ext.String.format('{0}Note{1}: When enabling QoS valid Download Bandwidth and Upload Bandwidth limits must be set for all WAN interfaces.'.t(), '<font color="red">','</font>') + '<br/>'
                          // Ext.String.format('Total: {0} kbps ({1} Mbit) download, {2} kbps ({3} Mbit) upload'.t(), d, d_Mbit, u, u_Mbit )
                }],

                items: [{
                    xtype: 'grid',
                    header: false,
                    columns: [{
                        header: 'Id'.t(),
                        width: 70,
                        align: 'right',
                        dataIndex: 'interfaceId',
                        renderer: function(value) {
                            if (value < 0) {
                                return i18n._("new");
                            } else {
                                return value;
                            }
                        }
                    }, {
                        header: 'WAN'.t(),
                        flex: 1,
                        dataIndex: 'name'
                    }, {
                        header: 'Config Type'.t(),
                        dataIndex: 'configType',
                        width: 150
                    }, {
                        header: 'Download Bandwidth'.t(),
                        dataIndex: 'downloadBandwidthKbps',
                        width: 180,
                        editor: {
                            xtype: 'numberfield',
                            allowBlank : false,
                            allowDecimals: false,
                            minValue: 0
                        },
                        renderer: function( value, metadata, record ) {
                            if (Ext.isEmpty(value)) {
                                return 'Not set'.t();
                            } else {
                                return value;
                                // var mbit_value = value/1000;
                                // return value + " kbps" + " (" + mbit_value + " Mbit" + ")";
                            }
                        }
                    }, {
                        header: 'Upload Bandwidth'.t(),
                        dataIndex: 'uploadBandwidthKbps',
                        width: 180,
                        editor: {
                            xtype: 'numberfield',
                            allowBlank : false,
                            allowDecimals: false,
                            minValue: 0
                        },
                        renderer: function( value, metadata, record ) {
                            if (Ext.isEmpty(value)) {
                                return 'Not set'.t();
                            } else {
                                return value;
                                // var mbit_value = value/1000;
                                // return value + " kbps" + " (" + mbit_value + " Mbit" + ")";
                            }
                        }
                    }]
                }]
            }, {
                xtype: 'panel',
                title: 'QoS Rules'.t(),
                bodyPadding: 10,
                defaults: {
                    xtype: 'combo',
                    labelAlign: 'right',
                    labelWidth: 120,
                    editable: false,
                    queryMode: 'local',
                    store: [
                        [0, 'Default'.t()],
                        [1, 'Very High'.t()],
                        [2, 'High'.t()],
                        [3, 'Medium'.t()],
                        [4, 'Low'.t()],
                        [5, 'Limited'.t()],
                        [6, 'Limited More'.t()],
                        [7, 'Limited Severely'.t()]
                    ]
                },
                items: [{
                    fieldLabel: 'Ping Priority'.t(),
                    bind: '{settings.qosSettings.pingPriority}'
                }, {
                    fieldLabel: 'DNS Priority'.t(),
                    bind: '{settings.qosSettings.dnsPriority}'
                }, {
                    fieldLabel: 'SSH Priority'.t(),
                    bind: '{settings.qosSettings.sshPriority}'
                }, {
                    fieldLabel: 'OpenVPN Priority'.t(),
                    bind: '{settings.qosSettings.openvpnPriority}'
                }]
            }]
        }, {
            title: 'Filter Rules'.t(),
        }, {
            title: 'UPnP'.t(),
        }, {
            title: 'DNS & DHCP'.t(),
        }, {
            title: 'Network Cards'.t(),
        }]
    }]
});