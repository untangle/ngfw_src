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
            scrollable: 'y',
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            defaults: {
                // border: false,
                // margin: '0 0 10 0',
                collapsible: true,
                animCollapse: false,
                titleCollapse: true,
                minHeight: 100
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
            }, {
                xtype: 'rules',
                title: 'QoS Custom Rules'.t(),

                columnFeatures: ['reorder', 'delete', 'edit'], // which columns to add
                recordActions: ['@edit', '@delete'],

                // dataProperty: 'portForwardRules',
                // ruleJavaClass: 'com.untangle.uvm.network.PortForwardRuleCondition',

                conditions: [
                    { name: 'DST_LOCAL', displayName: 'Destined Local'.t(), type: 'boolean', visible: true},
                    { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', visible: true, vtype:'ipall'},
                    { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype:'port', visible: true},
                    { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', visible: true, vtype:'ipall'},
                    { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'numberfield', vtype:'port', visible: rpc.isExpertMode},
                    { name: 'SRC_INTF', displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: [['a', 'a'], ['b', 'b']], visible: true},
                    { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'],['UDP','UDP'],['ICMP','ICMP'],['GRE','GRE'],['ESP','ESP'],['AH','AH'],['SCTP','SCTP']], visible: true}
                ],

                tbar: [{
                    xtype: 'component',
                    padding: 5,
                    html: Ext.String.format('{0}Note{1}: Custom Rules only match <b>Bypassed</b> traffic.'.t(), '<font color="red">','</font>')
                }],

                bind: {
                    store: {
                        data: '{settings.qosSettings.qosRules.list}'
                    }
                },

                columns: [{
                    header: 'Rule Id'.t(),
                    width: 70,
                    align: 'right',
                    resizable: false,
                    dataIndex: 'ruleId',
                    renderer: function(value) {
                        if (value < 0) {
                            return 'new'.t();
                        } else {
                            return value;
                        }
                    }
                }, {
                    xtype: 'checkcolumn',
                    header: 'Enable'.t(),
                    dataIndex: 'enabled',
                    resizable: false,
                    width: 70,
                    editor: {
                        xtype: 'checkbox',
                        fieldLabel: 'Enable'.t(),
                        bind: '{record.enabled}',
                    }
                    // renderer: function (val) {
                    //     return '<i class="fa + ' + (val ? 'fa-check' : 'fa-check-o') + '"></i>';
                    // }
                }, {
                    header: 'Description',
                    width: 200,
                    dataIndex: 'description',
                    renderer: function (value) {
                        if (value) {
                            return value;
                        }
                        return '<em>no description<em>';
                    },
                    editor: {
                        xtype: 'textfield',
                        fieldLabel: 'Description'.t(),
                        bind: '{record.description}',
                        emptyText: '[no description]'.t(),
                        allowBlank: false
                    }
                }, {
                    header: 'Conditions'.t(),
                    itemId: 'conditions',
                    flex: 1,
                    dataIndex: 'conditions',
                    renderer: 'conditionsRenderer'
                }, {
                    header: 'Priority'.t(),
                    width: 100,
                    dataIndex: 'priority',
                    editor: {
                        xtype: 'combo',
                        store: [
                            [1, 'Very High'.t()],
                            [2, 'High'.t()],
                            [3, 'Medium'.t()],
                            [4, 'Low'.t()],
                            [5, 'Limited'.t()],
                            [6, 'Limited More'.t()],
                            [7, 'Limited Severely'.t()]
                        ],
                        queryMode: 'local',
                        editable: false
                    }
                }]
            }, {
                xtype: 'grid',
                title: 'QoS Priorities'.t(),

                bind: {
                    store: {
                        data: '{settings.qosSettings.qosPriorities.list}'
                    }
                },

                selModel: {
                    type: 'cellmodel'
                },

                plugins: {
                    ptype: 'cellediting',
                    clicksToEdit: 1
                },

                columnLines: true,
                sortableColumns: false,
                enableColumnHide: false,

                columns: [{
                    header: 'Priority'.t(),
                    width: 150,
                    align: 'right',
                    dataIndex: 'priorityName',
                    renderer: function (value) {
                        return value.t()
                    }
                }, {
                    header: 'Upload Reservation'.t(),
                    dataIndex: 'uploadReservation',
                    width: 150,
                    editor : {
                        xtype: 'numberfield',
                        allowBlank : false,
                        minValue : 0.1,
                        maxValue : 100
                    },
                    renderer: function (value, metadata, record) {
                        return value === 0 ? 'No reservation' : value + '%';
                    }
                }, {
                    header: 'Upload Limit'.t(),
                    dataIndex: 'uploadLimit',
                    width: 150,
                    editor : {
                        xtype: 'numberfield',
                        allowBlank : false,
                        minValue : 0.1,
                        maxValue : 100
                    },
                    renderer: function (value, metadata, record) {
                        return value === 0 ? 'No reservation' : value + '%';
                    }
                }, {
                    header: 'Download Reservation'.t(),
                    dataIndex: 'downloadReservation',
                    width: 150,
                    editor : {
                        xtype: 'numberfield',
                        allowBlank : false,
                        minValue : 0.1,
                        maxValue : 100
                    },
                    renderer: function (value, metadata, record) {
                        return value === 0 ? 'No reservation' : value + '%';
                    }
                }, {
                    header: 'Download Limit'.t(),
                    dataIndex: 'downloadLimit',
                    width: 150,
                    editor : {
                        xtype: 'numberfield',
                        allowBlank : false,
                        minValue : 0.1,
                        maxValue : 100
                    },
                    renderer: function (value, metadata, record) {
                        return value === 0 ? 'No limit' : value + '%';
                    }
                }, {
                    flex: 1
                }]
            }, {
                xtype: 'grid',
                title: 'QoS Statistics'.t(),
                groupField:'interface_name',

                columnLines: true,
                enableColumnHide: false,

                tbar: [{
                    text: 'Refresh'.t(),
                    iconCls: 'fa fa-refresh',
                    handler: 'refreshQosStatistics'
                }],

                columns: [{
                    header: 'Interface'.t(),
                    width: 150,
                    dataIndex: 'interface_name',
                    renderer: function (value) {
                        return value.t();
                    }
                }, {
                    header: 'Priority'.t(),
                    dataIndex: 'priority',
                    width: 150
                }, {
                    header: 'Data'.t(),
                    dataIndex: 'sent',
                    width: 150,
                }, {
                    flex: 1
                }]
            }]
        }, {
            title: 'Filter Rules'.t(),
            layout: 'border',

            items: [{
                xtype: 'rules',
                region: 'center',
                title: 'Forward Filter Rules'.t(),

                columnFeatures: ['reorder', 'delete', 'edit'], // which columns to add
                recordActions: ['@edit', '@delete'],

                dataProperty: 'forwardFilterRules',

                label: 'Perform the following action(s):'.t(),

                conditions: [
                    { name: 'DST_LOCAL', displayName: 'Destined Local'.t(), type: 'boolean', visible: true},
                    { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', visible: true, vtype:'ipall'},
                    { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype:'port', visible: true},
                    // { name: 'DST_INTF',displayName: 'Destination Interface'.t(), type: 'checkgroup', values: Ung.Util.getInterfaceList(true, true), visible: true},
                    { name: 'SRC_MAC' , displayName: 'Source MAC'.t(), type: 'textfield', visible: true},
                    { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', visible: true, vtype: 'ipall'},
                    { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'textfield',vtype: 'port', visible: rpc.isExpertMode},
                    // { name: 'SRC_INTF',displayName: 'Source Interface'.t(), type: 'checkgroup', values: Ung.Util.getInterfaceList(true, true), visible: true},
                    { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'],['UDP','UDP'],['ICMP','ICMP'],['GRE','GRE'],['ESP','ESP'],['AH','AH'],['SCTP','SCTP']], visible: true}
                ],

                emptyRow: {
                    ruleId: -1,
                    enabled: true,
                    ipvsEnabled: false,
                    description: '',
                    javaClass: 'com.untangle.uvm.network.FilterRule',
                    conditions: {
                        javaClass: 'java.util.LinkedList',
                        list: []
                    },
                    blocked: false
                },

                bind: {
                    store: {
                        data: '{settings.forwardFilterRules.list}'
                    }
                },

                columns: [{
                    header: 'Rule Id'.t(),
                    width: 70,
                    align: 'right',
                    resizable: false,
                    dataIndex: 'ruleId',
                    renderer: function (value) {
                        return value < 0 ? 'new'.t() : value;
                    }
                }, {
                    xtype: 'checkcolumn',
                    header: 'Enable'.t(),
                    dataIndex: 'enabled',
                    resizable: false,
                    width: 70,
                    editor: {
                        xtype: 'checkbox',
                        fieldLabel: 'Enable Forward Filter Rule'.t(),
                        bind: '{record.enabled}',
                    }
                }, {
                    xtype: 'checkcolumn',
                    header: 'IPv6'.t(),
                    dataIndex: 'ipv6Enabled',
                    resizable: false,
                    width: 70,
                    editor: {
                        xtype: 'checkbox',
                        fieldLabel: 'Enable IPv6 Support'.t(),
                        bind: '{record.ipv6Enabled}',
                    }
                }, {
                    header: 'Description',
                    width: 200,
                    dataIndex: 'description',
                    renderer: function (value) {
                        return value || '<em>no description<em>';
                    },
                    editor: {
                        xtype: 'textfield',
                        fieldLabel: 'Description'.t(),
                        bind: '{record.description}',
                        emptyText: '[no description]'.t(),
                        allowBlank: false
                    }
                }, {
                    header: 'Conditions'.t(),
                    itemId: 'conditions',
                    flex: 1,
                    dataIndex: 'conditions',
                    renderer: 'conditionsRenderer'
                }, {
                    xtype: 'checkcolumn',
                    header: 'Block'.t(),
                    dataIndex: 'blocked',
                    resizable: false,
                    width: 70,
                    editor: {
                        xtype: 'combo',
                        fieldLabel: 'Action'.t(),
                        bind: '{record.blocked}',
                        editable: false,
                        store: [[true, 'Block'.t()], [false, 'Pass'.t()]],
                        queryMode: 'local'
                    }
                }],
            }, {
                xtype: 'rules',
                region: 'south',
                height: '60%',
                split: true,

                title: 'Input Filter Rules'.t(),

                columnFeatures: ['reorder', 'delete', 'edit'], // which columns to add
                recordActions: ['@edit', '@delete'],

                dataProperty: 'inputFilterRules',

                label: 'Perform the following action(s):'.t(),

                conditions: [
                    { name: 'DST_LOCAL', displayName: 'Destined Local'.t(), type: 'boolean', visible: true},
                    { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', visible: true, vtype:'ipall'},
                    { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype:'port', visible: true},
                    { name: 'DST_INTF',displayName: 'Destination Interface'.t(), type: 'checkgroup', values: [], visible: true},
                    { name: 'SRC_MAC' , displayName: 'Source MAC'.t(), type: 'textfield', visible: true},
                    { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', visible: true, vtype: 'ipall'},
                    { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'textfield',vtype: 'port', visible: rpc.isExpertMode},
                    { name: 'SRC_INTF',displayName: 'Source Interface'.t(), type: 'checkgroup', values: [], visible: true},
                    { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'],['UDP','UDP'],['ICMP','ICMP'],['GRE','GRE'],['ESP','ESP'],['AH','AH'],['SCTP','SCTP']], visible: true}
                ],

                emptyRow: {
                    ruleId: -1,
                    enabled: true,
                    ipvsEnabled: false,
                    description: '',
                    javaClass: 'com.untangle.uvm.network.FilterRule',
                    conditions: {
                        javaClass: 'java.util.LinkedList',
                        list: []
                    },
                    blocked: false,
                    readOnly: null
                },

                bind: {
                    store: {
                        data: '{settings.inputFilterRules.list}'
                    }
                },

                columns: [{
                    header: 'Rule Id'.t(),
                    width: 70,
                    align: 'right',
                    resizable: false,
                    dataIndex: 'ruleId',
                    renderer: function (value) {
                        return value < 0 ? 'new'.t() : value;
                    }
                }, {
                    xtype: 'checkcolumn',
                    header: 'Enable'.t(),
                    dataIndex: 'enabled',
                    resizable: false,
                    width: 70,
                    editor: {
                        xtype: 'checkbox',
                        fieldLabel: 'Enable Forward Filter Rule'.t(),
                        bind: '{record.enabled}',
                    }
                }, {
                    xtype: 'checkcolumn',
                    header: 'IPv6'.t(),
                    dataIndex: 'ipv6Enabled',
                    resizable: false,
                    width: 70,
                    editor: {
                        xtype: 'checkbox',
                        fieldLabel: 'Enable IPv6 Support'.t(),
                        bind: '{record.ipv6Enabled}',
                    }
                }, {
                    header: 'Description',
                    width: 200,
                    dataIndex: 'description',
                    renderer: function (value) {
                        return value || '<em>no description<em>';
                    },
                    editor: {
                        xtype: 'textfield',
                        fieldLabel: 'Description'.t(),
                        bind: '{record.description}',
                        emptyText: '[no description]'.t(),
                        allowBlank: false
                    }
                }, {
                    header: 'Conditions'.t(),
                    itemId: 'conditions',
                    flex: 1,
                    dataIndex: 'conditions',
                    renderer: 'conditionsRenderer'
                }, {
                    xtype: 'checkcolumn',
                    header: 'Block'.t(),
                    dataIndex: 'blocked',
                    resizable: false,
                    width: 70,
                    editor: {
                        xtype: 'combo',
                        fieldLabel: 'Action'.t(),
                        bind: '{record.blocked}',
                        editable: false,
                        store: [[true, 'Block'.t()], [false, 'Pass'.t()]],
                        queryMode: 'local'
                    }
                }],
            }]
        }, {
            title: 'UPnP'.t(),
            items: [{
                xtype: 'panel',
                header: false,
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
            }]
        }, {
            title: 'DNS & DHCP'.t(),
        }, {
            title: 'Network Cards'.t(),
        }]
    }]
});