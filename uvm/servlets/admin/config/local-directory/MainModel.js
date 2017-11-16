Ext.define('Ung.config.local-directory.MainModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.config-local-directory',

    data: {
        title: 'Local Directory'.t(),
        iconName: 'icon_config_directory',

        usersData: null
    },

    stores: {
        users: {
            data: '{usersData.list}'
        }
    }
});
