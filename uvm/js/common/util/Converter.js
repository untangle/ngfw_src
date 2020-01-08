Ext.define('Ung.util.Converter', {
    singleton: true,
    alternateClassName: 'Converter',

    mapValueFormat: '{0} [{1}]'.t(),

    timestamp: function (value) {
        if(Renderer.timestampOffset === null){
            Renderer.timestampOffset =  (new Date().getTimezoneOffset() * 60000) + rpc.timeZoneOffset;
        }
        if (!value) { return ''; }
        if ((typeof(value) === 'object') && value.time) { value = value.time; }
        if (value < 2696400000){ value *= 1000; }
        var date = new Date(value);
        date.setTime(value + Renderer.timestampOffset);
        return Ext.util.Format.date(date, 'timestamp_fmt'.t());
    },

    interface: function (v) {
        return (Map.interfaces[v] || 'None'.t()) + ' [' + v + ']';
    },
    country: function (v) { return Map.countries[v] || v; },
    policy: function (v) { return Map.policies[v] || 'None'.t(); },
    webReason: function (v) { return Map.webReasons[v] || 'no rule applied'.t(); },
    webCategory: function (v) { return Map.webCategories[v] || v; },
    protocol: function (v) { return Map.protocols[v] || v; },

    icmp: function (v) { return Map.icmps[v] || 'Unasigned'.t(); },
    ipaddr: function (v) { return v || ''; },

    httpMethod: function (v) { return Map.httpMethods[v] || v; },
    loginFailure: function (v) { return Map.loginFailureReasons[v] || ''; },

    priority: function (v) { return Map.httpMethods[v] || v; },
    emailAction: function (v) { return Map.emailActions[v] || 'unknown action'.t(); }

});
