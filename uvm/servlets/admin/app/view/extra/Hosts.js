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

    defaults: {
        border: false
    },

    viewModel: {
        data: {
            autoRefresh: false,
            autoSort: true
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
        defaultSortable: true,

        enableColumnHide: true,

        viewConfig: {
            stripeRows: false,
            enableTextSelection: true,
            trackOver: false,
        },

        selModel: {
            type: 'rowmodel'
        },

        plugins: [ 'gridfilters', {
            ptype: 'cellediting',
            clicksToEdit: 1
        }],

        fields: [{
            name: 'address',
            type: 'string',
            sortType: 'asIp'
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
            sortType: 'asHostname'
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
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'usernameCaptivePortal',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'usernameDevice',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'usernameOpenVpn',
            type: 'string',
            sortType: 'asUnString'
        },{
            name: 'usernameIpsecVpn',
            type: 'string',
            sortType: 'asUnString'
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
                width: Renderer.macWidth,
                filter: Renderer.stringFilter
            },{
                header: 'Vendor'.t(),
                dataIndex: 'macVendor',
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            }]
        },{
            header: 'Interface'.t(),
            dataIndex: 'interfaceId',
            width: Renderer.messageWidth,
            filter: Renderer.stringFilter,
            renderer: Renderer.interface
        },{
            header: 'Creation Time'.t(),
            dataIndex: 'creationTime',
            width: Renderer.timestampWidth,
            hidden: true,
            renderer: Renderer.timestamp,
            filter: Renderer.timestampFilter
        },{
            header: 'Last Access Time'.t(),
            dataIndex: 'lastAccessTime',
            width: Renderer.timestampWidth,
            hidden: true,
            renderer: Renderer.timestamp,
            filter: Renderer.timestampFilter
        },{
            header: 'Last Session Time'.t(),
            dataIndex: 'lastSessionTime',
            width: Renderer.timestampWidth,
            hidden: true,
            renderer: Renderer.timestamp,
            filter: Renderer.timestampFilter
        },{
            header: 'Last Completed TCP Session Time'.t(),
            dataIndex: 'lastCompletedTcpSessionTime',
            width: Renderer.timestampWidth,
            hidden: true,
            renderer: Renderer.timestamp,
            filter: Renderer.timestampFilter
        },{
            header: 'Entitled Status'.t(),
            dataIndex: 'entitled',
            width: Renderer.booleanWidth,
            hidden: true,
            renderer: Renderer.boolean,
            filter: Renderer.booleanFilter
        },{
            header: 'Active'.t(),
            dataIndex: 'active',
            width: Renderer.booleanWidth,
            renderer: Renderer.boolean,
            filter: Renderer.booleanFilter
        },{
            header: '<i class="fa fa-pencil fa-gray"></i> ' + 'HTTP User Agent'.t(),
            dataIndex: 'httpUserAgent',
            width: Renderer.messageWidth,
            filter: Renderer.stringFilter,
            tdCls: 'editable-column',
            editor: {
                xtype: 'textfield',
                emptyText: ''
            }
        },{
            header: 'Captive Portal Authenticated'.t(),
            dataIndex: 'captivePortalAuthenticated',
            width: Renderer.booleanWidth,
            renderer: Renderer.boolean,
            filter: Renderer.booleanFilter
        },{
            header: '<i class="fa fa-pencil fa-gray"></i> ' + 'Tags'.t(),
            width: Renderer.tagsWidth,
            xtype: 'widgetcolumn',
            tdCls: 'tag-cell editable-column',
            widget: {
                xtype: 'tagpicker',
                bind: {
                    tags: '{record.tags}'
                }
            }
        },{
            header: 'Hostname'.t(),
            dataIndex: 'hostname',
            width: Renderer.hostnameWidth,
            filter: Renderer.stringFilter
        },{
            header: 'Hostname Source'.t(),
            dataIndex: 'hostnameSource',
            width: Renderer.messageWidth,
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Hostname (DHCP)'.t(),
            dataIndex: 'hostnameDhcp',
            width: Renderer.hostnameWidth,
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Hostname (DNS)'.t(),
            dataIndex: 'hostnameDns',
            width: Renderer.hostnameWidth,
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Hostname (Device)'.t(),
            dataIndex: 'hostnameDevice',
            width: Renderer.hostnameWidth,
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Hostname (Device Last Known)'.t(),
            dataIndex: 'hostnameDeviceLastKnown',
            width: Renderer.hostnameWidth,
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Hostname (OpenVPN)'.t(),
            dataIndex: 'hostnameOpenVpn',
            width: Renderer.hostnameWidth,
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Hostname (Reports)'.t(),
            dataIndex: 'hostnameReports',
            width: Renderer.hostnameWidth,
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Hostname (Directory Connector)'.t(),
            dataIndex: 'hostnameDirectoryConnector',
            width: Renderer.hostnameWidth,
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Username'.t(),
            dataIndex: 'username',
            width: Renderer.usernameWidth,
            filter: Renderer.stringFilter
        },{
            header: 'Username Source'.t(),
            dataIndex: 'usernameSource',
            width: Renderer.messageWidth,
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Username (Directory Connector)'.t(),
            dataIndex: 'usernameDirectoryConnector',
            width: Renderer.usernameWidth,
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Username (Captive Portal)'.t(),
            dataIndex: 'usernameCaptivePortal',
            width: Renderer.usernameWidth,
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Username (Device)'.t(),
            dataIndex: 'usernameDevice',
            width: Renderer.usernameWidth,
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Username (OpenVPN)'.t(),
            dataIndex: 'usernameOpenVpn',
            width: Renderer.usernameWidth,
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Username (IPsec VPN)'.t(),
            dataIndex: 'usernameIpsecVpn',
            width: Renderer.usernameWidth,
            hidden: true,
            filter: Renderer.stringFilter
        },{
            header: 'Quota'.t(),
            columns: [{
                header: 'Size'.t(),
                dataIndex: 'quotaSize',
                width: Renderer.sizeWidth,
                filter: Renderer.numericFilter,
                renderer: Renderer.datasizeoptional
            },{
                header: 'Remaining'.t(),
                dataIndex: 'quotaRemaining',
                width: Renderer.sizeWidth,
                filter: Renderer.numericFilter,
                renderer: Renderer.datasizeoptional
            },{
                header: 'Issue Time'.t(),
                dataIndex: 'quotaIssueTime',
                width: Renderer.timestampWidth,
                hidden: true,
                renderer: Renderer.timestamp,
                filter: Renderer.timestampFilter
            },{
                header: 'Expiration Time'.t(),
                dataIndex: 'quotaExpirationTime',
                width: Renderer.timestampWidth,
                hidden: true,
                renderer: Renderer.timestamp,
                filter: Renderer.timestampFilter
            }, {
                xtype: 'actioncolumn',
                width: Renderer.actionWidth,
                align: 'center',
                header: 'Refill Quota'.t(),
                menuText: 'Refill Quota'.t(),
                iconCls: 'fa fa-refresh fa-green',
                handler: 'externalAction',
                action: 'refillQuota'
            }, {
                xtype: 'actioncolumn',
                width: Renderer.actionWidth,
                align: 'center',
                header: 'Drop Quota'.t(),
                menuText: 'Drop Quota'.t(),
                iconCls: 'fa fa-minus-circle',
                handler: 'externalAction',
                action: 'dropQuota'
            }]
        }]
    }, {
        region: 'east',
        xtype: 'recorddetails',
        title: 'Host Details'.t(),
        itemId: 'details',
        width: 350,
        bind: {
            collapsed: '{!hostsgrid.selection}'
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
    }, '->', {
        xtype: 'button',
        text: 'View Reports'.t(),
        iconCls: 'fa fa-line-chart',
        href: '#reports?cat=hosts',
        hrefTarget: '_self',
        hidden: true,
        bind: {
            hidden: '{!reportsAppStatus.enabled}'
        }
    }],
    bbar: ['->', {
        text: '<strong>' + 'Save'.t() + '</strong>',
        iconCls: 'fa fa-floppy-o',
        handler: 'saveHosts'
    }]
});
