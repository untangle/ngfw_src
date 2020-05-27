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
            scrollable: true,
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
                hidden: !Rpc.directData('rpc.isExpertMode'),
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
            scrollable: true,
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
                itemId: 'qos',
                layout: 'fit',
                disabled: true,
                hidden: true,
                bind: {
                    disabled: '{!settings.qosSettings.qosEnabled}',
                    hidden: '{!settings.qosSettings.qosEnabled}'
                },
                items: [{
                    xtype: 'ungrid',
                    itemId: 'bandwidth',
                    title: 'WAN Bandwidth'.t(),
                    scrollable: true,
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
                    itemId: 'rules',
                    title: 'QoS Rules'.t(),
                    scrollable: true,

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
                        scrollable: true,

                        border: false,

                        tbar: ['@add', '->', {
                            xtype: 'tbtext',
                            style: { fontSize: '12px' },
                            html: Ext.String.format('{0}Note{1}: Custom Rules only match <b>Bypassed</b> traffic.'.t(), '<font color="red">','</font>')
                        }, '@import', '@export'],

                        recordActions: ['edit', 'delete', 'reorder'],

                        emptyText: 'No QOS Rules defined'.t(),

                        listProperty: 'settings.qosSettings.qosRules.list',

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
                        },
                        Column.conditions,
                        {
                            header: 'Priority'.t(),
                            width: Renderer.priorityWidth,
                            dataIndex: 'priority',
                            renderer: Ung.config.network.MainController.qosPriorityRenderer
                        }],
                        editorFields: [
                            Field.enableRule(),
                            Field.description,
                            Field.conditions(
                                'com.untangle.uvm.network.QosRuleCondition', [
                                'DST_LOCAL',
                                'DST_ADDR',
                                'DST_PORT',
                                {
                                    name: 'PROTOCOL',
                                    values: [["TCP","TCP"],["UDP","UDP"]]
                                },
                                'SRC_INTF',
                                'SRC_ADDR',
                                'SRC_PORT',
                                'CLIENT_TAGGED',
                                'SERVER_TAGGED'
                            ]),
                            Field.priority
                        ]
                    }]
                }, {
                    xtype: 'ungrid',
                    itemId: 'priorities',
                    title: 'QoS Priorities'.t(),
                    scrollable: true,

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
                    itemId: 'qos_statistics',
                    title: 'QoS Statistics'.t(),
                    groupField:'interface_name',
                    scrollable: true,

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
            scrollable: true,

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
                    dataIndex: 'description',
                    flex: 1
                },
                Column.conditions,
                {
                    xtype: 'checkcolumn',
                    header: 'Block'.t(),
                    dataIndex: 'blocked',
                    resizable: false,
                    width: Renderer.booleanWidth,
                    listeners: {
                        beforecheckchange: function (col, rowIndex, checked, record) {
                            if (record.get('readOnly')) {
                                Ext.MessageBox.alert('Info', '<strong>' + record.get('description') + '</strong> cannot be edited!');
                                return false;
                            }
                        }
                    }
                }],
                editorFields: [
                    Field.enableRule('Enable Access Rule'.t()),
                    Field.enableIpv6,
                    Field.description,
                    Field.conditions(
                        'com.untangle.uvm.network.FilterRuleCondition', [{
                            // DST_LOCAL makes no sense on Access Rules because definitionally they are destined local
                            // However, we used to allow users to add it so we keep this here so it renders correctly
                            // but visible is false so it will not appear when creating new rules.
                            name:"DST_LOCAL",
                            visible: false
                        },
                        "DST_ADDR",
                        "DST_PORT",
                        "DST_INTF",
                        "SRC_MAC",
                        "SRC_ADDR",
                        "SRC_PORT",
                        "SRC_INTF",
                        "PROTOCOL",
                        "CLIENT_TAGGED",
                        "SERVER_TAGGED"
                    ]),
                    Field.blockedCombo
                ]
            }]
        }, {
            title: 'UPnP'.t(),
            itemId: 'upnp',
            scrollable: true,

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
                itemId: 'upnp',
                layout: 'fit',
                disabled: true,
                hidden: true,
                bind: {
                    disabled: '{!settings.upnpSettings.upnpEnabled}',
                    hidden: '{!settings.upnpSettings.upnpEnabled}'
                },
                items: [{
                    xtype: 'panel',
                    itemId: 'status',
                    title: 'Status'.t(),
                    scrollable: true,
                    items:[{
                        xtype: 'ungrid',
                        itemId: 'upnp_status',
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
                    itemId: 'access_control_rules',
                    title: 'Access Control Rules'.t(),
                    scrollable: true,
                    items: [{
                        xtype: 'ungrid',

                        tbar: ['@add', '->', '@import', '@export'],
                        recordActions: ['edit', 'delete', 'reorder'],

                        emptyText: 'No Access Control Rules defined'.t(),

                        listProperty: 'settings.upnpSettings.upnpRules.list',

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
                        },
                        Column.conditions,
                        {
                            header: 'Action'.t(),
                            width: Renderer.idWidth,
                            dataIndex: 'allow',
                            renderer: Ung.config.network.MainController.upnpAction
                        }],
                        editorFields: [
                            Field.enableRule(),
                            Field.description,
                            Field.conditions(
                                'com.untangle.uvm.network.UpnpRuleCondition',[
                                'DST_PORT',
                                'SRC_ADDR',
                                'SRC_PORT'
                            ]),
                            Field.allow
                        ]
                    }]
                }]
            }]
        }, {
            title: 'DNS & DHCP'.t(),
            itemId: 'dns_and_dhcp',
            xtype: 'panel',
            scrollable: true,
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
            scrollable: true,

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
                    if (value == null || value === "")
                        return 'Auto'.t();
                    return value;
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
            scrollable: true,
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
        }, {
            title: 'Dynamic Routing'.t(),
            itemId: 'dynamic_routing',
            scrollable: true,

            defaults:{
                margin: 10
            },

            items:[{
                xtype: 'checkbox',
                fieldLabel: 'Dynamic Routing Enabled'.t(),
                labelAlign: 'right',
                labelWidth: 160,
                bind: '{settings.dynamicRoutingSettings.enabled}'
            },{
                xtype: 'component',
                style: 'background-color: yellow;',
                bind:{
                    html: '<i class="fa fa-exclamation-triangle"></i> '  + '{dynamicRoutingWarningsMessages}',
                    hidden: '{dynamicRoutingWarningsCount == 0}'
                }
            },{
                xtype: 'tabpanel',
                itemId: 'dynamic_routing',
                layout: 'fit',
                disabled: true,
                hidden: true,
                bind: {
                    disabled: '{!settings.dynamicRoutingSettings.enabled}',
                    hidden: '{!settings.dynamicRoutingSettings.enabled}'
                },
                items: [{
                    xtype: 'panel',
                    width: '100%',
                    region: 'south',
                    itemId: 'status',
                    title: 'Status'.t(),
                    split: true,
                    collapsible: false,
                    items:[{
                        xtype: 'ungrid',
                        split: true,
                        itemId: 'dynamic_routing_status',
                        title: 'Acquired Dynamic Routes'.t(),

                        emptyText: 'No Acquired Dynamic Routes found'.t(),

                        bind: {
                            store: '{dymamicRoutes}'
                        },
                        columns: [{
                            header: 'Network'.t(),
                            dataIndex:'network',
                            width: Renderer.networkWidth
                        },{
                            header: 'Prefix'.t(),
                            dataIndex: 'prefix',
                            width: Renderer.prefixWidth
                        },{
                            header: 'Via'.t(),
                            dataIndex: 'via',
                            width: Renderer.ipWidth
                        },{
                            header: 'Interface'.t(),
                            dataIndex: 'interface',
                            width: Renderer.messageWidth,
                            renderer: Renderer.interface
                        },{
                            header: 'Device'.t(),
                            dataIndex: 'dev',
                            width: Renderer.idWidth
                        },{
                            header: 'Attributes'.t(),
                            dataIndex: 'attributes',
                            width: Renderer.messageWdith,
                            renderer: Ung.config.network.MainController.routeAttributes,
                            flex: 1
                        }],
                    },{
                        xtype: 'ungrid',
                        split: true,
                        itemId: 'bgp_status',
                        title: 'BGP Status'.t(),

                        emptyText: 'No BGP Status available'.t(),

                        bind: {
                            store: '{bgpStatus}',
                            hidden: '{!settings.dynamicRoutingSettings.bgpEnabled}'
                        },
                        columns: [{
                            header: 'Neighbor'.t(),
                            dataIndex: 'neighbor',
                            width: Renderer.networkWidth
                        }, {
                            header: 'AS'.t(),
                            dataIndex: 'as',
                            width: Renderer.idWidth
                        }, {
                            header: 'Msgs Recv'.t(),
                            dataIndex: 'msgsRecv',
                            width: Renderer.dataSize,
                            renderer: Renderer.count
                        }, {
                            header: 'Msgs Sent'.t(),
                            dataIndex: 'msgsSent',
                            width: Renderer.dataSize,
                            renderer: Renderer.count
                        }, {
                            header: 'Uptime'.t(),
                            dataIndex: 'uptime',
                            width: Renderer.timestampWidth,
                            flex: 1,
                            renderer: Renderer.elapsedTime
                        }],
                    }, {
                        xtype: 'ungrid',
                        split: true,
                        itemId: 'ospf_status',
                        title: 'OSPF Status'.t(),

                        emptyText: 'No OSPF Status available'.t(),

                        bind: {
                            store: '{ospfStatus}',
                            hidden: '{!settings.dynamicRoutingSettings.ospfEnabled}'
                        },
                        columns: [{
                            header: 'Neighbor ID'.t(),
                            dataIndex: 'neighbor',
                            width: Renderer.ipWidth
                        },{
                            header: 'Neighbor Address'.t(),
                            dataIndex: 'address',
                            width: Renderer.ipWidth
                        },{
                            header: 'Time Remaining'.t(),
                            dataIndex: 'time',
                            width: Renderer.timestamp
                        },{
                            header: 'Device'.t(),
                            dataIndex: 'dev',
                            width: Renderer.idWidth
                        },{
                            header: 'Interface'.t(),
                            dataIndex: 'interface',
                            width: Renderer.messageWidth,
                            renderer: Renderer.interface,
                            flex: 1
                        }]
                    }],
                    tbar: [{
                        xtype: 'button',
                        iconCls: 'fa fa-refresh',
                        text: 'Refresh',
                        handler: 'getDynamicRoutingStatus'
                    }]
                },{
                    xtype: 'panel',
                    itemId: 'bgp',
                    title: 'BGP'.t(),
                    scrollable: true,

                    items: [{
                        xtype: 'checkbox',
                        fieldLabel: 'BGP Enabled'.t(),
                        bind: '{settings.dynamicRoutingSettings.bgpEnabled}',
                        margin: 10
                    },{
                        xtype: 'textfield',
                        fieldLabel: 'Router ID'.t(),
                        bind: {
                            value: '{settings.dynamicRoutingSettings.bgpRouterId}',
                            disabled: '{!settings.dynamicRoutingSettings.bgpEnabled}'
                        },
                        emptyText: '[no router id]'.t(),
                        blankText: 'Router ID must be specified.'.t(),
                        vtype: 'routerId',
                        margin: 10
                    },{
                        xtype: 'textfield',
                        fieldLabel: 'Router AS'.t(),
                        bind: {
                            value: '{settings.dynamicRoutingSettings.bgpRouterAs}',
                            disabled: '{!settings.dynamicRoutingSettings.bgpEnabled}'
                        },
                        emptyText: '[no router as]'.t(),
                        blankText: 'Router AS must be specified.'.t(),
                        vtype: 'routerAs',
                        margin: 10
                    },{
                        xtype: 'tabpanel',
                        itemId: 'bgp',
                        bind:{
                            disabled: '{!settings.dynamicRoutingSettings.bgpEnabled}'
                        },
                        items:[{
                            xtype: 'ungrid',
                            itemId: 'neighbors',
                            title: 'Neighbors'.t(),

                            tbar: ['@add', '->', '@import', '@export'],
                            recordActions: ['edit', 'delete'],

                            emptyText: 'No BGP Neighbors defined'.t(),

                            listProperty: 'settings.dynamicRoutingSettings.bgpNeighbors.list',

                            emptyRow: {
                                ruleId: -1,
                                enabled: true,
                                description: '',
                                ipAddress: '',
                                as: '',
                                javaClass: 'com.untangle.uvm.network.DynamicRouteBgpNeighbor',
                            },

                            bind: {
                                store: '{bgpNeighbors}'
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
                                header: 'IP Address',
                                width: Renderer.ipWidth,
                                flex: 1,
                                dataIndex: 'ipAddress'
                            }, {
                                header: 'AS',
                                width: Renderer.idWidth,
                                flex: 1,
                                dataIndex: 'as'
                            }],
                            editorFields: [
                                Field.enableRule(),
                                Field.description,
                            {
                                xtype:'textfield',
                                bind: '{record.ipAddress}',
                                fieldLabel: 'Target IP Address'.t(),
                                emptyText: "[no target ip address]".t(),
                                allowBlank: false,
                                vtype: 'ip4Address'
                            },{
                                xtype:'textfield',
                                bind: '{record.as}',
                                fieldLabel: 'Target AS'.t(),
                                emptyText: "[no target as]".t(),
                                vtype: 'routerAs',
                                allowBlank: false
                            }]
                        },{
                            xtype: 'ungrid',
                            itemId: 'networks',
                            title: 'Networks'.t(),

                            tbar: ['@add', '->', '@import', '@export'],
                            recordActions: ['edit', 'delete'],

                            emptyText: 'No BGP Networks defined'.t(),

                            listProperty: 'settings.dynamicRoutingSettings.bgpNetworks.list',

                            emptyRow: {
                                ruleId: -1,
                                enabled: true,
                                description: '',
                                network: '',
                                prefix: 32,
                                javaClass: 'com.untangle.uvm.network.DynamicRouteNetwork',
                            },

                            bind: {
                                store: '{bgpNetworks}',
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
                                header: 'Network',
                                width: Renderer.networkWidth,
                                flex: 1,
                                dataIndex: 'network'
                            }, {
                                header: 'Netmask/Prefix'.t(),
                                width: Renderer.ipWidth,
                                dataIndex: 'prefix'
                            }],
                            editorFields: [
                                Field.enableRule(),
                                Field.description,
                                Field.network,
                                Field.netMask
                            ]
                        }]
                    }]
                },{
                    xtype: 'panel',
                    itemId: 'ospf',
                    title: 'OSPF'.t(),
                    scrollable: true,

                    items: [{
                        xtype: 'checkbox',
                        fieldLabel: 'OSPF Enabled'.t(),
                        bind: '{settings.dynamicRoutingSettings.ospfEnabled}',
                        margin: 10
                    },{
                        xtype: 'tabpanel',
                        itemId: 'ospf',

                        bind:{
                            disabled: '{!settings.dynamicRoutingSettings.ospfEnabled}'
                        },

                        items:[{
                            xtype: 'ungrid',
                            itemId: 'networks',
                            title: 'Networks'.t(),

                            tbar: ['@add', '->', '@import', '@export'],
                            recordActions: ['edit', 'delete'],

                            emptyText: 'No OSPF Networks defined'.t(),

                            listProperty: 'settings.dynamicRoutingSettings.ospfNetworks.list',

                            emptyRow: {
                                ruleId: -1,
                                enabled: true,
                                description: '',
                                network: '',
                                prefix: 32,
                                area: 0,
                                javaClass: 'com.untangle.uvm.network.DynamicRouteNetwork'
                            },

                            bind: {
                                store: '{ospfNetworks}',
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
                                header: 'Network',
                                width: Renderer.networkWidth,
                                flex: 1,
                                dataIndex: 'network'
                            }, {
                                header: 'Netmask/Prefix'.t(),
                                width: Renderer.ipWidth,
                                dataIndex: 'prefix'
                            }, {
                                header: 'Area',
                                width: Renderer.ipWidth,
                                flex: 1,
                                dataIndex: 'area',
                                renderer: Ung.config.network.MainController.ospfAreaRenderer
                            }],
                            editorFields: [
                                Field.enableRule(),
                                Field.description,
                                Field.network,
                                Field.netMask,
                            {
                                xtype: 'combo',
                                bind: {
                                    value: '{record.area}',
                                    store: '{ospfAreas}'
                                },
                                displayField: 'comboValueField',
                                valueField: 'ruleId',
                                fieldLabel: 'Area'.t(),
                                emptyText: "[enter area]".t(),
                                queryMode: 'local',
                                editable: false,
                                allowBlank: false
                            }]
                        },{
                            xtype: 'ungrid',
                            itemId: 'areas',
                            title: 'Areas'.t(),

                            tbar: ['@add', '->', '@import', '@export'],
                            recordActions: ['edit', 'delete'],

                            emptyText: 'No OSPF Areas defined'.t(),

                            listProperty: 'settings.dynamicRoutingSettings.ospfAreas.list',

                            emptyRow: {
                                ruleId: -1,
                                enabled: true,
                                description: '',
                                area: '',
                                type: 0,
                                authentication: 0,
                                javaClass: 'com.untangle.uvm.network.DynamicRouteOspfArea',
                                virtualLinks: {
                                    javaClass: "java.util.LinkedList",
                                    list: []
                                }
                            },

                            bind: {
                                store: '{ospfAreas}',
                            },

                            columns: [{
                                header: 'Rule Id'.t(),
                                width: Renderer.idWidth,
                                align: 'right',
                                resizable: false,
                                dataIndex: 'ruleId',
                                renderer: Renderer.id
                            }, {
                                header: 'Description',
                                width: Renderer.messageWidth,
                                flex: 1,
                                dataIndex: 'description'
                            }, {
                                header: 'Area',
                                width: Renderer.ipWidth,
                                flex: 1,
                                dataIndex: 'area'
                            }, {
                                header: 'Type',
                                width: Renderer.ipWidth,
                                flex: 1,
                                dataIndex: 'type',
                                renderer: Ung.config.network.MainController.ospfAreaTypeRenderer
                            }, {
                                header: 'Authentication',
                                width: Renderer.ipWidth,
                                flex: 1,
                                dataIndex: 'authentication',
                                renderer: Ung.config.network.MainController.ospfInterfaceAuthenticationRenderer
                            }],

                            editorXtype: 'ung.cmp.unospfarearecordeditor',
                            editorFields: [
                                Field.description,
                            {
                                xtype:'textfield',
                                bind: '{record.area}',
                                fieldLabel: 'Area'.t(),
                                emptyText: "[no area]".t(),
                                vtyoe: 'routerArea',
                                allowBlank: false
                            },{
                                xtype: 'combo',
                                bind: {
                                    value: '{record.type}',
                                    store: '{ospfAreaTypes}'
                                },
                                displayField: 'type',
                                valueField: 'value',
                                fieldLabel: 'Type'.t(),
                                queryMode: 'local',
                                editable: false,
                                allowBlank: false
                            },{
                                xtype: 'container',
                                layout: 'column',
                                margin: '0 0 5 0',
                                width: 800,
                                hidden: true,
                                disabled: true,
                                bind:{
                                    hidden: '{record.type}',
                                    disabled: '{record.type}'
                                },
                                items: [{
                                    xtype: 'label',
                                    html: 'Virtual Links'.t(),
                                    width: 190,
                                },{
                                    xtype: 'ungrid',
                                    itemId: 'unvirtuallinkgrid',
                                    width: 305,
                                    border: false,
                                    titleCollapse: true,
                                    tbar: ['@addInline'],
                                    recordActions: ['delete'],
                                    bind: '{record.virtualLinks.list}',
                                    maxHeight: 140,
                                    emptyRow: {
                                        field1: ''
                                    },
                                    columns: [{
                                        header: 'Virtual Link Address'.t(),
                                        dataIndex: 'field1',
                                        width: 200,
                                        flex: 1,
                                        editor : {
                                            xtype: 'textfield',
                                            vtype: 'ip4Address',
                                            emptyText: '[enter virtual address]'.t(),
                                            allowBlank: false
                                        }
                                    }]
                                },{
                                    xtype: 'label',
                                    html: '(optional)'.t(),
                                    cls: 'boxlabel'
                                }]
                            },{
                                xtype: 'combo',
                                bind: {
                                    value: '{record.authentication}',
                                    store: '{ospfAuthenticationTypes}'
                                },
                                displayField: 'type',
                                valueField: 'value',
                                fieldLabel: 'Authentication'.t(),
                                queryMode: 'local',
                                editable: false,
                                allowBlank: false
                             }]
                        },{
                            xtype: 'ungrid',
                            itemId: 'interfaces',
                            title: 'Interface Overrides'.t(),

                            tbar: ['@add', '->', '@import', '@export'],
                            recordActions: ['edit', 'delete'],

                            emptyText: 'No OSPF Interfaces defined'.t(),

                            listProperty: 'settings.dynamicRoutingSettings.ospfInterfaces.list',

                            emptyRow: {
                                ruleId: -1,
                                enabled: true,
                                description: '',
                                helloInterval: 10,
                                deadInterval: 40,
                                retransmitInterval: 5,
                                transmitDelay: 1,
                                autoInterfaceCost: true,
                                authentication: 0,
                                routerPriority: 1,
                                javaClass: 'com.untangle.uvm.network.DynamicRouteOspfInterface',
                            },

                            bind: {
                                store: '{ospfInterfaces}',
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
                                header: 'Device',
                                width: Renderer.ipWidth,
                                flex: 1,
                                dataIndex: 'dev'
                            }, {
                                header: 'Interface',
                                width: Renderer.messageWidth,
                                flex: 1,
                                dataIndex: 'dev',
                                renderer: Ung.config.network.MainController.ospDeviceRenderer
                            }, {
                                header: 'Hello Interval',
                                width: Renderer.intervalWidth,
                                flex: 1,
                                dataIndex: 'helloInterval'
                            }, {
                                header: 'Dead Interval',
                                width: Renderer.intervalWidth,
                                flex: 1,
                                dataIndex: 'deadInterval'
                            }, {
                                header: 'Retransmit Interval',
                                width: Renderer.intervalWidth,
                                flex: 1,
                                dataIndex: 'retransmitInterval'
                            }, {
                                header: 'Transmit Delay',
                                width: Renderer.intervalWidth,
                                flex: 1,
                                dataIndex: 'transmitDelay'
                            }, {
                                header: 'Authentication',
                                width: Renderer.ipWidth,
                                flex: 1,
                                dataIndex: 'authentication',
                                renderer: Ung.config.network.MainController.ospfInterfaceAuthenticationRenderer
                            }, {
                                header: 'Router Priority',
                                width: Renderer.intervalWidth,
                                flex: 1,
                                dataIndex: 'routerPriority'
                            }],
                            editorFields: [
                                Field.enableRule(),
                                Field.description,
                            {
                                xtype: 'combo',
                                bind: {
                                    value: '{record.dev}',
                                    store: '{ospfDevices}'
                                },
                                displayField: 'comboValueField',
                                valueField: 'dev',
                                fieldLabel: 'Device'.t(),
                                queryMode: 'local',
                                editable: false,
                                allowBlank: false,
                                listeners: {
                                    beforerender: Ung.config.network.MainController.ospfInterfaceComboBeforeRender
                                },
                            },{
                                xtype: 'textfield',
                                fieldLabel: 'Hello Interval'.t(),
                                bind: '{record.helloInterval}',
                                vtype: 'routerInterval'
                            },{
                                xtype: 'textfield',
                                fieldLabel: 'Dead Interval'.t(),
                                bind: '{record.deadInterval}',
                                vtype: 'routerInterval'
                            },{
                                xtype: 'textfield',
                                fieldLabel: 'Retransmit Interval'.t(),
                                bind: '{record.retransmitInterval}',
                                vtype: 'routerInterval'
                            },{
                                xtype: 'textfield',
                                fieldLabel: 'Transmit Delay'.t(),
                                bind: '{record.transmitDelay}',
                                vtype: 'routerInterval'
                            },{
                                xtype: 'checkbox',
                                fieldLabel: 'Auto Interface Cost'.t(),
                                bind: '{record.autoInterfaceCost}'
                            },{
                                xtype: 'textfield',
                                fieldLabel: 'Interface Cost'.t(),
                                disabled: true,
                                hidden: true,
                                bind:{
                                    value: '{record.interfaceCost}',
                                    disabled: '{record.autoInterfaceCost}',
                                    hidden: '{record.autoInterfaceCost}'
                                },
                                vtype: 'routerInterval'
                            },{
                                xtype: 'combo',
                                bind: {
                                    value: '{record.authentication}',
                                    store: '{ospfAuthenticationTypes}'
                                },
                                displayField: 'type',
                                valueField: 'value',
                                fieldLabel: 'Authentication'.t(),
                                queryMode: 'local',
                                editable: false,
                                allowBlank: false
                            },{
                                xtype: 'textfield',
                                fieldLabel: 'Password'.t(),
                                disabled: true,
                                hidden: true,
                                allowBlank: false,
                                bind:{
                                    value: '{record.authenticationPassword}',
                                    disabled: '{record.authentication != 1}',
                                    hidden: '{record.authentication != 1}'
                                }
                            },{
                                xtype: 'textfield',
                                fieldLabel: 'Key ID'.t(),
                                disabled: true,
                                hidden: true,
                                allowBlank: false,
                                bind:{
                                    value: '{record.authenticationKeyId}',
                                    disabled: '{record.authentication != 2}',
                                    hidden: '{record.authentication != 2}'
                                }
                            },{
                                xtype: 'textfield',
                                fieldLabel: 'Key'.t(),
                                disabled: true,
                                hidden: true,
                                allowBlank: false,
                                bind:{
                                    value: '{record.authenticationKey}',
                                    disabled: '{record.authentication != 2}',
                                    hidden: '{record.authentication != 2}'
                                }
                            },{
                                xtype: 'textfield',
                                fieldLabel: 'Router Priority'.t(),
                                bind: '{record.routerPriority}',
                                vtype: 'routerPriority'
                            }]
                        },{
                            xtype: 'panel',
                            title: 'Advanced Options'.t(),
                            itemId: 'advanced_options',
                            padding: 10,

                            bind:{
                                disabled: '{!settings.dynamicRoutingSettings.ospfEnabled}'
                            },

                            defaults:{
                                margin: 10,
                                labelWidth: 200,
                            },
                            items:[{
                                xtype: 'textfield',
                                fieldLabel: 'Router ID'.t(),
                                bind: '{settings.dynamicRoutingSettings.ospfRouterId}',
                                emptyText: '[auto-generated]'.t(),
                                vtype: 'routerId'
                            },{
                                xtype: 'checkbox',
                                fieldLabel: 'Specify Default Metric'.t(),
                                bind: '{settings.dynamicRoutingSettings.ospfUseDefaultMetricEnabled}',
                            },{
                                xtype: 'textfield',
                                fieldLabel: 'Default Metric'.t(),
                                disabled: true,
                                hidden: true,
                                bind: {
                                    value: '{settings.dynamicRoutingSettings.ospfDefaultMetric}',
                                    disabled: '{!settings.dynamicRoutingSettings.ospfUseDefaultMetricEnabled}',
                                    hidden: '{!settings.dynamicRoutingSettings.ospfUseDefaultMetricEnabled}'
                                },
                                vtype: 'metric'
                            },{
                                xtype: 'combo',
                                bind: {
                                    value: '{settings.dynamicRoutingSettings.ospfAbrType}',
                                    store: '{ospfAbrTypes}'
                                },
                                displayField: 'type',
                                valueField: 'value',
                                fieldLabel: 'ABR Type'.t(),
                                emptyText: "[enter area]".t(),
                                queryMode: 'local',
                                editable: false,
                                allowBlank: false
                            },{
                                xtype: 'textfield',
                                fieldLabel: 'Auto cost reference bandwidth (MBits/s)'.t(),
                                bind: '{settings.dynamicRoutingSettings.ospfAutoCost}',
                                vtype: 'routerAutoCost'
                            },{
                                xtype: 'fieldset',
                                title: 'Default Information originate'.t(),

                                items:[{
                                    xtype: 'combo',
                                    bind: {
                                        value: '{settings.dynamicRoutingSettings.ospfDefaultInformationOriginateType}',
                                        store: '{ospfDefaultInformationOriginates}'
                                    },
                                    displayField: 'type',
                                    valueField: 'value',
                                    fieldLabel: 'Type'.t(),
                                    emptyText: "[enter area]".t(),
                                    queryMode: 'local',
                                    editable: false,
                                    allowBlank: false
                                },{
                                    xtype: 'textfield',
                                    fieldLabel: 'Metric'.t(),
                                    hidden: true,
                                    disabled: true,
                                    bind: {
                                        value: '{settings.dynamicRoutingSettings.ospfDefaultInformationOriginateMetric}',
                                        disabled: '{settings.dynamicRoutingSettings.ospfDefaultInformationOriginateType == 0}',
                                        hidden: '{settings.dynamicRoutingSettings.ospfDefaultInformationOriginateType == 0}'
                                    },
                                    vtype: 'metric'
                                },{
                                    xtype: 'combo',
                                    fieldLabel: 'Metric Type'.t(),
                                    hidden: true,
                                    disabled: true,
                                    bind: {
                                        value: '{settings.dynamicRoutingSettings.ospfDefaultInformationOriginateExternalType}',
                                        disabled: '{settings.dynamicRoutingSettings.ospfDefaultInformationOriginateType == 0}',
                                        hidden: '{settings.dynamicRoutingSettings.ospfDefaultInformationOriginateType == 0}',
                                        store: '{ospfMetricTypes}'
                                    },
                                    displayField: 'type',
                                    valueField: 'value',
                                    queryMode: 'local',
                                    editable: false,
                                    allowBlank: false
                                }]
                            },{
                                xtype: 'fieldset',
                                title: 'Redistribute Connected'.t(),
                                collapsed: true,

                                checkboxToggle: true,
                                checkbox: {
                                    bind: '{settings.dynamicRoutingSettings.ospfRedistConnectedEnabled}'
                                },
                                items:[{
                                    xtype: 'textfield',
                                    fieldLabel: 'Metric'.t(),
                                    bind: '{settings.dynamicRoutingSettings.ospfRedistConnectedMetric}',
                                    vtype: 'metric'
                                },{
                                    xtype: 'combo',
                                    fieldLabel: 'Metric Type'.t(),
                                    hidden: true,
                                    disabled: true,
                                    bind: {
                                        value: '{settings.dynamicRoutingSettings.ospfRedistConnectedExternalType}',
                                        disabled: '{settings.dynamicRoutingSettings.ospfRedistConnectedEnabled == 0}',
                                        hidden: '{settings.dynamicRoutingSettings.ospfRedistConnectedEnabled == 0}',
                                        store: '{ospfMetricTypes}'
                                    },
                                    displayField: 'type',
                                    valueField: 'value',
                                    queryMode: 'local',
                                    editable: false,
                                    allowBlank: false
                                }]
                            },{
                                xtype: 'fieldset',
                                title: 'Redistribute Static'.t(),
                                collapsed: true,

                                checkboxToggle: true,
                                checkbox: {
                                    bind: '{settings.dynamicRoutingSettings.ospfRedistStaticEnabled}'
                                },
                                items:[{
                                    xtype: 'textfield',
                                    fieldLabel: 'Metric'.t(),
                                    bind: '{settings.dynamicRoutingSettings.ospfRedistStaticMetric}',
                                    vtype: 'metric'
                                },{
                                    xtype: 'combo',
                                    fieldLabel: 'Metric Type'.t(),
                                    hidden: true,
                                    disabled: true,
                                    bind: {
                                        value: '{settings.dynamicRoutingSettings.ospfRedistStaticExternalType}',
                                        disabled: '{settings.dynamicRoutingSettings.ospfRedistStaticEnabled == 0}',
                                        hidden: '{settings.dynamicRoutingSettings.ospfRedistStaticEnabled == 0}',
                                        store: '{ospfMetricTypes}'
                                    },
                                    displayField: 'type',
                                    valueField: 'value',
                                    queryMode: 'local',
                                    editable: false,
                                    allowBlank: false
                                }]
                            },{
                                xtype: 'fieldset',
                                title: 'Redistribute BGP'.t(),
                                collapsed: true,
                                bind:{
                                    disabled: '{!settings.dynamicRoutingSettings.bgpEnabled}'
                                },

                                checkboxToggle: true,
                                checkbox: {
                                    bind: '{settings.dynamicRoutingSettings.ospfRedistBgpEnabled}'
                                },
                                items:[{
                                    xtype: 'textfield',
                                    fieldLabel: 'Metric'.t(),
                                    bind: '{settings.dynamicRoutingSettings.ospfRedistBgpMetric}',
                                    vtype: 'metric'
                                },{
                                    xtype: 'combo',
                                    fieldLabel: 'Metric Type'.t(),
                                    hidden: true,
                                    disabled: true,
                                    bind: {
                                        value: '{settings.dynamicRoutingSettings.ospfRedistBgpExternalType}',
                                        disabled: '{settings.dynamicRoutingSettings.ospfRedistBgpEnabled == 0}',
                                        hidden: '{settings.dynamicRoutingSettings.ospfRedistBgpEnabled == 0}',
                                        store: '{ospfMetricTypes}'
                                    },
                                    displayField: 'type',
                                    valueField: 'value',
                                    queryMode: 'local',
                                    editable: false,
                                    allowBlank: false
                                }]
                            }]
                        }]
                    }]
                }]
            }]
        }, {
            title: 'Global Block Page'.t(),
            itemId: 'global-block-page',
            scrollable: true,
            padding: 16,
            defaults: {
                labelAlign: 'right'
            },

            items: [{
                xtype: 'component',
                html: 'If enabled, the provided URL will override any App specific block page!',
                margin: '0 0 8 0'
            }, {
                xtype: 'checkbox',
                boxLabel: 'Global Custom Block Page Enabled'.t(),
                bind: '{settings.globalCustomBlockPageEnabled}',
            }, {
                xtype: 'textfield',
                fieldLabel: 'Global Custom Block Page URL'.t(),
                labelAlign: 'top',
                width: 400,
                bind: {
                    disabled: '{!settings.globalCustomBlockPageEnabled}',
                    value: '{settings.globalCustomBlockPageUrl}'
                },
                listeners: {
                    blur: function(el) {
                        var url = el.getValue();
                        if (!url.startsWith('http://') && !url.startsWith("https://")) {
                            el.setValue('http://' + url);
                        }
                    }
                }
            }]
        }]
    }]
});
