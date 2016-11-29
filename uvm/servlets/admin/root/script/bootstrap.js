/**
 * Bootstrap file used to initialize the RPC backend connector
 * Only after RPC initialization and localization is applied, the Untangle application is actually created
 */

try {
    // Initialize the main RPC object which holds all data and the methods to communicate with backend
    var rpc = new JSONRpcClient('http://localhost/webui/JSON-RPC');

    // Populate rpc with the extra webui features
    try {
        var startUpInfo = rpc.UvmContext.getWebuiStartupInfo();
    } catch (ex) {
        Ext.get('app-spinner').destroy();
        Ext.get('app-message').setHtml('<span>Fatal error!</span> <br/><br/>' + ex);
    }

    Ext.apply(rpc, startUpInfo);

    /**
     * Attatch prototype method to strings for applying translations
     * Usage: "String value".t()
     * If the string does not have a translation key associated, the UI will highlight that
     */
    String.prototype.t = function() {
        return rpc.translations[this.valueOf()] || '<cite>' + this.valueOf() + '</cite>';
    };

    // Create Untangle application
    Ext.Loader.loadScript({
        url: 'script/ung-all.js',
        onLoad: function () {
            Ext.application({
                name: 'Ung',
                extend: 'Ung.Application',
                rpc: rpc
            });
        }
    });

    // Disable Ext Area to avoid unwanted debug messages
    Ext.enableAria = false;
    Ext.enableAriaButtons = false;
    Ext.enableAriaPanels = false;

    // important! override the default models ext idProperty so it does not interfere with backend 'id'
    Ext.data.Model.prototype.idProperty = '_id';
}
catch(err) {
    Ext.get('app-spinner').destroy();
    Ext.get('app-message').setHtml('<span>Fatal error!</span> <br/><br/> Backend connection cannot be established!');
}
