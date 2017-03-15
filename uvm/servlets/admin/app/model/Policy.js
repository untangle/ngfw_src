Ext.define ('Ung.model.Policy', {
    extend: 'Ext.data.Model' ,
    fields: [
        {name: 'policyId', type: 'int'},
        {name: 'displayName', type: 'string', convert: function (value, record) {
            return 'Policy ' + record.get('policyId');
        }}
    ],
    hasMany: {
        model: 'Ung.model.NodeProperty',
        name: 'appProperties'
    },
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});
