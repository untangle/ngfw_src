Ext.define('Ung.widget.InterfaceItem', {
    extend: 'Ext.Component',
    alias: 'widget.interfaceitem',

    bind: {
        html: '<p class="name" style="display: {iface.isWan ? \'block\' : \'none\'};">{iface.name}<sup style="display: {(iface.vrrpEnabled && iface.vrrpMaster) ? \'inline-block\' : \'none\'}">VRRP</sup></p>' +
            '<div class="speeds">' +
            '<div class="speed_up"><i class="fa fa-caret-down fa-lg"></i> <span>{inbound} kB/s</span></div>' +
            '<div class="speed_down"><i class="fa fa-caret-up fa-lg"></i> <span>{outbound} kB/s</span></div>' +
            '</div>' +
            '<p class="name" style="display: {!iface.isWan ? \'block\' : \'none\'};">{iface.name}<sup style="display: {(iface.vrrpEnabled && iface.vrrpMaster) ? \'inline-block\' : \'none\'}">VRRP</sup></p>' +
            // '<p class="name">{iface.vrrpEnabled}</p>' +
            '<span style="display: {!iface.isWan ? \'block\' : \'none\'}; margin-top: 5px; font-size: 11px;"><img src="' + '/skins/default/images/admin/icons/interface-devices.png" height="16"><br/>{devicesCount}</span>' +
            '<i class="fa fa-caret-down fa-lg pointer"></i>'
    },

    viewModel: {
        formulas: {
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
