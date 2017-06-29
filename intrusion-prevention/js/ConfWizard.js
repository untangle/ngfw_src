Ext.define('Ung.apps.intrusionprevention.ConfWizard', {
    extend: 'Ext.window.Window',
    alias: 'widget.app-intrusion-prevention-wizard',
    title: '<i class="fa fa-magic"></i> ' + 'Intrusion Prevention Setup Wizard'.t(),
    modal: true,

    controller: 'app-intrusion-prevention-wizard',
    viewModel: {
        type: 'app-intrusion-prevention-wizard'
    },

    width: 800,
    height: 450,

    layout: 'card',

    defaults: {
        border: false,
        scrollable: 'y',
        bodyPadding: 10
    },

    items: [{
        title: 'Welcome'.t(),
        header: false,
        itemId: 'welcome',
        items: [{
            xtype: 'component',
            html: '<h2>' + "Welcome to the Intrusion Prevention Setup Wizard!".t() + '</h2>'
        },{
            xtype: 'component',
            html: '<p>' + "Intrusion Prevention operates using rules to identify possible threats.  An enabled ruled performs an action, either logging or blocking traffic.  Not all rules are necessary for a given network environment and enabling all of them may negatively impact your network.".t() + '</p>'
        },{
            xtype: 'component',
            html: '<p>' + "This wizard is designed to help you correctly configure the appropriate amount of rules for your network by selecting rule identifiers: classtypes and categories.  The more that you select, the more rules will be enabled.  Again, too many enabled rules may negatively impact your network.".t() + '</p>'
        },{
            xtype: 'component',
            html: '<p>' + "It is highly suggested that you use Recommended values.".t() + '</p'
        }, {
            xtype: 'component',
            html: '<i class="fa fa-exclamation-triangle fa-red"></i> ' + 'WARNING: Completing this setup wizard will overwrite the previous settings with new settings. All previous settings will be lost!'.t()
        }]
    }, {
        title: 'Classtypes'.t(),
        header: false,
        itemId: 'classtypes',
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'component',
            html: '<h2>' + "Classtypes".t() + '</h2>'
        },{
            xtype: 'component',
            // html: '<h2>' + 'Classtypes are a generalized grouping for rules, such as attempts to gain user access.'.t() + '</h2>'
            html: '<p>' + 'Classtypes are a generalized grouping for rules, such as attempts to gain user access.'.t() + '</p>'
        }, {
            xtype: 'radiogroup',
            name: 'classtypes',
            columns: 1,
            simpleValue: true,
            bind: '{settings.activeGroups.classtypes}',
            items: [{
                xtype: 'radio',
                boxLabel: 'Recommended (default)'.t(),
                inputValue: 'recommended'
            },{
                name: 'classtypes_recommended_settings',
                xtype: 'fieldset',
                border: false,
                margin: '0 0 0 7',
                hidden: true,
                disabled: true,
                bind: {
                    hidden: '{settings.activeGroups.classtypes != "recommended"}',
                    disabled: '{settings.activeGroups.classtypes != "recommended"}'
                },
                html: "<i>" + "Recommended classtype settings".t() + "</i>",
            }, {
                xtype: 'radio',
                boxLabel: 'Custom'.t(),
                inputValue: 'custom'
            },{
                name: 'classtypes_custom_settings',
                xtype:'fieldset',
                border: false,
                margin: '0 0 0 7',
                hidden: true,
                disabled: true,
                bind: {
                    hidden: '{settings.activeGroups.classtypes != "custom"}',
                    disabled: '{settings.activeGroups.classtypes != "custom"}'
                }
            }]
        }]
    }, {
        title: 'Categories'.t(),
        header: false,
        itemId: 'categories',
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'component',
            html: '<h2>' + "Categories".t() + '</h2>'
        },{
            xtype: 'component',
            html: '<p>' + 'Categories are a different rule grouping that can span multiple classtypes, such as VOIP access.'.t() + '</p>'
        }, {
            xtype: 'radiogroup',
            name: 'categories',
            columns: 1,
            simpleValue: true,
            bind: '{settings.activeGroups.categories}',
            items: [{
                xtype: 'radio',
                boxLabel: 'Recommended (default)'.t(),
                inputValue: 'recommended'
            },{
                name: 'categories_recommended_settings',
                xtype: 'fieldset',
                border: false,
                margin: '0 0 0 7',
                hidden: true,
                disabled: true,
                bind: {
                    hidden: '{settings.activeGroups.categories != "recommended"}',
                    disabled: '{settings.activeGroups.categories != "recommended"}'
                },
                html: "<i>" + "Recommended category settings".t() + "</i>",
            }, {
                xtype: 'radio',
                boxLabel: 'Custom'.t(),
                inputValue: 'custom'
            },{
                name: 'categories_custom_settings',
                xtype:'fieldset',
                border: false,
                margin: '0 0 0 7',
                hidden: true,
                disabled: true,
                bind: {
                    hidden: '{settings.activeGroups.categories != "custom"}',
                    disabled: '{settings.activeGroups.categories != "custom"}'
                }
            }]
        }]
    }, {
        title: 'Finish'.t(),
        header: false,
        itemId: 'finish',
        items: [{
            xtype: 'component',
            html: '<h2>' + 'Congratulations!'.t() + '</h2>',
        }, {
            xtype: 'component',
            html: '<p><strong>' + 'Intrusion Prevention is now configured and enabled.'.t() + '</strong></p>'
        }]
    }],

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'bottom',
        ui: 'footer',
        defaults: {
            minWidth: 200
        },
        items: [{
            hidden: true,
            bind: {
                text: 'Previous'.t() + ' - <strong>' + '{prevBtnText}' + '</strong>',
                hidden: '{!prevBtn}'
            },
            iconCls: 'fa fa-chevron-circle-left',
            handler: 'onPrev'
        }, '->',  {
            hidden: true,
            bind: {
                text: 'Next'.t() + ' - <strong>' + '{nextBtnText}' + '</strong>',
                hidden: '{!nextBtn}'
            },
            iconCls: 'fa fa-chevron-circle-right',
            iconAlign: 'right',
            handler: 'onNext'
        }, {
            text: 'Close'.t(),
            hidden: true,
            bind: {
                hidden: '{nextBtn}'
            },
            iconCls: 'fa fa-check',
            handler: 'onFinish'
        }]
    }]


});
