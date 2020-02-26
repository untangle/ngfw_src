<!DOCTYPE html>
<%@ page contentType="text/html; charset=utf-8" %>
<html xmlns:uvm="http://java.untangle.com/jsp/uvm">
  <head>
    <meta charset="UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title>Setup Wizard</title>

    <style type="text/css">
        @import "/ext6.2/classic/theme-${extjsTheme}/resources/theme-${extjsTheme}-all.css?s=${buildStamp}";
    </style>

    <script type="text/javascript" src="/ext6.2/ext-all-debug.js?s=${buildStamp}"></script>
    <script type="text/javascript" src="/ext6.2/classic/theme-${extjsTheme}/theme-${extjsTheme}.js?s=${buildStamp}"></script>

    <script type="text/javascript" src="/jsonrpc/jsonrpc.js?s=${buildStamp}"></script>

    <!-- FontAwesome -->
    <link href="/ext6.2/fonts/font-awesome/css/font-awesome.min.css" rel="stylesheet" />

    <!-- Fonts -->
    <link href="/ext6.2/fonts/source-sans-pro/css/fonts.css" rel="stylesheet" type="text/css" />
    <link href="/ext6.2/fonts/roboto-condensed/css/fonts.css" rel="stylesheet" type="text/css" />

    <link href="styles/setup-all.css" rel="stylesheet" />

    <script>
      var rpc = {};
      Ext.onReady(function () {
        rpc.setup = new JSONRpcClient("/setup/JSON-RPC").SetupContext;
        rpc.setup.getSetupWizardStartupInfo(function (result, exception) {
            // if (Ung.Util.handleException(exception)) { return; }
            Ext.applyIf(rpc, result);

            if (!rpc.wizardSettings.steps || rpc.wizardSettings.steps.length == 0) {
                rpc.wizardSettings.steps = ['Welcome', 'License', 'ServerSettings', 'Interfaces', 'Internet', 'InternalNetwork', 'AutoUpgrades', 'Complete'];
            }

            // transform timezones string to array
            var tzArray = [];
            Ext.Array.each(eval(rpc.timezones), function (tz) {
                tzArray.push([tz[0], '(' + tz[1] + ') ' + tz[0]]);
            });
            rpc.timezones = tzArray;

            String.prototype.t = function() {
                if (rpc.language !== 'en') {
                    return rpc.translations[this.valueOf()] || (rpc.language === 'xx' ? '<cite>' + this.valueOf() + '</cite>' : this.valueOf());
                }
                return this.valueOf();
            };

            window.document.title = 'Setup Wizard'.t();

            Ext.Loader.loadScript({
                url: 'script/ung-setup-all.js',
                onLoad: function () {
                    Ext.application({
                        name: 'Ung',
                        extend: 'Ung.Setup'
                    });
                }
            });


        });
      });
    </script>

    <style type="text/css">
        span.fa:before {
            position: relative;
            top: 50%;
            transform: translateY(-50%);
            display: block;
        }
        cite {
            font-style: normal;
        }
    </style>

  </head>
  <body>
</body>
</html>
