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
                // console.log(get('hostsgrid.selection').getData());
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
        xtype: 'grid',
        region: 'center',
        itemId: 'hostsgrid',
        reference: 'hostsgrid',
        title: 'Current Hosts'.t(),
        store: 'hosts',
        forceFit: true,
        columns: [
            { header: 'Address'.t(), dataIndex: 'address', resizable: false },
            { header: 'MAC Address'.t(), dataIndex: 'macAddress' },
            { header: 'MAC Vendor'.t(), dataIndex: 'macVendor' },
            { header: 'Interface'.t(), dataIndex: 'interfaceId' },
            { header: 'Creation Time'.t(), dataIndex: 'creationTimeDate', hidden: true },
            { header: 'Last Access Time'.t(), dataIndex: 'lastAccessTimeDate', hidden: true },
            { header: 'Last Session Time'.t(), dataIndex: 'lastSessionTimeDate', hidden: true },
            { header: 'Last Completed TCP Session Time'.t(), dataIndex: 'lastCompletedTcpSessionTime', hidden: true },
            { header: 'Entitled Status'.t(), dataIndex: 'entitled', hidden: true },
            { header: 'Active'.t(), dataIndex: 'active', width: 80, renderer: 'boolRenderer' },
            { header: 'HTTP User Agent'.t(), dataIndex: 'httpUserAgent' },
            { header: 'Captive Portal Authenticated'.t(), dataIndex: 'captivePortalAuthenticated' },
            { header: 'Tags'.t(), dataIndex: 'tags' },
            { header: 'Tags String'.t(), dataIndex: 'tagsString' },
            { header: 'Hostname'.t(), dataIndex: 'hostname' },
            { header: 'Hostname Source'.t(), dataIndex: 'hostnameSource', hidden: true },
            { header: 'Hostname (DHCP)'.t(), dataIndex: 'hostnameDhcp', hidden: true },
            { header: 'Hostname (DNS)'.t(), dataIndex: 'hostnameDns', hidden: true },
            { header: 'Hostname (Device)'.t(), dataIndex: 'hostnameDevice', hidden: true },
            { header: 'Hostname (Device Last Known)'.t(), dataIndex: 'hostnameDeviceLastKnown', hidden: true },
            { header: 'Hostname (OpenVPN)'.t(), dataIndex: 'hostnameOpenVpn', hidden: true },
            { header: 'Hostname (Reports)'.t(), dataIndex: 'hostnameReports', hidden: true },
            { header: 'Hostname (Directory Connector)'.t(), dataIndex: 'hostnameDirectoryConnector', hidden: true },
            { header: 'Username'.t(), dataIndex: 'username' },
            { header: 'Username Source'.t(), dataIndex: 'usernameSource', hidden: true },
            { header: 'Username (Directory Connector)'.t(), dataIndex: 'usernameDirectoryConnector', hidden: true },
            { header: 'Username (Captive Porrtal)'.t(), dataIndex: 'usernameCaptivePortal', hidden: true },
            { header: 'Username (Device)'.t(), dataIndex: 'usernameDevice', hidden: true },
            { header: 'Username (OpenVPN)'.t(), dataIndex: 'usernameOpenVpn', hidden: true },
            { header: 'Username (IPsec VPN)'.t(), dataIndex: 'usernameIpsecVpn', hidden: true},
            {
                header: 'Quota'.t(),
                columns: [
                    { header: 'Size'.t(), dataIndex: 'quotaSize' },
                    { header: 'Remaining'.t(), dataIndex: 'quotaRemaining' },
                    { header: 'Issue Time'.t(), dataIndex: 'quotaIssueTime', hidden: true },
                    { header: 'Expiration Time'.t(), dataIndex: 'quotaExpirationTime', hidden: true }
                ]
            }
        ]
    }, {
        region: 'east',
        xtype: 'propertygrid',
        itemId: 'details',
        editable: false,
        width: 400,
        split: true,
        collapsible: false,
        resizable: true,
        shadow: false,
        hidden: true,

        cls: 'prop-grid',

        viewConfig: {
            stripeRows: false,
            getRowClass: function(record) {
                if (record.get('value') === null || record.get('value') === '') {
                    return 'empty';
                }
                return;
            }
        },

        nameColumnWidth: 200,
        bind: {
            title: '{hostsgrid.selection.hostname} ({hostsgrid.selection.address})',
            source: '{hostDetails}',
            hidden: '{!hostsgrid.selection}'
        },
        sourceConfig: {
            active:                      { displayName: 'Active'.t(), renderer: 'boolRenderer' },
            address:                     { displayName: 'Address'.t() },
            captivePortalAuthenticated:  { displayName: 'Captive Portal Authenticated'.t(), renderer: 'boolRenderer' },
            creationTime:                { displayName: 'Creation Time'.t(), renderer: 'timestampRenderer' },
            entitled:                    { displayName: 'Entitled'.t(), renderer: 'boolRenderer' },
            hostname:                    { displayName: 'Hostname'.t() },
            hostnameDhcp:                { displayName: 'Hostname'.t() + ' ' + '(DHCP)' },
            hostnameDns:                 { displayName: 'Hostname'.t() + ' ' + '(DNS)' },
            hostnameDevice:              { displayName: 'Hostname'.t() + ' ' + '(Device)'.t() },
            hostnameDeviceLastKnown:     { displayName: 'Hostname'.t() + ' ' + '(Device Last Known)'.t() },
            hostnameOpenVpn:             { displayName: 'Hostname'.t() + ' ' + '(OpenVPN)' },
            hostnameReports:             { displayName: 'Hostname'.t() + ' ' + '(Reports)' },
            hostnameDirectoryConnector:  { displayName: 'Hostname'.t() + ' ' + '(Directory Connector)' },
            hostnameSource:              { displayName: 'Hostname Source'.t() },
            httpUserAgent:               { displayName: 'HTTP'.t() + ' - ' + 'User Agent'.t() },
            interfaceId:                 { displayName: 'Interface Id'.t() },
            lastAccessTime:              { displayName: 'Last Access Time'.t(), renderer: 'timestampRenderer' },
            lastCompletedTcpSessionTime: { displayName: 'Last Completed TCP Session Time'.t(), renderer: 'timestampRenderer' },
            lastSessionTime:             { displayName: 'Last Session Time'.t(), renderer: 'timestampRenderer' },
            macAddress:                  { displayName: 'MAC Address'.t() },
            macVendor:                   { displayName: 'MAC Vendor'.t() },
            tags:                        { displayName: 'Tags'.t() },
            tagsString:                  { displayName: 'Tags String'.t() },
            quotaExpirationTime:         { displayName: 'Quota Expiration Time'.t(), renderer: 'timestampRenderer' },
            quotaIssueTime:              { displayName: 'Quota Issue Time'.t(), renderer: 'timestampRenderer' },
            quotaRemaining:              { displayName: 'Quota Remaining'.t() },
            quotaSize:                   { displayName: 'Quota Size'.t() },
            username:                    { displayName: 'Username'.t() },
            usernameDirectoryConnector:  { displayName: 'Username'.t() + ' ' + '(Directory Connector)' },
            usernameCaptivePortal:       { displayName: 'Username'.t() + ' ' + '(Captive Portal)' },
            usernameDevice:              { displayName: 'Username'.t() + ' ' + '(Device)'.t() },
            usernameOpenVpn:             { displayName: 'Username'.t() + ' ' + '(OpenVPN)' },
            usernameIpsecVpn:            { displayName: 'Username'.t() + ' ' + '(IPsec VPN)' },
            usernameSource:              { displayName: 'Username Source'.t() }
        },
        listeners: {
            beforeedit: function () {
                return false;
            }
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
