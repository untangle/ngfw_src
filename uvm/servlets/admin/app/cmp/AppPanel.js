Ext.define('Ung.cmp.AppPanel', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.apppanel',
    layout: 'fit',

    dockedItems: [{
        xtype: 'toolbar',
        ui: 'footer',
        dock: 'top',
        style: { background: '#D8D8D8' },
        items: [{
            xtype: 'button',
            iconCls: 'fa fa-arrow-circle-left',
            hrefTarget: '_self',
            bind: {
                text: 'Back to Apps (<strong>{policyName}</strong>)',
                href: '#apps/{policyId}'
            }
        }, {
            xtype: 'component',
            padding: '0 5',
            bind: {
                html: '<img src="/icons/apps/{props.name}.svg" style="vertical-align: middle;" width="16" height="16"/> <strong>{props.displayName}</strong>' +
                    ' <i class="fa fa-circle {state.colorCls}"></i>'
            }
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
            handler: 'setSettings'
        }]
    }],

    tabBar: {
        items: [{
            xtype: 'button',
            itemId: 'reportsBtn',
            ui: 'none',
            margin: '0 5',
            cls: 'view-reports',
            text: '<i class="fa fa-area-chart"></i> ' + 'View Reports'.t(),
            hrefTarget: '_self',
            hidden: true,
            bind: {
                href: '#reports?cat={props.name}',
                hidden: '{!reportsAppStatus.installed || !reportsAppStatus.enabled || !state.on}'
            }
        }]
    },

    listeners: {
        // generic listener for all tabs in Apps, redirection
        beforetabchange: function (tabPanel, newCard, oldCard) {
            var vm = this.getViewModel();
            if (vm.get('props.type') === 'FILTER') {
                Ung.app.redirectTo('#apps/' + vm.get('policyId') + '/' + vm.get('urlName') + '/' + newCard.getItemId());
            } else {
                Ung.app.redirectTo('#service/' + vm.get('props.name') + '/' + newCard.getItemId());
            }
        },

        tabchange: function (tabPanel) {
            Ung.app.hashBackup = window.location.hash; // keep track of hash for changes detection
        },

        afterrender: function (appPanel) {
            // code used for detecting user manual data change
            Ung.app.hashBackup = window.location.hash; // keep track of hash for changes detection
            Ext.Array.each(appPanel.query('field'), function (field) {
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

            // remove View Reports tab button if App does not have reports (e.g. Reports App)
            if (!appPanel.down('appreports')) {
                var rbtn = appPanel.getTabBar().down('#reportsBtn');
                appPanel.getTabBar().remove(rbtn);
            }

        },

        removed: function(){
            this.getViewModel().set('panel.saveDisabled', false);
        }
    }
});
