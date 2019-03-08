Ext.define('Ung.config.local-directory.MainModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.config-local-directory',

    data: {
        title: 'Local Directory'.t(),
        iconName: 'local-directory',

        usersData: null
    },

    stores: {
        users: {
            data: '{usersData.list}'
        }
    }
});
