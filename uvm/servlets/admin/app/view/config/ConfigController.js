Ext.define('Ung.view.config.ConfigController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config',

    control: {
        '#': {
            deactivate: 'onDeactivate'
        },
        '#subNav': {
            selectionchange: 'onSelect'
        }
    },

    listen: {
        global: {
            loadconfig: 'onLoadConfig'
        }
    },

    onSelect: function (el, sel) {
        // console.log(selected);
        sel.selected = true;
    },


    onLoadConfig: function (configName, configTab) {
        var view = this.getView();
        if (!configName) {
            view.setActiveItem(1);
            return;
        }

        if (view.down('#configCard')) {
            view.down('#configCard').destroy();
        }

        var cfgName = configName.charAt(0).toUpperCase() + configName.slice(1).toLowerCase();

        console.log(cfgName);

        view.down('#subNav').getStore().each(function (item) {
            if (item.get('url') === configName) {
                item.set('selected', 'x-item-selected');
            } else {
                item.set('selected', '');
            }
        });


        view.setLoading('Loading ' + cfgName.t() + '...');
        console.log(cfgName);
        Ext.require('Ung.view.config.' + cfgName.toLowerCase() + '.' + cfgName, function () {
            view.down('#configWrapper').add({
                xtype: 'ung.config.' + cfgName.toLowerCase(),
                region: 'center',
                itemId: 'configCard'
            });
            view.setLoading(false);
            view.setActiveItem(2);
            console.log(configTab);
            if (configTab) {
                view.down('#configCard').setActiveItem(configTab);
            }
        });
    },

    onDeactivate: function (view) {
        // console.log('here');
        // if (view.down('#configCard')) {
        //     view.setActiveItem(0);
        //     view.down('#configCard').destroy();
        // }
        // view.remove('configCard');
    }


});
