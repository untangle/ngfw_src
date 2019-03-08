Ext.define('Ung.store.Categories', {
    extend: 'Ext.data.Store',
    storeId: 'categories',

    fields: [{
        name: 'displayName', type: 'string'
    }, {
        name: 'name', type: 'string'
    }, {
        name: 'type', type: 'string', defaultValue: 'app'
    }, {
        name: 'icon', type: 'string',
        calculate: function (cat) {
            if (cat.type === 'system') {
                return '/icons/config/' + cat.name + '.svg';
            }
            return '/icons/apps/' + cat.name + '.svg';
        }
    }, {
        name: 'slug', type: 'string',
        calculate: function (cat) { return cat.name; }
    }],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});
