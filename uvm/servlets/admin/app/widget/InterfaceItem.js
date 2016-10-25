Ext.define('Ung.widget.InterfaceItem', {
    extend: 'Ext.Component',
    alias: 'widget.interfaceitem',

    bind: {
        html: '<p class="name" style="display: {displayWan};">{iface.name}</p>' +
            '<div class="speeds">' +
            '<div class="speed_up"><i class="material-icons">arrow_drop_up</i><span>{tx} kB/s</span></div>' +
            '<div class="speed_down"><i class="material-icons">arrow_drop_down</i><span>{rx} kB/s</span></div>' +
            '</div>' +
            '<p class="name" style="display: {displayNotWan};">{iface.name}</p>' +
            '<i class="material-icons pointer">arrow_drop_down</i>'
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