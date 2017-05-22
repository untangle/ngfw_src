Ext.define('Ung.view.config.Config', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.config',
    itemId: 'config',

    /* requires-start */
    requires: [
        'Ung.view.config.ConfigController',
        'Ung.cmp.GridEditorFields',
        'Ung.cmp.GridConditions',
        'Ung.cmp.GridColumns',
        'Ung.overrides.form.field.Date'
        // 'Ung.view.config.ConfigModel'
    ],
    /* requires-end */
    controller: 'config',
    viewModel: true,
    dockedItems: [{
        xtype: 'toolbar',
        ui: 'navigation',
        dock: 'top',
        border: false,
        style: {
            background: '#333435',
            zIndex: 9997
        },
        defaults: {
            xtype: 'button',
            border: false,
            hrefTarget: '_self'
        },
        items: Ext.clone(Util.subNav)
    }],

    items: [{
        xtype: 'dataview',
        store: {data: [
                { name: 'Network'.t(), url: 'network', icon: 'icon_config_network.png' },
                { name: 'Administration'.t(), url: 'administration', icon: 'icon_config_administration.png' },
                { name: 'Events'.t(), url: 'events', icon: 'icon_config_events.png' },
                { name: 'Email'.t(), url: 'email', icon: 'icon_config_email.png' },
                { name: 'Local Directory'.t(), url: 'localdirectory', icon: 'icon_config_directory.png' },
                { name: 'Upgrade'.t(), url: 'upgrade', icon: 'icon_config_upgrade.png' },
                { name: 'System'.t(), url: 'system', icon: 'icon_config_system.png' },
                { name: 'About'.t(), url: 'about', icon: 'icon_config_about.png' }
        ]},
        tpl: '<p class="apps-title">' + 'Configuration'.t() + '</p>' +
             '<tpl for=".">' +
                '<a href="#config/{url}" class="app-item" style="margin: 10px;">' +
                '<img src="' + '/skins/modern-rack/images/admin/config/{icon}" width=80 height=80/>' +
                '<span class="app-name">{name}</span>' +
                '</a>' +
            '</tpl>',
        itemSelector: 'a'
    }]
});
