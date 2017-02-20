Ext.define('Ung.view.config.Config', {
    extend: 'Ext.container.Container',
    xtype: 'ung.config',
    itemId: 'config',

    requires: [
        'Ung.view.config.ConfigController',
        'Ung.cmp.EditorFields',
        'Ung.overrides.form.field.Date'
        // 'Ung.view.config.ConfigModel'
    ],

    controller: 'config',

    items: [{
        xtype: 'dataview',
        store: {data: [
                { name: 'Network'.t(), url: 'network', icon: 'icon_config_network.png' },
                { name: 'Administration'.t(), url: 'administration', icon: 'icon_config_admin.png' },
                { name: 'Email'.t(), url: 'email', icon: 'icon_config_email.png' },
                { name: 'Local Directory'.t(), url: 'localdirectory', icon: 'icon_config_directory.png' },
                { name: 'Upgrade'.t(), url: 'upgrade', icon: 'icon_config_upgrade.png' },
                { name: 'System'.t(), url: 'system', icon: 'icon_config_system.png' },
                { name: 'About'.t(), url: 'about', icon: 'icon_config_about.png' }
        ]},
        tpl: '<p class="apps-title">' + 'Configuration'.t() + '</p>' +
             '<tpl for=".">' +
                '<a href="#config/{url}" class="app-item">' +
                '<img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/config/{icon}" width=80 height=80/>' +
                '<span class="app-name">{name}</span>' +
                '</a>' +
            '</tpl>',
        itemSelector: 'a'
    }, {
        xtype: 'dataview',
        store: {data: [
                { name: 'Sessions'.t(), url: 'sessions', icon: 'icon_config_sessions.png' },
                { name: 'Hosts'.t(), url: 'hosts', icon: 'icon_config_hosts.png' },
                { name: 'Devices'.t(), url: 'devices', icon: 'icon_config_devices.png' }
        ]},
        tpl: '<p class="apps-title">' + 'Tools'.t() + '</p>' +
             '<tpl for=".">' +
                '<a href="#{url}" class="app-item">' +
                '<img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/config/{icon}" width=80 height=80/>' +
                '<span class="app-name">{name}</span>' +
                '</a>' +
            '</tpl>',
        itemSelector: 'a'
    }]
});
