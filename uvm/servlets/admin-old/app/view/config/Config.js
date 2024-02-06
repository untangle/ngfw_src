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
    scrollable: true,

    items: [{
        xtype: 'dataview',
        store: {data: [
            { name: 'Network'.t(), url: 'network', icon: 'network.svg' },
            { name: 'Administration'.t(), url: 'administration', icon: 'administration.svg' },
            { name: 'Events'.t(), url: 'events', icon: 'events.svg' },
            { name: 'Email'.t(), url: 'email', icon: 'email.svg' },
            { name: 'Local Directory'.t(), url: 'local-directory', icon: 'local-directory.svg' },
            { name: 'Upgrade'.t(), url: 'upgrade', icon: 'upgrade.svg' },
            { name: 'System'.t(), url: 'system', icon: 'system.svg' },
            { name: 'About'.t(), url: 'about', icon: 'about.svg' }
        ]},
        tpl: '<p class="apps-title">' + 'Configuration'.t() + '</p>' +
             '<tpl for=".">' +
                '<a href="#config/{url}" class="app-item" style="margin: 10px;">' +
                '<img src="' + '/icons/config/{icon}" width=80 height=80/>' +
                '<span class="app-name">{name}</span>' +
                '</a>' +
            '</tpl>',
        itemSelector: 'a'
    }]
});
