Ext.define('Ung.view.main.Offline', {
    extend: 'Ext.window.Window',
    alias: 'widget.offline',

    title: 'Offline'.t(),
    modal: true,
    width: 700,
    height: 500,

    layout: 'fit',

    items: [{
        xtype: 'container',
        padding: 40,
        layout: { tyep: 'vbox', align: 'middle' },
        style: { background: '#FFF' },
        items: [{
            xtype: 'component',
            html: '<img src="/images/BrandingLogo.png" style="max-width: 166px; max-height: 100px;" />',
            width: 166,
            height: 100
        }, {
            xtype: 'component',
            html: '<h3>' + 'Welcome!'.t() + '</h3>' +
                '<p>' + 'The installation is complete and ready for deployment. The next step are registration and installing apps from the App Store.'.t() + '</p>' +
                '<p style="color: red;">' + 'Unfortunately, Your server was unable to contact the App Store.'.t() + '</p>' +
                '<p>' + 'Before Installing apps, this must be resolved.'.t() + '</p>' +
                '<p><strong>' + 'Possible Resolutions'.t() + '</strong></p>' +
                '<ol><li>' + 'Verify the network settings are correct and the Connectivity Test succeeds.'.t() + '</li>' +
                '<li>' + 'Verify that there are no upstream firewalls blocking HTTP access to the internet.'.t() + '</li>' +
                '<li>' + 'Verify the external interface has the correct IP and DNS settings.'.t() + '</li></ol>'
        }, {
            xtype: 'button',
            margin: '10 0',
            text: 'Open Network Settings'.t(),
            iconCls: 'fa fa-cog',
            href: '#config/network',
            hrefTarget: '_self',
            handler: function (btn) {
                btn.up('window').close();
                Ext.destroy(btn.up('window'));
            }
        }]
    }],
    buttons: [{
        text: 'Close'.t(),
        iconCls: 'fa fa-ban',
        handler: function (btn) {
            btn.up('window').close();
            Ext.destroy(btn.up('window'));
        }
    }]

});
