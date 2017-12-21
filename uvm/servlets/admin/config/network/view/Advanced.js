Ext.define('Ung.config.network.view.Advanced', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-advanced',
    itemId: 'advanced',
    viewModel: true,
    scrollable: true,

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
        itemId: 'advanced',

        items: [{
            title: 'Options'.t(),
            itemId: 'options',
            padding: 10,
            defaults: {
                xtype: 'checkbox',
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
                bind: '{settings.blockReplayPackets}',
                hidden: !rpc.isExpertMode,
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
            itemId: 'qos',
            items: [{
                xtype: 'combo',
                fieldLabel: 'Queue Discipline'.t(),
                labelAlign: 'right',
                width: 400,
                bind: {
                    store: '{queueDisciplineStore}',
                    value: '{settings.qosSettings.queueDiscipline}'
                },
                queryMode: 'local',
                editable: false
            }, {
                xtype: 'checkbox',
                fieldLabel: 'QoS Enabled'.t(),
                labelAlign: 'right',
                bind: '{settings.qosSettings.qosEnabled}'
            }, {
                xtype: 'combo',
                fieldLabel: 'Default Priority'.t(),
                labelAlign: 'right',
                hidden: true,
                bind: {
                    store: '{qosPriorityNoDefaultStore}',
                    value: '{settings.qosSettings.defaultPriority}',
                    disabled: '{!settings.qosSettings.qosEnabled}',
                    hidden: '{!settings.qosSettings.qosEnabled}'
                },
                queryMode: 'local',
                editable: false
            }, {
                xtype: 'tabpanel',
                layout: 'fit',
                disabled: true,
                hidden: true,
                bind: {
                    disabled: '{!settings.qosSettings.qosEnabled}',
                    hidden: '{!settings.qosSettings.qosEnabled}'
                },
                items: [{
                    xtype: 'ungrid',
                    title: 'WAN Bandwidth'.t(),
                    tbar: [{
                        xtype: 'tbtext',
                        padding: '8 5',
                        style: { fontSize: '12px' },
                        html: Ext.String.format('{0}Note{1}: When enabling QoS valid Download Bandwidth and Upload Bandwidth limits must be set for all WAN interfaces.'.t(), '<font color="red">','</font>') + '<br/>'
                    }],

                    listProperty: 'settings.interfaces.list',

                    emptyText: 'No WAN Bandwidth defined'.t(),

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
                        width: Renderer.idWidth,
                        align: 'right',
                        dataIndex: 'interfaceId'
                    }, {
                        header: 'WAN'.t(),
                        width: Renderer.messageidth,
                        flex: 1,
                        dataIndex: 'name'
                    }, {
                        header: 'Config Type'.t(),
                        dataIndex: 'configType',
                        width: Renderer.messageWidth,
                        renderer: Ung.config.network.MainController.addressedRenderer
                    }, {
                        header: 'Download Bandwidth'.t(),
                        dataIndex: 'downloadBandwidthKbps',
                        width: Renderer.messageWidth,
                        editor: {
                            xtype: 'numberfield',
                            allowBlank : true,
                            allowDecimals: false,
                            minValue: 0
                        },
                        renderer: Ung.config.network.MainController.qosBandwidthRenderer
                    }, {
                        header: 'Upload Bandwidth'.t(),
                        dataIndex: 'uploadBandwidthKbps',
                        width: Renderer.messageWidth,
                        editor: {
                            xtype: 'numberfield',
                            allowBlank : true,
                            allowDecimals: false,
                            minValue: 0
                        },
                        renderer: Ung.config.network.MainController.qosBandwidthRenderer
                    }]
                }, {
                    xtype: 'panel',
                    title: 'QoS Rules'.t(),

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

                        emptyText: 'No QOS Rules defined'.t(),

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
                            {name:"SRC_PORT",displayName: "Source Port".t(), type: 'textfield',vtype:"portMatcher", visible: rpc.isExpertMode},
                            {name:"CLIENT_TAGGED",displayName: 'Client Tagged'.t(), type: 'textfield', visible: true},
                            {name:"SERVER_TAGGED",displayName: 'Server Tagged'.t(), type: 'textfield', visible: true},
                        ],

                        label: 'Perform the following action(s):'.t(),

                        bind: '{qosRules}',

                        columns: [{
                            header: 'Rule Id'.t(),
                            width: Renderer.idWidth,
                            align: 'right',
                            resizable: false,
                            dataIndex: 'ruleId',
                            renderer: Renderer.id
                        }, {
                            xtype: 'checkcolumn',
                            header: 'Enable'.t(),
                            dataIndex: 'enabled',
                            resizable: false,
                            width: Renderer.booleanWidth,
                        }, {
                            header: 'Description',
                            width: Renderer.messageWidth,
                            dataIndex: 'description'
                        }, {
                            header: 'Conditions'.t(),
                            width: Renderer.conditionsWidth,
                            flex: 1,
                            dataIndex: 'conditions',
                            renderer: 'conditionsRenderer'
                        }, {
                            header: 'Priority'.t(),
                            width: Renderer.priorityWidth,
                            dataIndex: 'priority',
                            renderer: Ung.config.network.MainController.qosPriorityRenderer
                        }],
                        editorFields: [
                            Field.enableRule(),
                            Field.description,
                            Field.conditions,
                            Field.priority
                        ]
                    }]
                }, {
                    xtype: 'ungrid',
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
                        width: Renderer.priorityWidth,
                        flex: 1,
                        align: 'right',
                        dataIndex: 'priorityName',
                        // renderer: function (value) {
                        //     return value.t();
                        // }
                        renderer: Renderer.localized
                    }, {
                        header: 'Upload Reservation'.t(),
                        dataIndex: 'uploadReservation',
                        flex: 1,
                        width: Renderer.messageWidth,
                        editor : {
                            xtype: 'numberfield',
                            allowBlank : false,
                            minValue : 0.1,
                            maxValue : 100
                        },
                        renderer: Ung.config.network.MainController.qosPriorityReserverationRenderer
                    }, {
                        header: 'Upload Limit'.t(),
                        dataIndex: 'uploadLimit',
                        width: Renderer.messageWidth,
                        flex: 1,
                        editor : {
                            xtype: 'numberfield',
                            allowBlank : false,
                            minValue : 0.1,
                            maxValue : 100
                        },
                        renderer: Ung.config.network.MainController.qosPriorityLimitRenderer
                    }, {
                        header: 'Download Reservation'.t(),
                        dataIndex: 'downloadReservation',
                        width: Renderer.messageWidth,
                        flex: 1,
                        editor : {
                            xtype: 'numberfield',
                            allowBlank : false,
                            minValue : 0.1,
                            maxValue : 100
                        },
                        renderer: Ung.config.network.MainController.qosPriorityReserverationRenderer
                    }, {
                        header: 'Download Limit'.t(),
                        dataIndex: 'downloadLimit',
                        width: Renderer.messageWidth,
                        flex: 1,
                        editor : {
                            xtype: 'numberfield',
                            allowBlank : false,
                            minValue : 0.1,
                            maxValue : 100
                        },
                        renderer: Ung.config.network.MainController.qosPriorityLimitRenderer
                    }]
                }, {
                    xtype: 'ungrid',
                    itemId: 'qosStatistics',
                    title: 'QoS Statistics'.t(),
                    groupField:'interface_name',

                    columnLines: true,
                    enableColumnHide: false,

                    bind: '{qosStatistics}',

                    emptyText: 'No QOS Statistics available'.t(),

                    tbar: [{
                        text: 'Refresh'.t(),
                        iconCls: 'fa fa-refresh',
                        handler: 'externalAction',
                        action: 'refreshQosStatistics'
                    }],

                    columns: [{
                        header: 'Interface'.t(),
                        width: Renderer.messageWidth,
                        flex: 1,
                        dataIndex: 'interface_name'
                    }, {
                        header: 'Priority'.t(),
                        dataIndex: 'priority',
                        flex: 1,
                        width: Renderer.priorityWidth,
                    }, {
                        header: 'Data'.t(),
                        dataIndex: 'sent',
                        flex: 1,
                        width: Renderer.sizeWidth,
                        renderer: Renderer.datasize
                    }]
                }]
            }],
        }, {
            title: 'Access Rules'.t(),
            itemId: 'access_rules',
            layout: 'fit',

            items: [{
                xtype: 'ungrid',
                region: 'south',
                height: '70%',
                split: true,

                title: 'Access Rules'.t(),

                tbar: ['@add', '->', '@import', '@export'],
                recordActions: ['edit', 'delete', 'reorder'],

                emptyText: 'No Access Rules defined'.t(),

                listProperty: 'settings.accessRules.list',
                ruleJavaClass: 'com.untangle.uvm.network.FilterRuleCondition',

                conditions: [
                    //DST_LOCAL makes no sense on Access Rules because definitionally they are destined local
                    //However, we used to allow users to add it so we keep this here so it renders correctly
                    //but visible is false so it will not appear when creating new rules.
                    {name:"DST_LOCAL",displayName: "Destined Local".t(), type: "boolean", visible: false},
                    {name:"DST_ADDR",displayName: "Destination Address".t(), type: 'textfield', visible: true, vtype:"ipMatcher"},
                    {name:"DST_PORT",displayName: "Destination Port".t(), type: 'textfield',vtype:"portMatcher", visible: true},
                    {name:"DST_INTF",displayName: "Destination Interface".t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, true), visible: true},
                    {name:"SRC_MAC" ,displayName: "Source MAC".t(), type: 'textfield', visible: true},
                    {name:"SRC_ADDR",displayName: "Source Address".t(), type: 'textfield', visible: true, vtype:"ipMatcher"},
                    {name:"SRC_PORT",displayName: "Source Port".t(), type: 'textfield',vtype:"portMatcher", visible: rpc.isExpertMode},
                    {name:"SRC_INTF",displayName: "Source Interface".t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, true), visible: true},
                    {name:"PROTOCOL",displayName: "Protocol".t(), type: 'checkboxgroup', values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true},
                    {name:"CLIENT_TAGGED",displayName: 'Client Tagged'.t(), type: 'textfield', visible: true},
                    {name:"SERVER_TAGGED",displayName: 'Server Tagged'.t(), type: 'textfield', visible: true},

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

                bind: '{accessRules}',

                columns: [{
                    header: 'Rule Id'.t(),
                    width: Renderer.idWidth,
                    align: 'right',
                    resizable: false,
                    dataIndex: 'ruleId',
                    renderer: Renderer.id
                }, {
                    xtype: 'checkcolumn',
                    header: 'Enable'.t(),
                    dataIndex: 'enabled',
                    resizable: false,
                    width: Renderer.booleanWidth,
                }, {
                    xtype: 'checkcolumn',
                    header: 'IPv6'.t(),
                    dataIndex: 'ipv6Enabled',
                    resizable: false,
                    width: Renderer.booleanWidth,
                }, {
                    header: 'Description',
                    width: Renderer.messageWidth,
                    dataIndex: 'description'
                }, {
                    header: 'Conditions'.t(),
                    width: Renderer.conditionsWidth,
                    flex: 1,
                    dataIndex: 'conditions',
                    renderer: 'conditionsRenderer'
                }, {
                    xtype: 'checkcolumn',
                    header: 'Block'.t(),
                    dataIndex: 'blocked',
                    resizable: false,
                    width: Renderer.booleanWidth,
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
                    Field.enableRule('Enable Access Rule'.t()),
                    Field.enableIpv6,
                    Field.description,
                    Field.conditions,
                    Field.blockedCombo
                ]
            }]
        }, {
            title: 'UPnP'.t(),
            itemId: 'upnp',

            items:[{
                xtype: 'checkbox',
                fieldLabel: 'UPnP Enabled'.t(),
                labelAlign: 'right',
                bind: '{settings.upnpSettings.upnpEnabled}'
            }, {
                xtype: 'checkbox',
                fieldLabel: 'Secure Mode'.t(),
                labelAlign: 'right',
                disabled: true,
                hidden: true,
                bind: {
                    value: '{settings.upnpSettings.secureMode}',
                    disabled: '{!settings.upnpSettings.upnpEnabled}',
                    hidden: '{!settings.upnpSettings.upnpEnabled}'
                }
            },{
                xtype: 'tabpanel',
                layout: 'fit',
                disabled: true,
                hidden: true,
                bind: {
                    disabled: '{!settings.upnpSettings.upnpEnabled}',
                    hidden: '{!settings.upnpSettings.upnpEnabled}'
                },
                items: [{
                    xtype: 'panel',
                    title: 'Status'.t(),
                    items:[{
                        xtype: 'ungrid',
                        itemId: 'upnpStatus',
                        region: 'center',

                        enableColumnHide: false,
                        enableSorting: false,

                        emptyText: 'No UPnP Status available'.t(),

                        tbar: [{
                            text: 'Refresh'.t(),
                            iconCls: 'fa fa-refresh',
                            handler: 'externalAction',
                            action: 'refreshUpnpStatus'
                        }],

                        disabled: true,
                        hidden: true,
                        bind: {
                            store: '{upnpStatus}',
                            disabled: '{!settings.upnpSettings.upnpEnabled}',
                            hidden: '{!settings.upnpSettings.upnpEnabled}'
                        },

                        columns: [{
                            header: 'Protocol'.t(),
                            width: Renderer.protocolWidth,
                            flex: 1,
                            dataIndex: 'upnp_protocol'
                        }, {
                            header: 'Client IP Address'.t(),
                            width: Renderer.ipWidth,
                            flex: 1,
                            dataIndex: 'upnp_client_ip_address'
                        }, {
                            header: 'Client Port'.t(),
                            width: Renderer.portWidth,
                            flex: 1,
                            dataIndex: 'upnp_client_port'
                        }, {
                            header: 'Destination Port'.t(),
                            width: Renderer.portWidth ,
                            flex: 1,
                            dataIndex: 'upnp_destination_port'
                        }, {
                            header: 'Bytes'.t(),
                            width: Renderer.sizeWidth,
                            flex: 1,
                            dataIndex: 'bytes',
                            renderer: Renderer.dataSize
                        }, {
                            xtype: 'actioncolumn',
                            width: Renderer.actionWidth,
                            header: 'Delete'.t(),
                            align: 'center',
                            resizable: false,
                            tdCls: 'action-cell',
                            iconCls: 'fa fa-trash-o fa-red',
                            handler: 'externalAction',
                            action: 'deleteUpnp'
                        }]

                    }]
                },{
                    xtype: 'panel',
                    title: 'Access Control Rules'.t(),
                    items: [{
                        xtype: 'ungrid',

                        tbar: ['@add', '->', '@import', '@export'],
                        recordActions: ['edit', 'delete', 'reorder'],

                        emptyText: 'No Access Control Rules defined'.t(),

                        listProperty: 'settings.upnpSettings.upnpRules.list',
                        ruleJavaClass: 'com.untangle.uvm.network.UpnpRuleCondition',

                        conditions: [
                            {name:"DST_PORT",displayName: "Destination Port".t(), type: 'textfield', vtype:"portMatcher", visible: true},
                            {name:"SRC_ADDR",displayName: "Source Address".t(), type: 'textfield', vtype:"ipMatcher", visible: true},
                            {name:"SRC_PORT",displayName: "Source Port".t(), type: 'textfield', vtype:"portMatcher", visible: true}
                        ],

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
                        hidden: true,

                        bind: {
                            store: '{upnpRules}',
                            disabled: '{!settings.upnpSettings.upnpEnabled}',
                            hidden: '{!settings.upnpSettings.upnpEnabled}'
                        },

                        columns: [{
                            header: 'Rule Id'.t(),
                            width: Renderer.idWidth,
                            align: 'right',
                            resizable: false,
                            dataIndex: 'ruleId',
                            renderer: Renderer.id
                        }, {
                            xtype: 'checkcolumn',
                            header: 'Enable'.t(),
                            dataIndex: 'enabled',
                            resizable: false,
                            width: Renderer.booleanWidth,
                        }, {
                            header: 'Description',
                            width: Renderer.messageWidth,
                            flex: 1,
                            dataIndex: 'description'
                        }, {
                            header: 'Conditions'.t(),
                            width: Renderer.conditionsWidth,
                            flex: 1,
                            dataIndex: 'conditions',
                            renderer: 'conditionsRenderer'
                        }, {
                            header: 'Action'.t(),
                            width: Renderer.idWidth,
                            dataIndex: 'allow',
                            renderer: Ung.config.network.MainController.upnpAction
                        }],
                        editorFields: [
                            Field.enableRule(),
                            Field.description,
                            Field.conditions,
                            Field.allow
                        ]
                    }]
                }]
            }]
        }, {
            title: 'DNS & DHCP'.t(),
            itemId: 'dns_and_dhcp',
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
            xtype: 'ungrid',
            itemId: 'network_cards',
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
                width: Renderer.messageWidth,
                flex: 1,
                dataIndex: 'deviceName'
            }, {
                header: 'MTU'.t(),
                width: Renderer.sizeWidth,
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
                width: Renderer.messageWidth,
                flex: 1,
                renderer: Ung.config.network.MainController.networkMediaRenderer,
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
            }]
        }, {
            title: 'Netflow'.t(),
            itemId: 'netflow',
            defaults: {
                labelAlign: 'right'
            },

            items:[{
                xtype: 'checkbox',
                fieldLabel: 'Netflow Enabled'.t(),
                bind: '{settings.netflowSettings.enabled}'
            },{
                xtype: 'textfield',
                fieldLabel: 'Host'.t(),
                bind: {
                    disabled: '{!settings.netflowSettings.enabled}',
                    value: '{settings.netflowSettings.host}'
                }
            }, {
                xtype: 'numberfield',
                fieldLabel: 'Port'.t(),
                minValue: 0,
                allowDecimals: false,
                allowBlank: false,
                blankText: 'You must provide a valid port.'.t(),
                vtype: 'port',
                bind: {
                    disabled: '{!settings.netflowSettings.enabled}',
                    value: '{settings.netflowSettings.port}'
                }
            },{
                xtype: 'combo',
                fieldLabel: 'Version'.t(),
                store: [
                    [1,'v1'],
                    [5,'v5'],
                    [9,'v9']
                ],
                bind: {
                    disabled: '{!settings.netflowSettings.enabled}',
                    value: '{settings.netflowSettings.version}'
                },
                queryMode: 'local',
                editable: false
            }]
        }]
    }]
});
