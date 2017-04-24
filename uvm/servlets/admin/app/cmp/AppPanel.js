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
                    ' <i class="fa fa-circle {!instance.targetState ? "fa-orange" : (instance.targetState === "RUNNING" ? "fa-green" : "fa-gray") }"></i>'
            }
        }])
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        // border: false,
        items: [{
            text: '<strong>' + 'Help'.t() + '</strong>',
            itemId: 'helpBtn',
            iconCls: 'fa fa-question-circle fa-lg'
        },  '->', {
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

        tabchange: function (tabPanel, newCard) {
            var helpSource = tabPanel.getViewModel().get('props.name').replace(/-/g, '_');
            if (newCard.getItemId() !== 'status') {
                helpSource += '_' + newCard.getItemId();
            }
            tabPanel.down('#helpBtn').setHref(rpc.helpUrl + '?source=' + helpSource + '&' + Util.getAbout());
        },

        afterrender: function (tabPanel) {
            var vm = tabPanel.getViewModel();
            var helpSource = tabPanel.getViewModel().get('props.name').replace(/-/g, '_');
            var currentCard = tabPanel.getActiveTab();
            if (currentCard.getItemId() !== 'status') {
                helpSource += '_' + currentCard.getItemId();
            }

            // add policy name in the tabbar, needs a small delay for policiestree to be available
            Ext.defer(function () {
                try {
                    var p = Ext.getStore('policiestree').findRecord('policyId', vm.get('policyId'));
                    vm.set('policyName', p.get('name'));
                } catch (ex) {
                    vm.set('policyName', '');
                }

            }, 500);
            tabPanel.down('#helpBtn').setHref(rpc.helpUrl + '?source=' + helpSource + '&' + Util.getAbout());
        }
    }
});
