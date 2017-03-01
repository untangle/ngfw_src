Ext.define('Ung.config.localdirectory.MainModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.config.localdirectory',
    // requires: ['Ung.model.LocalDirectoryUser'],


    data: {
        title: 'Local Directory'.t(),
        iconName: 'icon_config_directory',

        usersData: null
    },

    stores: {
        users: {
            // model: 'Ung.model.LocalDirectoryUser',
            data: '{usersData.list}'
        }
    }
});
