Ext.define('Ung.config.network.view.DynamicRouting', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-network-dynamicrouting',
    itemId: 'dynamic_routing',
    scrollable: true,
    withValidation: false,
    viewModel: true,

    title: 'Dynamic Routing'.t(),
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
                allowBlank: false,
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
                allowBlank: false,
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
                    recordActions: ['edit', 'copy', 'delete'],
                    copyId: 'ruleId',  
                    copyAppendField: 'description',

                    emptyText: 'No BGP Neighbors defined'.t(),

                    importValidationJavaClass: true,

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
                    recordActions: ['edit', 'copy', 'delete'],
                    copyId: 'ruleId',  
                    copyAppendField: 'description',

                    emptyText: 'No BGP Networks defined'.t(),

                    importValidationJavaClass: true,

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
                    recordActions: ['edit', 'copy', 'delete'],
                    copyId: 'ruleId',  
                    copyAppendField: 'description',

                    emptyText: 'No OSPF Networks defined'.t(),

                    importValidationJavaClass: true,

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
                    recordActions: ['edit', 'copy', 'delete'],
                    copyId: 'ruleId',  
                    copyAppendField: 'description',

                    emptyText: 'No OSPF Areas defined'.t(),

                    importValidationJavaClass: true,

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
                        vtype: 'routerArea',
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
                                field1: '',
                            },
                            
                            importValidationJavaClass: true,

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
                    recordActions: ['edit', 'copy', 'delete'],
                    copyId: 'ruleId',  
                    copyAppendField: 'description',

                    emptyText: 'No OSPF Interfaces defined'.t(),

                    importValidationJavaClass: true,

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
});
