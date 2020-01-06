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

    interface: function (v) { return Map.interfaces[v] || v; },
    country: function (v) { return Map.countries[v] || v; },
    policy: function (v) { return Map.policies[v] || v; },
    webReason: function (v) { return Map.webReasons[v] || v; },
    webCategory: function (v) { return Map.webCategories[v] || v; }

});
