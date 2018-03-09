Ext.define('Ung.apps.policymanager.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-policy-manager-status',
    itemId: 'status',
    title: 'Status'.t(),
    scrollable: true,

    viewModel: true,

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    items: [{
        border: false,
        bodyPadding: 10,
        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/icons/apps/policy-manager.svg" width="80" height="80"/>' +
                '<h3>Policy Manager</h3>' +
                '<p>' + 'Policy Manager enables administrators to create different policies and handle different sessions with different policies based on rules.'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appreports',
        }, {
            xtype: 'appremove'
        }]
    }]
});
