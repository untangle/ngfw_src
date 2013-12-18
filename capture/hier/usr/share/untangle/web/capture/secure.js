function getServer() {
    var start = document.URL.indexOf('://') + 3;
    var end = document.URL.indexOf('/', start);
    return document.URL.substring(start,end);
}

function imgLoadFailure(e) {
    console.log("imgLoadFailure",e);
    var cont = document.getElementById('submit');
    var newParagraph = document.createElement('p');
    var text = document.createTextNode("HTTPS certificate not installed !");
    var newLine = document.createElement('br');
    newParagraph.appendChild(text);
    newParagraph.appendChild(newLine);
    var newLink = document.createElement('a');
    var linkText = document.createTextNode("Click here to download the HTTPS certificate");
    newLink.appendChild(linkText);
    newLink.href = '/cert';
    newLink.title ="Download HTTPS cert"
    newParagraph.appendChild(newLink);
    cont.parentElement.appendChild(newParagraph);
}


function checkSecureEndpoint() {
    var img = new Image();
    img.onerror = imgLoadFailure;
    img.onabort = imgLoadFailure;
    img.src = 'https://' + getServer() + '/images/BrandingLogo.png';
}

