Ext.define('Ung.view.config.ConfigController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config',

    init: function () {
        this.configItems = [
            { name: 'Network', icon: 'icon_config_network.png' },
            { name: 'Administration', icon: 'icon_config_admin.png' },
            { name: 'Email', icon: 'icon_config_email.png' },
            { name: 'Local Directory', icon: 'icon_config_directory.png' },
            { name: 'Upgrade', icon: 'icon_config_upgrade.png' },
            { name: 'System', icon: 'icon_config_system.png' },
            { name: 'About', icon: 'icon_config_about.png' }
        ];
        this.toolItems = [
            { name: 'Sessions', icon: 'icon_config_sessions.png' },
            { name: 'Hosts', icon: 'icon_config_hosts.png' },
            { name: 'Devices', icon: 'icon_config_devices.png' }
        ];
    },

    onBeforeRender: function () {
        var config, tool, i, configs = [], tools = [];

        for (i = 0; i < this.configItems.length; i += 1) {
            config = this.configItems[i];
            configs.push({
                xtype: 'ung.configitem',
                name: config.name.t(),
                icon: config.icon,
                href: '#config/' + config.name.toLowerCase().replace(/ /g, '')
            });
        }

        for (i = 0; i < this.toolItems.length; i += 1) {
            tool = this.toolItems[i];
            tools.push({
                xtype: 'ung.configitem',
                name: tool.name.t(),
                icon: tool.icon,
                href: '#' + tool.name.toLowerCase().replace(/ /g, '')
            });
        }

        this.getView().lookupReference('configs').removeAll(true);
        this.getView().lookupReference('configs').add(configs);
        this.getView().lookupReference('tools').removeAll(true);
        this.getView().lookupReference('tools').add(tools);

    },

    onItemBeforeRender: function (item) {
        item.el.on('click', function () {
            Ung.app.redirectTo('#config/' + item.getViewModel().get('name'), true);
        });
    }

});
