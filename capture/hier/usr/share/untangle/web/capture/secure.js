function getServer() {
    var start = document.URL.indexOf('://') + 3;
    var end = document.URL.indexOf('/', start);
    return document.URL.substring(start,end);
}

function imgLoadFailure() {
    var form = document.getElementsByTagName('form');
    var cont =  form[0];
    var newParagraph = document.createElement('p');
    var text = document.createTextNode("NOTICE: The server root certificate is not installed on your computer or device.  This may cause warnings or errors when connecting to HTTPS web sites.");
    var newLine = document.createElement('br');
    newParagraph.appendChild(text);
    newParagraph.appendChild(document.createElement('br'));
    newParagraph.appendChild(document.createElement('br'));
    var newLink = document.createElement('a');
    var linkText = document.createTextNode("Click this link to download and install the root certificate.");
    newLink.appendChild(linkText);
    newLink.href = '/cert';
    newLink.title ="Download server root CA certificate"
    newParagraph.appendChild(newLink);
    cont.parentElement.appendChild(newParagraph);
}

function checkSecureEndpoint() {
    var img = new Image();
    img.onerror = imgLoadFailure;
    img.onabort = imgLoadFailure;
    img.src = 'https://' + getServer() + '/images/BrandingLogo.png';
}
