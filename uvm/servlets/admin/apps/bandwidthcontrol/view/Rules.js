Ext.define('Ung.apps.bandwidthcontrol.view.Rules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-bandwidthcontrol-rules',
    itemId: 'rules',
    title: 'Rules'.t(),

    viewModel: true,
    disabled: true,
    bind: {
        disabled: '{!isConfigured}'
    }
});
