Ext.define('Ung.view.extra.Hosts', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.hosts',

    /* requires-start */
    requires: [
        'Ung.view.extra.HostsController'
    ],
    /* requires-end */
    controller: 'hosts',

    layout: 'border',

    dockedItems: [{
        xtype: 'toolbar',
        ui: 'navigation',
        dock: 'top',
        border: false,
        style: {
            background: '#333435',
            zIndex: 9997
        },
        defaults: {
            xtype: 'button',
            border: false,
            hrefTarget: '_self'
        },
        items: Ext.Array.insert(Ext.clone(Util.subNav), 0, [{
            xtype: 'component',
            margin: '0 0 0 10',
            style: {
                color: '#CCC'
            },
            html: 'Current Hosts'.t()
        }])
    }],

    defaults: {
        border: false
    },

    viewModel: {
        data: {
            autoRefresh: false
        }
    },

    items: [{
        xtype: 'ungrid',

        region: 'center',
        itemId: 'hostsgrid',
        reference: 'hostsgrid',
        title: 'Current Hosts'.t(),
        store: 'hosts',
        stateful: true,

        enableColumnHide: true,

        viewConfig: {
            stripeRows: true,
            enableTextSelection: true
        },

        plugins: ['gridfilters'],

        fields: [{
            name: 'address',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'macAddress',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'macVendor',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'interfaceId',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'creationTime',
            sortType: 'asTimestamp'
        },{
            name: 'lastAccessTime',
            sortType: 'asTimestamp'
        },{
            name: 'lastSessionTime',
            sortType: 'asTimestamp'
        },{
            name: 'lastCompletedTcpSessionTime',
            sortType: 'asTimestamp'
        },{
            name: 'entitled',
        },{
            name: 'active',
        },{
            name: 'httpUserAgent',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'captivePortalAuthenticated',
        },{
            name: 'tags',
        },{
            name: 'hostname',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'hostnameSource',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'hostnameDhcp',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'hostnameDns',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'hostnameDevice',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'hostnameDeviceLastKnown',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'hostnameOpenVpn',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'hostnameReports',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'hostnameDirectoryConnector',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'username',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'usernameSource',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'usernameDirectoryConnector',
        },{
            name: 'usernameCaptivePortal',
        },{
            name: 'usernameDevice',
        },{
            name: 'usernameOpenVpn',
        },{
            name: 'usernameIpsecVpn',
        },{
            name: 'quotaSize',
        },{
            name: 'quotaRemaining',
        },{
            name: 'quotaIssueTime',
            sortType: 'asTimestamp'
        },{
            name: 'quotaExpirationTime',
            sortType: 'asTimestamp'
        }],

        columns: [{
            header: 'Address'.t(),
            dataIndex: 'address',
            filter: Renderer.stringFilter
        },{
            header: 'MAC'.t(),
            columns:[{
                header: 'Address'.t(),
                dataIndex: 'macAddress',
                filter: Renderer.stringFilter
            },{
                header: 'Vendor'.t(),
                dataIndex: 'macVendor',
                filter: Renderer.stringFilter
            }]
        },{
            header: 'Interface'.t(),
            dataIndex: 'interfaceId',
            filter: Renderer.stringFilter,
            rtype: 'interface'
        },{
            header: 'Creation Time'.t(),
            dataIndex: 'creationTime',
            width: Renderer.timestampWidth,
            hidden: true,
            rtype: 'timestamp',
            filter: Renderer.timestampFilter
        },{
            header: 'Last Access Time'.t(),
            dataIndex: 'lastAccessTime',
            width: Renderer.timestampWidth,
            hidden: true,
            rtype: 'timestamp',
            filter: Renderer.timestampFilter
        },{
            header: 'Last Session Time'.t(),
            dataIndex: 'lastSessionTime',
            width: Renderer.timestampWidth,
            hidden: true,
            rtype: 'timestamp',
            filter: Renderer.timestampFilter
        },{
            header: 'Last Completed TCP Session Time'.t(),
            dataIndex: 'lastCompletedTcpSessionTime',
            width: Renderer.timestampWidth,
            hidden: true,
            rtype: 'timestamp',
            filter: Renderer.timestampFilter
        },{
            header: 'Entitled Status'.t(),
            dataIndex: 'entitled',
            hidden: true,
            rtype: 'boolean',
            filter: Renderer.booleanFilter
        },{
            header: 'Active'.t(),
            dataIndex: 'active',
            width: 80,
            rtype: 'boolean',
            filter: Renderer.booleanFilter
        },{
            header: 'HTTP User Agent'.t(),
            dataIndex: 'httpUserAgent',
            filter: Renderer.stringFilter
        },{
            header: 'Captive Portal Authenticated'.t(),
            dataIndex: 'captivePortalAuthenticated',
            rtype: 'boolean',
            filter: Renderer.booleanFilter
        },{
            header: 'Tags'.t(),
            dataIndex: 'tags',
            rtype: 'tags'
        },{
            header: 'Hostname'.t(),
            dataIndex: 'hostname',
            filter: Renderer.stringFilter
        },{
            header: 'Hostname Source'.t(),
            dataIndex: 'hostnameSource',
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Hostname (DHCP)'.t(),
            dataIndex: 'hostnameDhcp',
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Hostname (DNS)'.t(),
            dataIndex: 'hostnameDns',
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Hostname (Device)'.t(),
            dataIndex: 'hostnameDevice',
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Hostname (Device Last Known)'.t(),
            dataIndex: 'hostnameDeviceLastKnown',
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Hostname (OpenVPN)'.t(),
            dataIndex: 'hostnameOpenVpn',
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Hostname (Reports)'.t(),
            dataIndex: 'hostnameReports',
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Hostname (Directory Connector)'.t(),
            dataIndex: 'hostnameDirectoryConnector',
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Username'.t(),
            dataIndex: 'username',
            filter: Renderer.stringFilter
        },{
            header: 'Username Source'.t(),
            dataIndex: 'usernameSource',
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Username (Directory Connector)'.t(),
            dataIndex: 'usernameDirectoryConnector',
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Username (Captive Portal)'.t(),
            dataIndex: 'usernameCaptivePortal',
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Username (Device)'.t(),
            dataIndex: 'usernameDevice',
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Username (OpenVPN)'.t(),
            dataIndex: 'usernameOpenVpn',
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Username (IPsec VPN)'.t(),
            dataIndex: 'usernameIpsecVpn',
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Quota'.t(),
            columns: [{
                header: 'Size'.t(),
                dataIndex: 'quotaSize',
                filter: Renderer.numericFilter,
                rtype: 'datasize'
            },{
                header: 'Remaining'.t(),
                dataIndex: 'quotaRemaining',
                filter: Renderer.numericFilter,
                rtype: 'datasize'
            },{
                header: 'Issue Time'.t(),
                dataIndex: 'quotaIssueTime',
                width: Renderer.timestampWidth,
                hidden: true,
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            },{
                header: 'Expiration Time'.t(),
                dataIndex: 'quotaExpirationTime',
                width: Renderer.timestampWidth,
                hidden: true,
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                xtype: 'actioncolumn',
                width: 80,
                align: 'center',
                header: 'Refill Quota'.t(),
                iconCls: 'fa fa-refresh fa-green',
                handler: 'externalAction',
                action: 'refillQuota'
            }, {
                xtype: 'actioncolumn',
                width: 80,
                align: 'center',
                header: 'Drop Quota'.t(),
                iconCls: 'fa fa-minus-circle',
                handler: 'externalAction',
                action: 'dropQuota'
            }]
        }]
    }, {
        region: 'east',
        xtype: 'unpropertygrid',
        title: 'Host Details'.t(),
        itemId: 'details',

        bind: {
            source: '{hostDetails}'
        }
    }],
    tbar: [{
        xtype: 'button',
        text: 'Refresh'.t(),
        iconCls: 'fa fa-repeat',
        handler: 'getHosts',
        bind: {
            disabled: '{autoRefresh}'
        }
    }, {
        xtype: 'button',
        text: 'Auto Refresh'.t(),
        bind: {
            iconCls: '{autoRefresh ? "fa fa-check-square-o" : "fa fa-square-o"}'
        },
        enableToggle: true,
        toggleHandler: 'setAutoRefresh'
    }, {
        xtype: 'button',
        text: 'Reset View'.t(),
        iconCls: 'fa fa-refresh',
        itemId: 'resetBtn',
        handler: 'resetView',
    },
    '-', {
        xtype: 'ungridfilter'
    },{
        xtype: 'ungridstatus',
        tplFiltered: '{0} filtered, {1} total hosts'.t(),
        tplUnfiltered: '{0} hosts'.t()
    }, '->', {
        xtype: 'button',
        text: 'View Reports'.t(),
        iconCls: 'fa fa-line-chart',
        href: '#reports/hosts',
        hrefTarget: '_self'
    }]
});
