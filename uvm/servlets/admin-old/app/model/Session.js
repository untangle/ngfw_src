Ext.define ('Ung.model.Session', {
    extend: 'Ext.data.Model',
    fields: [
        { name: 'bypassed', type: 'boolean' },
        { name: 'clientCountry', type: 'string' },
        { name: 'clientIntf', type: 'auto' },
        { name: 'clientKBps', type: 'number', convert: function (val) { return val ? Math.round(val*1000)/1000 : null; } },
        { name: 'clientLatitude', type: 'number' },
        { name: 'clientLongitude', type: 'number' },
        { name: 'creationTime', type: 'auto' },
        { name: 'hostname', type: 'string' },
        { name: 'localAddr', type: 'auto' },
        { name: 'natted', type: 'boolean' },
        { name: 'pipeline', type: 'auto' },
        { name: 'policy', type: 'auto' },
        { name: 'portForwarded', type: 'boolean' },
        { name: 'postNatClient', type: 'string' },
        { name: 'postNatClientPort', type: 'number' },
        { name: 'postNatServer', type: 'string' },
        { name: 'postNatServerPort', type: 'number' },
        { name: 'preNatClient', type: 'string' },
        { name: 'preNatClientPort', type: 'number' },
        { name: 'preNatServer', type: 'string' },
        { name: 'preNatServerPort', type: 'number' },
        { name: 'priority', type: 'number' },
        { name: 'protocol', type: 'string' },
        { name: 'qosPriority', type: 'number' },
        { name: 'remoteAddr', type: 'auto' },
        { name: 'serverCountry', type: 'string' },
        { name: 'serverIntf', type: 'auto' },
        { name: 'serverKBps', type: 'number', convert: function (val) { return val ? Math.round(val*1000)/1000 : null; } },
        { name: 'serverLatitude', type: 'number' },
        { name: 'serverLongitude', type: 'number' },
        { name: 'sessionId', type: 'number' },
        { name: 'state', type: 'auto' },
        { name: 'tags', type: 'auto' },
        { name: 'tagsString', type: 'string' },
        { name: 'totalKBps', type: 'number', convert: function (val) { return val ? Math.round(val*1000)/1000 : null ; } },
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
