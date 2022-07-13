"use strict";
function addSettings(){
    let quickLinks = document.querySelector('#quick-links') ;
    if (!quickLinks){
        return;
    }
    let list = quickLinks.querySelector('.dropdown-contents') ;
    let settingsListItem = document.createElement('li') ;
    if(list.getElementsByClassName('app-settings-icon').length > 0){
        return;
    }
    settingsListItem.setAttribute('class', 'small-icon app-settings-icon') ;
    let settings = document.createElement('a') ;
    settings.innerHTML = 'עפפ סעטטינגס' ;
    settings.setAttribute('href', './settings') ;
    settings.setAttribute('role', 'menuitem') ;
    settingsListItem.appendChild(settings) ;
    list.appendChild(settingsListItem);
}

function addCopyright(){
    let br = document.createElement("br");
    let span = document.createElement("span")
    span.innerText = "App by KF MDM v" + android.getVersionString();
    let copyright = document.querySelectorAll('.copyright').item(0);
    if (copyright){
        copyright.appendChild(br);
        copyright.appendChild(span);
    }
}


addSettings();
addCopyright();