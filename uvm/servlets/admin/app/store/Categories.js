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
                return '/skins/modern-rack/images/admin/config/icon_config_' + cat.name + '.png';
            }
            return '/skins/modern-rack/images/admin/apps/' + cat.name + '_80x80.png';
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
