Ext.define('Ung.view.shd.Hosts', {
    extend: 'Ext.tab.Panel',
    xtype: 'ung.hosts',
    // layout: 'border',
    requires: [
        'Ung.view.shd.HostsController'
    ],

    controller: 'hosts',

    layout: 'border',

    defaults: {
        border: false
    },

    items: [{
        xtype: 'grid',
        itemId: 'hostsgrid',
        title: 'Current Hosts'.t(),
        store: 'hosts',
        columns: [
            { header: 'IP'.t(), dataIndex: 'address' },
            { header: 'MAC Address'.t(), dataIndex: 'macAddress' },
            { header: 'MAC Vendor'.t(), dataIndex: 'macVendor' },
            { header: 'Interface'.t(), dataIndex: 'interfaceId' },
            { header: 'Last Access Time'.t(), dataIndex: 'lastAccessTimeDate', hidden: true },
            { header: 'Last Session Time'.t(), dataIndex: 'lastSessionTimeDate', hidden: true },
            { header: 'Last Completed TCP Session Time'.t(), dataIndex: 'lastCompletedTcpSessionTime', hidden: true },
            { header: 'Entitled Status'.t(), dataIndex: 'entitledStatus', hidden: true },
            { header: 'Active'.t(), dataIndex: 'active' },
            { header: 'Hostname'.t(), dataIndex: 'hostname' },
            { header: 'User Name'.t(), dataIndex: 'username' },
            {
                header: 'Penalty'.t(),
                columns: [
                    { header: 'Boxed'.t(), dataIndex: 'penaltyBoxed' },
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
        title: 'Penalty Box Hosts'.t(),
        html: 'penalty hosts'
    }, {
        title: 'Current Quotas'.t(),
        html: 'quotas'
    }, {
        title: 'Reports'.t(),
        html: 'reports'
    }]
});