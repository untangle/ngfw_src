Ext.define ('Ung.model.Widget', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'displayColumns', type: 'auto', defaultValue: null },
        { name: 'enabled', type: 'auto', defaultValue: true },
        { name: 'entryId', type: 'auto', defaultValue: null },
        { name: 'javaClass', type: 'string', defaultValue: 'com.untangle.uvm.DashboardWidgetSettings' },
        { name: 'refreshIntervalSec', type: 'auto', defaultValue: null },
        { name: 'timeframe', type: 'auto', defaultValue: null },
        { name: 'type', type: 'string' },
        { name: 'itemId', calculate: function () {
            return 'widget-' + Ext.Number.randomInt(100000, 999999);
        } }
    ],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json',
            // rootProperty: 'list'
        }
    }
});
