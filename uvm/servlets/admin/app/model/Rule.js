Ext.define ('Ung.model.Rule', {
    extend: 'Ext.data.Model' ,

    alias: 'model.rule',

    fields: [
        { name: 'ruleId', type: 'auto', defaultValue: null },
        { name: 'description', type: 'string', defaultValue: '' },
        { name: 'enabled', type: 'boolean', defaultValue: true },
        { name: 'conditions', type: 'auto' },

        { name: 'markedForDelete', defaultValue: false },
        { name: 'markedForNew', defaultValue: false }

        // { name: 'conditionsMap', mapping: function (data) {
        //     return data.conditions.list;
        // }},
        // { name: 'conditionsList', calculate: function(data) {
        //     //console.log(data);
        //     var conds = data.conditions;
        //     var resp = '', i, cond;
        //     for (i = 0; i < conds.list.length; i += 1) {
        //         cond = conds.list[i];
        //         resp += cond.conditionType + (cond.invert ? ' &ne; ' : ' = ') + cond.value + '<br/>';
        //     }
        //     //console.log(val);
        //     return resp;
        // } },

        // { name: 'markedForDelete', defaultValue: false },
        // { name: 'newDestination' },
        // { name: 'newPort' }
        // { name: 'string', type: 'string', defaultValue: '' },
        // { name: 'blocked', type: 'boolean', defaultValue: true },
        // { name: 'flagged', type: 'boolean', defaultValue: true },
        // { name: 'category', type: 'string', defaultValue: null },
        // { name: 'id', defaultValue: null },
        // { name: 'readOnly', type: 'boolean', defaultValue: null },
        // { name: 'javaClass', type: 'string', defaultValue: 'com.untangle.node.bandwidth_control.BandwidthControlRule' },

        // { name: 'action', type: 'auto'},
        // { name: 'actionType', type: 'string', mapping: 'action.actionType', defaultValue: 'SET_PRIORITY' },
        // { name: 'actionPriority', type: 'int', mapping: 'action.priority', defaultValue: 1 },
        // { name: 'actionPenaltyTime', type: 'int', mapping: 'action.penaltyTime', defaultValue: 0 },
        // { name: 'actionQuotaBytes', type: 'int', mapping: 'action.quotaBytes', defaultValue: 1 },
        // { name: 'actionQuotaTime', type: 'int', mapping: 'action.quotaTime', defaultValue: -1 }
    ],
    // hasMany: 'Ung.model.Condition',
    proxy: {
        // autoLoad: true,
        type: 'memory',
        reader: {
            type: 'json',
            // rootProperty: 'list'
        }
    }
});