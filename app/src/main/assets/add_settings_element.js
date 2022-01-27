
function addSettings(){
    let quickLinks = document.querySelector('#quick-links') ;
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
    list.append(settingsListItem);
}

addSettings();