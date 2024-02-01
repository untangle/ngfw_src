Ext.define('Ung.view.apps.SimpleItem', {
    extend: 'Ext.container.Container',
    alias: 'widget.simpleitem',

    baseCls: 'simpleitem',

    width: 135,
    height: 135,

    viewModel: {
        formulas: {
            html: function (get) {
                var html = '';
                if (get('parentPolicy') || get('installing')) {
                    html += '<a class="app-item">';
                } else {
                    html += '<a href="' + get('route') + '" class="app-item">';
                }
                if (get('app.hasPowerButton')) {
                    html += '<span class="state ' + get('powerCls') + '"><i class="fa fa-power-off"></i></span>';
                }
                if (get('licenseMessage')) {
                    html += '<span class="license">' + get('licenseMessage') +  '</span>';
                }
                html += '<img src="' + '/icons/apps/' + get('app.name') + '.svg" width=80 height=80/>' +
                        '<span class="app-name">' + get('app.displayName') + '</span>';

                if (get('parentPolicy')) {
                    html += '<span class="parent-policy">[' + get('parentPolicy') + ']</span>';
                }
                html += '<span class="loader"></span>';
                html += '</a>';

                return html;
            }
        }
    },

    hidden: true,
    bind: {
        hidden: '{!instanceId && !installing}',
        disabled: '{parentPolicy || installing}',
        userCls: '{parentPolicy ? "from-parent" : (installing ? "installing" : "")}',
        html: '{html}'
    }
});
