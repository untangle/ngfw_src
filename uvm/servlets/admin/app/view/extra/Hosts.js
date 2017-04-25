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
        formulas: {
            hostDetails: function (get) {
                if (get('hostsgrid.selection')) {
                    var data = get('hostsgrid.selection').getData();
                    delete data._id;
                    delete data.javaClass;
                    return data;
                }
                return;
            }
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
        forceFit: false,
        viewConfig: {
            stripeRows: true,
            enableTextSelection: true
        },

        columns: [{
            header: 'Address'.t(),
            dataIndex: 'address',
            resizable: false
        },{
            header: 'MAC Address'.t(),
            dataIndex: 'macAddress'
        },{
            header: 'MAC Vendor'.t(),
            dataIndex: 'macVendor'
        },{
            header: 'Interface'.t(),
            dataIndex: 'interfaceId',
            rtype: 'interface'
        },{
            header: 'Creation Time'.t(),
            dataIndex: 'creationTimeDate',
            hidden: true,
            rtype: 'timestamp'
        },{
            header: 'Last Access Time'.t(),
            dataIndex: 'lastAccessTime',
            hidden: true,
            rtype: 'timestamp'
        },{
            header: 'Last Session Time'.t(),
            dataIndex: 'lastSessionTime',
            hidden: true,
            rtype: 'timestamp'
        },{
            header: 'Last Completed TCP Session Time'.t(),
            dataIndex: 'lastCompletedTcpSessionTime',
            hidden: true,
            rtype: 'timestamp'
        },{
            header: 'Entitled Status'.t(),
            dataIndex: 'entitled',
            hidden: true
        },{
            header: 'Active'.t(),
            dataIndex: 'active',
            width: 80,
            rtype: 'boolean'
        },{
            header: 'HTTP User Agent'.t(),
            dataIndex: 'httpUserAgent'
        },{
            header: 'Captive Portal Authenticated'.t(),
            dataIndex: 'captivePortalAuthenticated'
        },{
            header: 'Tags'.t(),
            dataIndex: 'tags',
            rtype: 'tags'
        },{
            header: 'Tags String'.t(),
            dataIndex: 'tagsString'
        },{
            header: 'Hostname'.t(),
            dataIndex: 'hostname'
        },{
            header: 'Hostname Source'.t(),
            dataIndex: 'hostnameSource',
            hidden: true
        },{
            header: 'Hostname (DHCP)'.t(),
            dataIndex: 'hostnameDhcp',
            hidden: true
        },{
            header: 'Hostname (DNS)'.t(),
            dataIndex: 'hostnameDns',
            hidden: true
        },{
            header: 'Hostname (Device)'.t(),
            dataIndex: 'hostnameDevice',
            hidden: true
        },{
            header: 'Hostname (Device Last Known)'.t(),
            dataIndex: 'hostnameDeviceLastKnown',
            hidden: true
        },{
            header: 'Hostname (OpenVPN)'.t(),
            dataIndex: 'hostnameOpenVpn',
            hidden: true
        },{
            header: 'Hostname (Reports)'.t(),
            dataIndex: 'hostnameReports',
            hidden: true
        },{
            header: 'Hostname (Directory Connector)'.t(),
            dataIndex: 'hostnameDirectoryConnector',
            hidden: true
        },{
            header: 'Username'.t(),
            dataIndex: 'username'
        },{
            header: 'Username Source'.t(),
            dataIndex: 'usernameSource',
            hidden: true
        },{
            header: 'Username (Directory Connector)'.t(),
            dataIndex: 'usernameDirectoryConnector',
            hidden: true
        },{
            header: 'Username (Captive Porrtal)'.t(),
            dataIndex: 'usernameCaptivePortal',
            hidden: true
        },{
            header: 'Username (Device)'.t(),
            dataIndex: 'usernameDevice',
            hidden: true
        },{
            header: 'Username (OpenVPN)'.t(),
            dataIndex: 'usernameOpenVpn',
            hidden: true
        },{
            header: 'Username (IPsec VPN)'.t(),
            dataIndex: 'usernameIpsecVpn',
            hidden: true
        },{
            header: 'Quota'.t(),
            columns: [{
                header: 'Size'.t(),
                dataIndex: 'quotaSize'
            },{
                header: 'Remaining'.t(),
                dataIndex: 'quotaRemaining'
            },{
                header: 'Issue Time'.t(),
                dataIndex: 'quotaIssueTime',
                hidden: true
            },{
                header: 'Expiration Time'.t(),
                dataIndex: 'quotaExpirationTime',
                hidden: true,
                rtype: 'timestamp'
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
        iconCls: 'fa fa-refresh',
        enableToggle: true,
        toggleHandler: 'setAutoRefresh'
    }, {
        xtype: 'button',
        text: 'Reset View'.t(),
        iconCls: 'fa fa-refresh',
        itemId: 'resetBtn',
        handler: 'resetView',
    }, '-', 'Filter:'.t(), {
        xtype: 'textfield',
        checkChangeBuffer: 200
    }, '->', {
        xtype: 'button',
        text: 'View Reports'.t(),
        iconCls: 'fa fa-line-chart',
        href: '#reports/hosts',
        hrefTarget: '_self'
    }]
});
