
function addSettings(){
    let quickLinks = document.querySelector('#quick-links') ;
    if (!quickLinks){
        return;
    }
    let list = quickLinks.querySelector('.dropdown-contents') ;
    let settingsListItem = document.createElement('li') ;
    if(list.getElementsByClassName('icon fa-gear fa-fw').length > 0){
        return;
    }


    let settings = document.createElement('a') ;
    settings.setAttribute('href', './settings') ;
    settings.setAttribute('role', 'menuitem') ;
    let settingsIcon = document.createElement('i') ;
    settingsIcon.setAttribute('class', 'icon fa-gear fa-fw') ;
    settingsTitle = document.createElement('span') ;
    settingsTitle.innerHTML = 'עפפ סעטטינגס' ;
    settings.appendChild(settingsIcon) ;
    settings.appendChild(settingsTitle) ;
    settingsListItem.appendChild(settings) ;
    list.append(settingsListItem);
}

function addCopyright(){
    let span = document.createElement("span")
    span.innerText = "App by KF MDM v" + android.getVersionString();
    let copyright = document.querySelectorAll('.copyright').item(0);
    if (copyright){
        copyright.appendChild(span);
    }
}


addSettings();
addCopyright();