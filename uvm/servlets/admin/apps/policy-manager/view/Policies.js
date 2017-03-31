Ext.define('Ung.apps.policymanager.view.Policies', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-policy-manager-policies',
    itemId: 'policies',
    title: 'Policies'.t(),

    layout: 'border',
    items: [{
        xtype: 'treepanel',
        reference: 'tree',
        region: 'west',
        weight: 30,
        split: true,
        collapsible: false,
        width: 250,
        minWidth: 200,
        rootVisible: false,
        displayField: 'name',
        useArrows: true,
        listeners: {
            select: 'onPolicySelect'
        }
    }, {
        region: 'north',
        split: true,
        collapsible: false,
        height: 200,
        xtype: 'propertygrid',

    }, {
        region: 'center',
        xtype: 'grid',
        reference: 'apps',
        html: 'apps',
        bind: '{appsStore}',
        emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/>Select a Policy ...</p>',
        // bind: {
        //     store: {
        //         data: '{appsData}'
        //     }
        // },
        columns: [{
            header: 'Status',
            dataIndex: 'status',
        }, {
            header: 'Name',
            width: 250,
            dataIndex: 'displayName',
            renderer: function (val) {
                return '<strong>' + val + '</strong>';
            }
        }, {
            header: 'Parent Policy',
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
                    iconCls: 'fa {record.status === "RUNNING" ? "fa-stop" : "fa-play"}'
                }
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
                    iconCls: 'fa {(record.status && !record.parentPolicy) ? "fa-ban" : "fa-download"}'
                }
            }
        }]
    }]
});
