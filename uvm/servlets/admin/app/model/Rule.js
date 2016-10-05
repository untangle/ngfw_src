Ext.define ('Ung.model.Rule', {
    extend: 'Ext.data.Model' ,
    fields: [
        //{ name: 'conditions'},
        { name: 'name', type: 'string', defaultValue: null },
        { name: 'string', type: 'string', defaultValue: '' },
        { name: 'blocked', type: 'boolean', defaultValue: true },
        { name: 'flagged', type: 'boolean', defaultValue: true },
        { name: 'category', type: 'string', defaultValue: null },
        { name: 'description', type: 'string', defaultValue: '' },
        { name: 'enabled', type: 'boolean', defaultValue: true },
        { name: 'id', defaultValue: null },
        { name: 'readOnly', type: 'boolean', defaultValue: null },
        { name: 'javaClass', type: 'string', defaultValue: 'com.untangle.node.bandwidth_control.BandwidthControlRule' },
        { name: 'ruleId', type: 'int', defaultValue: null },

        { name: 'action', type: 'auto'},
        { name: 'actionType', type: 'string', mapping: 'action.actionType', defaultValue: 'SET_PRIORITY' },
        { name: 'actionPriority', type: 'int', mapping: 'action.priority', defaultValue: 1 },
        { name: 'actionPenaltyTime', type: 'int', mapping: 'action.penaltyTime', defaultValue: 0 },
        { name: 'actionQuotaBytes', type: 'int', mapping: 'action.quotaBytes', defaultValue: 1 },
        { name: 'actionQuotaTime', type: 'int', mapping: 'action.quotaTime', defaultValue: -1 }
    ],
    hasMany: 'Ung.model.Condition',
    proxy: {
        autoLoad: true,
        type: 'memory',
        reader: {
            type: 'json',
            rootProperty: 'list'
        }
    }
});