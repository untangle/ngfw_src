Ext.define('Ung.view.apps.AppsModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.apps',

    data: {
        nodes: []
    },

    stores: {
        nodesStore: {
            data: '{nodes}',
            sorters: [{
                property: 'viewPosition',
                direction: 'ASC'
            }]
        }
    }

});
