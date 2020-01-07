Ext.define('Ung.util.Renderer2', {
    singleton: true,
    alternateClassName: 'Renderer2',

    memory: function (value) {
        var meg = value/1024/1024;
        return (Math.round( meg*10 )/10).toString() + ' MB';
    },

    disk: function (value) {
        var gig = value/1024/1024/1024;
        return (Math.round( gig*10 )/10).toString() + ' GB';
    }

});
