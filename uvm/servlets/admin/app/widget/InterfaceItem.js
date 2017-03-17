Ext.define('Ung.widget.InterfaceItem', {
    extend: 'Ext.Component',
    alias: 'widget.interfaceitem',

    bind: {
        html: '<p class="name" style="display: {displayWan};">{iface.name}</p>' +
            '<div class="speeds">' +
            '<div class="speed_up"><i class="fa fa-caret-down fa-lg"></i> <span>{tx} kB/s</span></div>' +
            '<div class="speed_down"><i class="fa fa-caret-up fa-lg"></i> <span>{rx} kB/s</span></div>' +
            '</div>' +
            '<p class="name" style="display: {displayNotWan};">{iface.name}</p>' +
            '<i class="fa fa-caret-down fa-lg pointer"></i>'
    },

    viewModel: {
        formulas: {
            displayWan: function (get) {
                return get('iface.isWan') ? 'block' : 'none';
            },
            displayNotWan: function (get) {
                return get('iface.isWan') ? 'none' : 'block';
            },
            tx: function (get) {
                var stats = get('stats').getData(),
                    propStr = 'interface_' + get('iface.interfaceId') + '_txBps';
                if (stats.hasOwnProperty(propStr)) {
                    return (stats[propStr]/1024).toFixed(2);
                } else {
                    return '-';
                }
            },
            rx: function (get) {
                var stats = get('stats').getData(),
                    propStr = 'interface_' + get('iface.interfaceId') + '_rxBps';
                if (stats.hasOwnProperty(propStr)) {
                    return (stats[propStr]/1024).toFixed(2);
                } else {
                    return '-';
                }
            }
        }
    }
});
