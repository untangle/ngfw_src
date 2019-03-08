Ext.define('Ung.apps.policymanager.view.Policies', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-policy-manager-policies',
    itemId: 'policies',
    title: 'Policies'.t(),
    scrollable: true,

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
        xtype: 'grid',
        reference: 'apps',
        title: 'Apps'.t(),
        bind: '{appsStore}',

        emptyText: 'Select Policy'.t(),
        viewConfig: {
            getRowClass: function (rec) {
                return rec.get('parentPolicy') ? 'parent-policy' : '';
            }
        },

        columns: [{
            header: 'Id',
            align: 'right',
            width: Renderer.idWidth,
            dataIndex: 'instanceId',
        }, {
            header: 'Status',
            width: Renderer.messageWidth,
            dataIndex: 'state',
            renderer: function(state){
                if(!state){
                    return '';
                }
                return Ext.String.format('<i class="fa fa-circle {0}"></i> {1}', state.get('colorCls'), state.get('status'));
            }
        }, {
            header: 'Name',
            width: Renderer.messageWidth,
            dataIndex: 'displayName',
            flex: 1,
            renderer: function (val, meta, rec) {
                return '<strong>' + val + '</strong>';
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
                    text: '{record.state.on ? "Stop" : "Run"}',
                    iconCls: 'fa {record.state.on ? "fa-stop" : "fa-play"}',
                    handler: '{record.state.on ? "onStop" : "onStart"}',
                },
            },
            renderer: function (val, meta, record) {
                if (!record.get('state')) {
                    meta.style = 'display: none';
                }
            }
        }, {
            header: 'Manage'.t(),
            xtype: 'widgetcolumn',
            width: Renderer.actionWidth + 20,
            widget: {
                xtype: 'button',
                bind: {
                    text: '{(record.state && !record.parentPolicy) ? "Remove" : "Install"}',
                    iconCls: 'fa {(record.state && !record.parentPolicy) ? "fa-ban" : "fa-download"}',
                    handler: '{(record.state && !record.parentPolicy) ? "onRemove" : "onInstall"}',
                }
            }
        }]
    }]
});
