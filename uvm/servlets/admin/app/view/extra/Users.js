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
                data: '{usersData}'
            }
        }
        // formulas: {
        //     userDetails: function (get) {
        //         this.userDetailsGet = get;
        //         this.getView().down('#usersgrid').getSelectionModel().select(0);
        //         this.userDetailsSelectTask = new Ext.util.DelayedTask( Ext.bind(function(){
        //             this.getView().down('#usersgrid').getSelectionModel().select(0);
        //             if( !this.userDetailsGet('usersgrid.selection') ){
        //                 this.userDetailsSelectTask.delay(100);
        //             }
        //         }, this) );
        //         this.userDetailsSelectTask.delay(100);
        //         if (get('usersgrid.selection')) {
        //             var data = get('usersgrid.selection').getData();
        //             delete data._id;
        //             delete data.javaClass;
        //             for( var k in data ){
        //                 /*
        //                  * Encode objects and arrays for details
        //                  */
        //                 if( ( typeof( data[k] ) == 'object' ) ||
        //                     ( typeof( data[k] ) == 'array' ) ){
        //                     data[k] = Ext.encode(data[k]);
        //                 }
        //             }
        //             return data;
        //         }
        //         return;
        //     }
        // }
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
        forceFit: false,

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
            width: 200,
            dataIndex: 'username',
            filter: {
                type: 'string'
            }
        }, {
            header: 'Creation Time'.t(),
            width: 160,
            dataIndex: 'creationTime',
            rtype: 'timestamp',
            filter: {
                type: 'date'
            }
        }, {
            header: 'Last Access Time'.t(),
            width: 160,
            dataIndex: 'lastAccessTime',
            rtype: 'timestamp',
            filter: {
                type: 'date'
            }
        }, {
            header: 'Last Session Time'.t(),
            width: 160,
            dataIndex: 'lastSessionTime',
            rtype: 'timestamp',
            filter: {
                type: 'date'
            }
        }, {
            header: 'Quota Size'.t(),
            dataIndex: 'quotaSize',
            renderer: function (value) {
                return value === 0 || value === '' ? '' : value;
            },
            filter: {
                type: 'numeric'
            },
            rtype: 'datasize'
        }, {
            header: 'Quota Remaining'.t(),
            dataIndex: 'quotaRemaining',
            filter: {
                type: 'numeric'
            },
            rtype: 'datasize'
        }, {
            header: 'Quota Issue Time'.t(),
            dataIndex: 'quotaIssueTime',
            filter: {
                type: 'date'
            },
            rtype: 'timestamp'
        }, {
            header: 'Quota Expiration Time'.t(),
            dataIndex: 'quotaExpirationTime',
            filter: {
                type: 'date'
            },
            rtype: 'timestamp'
        }, {
            xtype: 'actioncolumn',
            width: 80,
            align: 'center',
            header: 'Refill Quota'.t(),
            iconCls: 'fa fa-refresh fa-green',

            handler: 'externalAction', // this handler is defined in the ungrid controller
            action: 'refillQuota' // this is the method to be called in this view controller
        }, {
            xtype: 'actioncolumn',
            width: 80,
            align: 'center',
            header: 'Drop Quota'.t(),
            iconCls: 'fa fa-minus-circle',

            handler: 'externalAction',
            action: 'dropQuota'
        }, {
            header: 'Tags'.t(),
            dataIndex: 'tags',
            flex: 1,
            filter: {
                type: 'string'
            },
            rtype: 'tags'
        }, {
            header: 'Tags String'.t(),
            dataIndex: 'tagsString',
            hidden: true,
            filter: {
                type: 'string'
            }
        }],
        editorFields: [{
            xtype: 'textfield',
            bind: '{record.username}',
            fieldLabel: 'Username'.t(),
            emptyText: '[enter Username]'.t(),
            allowBlank: false,
        }, {
            xtype: 'numberfield',
            bind: '{record.quotaSize}',
            fieldLabel: 'Quota Size'.t(),
            emptyText: '[enter quota]'.t()
        }]
    }, {
        // region: 'east',
        // xtype: 'unpropertygrid',
        // title: 'User Details'.t(),
        // itemId: 'details',
        // hidden: true,
        // bind: {
        //     source: '{userDetails}',
        //     hidden: '{!usersgrid.selection}'
        // }
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
    }],
    bbar: ['->', {
        text: '<strong>' + 'Save'.t() + '</strong>',
        iconCls: 'fa fa-floppy-o',
        handler: 'saveUsers'
    }]
});
