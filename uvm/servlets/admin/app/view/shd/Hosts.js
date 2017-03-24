Ext.define('Ung.view.shd.Hosts', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.hosts',

    /* requires-start */
    requires: [
        'Ung.view.shd.HostsController'
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
            html: '<strong>' + 'Current Hosts'.t() + '</strong>'
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
            { header: 'IP'.t(), dataIndex: 'address', resizable: false },
            { header: 'MAC Address'.t(), dataIndex: 'macAddress' },
            { header: 'MAC Vendor'.t(), dataIndex: 'macVendor' },
            { header: 'Interface'.t(), dataIndex: 'interfaceId' },
            { header: 'Last Access Time'.t(), dataIndex: 'lastAccessTimeDate', hidden: true },
            { header: 'Last Session Time'.t(), dataIndex: 'lastSessionTimeDate', hidden: true },
            { header: 'Last Completed TCP Session Time'.t(), dataIndex: 'lastCompletedTcpSessionTime', hidden: true },
            { header: 'Entitled Status'.t(), dataIndex: 'entitledStatus', hidden: true },
            { header: 'Active'.t(), dataIndex: 'active', width: 80, renderer: 'boolRenderer' },
            { header: 'Hostname'.t(), dataIndex: 'hostname' },
            { header: 'User Name'.t(), dataIndex: 'username' },
            {
                header: 'Penalty'.t(),
                columns: [
                    { header: 'Boxed'.t(), dataIndex: 'penaltyBoxed', renderer: 'boolRenderer' },
                    { header: 'Entry Time'.t(), dataIndex: 'penaltyBoxEntryTime', hidden: true },
                    { header: 'Exit Time'.t(), dataIndex: 'penaltyBoxExitTime', hidden: true }
                ]
            }, {
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
            hostnameKnown:               { displayName: 'Hostname Known'.t(), renderer: 'boolRenderer' },
            httpUserAgent:               { displayName: 'HTTP'.t() + ' - ' + 'User Agent'.t() },
            interfaceId:                 { displayName: 'Interface Id'.t() },
            lastAccessTime:              { displayName: 'Last Access Time'.t(), renderer: 'timestampRenderer' },
            lastCompletedTcpSessionTime: { displayName: 'Last Completed TCP Session Time'.t(), renderer: 'timestampRenderer' },
            lastSessionTime:             { displayName: 'Last Session Time'.t(), renderer: 'timestampRenderer' },
            macAddress:                  { displayName: 'MAC Address'.t() },
            macVendor:                   { displayName: 'MAC Vendor'.t() },
            penaltyBoxed:                { displayName: 'Penalty Boxed'.t(), renderer: 'boolRenderer' },
            penaltyBoxEntryTime:         { displayName: 'Penalty Box Entry Time'.t(), renderer: 'timestampRenderer' },
            penaltyBoxExitTime:          { displayName: 'Penalty Box Exit Time'.t(), renderer: 'timestampRenderer' },
            quotaExpirationTime:         { displayName: 'Quota Expiration Time'.t(), renderer: 'timestampRenderer' },
            quotaIssueTime:              { displayName: 'Quota Issue Time'.t(), renderer: 'timestampRenderer' },
            quotaRemaining:              { displayName: 'Quota Remaining'.t() },
            quotaSize:                   { displayName: 'Quota Size'.t() },
            username:                    { displayName: 'Username'.t() },
            usernameAdConnector:         { displayName: 'Username Ad Connector'.t() },
            usernameCapture:             { displayName: 'Username Capture'.t() },
            usernameDevice:              { displayName: 'Username Device'.t() },
            usernameOpenvpn:             { displayName: 'Username OpenVPN'.t() },
            usernameSource:              { displayName: 'Username Source'.t() },
            usernameTunnel:              { displayName: 'Username Tunnel'.t() }
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
    }]
});
