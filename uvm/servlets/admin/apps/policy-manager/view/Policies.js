Ext.define('Ung.apps.policymanager.view.Policies', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-policy-manager-policies',
    itemId: 'policies',
    title: 'Policies'.t(),

    layout: 'border',
    items: [{
        region: 'west',
        split: true,
        collapsible: false,
        width: 300,
        minWidth: 300,

        xtype: 'treepanel',
        reference: 'tree',
        title: 'Manage Policies',
        rootVisible: false,
        displayField: 'name',
        store: 'policiestree',
        useArrows: true,

        tbar: [{
            text: 'Add Policy',
            iconCls: 'fa fa-plus-circle',
            handler: 'addPolicy'
        }],
        columns: [{
            xtype: 'treecolumn',
            flex: 1,
            dataIndex: 'name',
            renderer: function (val, meta, rec) {
                return '<strong>' + rec.get('name') + '</strong> [' + rec.get('policyId') + ']';
            }
        }, {
            xtype: 'actioncolumn',
            width: 20,
            align: 'center',
            iconCls: 'fa fa-pencil',
            handler: 'editPolicy'
        }, {
            xtype: 'actioncolumn',
            width: 20,
            align: 'center',
            iconCls: 'fa fa-times',
            isDisabled: function (view, rowIndex, colIndex, item , record) {
                return record.get('policyId') === 1 || !record.isLeaf();
            },
            handler: 'removePolicy'
        }],
        listeners: {
            select: 'onPolicySelect'
        }
    }, {
        region: 'center',
        xtype: 'grid',
        reference: 'apps',
        title: 'Apps'.t(),
        bind: '{appsStore}',

        viewConfig: {
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/>Select a Policy ...</p>',
            getRowClass: function (rec) {
                return rec.get('parentPolicy') ? 'parent-policy' : '';
            }
        },
        // bind: {
        //     store: {
        //         data: '{appsData}'
        //     }
        // },
        columns: [{
            header: 'Id',
            align: 'right',
            width: 50,
            dataIndex: 'instanceId',
        }, {
            header: 'Status',
            dataIndex: 'status',
        }, {
            header: 'Name',
            width: 200,
            dataIndex: 'displayName',
            renderer: function (val, meta, rec) {
                return '<strong>' + val + '</strong>';
                // return '<img src="/skins/modern-rack/images/admin/apps/' + rec.get('name') + '_80x80.png" width=16 height=16/>' + '<strong>' + val + '</strong>';
            }
        }, {
            header: 'Inherited from',
            dataIndex: 'parentPolicy'
        }, {
            xtype: 'widgetcolumn',
            width: 80,
            widget: {
                xtype: 'button',
                disabled: true,
                bind: {
                    disabled: '{record.parentPolicy}',
                    text: '{record.status === "RUNNING" ? "Stop" : "Run"}',
                    iconCls: 'fa {record.status === "RUNNING" ? "fa-stop" : "fa-play"}',
                    handler: '{record.status === "RUNNING" ? "onStop" : "onStart"}',
                },
            },
            renderer: function (val, meta, record) {
                if (!record.get('status')) {
                    meta.style = 'display: none';
                }
            }
            // onWidgetAttach: function (col, widget, rec) {
            //     widget.setVisible(rec.get('status') ? true : false);
            // }
        }, {
            xtype: 'widgetcolumn',
            width: 80,
            widget: {
                xtype: 'button',
                bind: {
                    text: '{(record.status && !record.parentPolicy) ? "Remove" : "Install"}',
                    iconCls: 'fa {(record.status && !record.parentPolicy) ? "fa-ban" : "fa-download"}',
                    handler: '{(record.status && !record.parentPolicy) ? "onRemove" : "onInstall"}',
                }
            }
        }]
    }]
});
