Ext.define('Ung.config.localdirectory.LocalDirectoryModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.config.localdirectory',
    // requires: ['Ung.model.LocalDirectoryUser'],


    data: {
        usersData: null
    },

    stores: {
        users: {
            // model: 'Ung.model.LocalDirectoryUser',
            data: '{usersData.list}'
        }
    }
});