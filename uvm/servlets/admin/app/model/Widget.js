Ext.define ('Ung.model.Widget', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'displayColumns', type: 'auto', default: null },
        { name: 'enabled', type: 'auto', default: true },
        { name: 'entryId', type: 'auto', default: null },
        { name: 'javaClass', type: 'string', defaultValue: 'com.untangle.uvm.DashboardWidgetSettings' },
        { name: 'refreshIntervalSec', type: 'auto', default: null },
        { name: 'timeframe', type: 'auto', default: null },
        { name: 'type', type: 'string' }
    ],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json',
            rootProperty: 'list'
        }
    }
});
