Ext.define('Ung.view.apps.SimpleItem', {
    extend: 'Ext.container.Container',
    alias: 'widget.simpleitem',

    baseCls: 'simpleitem',

    width: 140,
    height: 140,

    viewModel: {
        formulas: {
            html: function (get) {
                var html = '';
                // <tpl if="parentPolicy"><a class="app-item disabled"><tpl elseif="route"><a href="{route}" class="app-item {extraCls}"><tpl else><a class="app-item {extraCls}"></tpl>
                if (get('parentPolicy') || get('installing')) {
                    html += '<a class="app-item disabled">';
                } else {
                    html += '<a href="' + get('route') + '" class="app-item">';
                }
                if (get('app.hasPowerButton')) {
                    html += '<span class="state ' + get('targetState') + '"><i class="fa fa-power-off"></i></span>';
                }
                if (get('licenseMessage')) {
                    html += '<span class="license">' + get('licenseMessage') +  '</span>';
                }
                html += '<img src="' + '/skins/modern-rack/images/admin/apps/' + get('app.name') + '_80x80.png" width=80 height=80/>' +
                        '<span class="app-name">' + get('app.displayName') + '</span>';

                if (get('parentPolicy')) {
                    html += '<span class="parent-policy">[' + get('parentPolicy') + ']</span>';
                }
                html += '</a>';

                return html;
            }
        }
    },

    hidden: true,
    bind: {
        hidden: '{!instanceId && !installing}',
        userCls: '{(parentPolicy || installing) ? "dsbl" : ""}',
        html: '{html}'
    }

    // // layout: {
    // //     type: 'absolute'
    // // },

    // items: [{
    //     xtype: 'component',
    //     cls: 'license',
    //     bind: {
    //         html: '{licenseMessage}'
    //     }
    // }, {
    //     xtype: 'component',
    //     style: { marginTop: '20px' },
    //     bind: {
    //         html: '<img src="' + '/skins/modern-rack/images/admin/apps/{app.name}_80x80.png" width=80 height=80/>'
    //     }
    // }, {
    //     xtype: 'component',
    //     bind: {
    //         userCls: 'state {targetState}',
    //         html: '<i class="fa fa-power-off"></i>'
    //     }
    // }, {
    //     xtype: 'component',
    //     cls: 'app-name',
    //     bind: {
    //         html: '{app.displayName}'
    //     }
    // }, {
    //     xtype: 'component',
    //     cls: 'parent-policy',
    //     hidden: true,
    //     bind: {
    //         html: '[{parentPolicy}]',
    //         hidden: '{!parentPolicy}'
    //     }
    // }, {
    //     xtype: 'component',
    //     cls: 'loader'
    // }],

    // // disabled: true,

    // bind: {
    //     hidden: '{!instanceId && !installing}',
    //     userCls: '{(parentPolicy || installing) ? "dsbl" : ""}',
    //     html: '<a href="{route}">' +
    //         '<span class="state {targetState}"><i class="fa fa-power-off"></i></span>' +
    //         '<span class="license">{licenseMessage}</span>' +
    //         '<img src="' + '/skins/modern-rack/images/admin/apps/{app.name}_80x80.png" width=80 height=80/>' +
    //         '<span class="app-name">{app.displayName}</span>' +
    //         ('{parentPolicy}' ? '<span class="parent-policy">[{parentPolicy}]</span>' : '') +
    //         '<span class="new">NEW</span>' +
    //         '<span class="loader"></span>' +
    //         '</a>'
    // },

});
