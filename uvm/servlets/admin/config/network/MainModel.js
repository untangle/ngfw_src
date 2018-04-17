Ext.define('Ung.config.network.MainModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.config-network',

    formulas: {
        // used in Interfaces view when showing/hiding interface specific configurations
        si: function (get) { return get('interfacesGrid.selection'); },
        fullHostName: function (get) {
            var domain = get('settings.domainName'),
                host = get('settings.hostName');
            if (domain !== null && domain !== '') {
                return host + '.' + domain;
            }
            return host;
        },

        qosPriorityNoDefaultStore: function (get) {
            return get('qosPriorityStore').slice(1);
        },
    },
    data: {
        title: 'Network'.t(),
        iconName: 'network',

        // si = selected interface (from grid)
        settings: null,
        // si: null,
        siStatus: null,
        siArp: null,
        accessRulesSshEnabled: null,
        accessRulesCount: 0,

        qosPriorityStore: [
            [0, 'Default'.t()],
            [1, 'Very High'.t()],
            [2, 'High'.t()],
            [3, 'Medium'.t()],
            [4, 'Low'.t()],
            [5, 'Limited'.t()],
            [6, 'Limited More'.t()],
            [7, 'Limited Severely'.t()]
        ],

        queueDisciplineStore: [
            ['pfifo', 'First In, First Out (pfifo)'.t()],
            ['fq_codel', 'Fair/Flow Queueing + Codel (fq_codel)'.t()],
            ['sfq', 'Stochastic Fairness Queueing (sfq)'.t()]
        ],

        upnpStatus: null
    },
    stores: {
        interfaces:         { model: 'Ung.model.Interface', data: '{settings.interfaces.list}', sorters: 'interfaceId' ,
            listeners: {
                datachanged: 'interfacesGridReconfigure'
            }
        },
        devInterfaces:      { source: '{interfaces}', filters: [{ property: 'isVlanInterface', value: false}] },
        interfaceArp:       {
            data: '{siArp}',
            fields: [{
                name: 'macAddress',
                type: 'string'
            }, {
                name: 'address',
                sortType: 'asIp'
            }],
            sorters: [{
                property: 'address',
                direction: 'ASC'
            }],
        },
        nextHopDevices:     { 
            data: '{nextHopDevicesList}',
            fields:[{
                name: 'key'
            },{
                name: 'value'
            }]
        },
        // Port Forward
        portForwardRules:   { data: '{settings.portForwardRules.list}' },
        // NAT
        natRules:           { data: '{settings.natRules.list}' },
        // Bypass
        bypassRules:        { data: '{settings.bypassRules.list}' },
        // Routes
        staticRoutes:       { data: '{settings.staticRoutes.list}' },
        // DNS
        staticDnsEntries:   { data: '{settings.dnsSettings.staticEntries.list}' },
        localServers:       { data: '{settings.dnsSettings.localServers.list}' },
        // DHCP
        staticDhcpEntries:  {
            data: '{settings.staticDhcpEntries.list}',
            fields:[{
                name: 'macAddress',
                type: 'string'
            }, {
                name: 'address',
                sortType: 'asIp'
            }, {
                name: 'description',
                type: 'string',
                sortType: 'asUnString'
            }]
        },
        dynamicDhcpEntries:  {
            data: [],
            fields:[{
                name: 'macAddress',
                type: 'string'
            }, {
                name: 'address',
                sortType: 'asIp'
            }, {
                name: 'hostname',
                type: 'string',
            }, {
                name: 'date',
                type: 'string',
            }]
        },
        // Advanced
        devices:            { data: '{settings.devices.list}' },
        qosPriorities:      { data: '{settings.qosSettings.qosPriorities.list}' },
        qosRules:           { data: '{settings.qosSettings.qosRules.list}' },
        qosStatistics: {
            data: [],
            fields: [{
                name: 'tokens'
            },{
                name: 'priority'
            },{
                name: 'rate'
            },{
                name: 'burst'
            },{
                name: 'ctokens'
            },{
                name: 'interface_name'
            },{
                name: 'sent'
            }],
            sorters: [{
                property: 'interface_name',
                direction: 'ASC'
            }],
        },
        filterRules:        { data: '{settings.filterRules.list}' },
        accessRules:        { data: '{settings.accessRules.list}' },
        upnpRules:          { data: '{settings.upnpSettings.upnpRules.list}' },
        upnpStatus: {
            data: [],
            fields: [{
                name: 'upnp_client_ip_address',
                sortType: 'asIp'
            },{
                name: 'upnp_destination_port'
            },{
                name: 'upnp_protocol',
                type: 'string'
            },{
                name:'upnp_client_port'
            },{
                name: 'bytes'
            }],
            sorters: [{
                property: 'upnp_client_ip_address',
                direction: 'ASC'
            }],
        },
        bgpNeighbors:       { data: '{settings.dynamicRoutingSettings.bgpNeighbors.list}' },
        bgpNetworks:        { data: '{settings.dynamicRoutingSettings.bgpNetworks.list}' },
        ospfNetworks:       { data: '{settings.dynamicRoutingSettings.ospfNetworks.list}' },
        wanInterfaces: {
            source: '{interfaces}',
            filters: [{ property: 'configType', value: 'ADDRESSED' }, { property: 'isWan', value: true }]
        },
        ospfAreas: {
            data: '{settings.dynamicRoutingSettings.ospfAreas.list}',
            fields:[{
                name: 'ruleId',
                type: 'integer'
            },{
                name: 'area',
                type: 'string'
            },{
                name: 'description',
                type: 'string'
            },{
                name: 'comboValueField',
                type: 'string',
                convert: function(v, record){
                    return record.get('area') + ' - ' + record.get('description');
                }
            }]
        },
        ospfInterfaces: {
            data: '{settings.dynamicRoutingSettings.ospfInterfaces.list}',
            fields:[{
                name: 'ruleId',
                type: 'integer'
            },{
                name: 'description',
                type: 'string'
            }]
        },
        ospfAreaTypes: {
            data: [{
                value: 0,
                type: 'Normal'.t()
            },{
                value: 1,
                type: 'Stub'.t()
            },{
                value: 2,
                type: 'Stub, no summary'.t()
            },{
                value: 3,
                type: 'Not so stubby'.t()
            },{
                value: 4,
                type: 'Not so stubby, no summary'.t()
            }]
        },
        ospfAuthenticationTypes: {
            data: [{
                value: 0,
                type: 'None'.t()
            },{
                value: 1,
                type: 'Text'.t()
            },{
                value: 2,
                type: 'MD5'.t()
            }]
        },
        ospfAbrTypes:{
            data: [{
                value: 0,
                type: 'Default'.t()
            },{
                value: 1,
                type: 'Cisco'.t()
            },{
                value: 2,
                type: 'IBM'.t()
            },{
                value: 3,
                type: 'Shortcut'.t()
            },{
                value: 4,
                type: 'Standard'.t()
            }]
        },
        ospfDevices:{
            data: [],
            fields:[{
                name: 'dev',
                type: 'string'
            },{
                name: 'interface',
                type: 'string'
            },{
                name: 'comboValueField',
                type: 'string',
                convert: function(v, record){
                    return record.get('dev') + ' - ' + record.get('interface');
                }
            }]
        },
        ospfDefaultInformationOriginates:{
            data: [{
                value: 0,
                type: 'Never'.t()
            },{
                value: 1,
                type: 'Regular'.t()
            },{
                value: 2,
                type: 'Always'.t()
            }]
        },
        ospfMetricTypes:{
            data: [{
                value: 1,
                type: 'Type 1'.t()
            },{
                value: 2,
                type: 'Type 2'.t()
            }]
        },
        dymamicRoutes: {
            data: [],
            fields: [{
                name: 'network',
                sortType: 'asIp'
            },{
                name: 'prefix',
                type: 'integer'
            },{
                name: 'via',
                sortType: 'asIp'
            },{
                name: 'dev',
                type: 'string'
            },{
                name: 'interface',
                type: 'string'
            },{
                name: 'attributes'
            }]
        },
        bgpStatus: {
            data: [],
            fields: [{
                name: 'neighbor',
                sortType: 'asIp'
            },{
                name: 'as',
                type: 'integer'
            },{
                name: 'msgsRecv',
                typ: 'integer'
            },{
                name: 'msgsSent',
                typ: 'integer'
            },{
                name: 'uptime',
                typ: 'integer'
            }]
        },
        ospfStatus: {
            data: [],
            fields: [{
                name: 'neighbor',
                sortType: 'asIp'
            },{
                name: 'address',
                sortType: 'asIp'
            },{
                name: 'time',
                type: 'float'
            },{
                name: 'dev',
                typ: 'string'
            },{
                name: 'interface',
                typ: 'string'
            }]
        }
    }
});
