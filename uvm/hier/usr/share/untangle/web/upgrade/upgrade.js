function doIt()
{
    request.open("GET", "/upgrade.log", false);
    request.send(null);
    if (request.readyState == 4) {
        if (request.status == 200 ) {
            var lines = request.responseText.split("\n");
            var lastLog = "";
            for (var i=Math.max(0,lines.length-30) ; i < lines.length-1; i++) {
                lastLog += lines[i] + "<br>";
            }
            document.getElementById("upgrade").innerHTML = lastLog;
            return;
        } else {
            location.href = "/admin";
        }
    }
}

var request = false;
/*@cc_on @*/
/*@if (@_jscript_version >= 5)
// JScript gives us Conditional compilation, we can cope with old IE versions.
// and security blocked creation of the objects.
try {
request = new ActiveXObject("Msxml2.REQUEST");
} catch (e) {
try {
request = new ActiveXObject("Microsoft.REQUEST");
} catch (E) {
request = false;
}
}
@end @*/

if (!request && typeof XMLHttpRequest != 'undefined') {
    try {
        request = new XMLHttpRequest();
    } catch (e) {
        request=false;
    }
}
if (!request && window.createRequest) {
    try {
        request = window.createRequest();
    } catch (e) {
        request=false;
    }
}

setInterval(function() {doIt();}, 1000); // every second
