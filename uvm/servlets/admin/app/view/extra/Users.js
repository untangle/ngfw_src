Ext.define('Ung.view.extra.Users', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.users',

    /* requires-start */
    requires: [
        'Ung.view.extra.UsersController'
    ],
    /* requires-end */
    controller: 'users',

    viewModel: {
        data: {
            autoRefresh: false,
            usersData: []
        },
        stores: {
            users: {
                data: '{usersData}'
            }
        }
    },

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
            html: 'Current Users'.t()
        }])
    }],

    defaults: {
        border: false
    },

    // title: 'Current Sessions'.t(),

    items: [{
        region: 'center',
        xtype: 'grid',
        itemId: 'list',
        // store: 'sessions',

        sortField: 'username',
        sortOrder: 'ASC',

        plugins: 'gridfilters',
        columnLines: true,

        viewConfig: {
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/>No Data!</p>',
        },

        bind: '{users}',

        columns: [{
            header: 'Username'.t(),
            dataIndex: 'username',
            width: 200,
            filter: {
                type: 'string'
            }
        }, {
            header: 'Last Access Time'.t(),
            dataIndex: 'lastAccessTimeDate',
            width: 150,
            renderer: function (value, metaData, record) {
                var val = record.get('lastAccessTime');
                return val === 0 || val === '' ? '' : Util.timestampFormat(val);
            },
            filter: {
                type: 'date'
            }
        }, {
            header: 'Quota Size'.t(),
            dataIndex: 'quotaSize',
            width: 100,
            renderer: function (value) {
                return value === 0 || value === '' ? '' : value;
            },
            filter: {
                type: 'numeric'
            }
        }, {
            header: 'Quota Remaining'.t(),
            dataIndex: 'quotaRemaining',
            width: 150,
            filter: {
                type: 'numeric'
            }
        }, {
            header: 'Quota Issue Time'.t(),
            dataIndex: 'quotaIssueTimeDate',
            width: 150,
            renderer: function (value, metaData, record) {
                var val = record.get('quotaIssueTime');
                return val === 0 || val === '' ? '' : Util.timestampFormat(val);
            },
            filter: {
                type: 'date'
            }
        }, {
            header: 'Quota Expiration Time'.t(),
            dataIndex: 'quotaExpirationTimeDate',
            width: 150,
            renderer: function (value, metaData, record) {
                var val = record.get('quotaExpirationTime');
                return val === 0 || val === '' ? '' : Util.timestampFormat(val);
            },
            filter: {
                type: 'date'
            }
        }, {
            header: 'Tags'.t(),
            dataIndex: 'tagsString',
            flex: 1,
            filter: {
                type: 'string'
            }
        }]
    }],
    tbar: [{
        xtype: 'button',
        text: 'Refresh'.t(),
        iconCls: 'fa fa-repeat',
        handler: 'getUsers',
        bind: {
            disabled: '{autoRefresh}'
        }
    }, {
        xtype: 'button',
        text: 'Auto Refresh'.t(),
        iconCls: 'fa fa-refresh',
        enableToggle: true,
        toggleHandler: 'setAutoRefresh'
    }, '->', {
        xtype: 'button',
        text: 'View Reports'.t(),
        iconCls: 'fa fa-line-chart',
        href: '#reports/users',
        hrefTarget: '_self'
    }]
});
