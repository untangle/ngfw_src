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
            header: 'Policy'.t(),
            xtype: 'treecolumn',
            flex: 1,
            dataIndex: 'name',
            renderer: function (val, meta, rec) {
                return '<strong>' + rec.get('name') + '</strong> [' + rec.get('policyId') + ']';
            }
        }, {
            header: 'Edit'.t(),
            xtype: 'actioncolumn',
            width: Renderer.actionWidth,
            align: 'center',
            iconCls: 'fa fa-pencil',
            handler: 'editPolicy'
        }, {
            header: 'Delete'.t(),
            xtype: 'actioncolumn',
            width: Renderer.actionWidth,
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
        xtype: 'ungrid',
        reference: 'apps',
        title: 'Apps'.t(),
        bind: '{appsStore}',

        emptyText: 'Select Policy'.t(),
        viewConfig: {
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
            width: Renderer.idWidth,
            dataIndex: 'instanceId',
        }, {
            header: 'Status',
            width: Renderer.messageWidth,
            dataIndex: 'status',
        }, {
            header: 'Name',
            width: Renderer.messageWidth,
            dataIndex: 'displayName',
            flex: 1,
            renderer: function (val, meta, rec) {
                return '<strong>' + val + '</strong>';
                // return '<img src="/skins/modern-rack/images/admin/apps/' + rec.get('name') + '_80x80.png" width=16 height=16/>' + '<strong>' + val + '</strong>';
            }
        }, {
            header: 'Inherited from',
            dataIndex: 'parentPolicy',
            width: Renderer.messageWidth
        }, {
            header: 'Action'.t(),
            xtype: 'widgetcolumn',
            width: Renderer.actionWidth,
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
            header: 'Manage'.t(),
            xtype: 'widgetcolumn',
            width: Renderer.actionWidth + 20,
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
