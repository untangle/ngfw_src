Ext.define('Ung.view.config.Config', {
    extend: 'Ext.container.Container',
    xtype: 'ung.config',
    itemId: 'config',

    requires: [
        'Ung.view.config.ConfigController',
        // 'Ung.view.config.ConfigModel'
    ],

    controller: 'config',
    viewModel: {
        // data: {
        //     confs: [
        //         { name: 'Network'.t(), url: 'network', icon: 'icon_config_network.png' },
        //         { name: 'Administration'.t(), url: 'administration', icon: 'icon_config_admin.png' },
        //         { name: 'Email'.t(), url: 'email', icon: 'icon_config_email.png' },
        //         { name: 'Local Directory'.t(), url: 'localdirectory', icon: 'icon_config_directory.png' },
        //         { name: 'Upgrade'.t(), url: 'upgrade', icon: 'icon_config_upgrade.png' },
        //         { name: 'System'.t(), url: 'system', icon: 'icon_config_system.png' },
        //         { name: 'About'.t(), url: 'about', icon: 'icon_config_about.png' }
        //     ]
        // }
    },

    layout: 'fit',

    items: [{
        xtype: 'dataview',
        style: {
            textAlign: 'center'
        },
        store: {
            data: [
                { name: 'Network'.t(), url: 'network', icon: 'sitemap' },
                { name: 'Administration'.t(), url: 'administration', icon: 'user-secret' },
                { name: 'Email'.t(), url: 'email', icon: 'envelope' },
                { name: 'Local Directory'.t(), url: 'localdirectory', icon: 'folder-open' },
                { name: 'Upgrade'.t(), url: 'upgrade', icon: 'arrow-circle-up' },
                { name: 'System'.t(), url: 'system', icon: 'cogs' },
                { name: 'About'.t(), url: 'about', icon: 'info-circle' }
            ]
        },
        tpl: '<!--<p class="heading">Settings</p>-->' +
             '<tpl for=".">' +
                '<a href="#config/{url}" class="app-item">' +
                '<i class="fa fa-{icon} fa-5x"></i>' +
                '<span class="app-name">{name}</span>' +
                '</a>' +
            '</tpl>',
        itemSelector: 'a'
    }, {
        // xtype: 'container',
        // layout: 'border',
        // itemId: 'configWrapper',
        // items: [{
        //     xtype: 'dataview',
        //     region: 'north',
        //     itemId: 'subNav',
        //     height: 32,
        //     baseCls: 'sub-nav',
        //     store: {
        //         data: [
        //             { name: 'Network'.t(), url: 'network', icon: 'sitemap' },
        //             { name: 'Administration'.t(), url: 'administration', icon: 'user-secret' },
        //             { name: 'Email'.t(), url: 'email', icon: 'envelope' },
        //             { name: 'Local Directory'.t(), url: 'localdirectory', icon: 'folder-open' },
        //             { name: 'Upgrade'.t(), url: 'upgrade', icon: 'arrow-circle-up' },
        //             { name: 'System'.t(), url: 'system', icon: 'cogs' },
        //             { name: 'About'.t(), url: 'about', icon: 'info-circle' }
        //         ]
        //     },
        //     tpl: '<tpl for=".">' +
        //             '<a href="#config/{url}" class="nav-item {selected}">' +
        //             '<i class="fa fa-{icon}"></i> ' +
        //             '<span class="app-name">{name}</span>' +
        //             '</a>' +
        //         '</tpl>',
        //     itemSelector: 'a'
        // }]
    }]

    // initComponent:
});