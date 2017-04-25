Ext.define('Ung.widget.InterfaceItem', {
    extend: 'Ext.Component',
    alias: 'widget.interfaceitem',

    bind: {
        html: '<p class="name" style="display: {displayWan};">{iface.name}</p>' +
            '<div class="speeds">' +
            '<div class="speed_up"><i class="fa fa-caret-down fa-lg"></i> <span>{inbound} kB/s</span></div>' +
            '<div class="speed_down"><i class="fa fa-caret-up fa-lg"></i> <span>{outbound} kB/s</span></div>' +
            '</div>' +
            '<p class="name" style="display: {displayNotWan};">{iface.name}</p>' +
            '<span style="display: {displayNotWan}; margin-top: 15px;"><img src="' + '/skins/default/images/admin/icons/interface-devices.png"><br/>{devicesCount}</span>' +
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
            outbound: function (get) {
                var field = get('iface.isWan') ? 'tx' : 'rx';
                var stats = get('stats').getData(),
                    propStr = 'interface_' + get('iface.interfaceId') + '_' + field + 'Bps';
                if (stats.hasOwnProperty(propStr)) {
                    return (stats[propStr]/1024).toFixed(2);
                } else {
                    return '-';
                }
            },
            inbound: function (get) {
                var field = get('iface.isWan') ? 'rx' : 'tx';
                var stats = get('stats').getData(),
                    propStr = 'interface_' + get('iface.interfaceId') + '_' + field + 'Bps';
                if (stats.hasOwnProperty(propStr)) {
                    return (stats[propStr]/1024).toFixed(2);
                } else {
                    return '-';
                }
            }
        }
    }
});
