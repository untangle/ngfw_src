Ext.define ('Ung.model.Category', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'categoryName', type: 'string' },
        { name: 'displayName', type: 'string' },
        { name: 'icon', type: 'string' },
        {
            name: 'slug',
            calculate: function (cat) {
                return cat.categoryName.replace(/ /g, '-').toLowerCase();
            }
        },
    ],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});
