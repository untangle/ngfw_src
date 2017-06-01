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
            usersData: []
        },
        stores: {
            users: {
                data: '{usersData}',
                fields: [{
                    name: 'username',
                    type: 'string',
                    sortType: 'asUnString'
                }, {
                    name: 'creationTime',
                }, {
                    name: 'lastAccessTime',
                }, {
                    name: 'lastSessionTime',
                }, {
                    name: 'quotaSize',
                }, {
                    name: 'quotaRemaining',
                }, {
                    name: 'quotaIssueTime',
                }, {
                    name: 'quotaExpirationTime',
                }, {
                    name: 'tags'
                }]
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

    items: [{
        region: 'center',
        xtype: 'ungrid',
        itemId: 'usersgrid',
        reference: 'usersgrid',
        title: 'Current Users'.t(),
        stateful: true,

        // the view passed to the grid for accessing it's controller
        parentView: '#users',

        sortField: 'username',
        sortOrder: 'ASC',

        plugins: ['gridfilters'],
        columnLines: true,

        enableColumnHide: true,

        bind: '{users}',

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'delete'],
        emptyRow: {
            username: '',
            lastAccessTime: 0,
            lastSessionTime: 0,
            quotaSize: 0,
            quotaRemaining: 0,
            quotaIssueTime: 0,
            quotaExpirationTime: 0,
            tags: {
                javaClass: 'java.util.LinkedList',
                list: []
            },
            javaClass: 'com.untangle.uvm.UserTableEntry'
        },

        columns: [{
            header: 'Username'.t(),
            dataIndex: 'username',
            filter: {
                type: 'string'
            }
        }, {
            header: 'Creation Time'.t(),
            dataIndex: 'creationTime',
            rtype: 'timestamp',
            filter: {
                type: 'date'
            }
        }, {
            header: 'Last Access Time'.t(),
            dataIndex: 'lastAccessTime',
            rtype: 'timestamp',
            filter: {
                type: 'date'
            }
        }, {
            header: 'Last Session Time'.t(),
            dataIndex: 'lastSessionTime',
            rtype: 'timestamp',
            filter: {
                type: 'date'
            }
        }, {
            header: 'Quota'.t(),
            columns: [{
                header: 'Size'.t(),
                dataIndex: 'quotaSize',
                renderer: function (value) {
                    return value === 0 || value === '' ? '' : value;
                },
                filter: {
                    type: 'numeric'
                },
                rtype: 'datasize'
            }, {
                header: 'Remaining'.t(),
                dataIndex: 'quotaRemaining',
                filter: {
                    type: 'numeric'
                },
                rtype: 'datasize'
            }, {
                header: 'Issue Time'.t(),
                dataIndex: 'quotaIssueTime',
                filter: {
                    type: 'date'
                },
                rtype: 'timestamp'
            }, {
                header: 'Expiration Time'.t(),
                dataIndex: 'quotaExpirationTime',
                filter: {
                    type: 'date'
                },
                rtype: 'timestamp'
            }, {
                xtype: 'actioncolumn',
                width: 50,
                align: 'center',
                header: 'Refill'.t(),
                iconCls: 'fa fa-refresh fa-green',
                handler: 'externalAction',
                action: 'refillQuota'
            }, {
                xtype: 'actioncolumn',
                width: 50,
                align: 'center',
                header: 'Drop'.t(),
                iconCls: 'fa fa-minus-circle',
                handler: 'externalAction',
                action: 'dropQuota'
            }]
        }, {
            header: 'Tags'.t(),
            width: 300,
            xtype: 'widgetcolumn',
            tdCls: 'tag-cell',
            // flex: 1,
            widget: {
                xtype: 'tagpicker',
                bind: {
                    tags: '{record.tags}'
                }
            }
        }],
        editorFields: [{
            xtype: 'textfield',
            bind: '{record.username}',
            fieldLabel: 'Username'.t(),
            emptyText: '[enter Username]'.t(),
            allowBlank: false,
        }, {
            xtype: 'unitsfield',
            fieldLabel: 'Quota Size'.t(),
            minValue: 0,
            maxValue: 1024,
            units: [
                [ 1, 'B'.t() ],
                [ 1024, 'KB'.t() ],
                [ 1048576, 'MB'.t() ],
                [ 1073741824, 'GB'.t() ],
                [ 1099511627776, 'TB'.t() ],
                [ 1125899906842624, 'PB'.t() ]
            ],
            bind: {
                value: '{record.quotaSize}'
            }
        }]
    }, {
        region: 'east',
        xtype: 'unpropertygrid',
        title: 'User Details'.t(),
        itemId: 'details',
        collapsed: true,

        bind: {
            source: '{userDetails}'
        }
    }],
    tbar: [{
        xtype: 'button',
        text: 'Refresh'.t(),
        iconCls: 'fa fa-repeat',
        handler: 'getUsers'
    }, {
        xtype: 'button',
        text: 'Reset View'.t(),
        iconCls: 'fa fa-refresh',
        itemId: 'resetBtn',
        handler: 'resetView',
    }, '->', {
        xtype: 'button',
        text: 'View Reports'.t(),
        iconCls: 'fa fa-line-chart',
        href: '#reports/users',
        hrefTarget: '_self'
    }, {
        xtype: 'button',
        text: 'Help'.t(),
        iconCls: 'fa fa-question-circle',
        href: rpc.helpUrl + '?source=users&' + Util.getAbout()
    }],
    bbar: ['->', {
        text: '<strong>' + 'Save'.t() + '</strong>',
        iconCls: 'fa fa-floppy-o',
        handler: 'saveUsers'
    }]
});
