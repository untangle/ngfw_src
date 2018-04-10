Ext.define('Ung.store.Widgets', {
    extend: 'Ext.data.Store',
    alias: 'store.widgets',
    storeId: 'widgets',
    model: 'Ung.model.Widget',
    listeners: {
        // load: function () { },
        // datachanged: function () { },
        update: function (store, record, operation, modifiedFieldNames, details) {
            if (operation === 'edit' && Ext.Array.contains(modifiedFieldNames, 'enabled')) {
                Ext.fireEvent('updatewidget', store, record, operation, modifiedFieldNames, details);
            }
        },
        remove: function (store, records) {
            Ext.fireEvent('removewidgets', store, records);
        },
        add: function (store, records) {
            Ext.fireEvent('addwidgets', store, records);
        },
        // refresh: function (store) { }
    }
});
