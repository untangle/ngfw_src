Ext.define('Ung.apps.ad-blocker.view.AdFilters', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ad-blocker-adfilters',
    itemId: 'ad-filters',
    title: 'Ad Filters'.t(),
    scrollable: true,

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
                width: Renderer.messagwWidth,
                dataIndex: 'string',
                flex: 1
            }, {
                header: 'Action'.t(),
                width: Renderer.messagwWidth,
                dataIndex: 'blocked',
                resizable: false,
                renderer: Ung.apps.adblocker.MainController.actionRenderer
            }, {
                header: 'Slow'.t(),
                width: Renderer.messagwWidth,
                dataIndex: 'flagged',
                resizable: false,
                renderer: Ung.apps.adblocker.MainController.modeRenderer
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

        emptyText: 'No User Defined Filters defined'.t(),

        emptyRow: {
            string: '',
            enabled: true,
            blocked: true,
            javaClass: 'com.untangle.uvm.app.GenericRule'
        },

        columns: [
            Column.enabled, {
                header: 'Rule'.t(),
                width: Renderer.messagwWidth,
                dataIndex: 'string',
                flex: 1,
                editor: {
                    xtype: 'textfield',
                    emptyText: '[enter rule]'.t(),
                    allowBlank: false
                }
            }, {
                header: 'Action'.t(),
                width: Renderer.messagwWidth,
                dataIndex: 'blocked',
                resizable: false,
                renderer: Ung.apps.adblocker.MainController.actionRenderer
            }, {
                header: 'Slow'.t(),
                width: Renderer.messagwWidth,
                dataIndex: 'flagged',
                resizable: false,
                renderer: Ung.apps.adblocker.MainController.modeRenderer
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
