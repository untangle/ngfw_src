Ext.define('Ung.config.events.view.Triggers', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.events.triggers',

    title: 'Triggers'.t(),

    bodyPadding: 10,
    layout: { type: 'vbox', align: 'stretch' },

    items: [{
        xtype: 'uneventgrid',
        title: 'Trigger Rules'.t(),
        region: 'center',

        controller: 'uneventsgrid',

        bind:{
            store: '{triggerRules}',
            conditions: '{conditions}'
        },

        listProperty: 'settings.triggerRules.list',
        tbar: ['@add'],
        // !!! add copy action
        recordActions: ['edit', 'copy', 'delete', 'reorder'],

        ruleJavaClass: 'com.untangle.uvm.event.EventRuleCondition',
        // conditions: [
        //     Condition.fieldCondition
        // ],

        emptyRow: {
            javaClass: 'com.untangle.uvm.event.TriggerRule',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: [{
                    comparator: '=',
                    field: 'class',
                    fieldValue: '*SystemStatEvent*',
                    javaClass: 'com.untangle.uvm.event.EventRuleCondition'
                }]
            },
            ruleId: -1,
            enabled: true
        },

        columns: [
            Column.ruleId,
            Column.enabled,
            Column.description,
            {
                header: 'Action'.t(),
                dataIndex: 'action',
                width: 250,
                renderer: function (value, metaData, record) {
                    if (typeof value === 'undefined') {
                        return 'Unknown action'.t();
                    }
                    switch(value) {
                      case 'TAG_HOST': return 'Tag Host'.t();
                      case 'TAG_USER':return 'Tag User'.t();
                      case 'TAG_DEVICE':return 'Tag Device'.t();
                    default: return 'Unknown Action: ' + value;
                    }
                }
            }],

        editorFields: [
            Field.enableRule(),
            Field.description,
            Field.conditions, {
                xtype: 'combo',
                reference: 'actionType',
                publishes: 'value',
                fieldLabel: 'Action Type'.t(),
                bind: '{record.action}',
                allowBlank: false,
                editable: false,
                store: [
                    ['TAG_HOST', 'Tag Host'.t()],
                    ['TAG_USER', 'Tag User'.t()],
                    ['TAG_DEVICE', 'Tag Device'.t()]
                ],
                queryMode: 'local'
            }, {
                xtype: 'textfield',
                bind: {
                    value: '{record.tagTarget}'
                },
                fieldLabel: 'Tag Target'.t(),
                allowBlank: false
            }, {
                xtype: 'textfield',
                bind: {
                    value: '{record.tagName}'
                },
                fieldLabel: 'Tag Name'.t(),
                allowBlank: false
            }, {
                xtype: 'numberfield',
                bind: {
                    value: '{record.tagLifetimeSec}'
                },
                fieldLabel: 'Tag Lifetime (sec)'.t(),
                allowBlank: false
            }]
    }]
});
