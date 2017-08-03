Ext.define('Ung.cmp.AppPanel', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.apppanel',
    layout: 'fit',

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
            border: false,
        },
        items: Ext.Array.insert(Ext.clone(Util.subNav), 0, [{
            xtype: 'button',
            iconCls: 'fa fa-arrow-circle-left fa-lg',
            hrefTarget: '_self',
            bind: {
                text: 'Back to Apps (<strong>{policyName}</strong>)',
                href: '#apps/{policyId}'
            }
        }, '-', {
            xtype: 'component',
            padding: '0 5',
            style: {
                color: '#CCC'
            },
            bind: {
                html: '<img src="/skins/modern-rack/images/admin/apps/{props.name}_17x17.png" style="vertical-align: middle;" width="17" height="17"/> {props.displayName}' +
                    ' <i class="fa fa-circle {!instance.targetState ? "fa-orange" : (runState === "RUNNING" ? "fa-green" : "fa-gray") }"></i>'
            }
        }])
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        items: ['->', {
            text: '<strong>' + 'Save'.t() + '</strong>',
            iconCls: 'fa fa-floppy-o fa-lg',
            handler: 'setSettings'
        }]
    }],



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

            var vm = appPanel.getViewModel();
            // add policy name in the tabbar, needs a small delay for policiestree to be available
            Ext.defer(function () {
                try {
                    var p = Ext.getStore('policiestree').findRecord('policyId', vm.get('policyId'));
                    vm.set('policyName', p.get('name'));
                } catch (ex) {
                    vm.set('policyName', '');
                }

            }, 500);
        }
    }
});
