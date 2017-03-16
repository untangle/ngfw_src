Ext.define('Ung.config.network.MainModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.config.network',

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
        iconName: 'icon_config_network',

        // si = selected interface (from grid)
        settings: null,
        // si: null,
        siStatus: null,
        siArp: null,
        inputFilterRulesSshEnabled: null,
        inputFilterRulesCount: 0,

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
            ['driver', 'Driver Defaults'.t()],
            ['pfifo', 'First In, First Out (pfifo)'.t()],
            ['fq_codel', 'Fair/Flow Queueing + Codel (fq_codel)'.t()],
            ['sfq', 'Stochastic Fairness Queueing (sfq)'.t()]
        ],

        upnpStatus: null
    },
    stores: {
        interfaces:         { data: '{settings.interfaces.list}' },
        interfaceArp:       { data: '{siArp}' },
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
        staticDhcpEntries:  { data: '{settings.staticDhcpEntries.list}' },
        // Advanced
        devices:            { data: '{settings.devices.list}' },
        qosPriorities:      { data: '{settings.qosSettings.qosPriorities.list}' },
        qosRules:           { data: '{settings.qosSettings.qosRules.list}' },
        forwardFilterRules: { data: '{settings.forwardFilterRules.list}' },
        inputFilterRules:   { data: '{settings.inputFilterRules.list}' },
        upnpRules:          { data: '{settings.upnpSettings.upnpRules.list}' },
        wanInterfaces: {
            source: '{interfaces}',
            filters: [{ property: 'configType', value: 'ADDRESSED' }, { property: 'isWan', value: true }]
        },
    }
});
