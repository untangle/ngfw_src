Ext.define('Ung.config.about.Main', {
    extend: 'Ung.cmp.ConfigPanel',
    alias: 'widget.config.about',

    /* requires-start */
    requires: [
        'Ung.config.about.AboutController'
    ],
    /* requires-end */

    controller: 'config.about',

    viewModel: {
        data: {
            title: 'About'.t(),
            iconName: 'icon_config_about',

            kernelVersion: '',
            modificationState: '',
            rebootCount: '',
            activeSize: '',
            maxActiveSize: ''
        }
    },

    items: [{
        xtype: 'config.about.server'
    }, {
        xtype: 'config.about.licenses'
    }, {
        xtype: 'config.about.licenseagreement'
    }]

});
