Ext.define('Ung.util.Renderer2', {
    singleton: true,
    alternateClassName: 'Renderer2',

    interface: function (id) {
        return Map.interfaces[id] || id;
    }

});
