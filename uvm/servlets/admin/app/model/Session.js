Ext.define ('Ung.model.Session', {
    extend: 'Ext.data.Model',
    fields: [
        { name: 'protocol', type: 'string' },
        { name: 'clientKBps', type: 'number', convert: function (val) { return val ? Math.round(val*1000)/1000 : null; } },
        { name: 'serverKBps', convert: function (val) { return val ? Math.round(val*1000)/1000 : null; } },
        { name: 'totalKBps', convert: function (val) { return val ? Math.round(val*1000)/1000 : null ; } },
        { name: 'clientIntf', convert: function (val) {
            if (!val || val < 0) {
                return '';
            }
            return val;
        } },
        { name: 'serverIntf', convert: function (val) {
            if (!val || val < 0) {
                return '';
            }
            return val;
        } }
    ],
    proxy: {
        autoLoad: true,
        type: 'memory',
        reader: {
            type: 'json'
            // rootProperty: 'list'
        }
    }
});