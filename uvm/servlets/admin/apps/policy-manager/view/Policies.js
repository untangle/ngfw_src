Ext.define('Ung.apps.policymanager.view.Policies', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-policy-manager-policies',
    itemId: 'policies',
    title: 'Policies'.t(),

    layout: 'border',
    items: [{
        region: 'west',
        border: false,
        split: true,
        collapsible: false,
        width: 300,
        minWidth: 300,

        layout: 'border',

        items: [{
            xtype: 'treepanel',
            reference: 'tree',
            title: 'Manage Policies',
            region: 'center',
            rootVisible: false,
            displayField: 'name',
            store: 'policiestree',
            useArrows: true,
            // viewConfig: {
            //     plugins: {
            //         ptype: 'treeviewdragdrop'
            //     }
            // },
            columns: [{
                xtype: 'treecolumn',
                flex: 1,
                dataIndex: 'name',
                renderer: function (val, meta, rec) {
                    return '<strong>' + rec.get('name') + '</strong> [' + rec.get('policyId') + ']';
                }
            }],
            listeners: {
                select: 'onPolicySelect',
                // drop: 'onDrop'
            },
            bbar: [{
                text: 'New Policy',
                iconCls: 'fa fa-plus-circle',
                handler: 'newPolicy'
            }]
        }, {
            region: 'south',
            split: true,
            collapsible: false,
            // title: 'Edit Policy',
            height: 'auto',
            xtype: 'form',
            layout: 'anchor',
            bodyPadding: 10,
            // hidden: true,
            bind: {
                title: '{!tree.selection ? "New Policy" : "Edit Policy"}'
            },
            defaults: {
                anchor: '100%'
            },
            items: [{
                xtype:  'textfield',
                name: 'name',
                fieldLabel: '<strong>' + 'Name'.t() + '</strong>',
                emptyText: 'enter policy name',
                allowBlank: false,
                labelAlign: 'top',
                bind: '{tree.selection.name}'
            }, {
                xtype:  'textarea',
                name: 'description',
                fieldLabel: '<strong>' + 'Description'.t() + '</strong>',
                emptyText: 'enter policy description',
                labelAlign: 'top',
                bind: '{tree.selection.description}'
            }, {
                xtype: 'combo',
                name: 'parentPolicyId',
                fieldLabel: '<strong>' + 'Parent'.t() + '</strong>',
                reference: 'policiesCombo',
                labelAlign: 'top',
                // disabled: true,
                emptyText: 'Select parent',
                allowBlank: false,
                bind: {
                    value: '{tree.selection.parentPolicyId}',
                    disabled: '{tree.selection.policyId === 1}'
                },
                // displayField: 'name',
                // valueField: 'parentPolicyId',
                queryMode: 'local',
                editable: false
            }],
            buttons: [{
                bind: {
                    text: '{tree.selection ? "Save" : "Add"}',
                    handler: '{tree.selection ? "setSettings" : "addPolicy"}'
                },
                formBind: true,
                iconCls: 'fa fa-floppy-o'
            }]
        }]
    }, {
        region: 'center',
        xtype: 'grid',
        reference: 'apps',
        title: 'Apps'.t(),
        bind: '{appsStore}',

        // dockedItems: [{
        //     xtype: 'toolbar',
        //     border: false,
        //     dock: 'top',
        //     cls: 'report-header',
        //     // height: 53,
        //     padding: '10 5',
        //     items: [{
        //         xtype: 'component',
        //         bind: {
        //             html: '<h2>{tree.selection.name}</h2><p>{tree.selection.description}</p>'
        //         }
        //     }],
        //     hidden: true,
        //     bind: {
        //         hidden: '{!tree.selection}'
        //     }
        // }],

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
