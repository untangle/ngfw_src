Ext.define('Ung.view.apps.Simple', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.apps-simple',

    scrollable: 'y',

    border: false,
    bodyBorder: false,

    // layout: {
    //     type: 'vbox',
    //     align: 'middle'
    // },

    defaults: {
        border: false,
        bodyBorder: false
    },

    items: [{
        xtype: 'container',
        items: [{
            xtype: 'component',
            cls: 'apps-title',
            html: 'Apps'.t()
        }]
    }, {
        xtype: 'container',
        // layout: {
        //     type: 'column'
        // },
        cls: 'simple',
        itemId: '_apps'
    }, {
        xtype: 'container',
        items: [{
            xtype: 'component',
            cls: 'apps-title',
            html: 'Service Apps'.t()
        }]
    }, {
        xtype: 'container',
        layout: {
            type: 'column'
        },
        itemId: '_services'
    }]
});

// Ext.define('Ung.view.apps.Basic', {
//     extend: 'Ext.panel.Panel',
//     alias: 'widget.apps-basic',

//     scrollable: true,
//     layout: { type: 'vbox', align: 'stretch' },
//     items: [{
//         xtype: 'component',
//         cls: 'apps-title',
//         hidden: true,
//         bind: {
//             html: 'Apps'.t() + ' ({appsCount})',
//             hidden: '{!policyName}'
//         }
//     }, {
//         xtype: 'dataview',
//         bind: '{installedApps}',
//         tpl: '<tpl for=".">' +
//                 '<tpl if="parentPolicy"><a class="app-item disabled"><tpl elseif="route"><a href="{route}" class="app-item {extraCls}"><tpl else><a class="app-item {extraCls}"></tpl>' +
//                 '<tpl if="hasPowerButton && runState"><span class="state {runState}"><i class="fa fa-power-off"></i></span></tpl>' +
//                 '<tpl if="licenseMessage"><span class="license">{licenseMessage}</span></tpl>' +
//                 '<img src="' + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
//                 '<span class="app-name">{displayName}</span>' +
//                 '<tpl if="parentPolicy"><span class="parent-policy">[{parentPolicy}]</span></tpl>' +
//                 '<span class="new">NEW</span>' +
//                 '<span class="loader"></span>' +
//                 '</a>' +
//             '</tpl>',
//         itemSelector: 'a'
//     }, {
//         xtype: 'component',
//         padding: '20 40',
//         html: 'No Apps ...',
//         // items: [{
//         //     xtype: 'button',
//         //     scale: 'large',
//         //     text: 'Install Apps'.t(),
//         //     padding: '0 20 0 0',
//         //     iconCls: 'fa fa-download',
//         //     handler: 'showInstall'
//         // }],
//         hidden: true,
//         // bind: {
//         //     hidden: '{appsCount > 0 || !policyName}'
//         // }
//     }, {
//         xtype: 'component',
//         cls: 'apps-title',
//         hidden: true,
//         bind: {
//             html: 'Service Apps'.t() + ' ({servicesCount})',
//             hidden: '{!policyName}'
//         }
//     }, {
//         xtype: 'dataview',
//         bind: '{installedServices}',
//         tpl: '<tpl for=".">' +
//                 '<tpl if="route"><a href="{route}" class="app-item {extraCls}"><tpl else><a class="app-item {extraCls}"></tpl>' +
//                 '<tpl if="hasPowerButton && runState"><span class="state {runState}"><i class="fa fa-power-off"></i></span></tpl>' +
//                 '<tpl if="licenseMessage"><span class="license">{licenseMessage}</span></tpl>' +
//                 '<img src="' + '/skins/modern-rack/images/admin/apps/{name}_80x80.png" width=80 height=80/>' +
//                 '<span class="app-name">{displayName}</span>' +
//                 '<span class="new">NEW</span>' +
//                 '<span class="loader"></span>' +
//                 '</a>' +
//             '</tpl>',
//         itemSelector: 'a'
//     }, {
//         xtype: 'component',
//         padding: '20 40',
//         html: 'No Service Apps ...',
//         // items: [{
//         //     xtype: 'button',
//         //     scale: 'large',
//         //     text: 'Install Apps'.t(),
//         //     padding: '0 20 0 0',
//         //     iconCls: 'fa fa-download',
//         //     handler: 'showInstall'
//         // }],
//         hidden: true,
//         // bind: {
//         //     hidden: '{servicesCount > 0 || !policyName}'
//         // }
//     }]
// });
