Ext.define ('Ung.model.Condition', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'conditionType', type: 'string', defaultValue: 'DST_ADDR' },
        { name: 'invert', type: 'boolean', defaultValue: false },
        { name: 'javaClass', type: 'string', defaultValue: 'com.untangle.node.bandwidth_control.BandwidthControlRuleCondition' },
        { name: 'value', type: 'auto', defaultValue: ''}
    ],
    proxy: {
        autoLoad: true,
        type: 'memory',
        reader: {
            type: 'json',
            rootProperty: 'list'
        }
    }
});