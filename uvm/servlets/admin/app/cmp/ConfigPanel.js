Ext.define('Ung.cmp.ConfigPanel', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.configpanel',
    layout: 'fit',

    dockedItems: [{
        xtype: 'toolbar',
        ui: 'footer',
        dock: 'top',
        style: { background: '#D8D8D8' },
        items: [{
            text: 'Back to Config',
            iconCls: 'fa fa-arrow-circle-left',
            hrefTarget: '_self',
            href: '#config'
        }, {
            xtype: 'component',
            padding: '0 5',
            bind: { html: '<img src="/icons/config/{iconName}.svg" style="vertical-align: middle;" width="17" height="17"/> <strong>{title}</strong>' }
        }]
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        items: ['->', {
            text: '<strong>' + 'Save'.t() + '</strong>',
            bind:{
                disabled: '{panel.saveDisabled}'
            },
            iconCls: 'fa fa-floppy-o fa-lg',
            handler: 'saveSettings'
        }]
    }],

    listeners: {
        // generic listener for all tabs in Apps, redirection
        beforetabchange: function (tabPanel, newCard, oldCard) {
            Ung.app.redirectTo('#config/' + tabPanel.name + '/' + newCard.getItemId());
        },

        tabchange: function (tabPanel) {
            Ung.app.hashBackup = window.location.hash; // keep track of hash for changes detection
        },

        afterrender: function (configPanel) {
            // code used for detecting user manual data change
            Ung.app.hashBackup = window.location.hash; // keep track of hash for changes detection
            Ext.Array.each(configPanel.query('field'), function (field) {
                // setup the _initialValue of the field on focus
                field.on('focus', function () {
                    if (!field.hasOwnProperty('_initialValue')) {
                        field._initialValue = field.getValue();
                    }
                });
                field.on('blur', function () {
                    // on field blur check if new value is different than the initial one
                    // add an _isChanged prop which is true if field value is really changed by the user manually
                    if (field._initialValue !== field.getValue()) {
                        field._isChanged = true;
                    } else {
                        field._isChanged = false;
                    }
                });
            });
        },

        removed: function(){
            this.getViewModel().set('panel.saveDisabled', false);
        }
    }

});
