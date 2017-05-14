Ext.define('Ung.config.network.view.Advanced', {
    extend: 'Ext.panel.Panel',

    alias: 'widget.config-network-advanced',
    itemId: 'advanced',
    viewModel: true,

    title: 'Advanced'.t(),

    layout: 'fit',

    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: '<i class="fa fa-exclamation-triangle" style="color: red;"></i> '  + 'Advanced settings require careful configuration. Misconfiguration can compromise the proper operation and security of your server.'.t()
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
            }]
        }, {
            title: 'QoS'.t(),

            items: [{
                xtype: 'combo',
                fieldLabel: 'Queue Discipline'.t(),
                labelAlign: 'right',
                padding: 5,
                width: 400,
                bind: {
                    store: '{queueDisciplineStore}',
                    value: '{settings.qosSettings.queueDiscipline}'
                },
                queryMode: 'local',
                editable: false
            }, {
                xtype: 'checkbox',
                padding: 5,
                fieldLabel: 'QoS Enabled'.t(),
                labelAlign: 'right',
                bind: '{settings.qosSettings.qosEnabled}'
            }, {
                xtype: 'combo',
                padding: 5,
                fieldLabel: 'Default Priority'.t(),
                labelAlign: 'right',
                bind: {
                    store: '{qosPriorityNoDefaultStore}',
                    value: '{settings.qosSettings.defaultPriority}',
                    disabled: '{!settings.qosSettings.qosEnabled}'
                },
                queryMode: 'local',
                editable: false
            }, {
                xtype: 'tabpanel',
                // tabPosition: 'left',
                // tabRotation: 0,
                // tabStretchMax: false,
                layout: 'fit',
                disabled: true,
                bind: {
                    disabled: '{!settings.qosSettings.qosEnabled}'
                },
                items: [{
                    xtype: 'grid', // normal grid because uses a chained store
                    title: 'WAN Bandwidth'.t(),
                    // bodyPadding: 10,
                    tbar: [{
                        xtype: 'tbtext',
                        padding: '8 5',
                        style: { fontSize: '12px' },
                        html: Ext.String.format('{0}Note{1}: When enabling QoS valid Download Bandwidth and Upload Bandwidth limits must be set for all WAN interfaces.'.t(), '<font color="red">','</font>') + '<br/>'
                        // Ext.String.format('Total: {0} kbps ({1} Mbit) download, {2} kbps ({3} Mbit) upload'.t(), d, d_Mbit, u, u_Mbit )
                    }],

                    listProperty: 'settings.interfaces.list',

                    bind: '{wanInterfaces}',

                    selModel: {
                        type: 'cellmodel'
                    },

                    plugins: {
                        ptype: 'cellediting',
                        clicksToEdit: 1
                    },

                    header: false,

                    sortableColumns: false,
                    enableColumnHide: false,

                    columns: [{
                        header: 'Id'.t(),
                        width: 70,
                        align: 'right',
                        dataIndex: 'interfaceId'
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
                        width: 250,
                        editor: {
                            xtype: 'numberfield',
                            allowBlank : true,
                            allowDecimals: false,
                            minValue: 0
                        },
                        renderer: function (value) {
                            return Ext.isEmpty(value) ? 'Not set'.t() : value + ' kbps' + ' (' + value/1000 + ' Mbit' + ')';
                        }
                    }, {
                        header: 'Upload Bandwidth'.t(),
                        dataIndex: 'uploadBandwidthKbps',
                        width: 250,
                        editor: {
                            xtype: 'numberfield',
                            allowBlank : true,
                            allowDecimals: false,
                            minValue: 0
                        },
                        renderer: function (value) {
                            return Ext.isEmpty(value) ? 'Not set'.t() : value + ' kbps' + ' (' + value/1000 + ' Mbit' + ')';
                        }
                    }]
                }, {
                    xtype: 'panel',
                    title: 'QoS Rules'.t(),
                    // bodyPadding: 10,

                    layout: {
                        type: 'vbox',
                        align: 'stretch'
                    },

                    items: [{
                        border: false,
                        defaults: {
                            xtype: 'combo',
                            labelAlign: 'top',
                            labelWidth: 120,
                            editable: false,
                            queryMode: 'local',
                            padding: 10,
                        },
                        layout: {
                            type: 'hbox',
                            align: 'stretch'
                        },
                        items: [{
                            fieldLabel: 'Ping Priority'.t(),
                            bind: {
                                store: '{qosPriorityStore}',
                                value: '{settings.qosSettings.pingPriority}'
                            }
                        }, {
                            fieldLabel: 'DNS Priority'.t(),
                            bind: {
                                store: '{qosPriorityStore}',
                                value: '{settings.qosSettings.dnsPriority}'
                            }
                        }, {
                            fieldLabel: 'SSH Priority'.t(),
                            bind: {
                                store: '{qosPriorityStore}',
                                value: '{settings.qosSettings.sshPriority}'
                            }
                        }, {
                            fieldLabel: 'OpenVPN Priority'.t(),
                            bind: {
                                store: '{qosPriorityStore}',
                                value: '{settings.qosSettings.openvpnPriority}'
                            }
                        }]
                    }, {
                        xtype: 'ungrid',
                        title: 'QoS Custom Rules'.t(),

                        border: false,

                        tbar: ['@add', '->', {
                            xtype: 'tbtext',
                            style: { fontSize: '12px' },
                            html: Ext.String.format('{0}Note{1}: Custom Rules only match <b>Bypassed</b> traffic.'.t(), '<font color="red">','</font>')
                        }, '@import', '@export'],

                        recordActions: ['edit', 'delete', 'reorder'],

                        listProperty: 'settings.qosSettings.qosRules.list',
                        ruleJavaClass: 'com.untangle.uvm.network.QosRuleCondition',

                        emptyRow: {
                            ruleId: -1,
                            enabled: true,
                            description: '',
                            priority: 1,
                            javaClass: 'com.untangle.uvm.network.QosRule',
                            conditions: {
                                javaClass: 'java.util.LinkedList',
                                list: []
                            }
                        },

                        conditions: [
                            {name:"DST_LOCAL",displayName: "Destined Local".t(), type: "boolean", visible: true},
                            {name:"DST_ADDR",displayName: "Destination Address".t(), type: 'textfield', visible: true, vtype:"ipMatcher"},
                            {name:"DST_PORT",displayName: "Destination Port".t(), type: 'textfield',vtype:"portMatcher", visible: true},
                            {name:"PROTOCOL",displayName: "Protocol".t(), type: 'checkboxgroup', values: [["TCP","TCP"],["UDP","UDP"]], visible: true},
                            {name:"SRC_INTF",displayName: "Source Interface".t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, true), visible: true},
                            {name:"SRC_ADDR",displayName: "Source Address".t(), type: 'textfield', visible: true, vtype:"ipMatcher"},
                            {name:"SRC_PORT",displayName: "Source Port".t(), type: 'textfield',vtype:"portMatcher", visible: rpc.isExpertMode}
                        ],

                        label: 'Perform the following action(s):'.t(),

                        bind: '{qosRules}',

                        columns: [{
                            header: 'Rule Id'.t(),
                            width: 70,
                            align: 'right',
                            resizable: false,
                            dataIndex: 'ruleId',
                            renderer: function(value) {
                                return value < 0 ? 'new'.t() : value;
                            }
                        }, {
                            xtype: 'checkcolumn',
                            header: 'Enable'.t(),
                            dataIndex: 'enabled',
                            resizable: false,
                            width: 70
                        }, {
                            header: 'Description',
                            width: 200,
                            dataIndex: 'description',
                            renderer: function (value) {
                                return value || '<em>no description<em>';
                            }
                        }, {
                            header: 'Conditions'.t(),
                            flex: 1,
                            dataIndex: 'conditions',
                            renderer: 'conditionsRenderer'
                        }, {
                            header: 'Priority'.t(),
                            width: 100,
                            dataIndex: 'priority',
                            renderer: function (value) {
                                switch (value) {
                                  case 1: return 'Very High'.t();
                                  case 2: return 'High'.t();
                                  case 3: return 'Medium'.t();
                                  case 4: return 'Low'.t();
                                  case 5: return 'Limited'.t();
                                  case 6: return 'Limited More'.t();
                                  case 7: return 'Limited Severely'.t();
                                }
                            }
                        }],
                        editorFields: [
                            Field.enableRule(),
                            Field.description,
                            Field.conditions,
                            Field.priority
                        ]
                    }]
                }, {
                    xtype: 'grid',
                    title: 'QoS Priorities'.t(),

                    bind: '{qosPriorities}',

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
                            return value.t();
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
                    itemId: 'qosStatistics',
                    title: 'QoS Statistics'.t(),
                    groupField:'interface_name',

                    columnLines: true,
                    enableColumnHide: false,

                    bind: '{qosStatistics}',

                    viewConfig: {
                        emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-exclamation-triangle fa-2x"></i> <br/>No Data!</p>',
                    },

                    tbar: [{
                        text: 'Refresh'.t(),
                        iconCls: 'fa fa-refresh',
                        handler: 'refreshQosStatistics'
                    }],

                    columns: [{
                        header: 'Interface'.t(),
                        width: 150,
                        dataIndex: 'interface_name'
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
            }],
        }, {
            title: 'Filter Rules'.t(),
            layout: 'border',

            items: [{
                xtype: 'ungrid',
                region: 'center',
                title: 'Forward Filter Rules'.t(),

                tbar: ['@add', '->', '@import', '@export'],
                recordActions: ['edit', 'delete', 'reorder'],

                listProperty: 'settings.forwardFilterRules.list',
                ruleJavaClass: 'com.untangle.uvm.network.FilterRuleCondition',

                conditions: [
                    {name:"DST_LOCAL",displayName: "Destined Local".t(), type: "boolean", visible: true},
                    {name:"DST_ADDR",displayName: "Destination Address".t(), type: 'textfield', visible: true, vtype:"ipMatcher"},
                    {name:"DST_PORT",displayName: "Destination Port".t(), type: 'textfield',vtype:"portMatcher", visible: true},
                    {name:"DST_INTF",displayName: "Destination Interface".t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, true), visible: true},
                    {name:"SRC_MAC" ,displayName: "Source MAC".t(), type: 'textfield', visible: true},
                    {name:"SRC_ADDR",displayName: "Source Address".t(), type: 'textfield', visible: true, vtype:"ipMatcher"},
                    {name:"SRC_PORT",displayName: "Source Port".t(), type: 'textfield',vtype:"portMatcher", visible: rpc.isExpertMode},
                    {name:"SRC_INTF",displayName: "Source Interface".t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, true), visible: true},
                    {name:"PROTOCOL",displayName: "Protocol".t(), type: 'checkboxgroup', values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true}
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

                bind: '{forwardFilterRules}',

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
                    width: 70
                }, {
                    xtype: 'checkcolumn',
                    header: 'IPv6'.t(),
                    dataIndex: 'ipv6Enabled',
                    resizable: false,
                    width: 70
                }, {
                    header: 'Description',
                    width: 200,
                    dataIndex: 'description',
                    renderer: function (value) {
                        return value || '<em>no description<em>';
                    }
                }, {
                    header: 'Conditions'.t(),
                    flex: 1,
                    dataIndex: 'conditions',
                    renderer: 'conditionsRenderer'
                }, {
                    xtype: 'checkcolumn',
                    header: 'Block'.t(),
                    dataIndex: 'blocked',
                    resizable: false,
                    width: 70
                }],
                editorFields: [
                    Field.enableRule('Enable Forward Filter Rule'.t()),
                    Field.enableIpv6,
                    Field.description,
                    Field.conditions,
                    Field.blockedCombo
                ]
            }, {
                xtype: 'ungrid',
                region: 'south',
                height: '70%',
                split: true,

                title: 'Input Filter Rules'.t(),

                tbar: ['@add', '->', '@import', '@export'],
                recordActions: ['edit', 'delete', 'reorder'],

                listProperty: 'settings.inputFilterRules.list',
                ruleJavaClass: 'com.untangle.uvm.network.FilterRuleCondition',

                conditions: [
                    {name:"DST_LOCAL",displayName: "Destined Local".t(), type: "boolean", visible: true},
                    {name:"DST_ADDR",displayName: "Destination Address".t(), type: 'textfield', visible: true, vtype:"ipMatcher"},
                    {name:"DST_PORT",displayName: "Destination Port".t(), type: 'textfield',vtype:"portMatcher", visible: true},
                    {name:"DST_INTF",displayName: "Destination Interface".t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, true), visible: true},
                    {name:"SRC_MAC" ,displayName: "Source MAC".t(), type: 'textfield', visible: true},
                    {name:"SRC_ADDR",displayName: "Source Address".t(), type: 'textfield', visible: true, vtype:"ipMatcher"},
                    {name:"SRC_PORT",displayName: "Source Port".t(), type: 'textfield',vtype:"portMatcher", visible: rpc.isExpertMode},
                    {name:"SRC_INTF",displayName: "Source Interface".t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, true), visible: true},
                    {name:"PROTOCOL",displayName: "Protocol".t(), type: 'checkboxgroup', values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true}
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
                    readOnly: false
                },

                bind: '{inputFilterRules}',

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
                    width: 70
                }, {
                    xtype: 'checkcolumn',
                    header: 'IPv6'.t(),
                    dataIndex: 'ipv6Enabled',
                    resizable: false,
                    width: 70
                }, {
                    header: 'Description',
                    width: 200,
                    dataIndex: 'description',
                    renderer: function (value) {
                        return value || '<em>no description<em>';
                    }
                }, {
                    header: 'Conditions'.t(),
                    flex: 1,
                    dataIndex: 'conditions',
                    renderer: 'conditionsRenderer'
                }, {
                    xtype: 'checkcolumn',
                    header: 'Block'.t(),
                    dataIndex: 'blocked',
                    resizable: false,
                    width: 70,
                    listeners: {
                        beforecheckchange: function (col, rowIndex, checked, record) {
                            if (record.get('readOnly')) {
                                Ext.MessageBox.alert('Info', '<strong>' + record.get('description') + '</strong> connot be edited!');
                                return false;
                            }
                        }
                    }
                }],
                editorFields: [
                    Field.enableRule('Enable Input Filter Rule'.t()),
                    Field.enableIpv6,
                    Field.description,
                    Field.conditions,
                    Field.blockedCombo
                ]
            }]
        }, {
            title: 'UPnP'.t(),
            layout: 'border',

            dockedItems: [{
                xtype: 'toolbar',
                dock: 'top',
                items: [{
                    xtype: 'checkbox',
                    fieldLabel: 'Enabled'.t(),
                    labelAlign: 'right',
                    bind: '{settings.upnpSettings.upnpEnabled}'
                }, {
                    xtype: 'checkbox',
                    fieldLabel: 'Secure Mode'.t(),
                    labelAlign: 'right',
                    disabled: true,
                    bind: {
                        value: '{settings.upnpSettings.secureMode}',
                        disabled: '{!settings.upnpSettings.upnpEnabled}'
                    }
                }]
            }],

            items: [{
                xtype: 'grid',
                itemId: 'upnpStatus',
                region: 'center',

                title: 'Status'.t(),
                enableColumnHide: false,
                enableSorting: false,
                forceFit: true,

                viewConfig: {
                    emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-exclamation-triangle fa-2x"></i> <br/>No Data!</p>',
                },

                tbar: [{
                    text: 'Refresh'.t(),
                    iconCls: 'fa fa-refresh',
                    handler: 'refreshUpnpStatus'
                }],

                disabled: true,
                bind: {
                    disabled: '{!settings.upnpSettings.upnpEnabled}'
                },

                store: {
                    data: [] // todo: to set data
                },

                columns: [{
                    header: 'Protocol'.t(),
                    width: 100,
                    dataIndex: 'upnp_protocol'
                }, {
                    header: 'Client IP Address'.t(),
                    width: 150,
                    dataIndex: 'upnp_client_ip_address'
                }, {
                    header: 'Client Port'.t(),
                    width: 150,
                    dataIndex: 'upnp_client_port'
                }, {
                    header: 'Destination Port'.t(),
                    width: 150,
                    dataIndex: 'upnp_destination_port'
                }, {
                    header: 'Bytes'.t(),
                    flex: 1,
                    dataIndex: 'bytes',
                    renderer: function (value, metadata, record) {
                        if (Ext.isEmpty(value)) {
                            return 'Not set'.t();
                        } else {
                            var v = value;
                            switch (value[value.length-1]) {
                              case 'K':
                                v = parseInt(value.substr(0,value.length - 1), 10);
                                value = v * 1024;
                                break;
                              case 'M':
                                v = parseInt(value.substr(0,value.length - 1), 10);
                                value = v * 1024 * 1024;
                                break;
                            }
                            value = value / 1000;
                            var mbit_value = value/1000;
                            return Number(value).toFixed(2) + ' kbps' + ' (' + Number(mbit_value).toFixed(2) + ' Mbit' + ')';
                        }
                    }
                }, {
                    xtype: 'actioncolumn',
                    width: 60,
                    header: 'Delete'.t(),
                    align: 'center',
                    resizable: false,
                    tdCls: 'action-cell',
                    iconCls: 'fa fa-trash-o fa-red'
                }]
            }, {
                xtype: 'ungrid',
                region: 'south',
                height: '50%',
                split: true,
                title: 'Access Control Rules'.t(),

                tbar: ['@add', '->', '@import', '@export'],
                recordActions: ['edit', 'delete', 'reorder'],

                listProperty: 'settings.upnpSettings.upnpRules.list',
                ruleJavaClass: 'com.untangle.uvm.network.UpnpRuleCondition',

                conditions: [
                    {name:"DST_PORT",displayName: "Destination Port".t(), type: 'textfield', vtype:"portMatcher", visible: true},
                    {name:"SRC_ADDR",displayName: "Source Address".t(), type: 'textfield', vtype:"ipMatcher", visible: true},
                    {name:"SRC_PORT",displayName: "Source Port".t(), type: 'textfield', vtype:"portMatcher", visible: true}
                ],

                label: 'Perform the following action(s):'.t(),

                emptyRow: {
                    ruleId: -1,
                    enabled: true,
                    description: '',
                    javaClass: 'com.untangle.uvm.network.UpnpRule',
                    conditions: {
                        javaClass: 'java.util.LinkedList',
                        list: []
                    },
                    priority: 1,
                    allow: true
                },

                disabled: true,

                bind: {
                    store: '{upnpRules}',
                    disabled: '{!settings.upnpSettings.upnpEnabled}'
                },

                columns: [{
                    header: 'Rule Id'.t(),
                    width: 70,
                    align: 'right',
                    resizable: false,
                    dataIndex: 'ruleId',
                    renderer: function(value) {
                        return value < 0 ? 'new'.t() : value;
                    }
                }, {
                    xtype: 'checkcolumn',
                    header: 'Enable'.t(),
                    dataIndex: 'enabled',
                    resizable: false,
                    width: 70
                }, {
                    header: 'Description',
                    width: 200,
                    dataIndex: 'description',
                    renderer: function (value) {
                        return value || '<em>no description<em>';
                    }
                }, {
                    header: 'Conditions'.t(),
                    flex: 1,
                    dataIndex: 'conditions',
                    renderer: 'conditionsRenderer'
                }, {
                    header: 'Action'.t(),
                    width: 100,
                    dataIndex: 'allow',
                    renderer: function (value) {
                        return value ? 'Allow'.t() : 'Deny'.t();
                    }
                }],
                editorFields: [
                    Field.enableRule(),
                    Field.description,
                    Field.conditions,
                    Field.allow
                ]
            }]
        }, {
            title: 'DNS & DHCP'.t(),
            xtype: 'panel',
            tbar: [{
                xtype: 'tbtext',
                padding: '8 5',
                style: { fontSize: '12px' },
                html: '<strong>' + 'Custom dnsmasq options.'.t() + '</strong> <br/>' +
                    '<i class="fa fa-exclamation-triangle" style="color: red;"></i> ' + 'Warning: Invalid syntax will halt all DHCP & DNS services.'.t()
            }],
            layout: 'fit',
            items: [{
                xtype: 'textarea',
                margin: 10,
                fieldStyle: {
                    fontFamily: 'monospace'
                },
                bind: '{settings.dnsmasqOptions}'
            }]
        }, {
            xtype: 'grid',
            title: 'Network Cards'.t(),

            bind: '{devices}',

            selModel: {
                type: 'cellmodel'
            },

            plugins: {
                ptype: 'cellediting',
                clicksToEdit: 1
            },

            columns: [{
                header: 'Device Name'.t(),
                width: 250,
                dataIndex: 'deviceName'
            }, {
                header: 'MTU'.t(),
                width: 100,
                dataIndex: 'mtu',
                renderer: function (value) {
                    return value || 'Auto'.t();
                },
                editor: {
                    xtype: 'numberfield'
                }
            }, {
                header: 'Ethernet Media'.t(),
                dataIndex: 'duplex',
                width: 250,
                renderer: function (value) {
                    switch (value) {
                      case 'AUTO': return 'Auto'.t();
                      case 'M10000_FULL_DUPLEX': return '10000 Mbps, Full Duplex'.t();
                      case 'M10000_HALF_DUPLEX': return '10000 Mbps, Half Duplex'.t();
                      case 'M1000_FULL_DUPLEX': return '1000 Mbps, Full Duplex'.t();
                      case 'M1000_HALF_DUPLEX': return '1000 Mbps, Half Duplex'.t();
                      case 'M100_FULL_DUPLEX': return '100 Mbps, Full Duplex'.t();
                      case 'M100_HALF_DUPLEX': return '100 Mbps, Half Duplex'.t();
                      case 'M10_FULL_DUPLEX': return '10 Mbps, Full Duplex'.t();
                      case 'M10_HALF_DUPLEX': return '10 Mbps, Half Duplex'.t();
                    }
                },
                editor: {
                    xtype: 'combo',
                    store: [
                        ['AUTO', 'Auto'.t()],
                        ['M10000_FULL_DUPLEX', '10000 Mbps, Full Duplex'.t()],
                        ['M10000_HALF_DUPLEX', '10000 Mbps, Half Duplex'.t()],
                        ['M1000_FULL_DUPLEX', '1000 Mbps, Full Duplex'.t()],
                        ['M1000_HALF_DUPLEX', '1000 Mbps, Half Duplex'.t()],
                        ['M100_FULL_DUPLEX', '100 Mbps, Full Duplex'.t()],
                        ['M100_HALF_DUPLEX', '100 Mbps, Half Duplex'.t()],
                        ['M10_FULL_DUPLEX', '10 Mbps, Full Duplex'.t()],
                        ['M10_HALF_DUPLEX', '10 Mbps, Half Duplex'.t()]
                    ],
                    queryMode: 'local',
                    editable: false
                }
            }, {
                flex: 1
            }]
        }]
    }]
});
