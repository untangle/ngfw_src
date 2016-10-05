Ext.define('Ung.view.config.ConfigController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config',

    init: function (view) {
        this.configNames = ['Network', 'Administration', 'Email', 'Local Directory', 'Upgrade', 'System', 'About'];
        this.toolNames = ['Policy Manager', 'Sessions', 'Hosts', 'Devices'];
    },

    onBeforeRender: function () {
        var configName, toolName, i, configs = [], tools = [];

        for (i = 0; i < this.configNames.length; i += 1) {
            configName = this.configNames[i];
            configs.push({
                xtype: 'ung.configitem',
                viewModel: {
                    data: {
                        displayName: configName.t(),
                        iconName: configName.toLowerCase().replace(/ /g, '_')
                    }
                }
            });
        }

        for (i = 0; i < this.toolNames.length; i += 1) {
            toolName = this.toolNames[i];
            tools.push({
                xtype: 'ung.configitem',
                viewModel: {
                    data: {
                        displayName: toolName.t(),
                        iconName: toolName.toLowerCase().replace(/ /g, '_')
                    }
                }
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
