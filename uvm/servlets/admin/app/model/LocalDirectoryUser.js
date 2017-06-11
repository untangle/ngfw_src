Ext.define('Ung.model.LocalDirectoryUser', {
    extend: 'Ext.data.Model' ,

    alias: 'model.local-directoryuser',

    fields: [
        { name: 'username' },
        { name: 'expirationTime' },
        { name: 'lastName' },
        { name: 'markedForDelete', defaultValue: false },
        { name: 'markedForNew', defaultValue: false },
        { name: 'test', defaultValue: 'sometest' },
        { name: 'dt', type: 'string', claculate: function (data) {
            return data.lastName + ' ';
        },
            depends : ['lastName']
        },
    ],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});
