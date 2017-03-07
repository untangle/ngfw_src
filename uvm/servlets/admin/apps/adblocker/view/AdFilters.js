Ext.define('Ung.apps.adblocker.view.AdFilters', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-adblocker-adfilters',
    itemId: 'adfilters',
    title: 'Ad Filters'.t(),

    layout: 'border',
    border: false,

    defaults: {
        xtype: 'ungrid',
    },

    items: [{
        region: 'center',
        title: 'Standard Filters'.t(),

        listProperty: 'settings.rules.list',
        bind: '{rules}',

        columns: [
            Column.enabled, {
                header: 'Rule'.t(),
                width: 200,
                dataIndex: 'string',
                flex: 1
            }, {
                header: 'Action'.t(),
                width: 100,
                dataIndex: 'blocked',
                resizable: false,
                renderer: function (value) {
                    return value ? 'Block'.t() : 'Pass'.t();
                }
            }, {
                header: 'Slow'.t(),
                width: 100,
                dataIndex: 'flagged',
                resizable: false,
                renderer: function(value) {
                    return value ? 'Yes'.t() : '';
                }
            }]
    }, {
        region: 'south',
        height: '50%',
        split: true,

        title: 'User Defined Filters'.t(),

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'delete'],

        listProperty: 'settings.userRules.list',
        bind: '{userRules}',

        emptyRow: {
            string: '',
            enabled: true,
            blocked: true,
            javaClass: 'com.untangle.uvm.node.GenericRule'
        },

        columns: [
            Column.enabled, {
                header: 'Rule'.t(),
                width: 200,
                dataIndex: 'string',
                flex: 1,
                editor: {
                    xtype: 'textfield',
                    emptyText: '[enter rule]'.t(),
                    allowBlank: false
                }
            }, {
                header: 'Action'.t(),
                width: 100,
                dataIndex: 'blocked',
                resizable: false,
                renderer: function (value) {
                    return value ? 'Block'.t() : 'Pass'.t();
                }
            }, {
                header: 'Slow'.t(),
                width: 100,
                dataIndex: 'flagged',
                resizable: false,
                renderer: function(value) {
                    return value ? 'Yes'.t() : '';
                }
            }],
        editorFields: [{
            xtype: 'checkbox',
            bind: '{record.enabled}',
            fieldLabel: 'Enable'.t()
        }, {
            xtype: 'textfield',
            bind: '{record.string}',
            fieldLabel: 'Rule'.t(),
            emptyText: '[enter rule]'.t(),
            allowBlank: false,
            width: 400
        }, {
            xtype: 'combo',
            bind: '{record.blocked}',
            fieldLabel: 'Action'.t(),
            editable: false,
            store: [[true, 'Block'.t()], [false, 'Pass'.t()]],
            width: 200,
            queryMode: 'local'
        }]
    }]
});
