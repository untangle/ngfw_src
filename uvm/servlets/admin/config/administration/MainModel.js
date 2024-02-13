Ext.define('Ung.config.administration.MainModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.config-administration',

    data: {
        title: 'Administration'.t(),
        iconName: 'administration',

        adminSettings: null,
        systemSettings: null,

        serverCertificates: null,
        rootCertificateInformation: null,
        serverCertificateVerification: null,
        rootCertificates: null
    },
    stores: {
        accounts: { data: '{adminSettings.users.list}' },
        certificates: { data: '{serverCertificates.list}' },
        rootCertStore: {data: '{rootCertificates.list}'}
    }
});
