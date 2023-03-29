package com.kfmdmsolutions.ivelt.javascript;

public class AddSettingsElement {
    public static final String JS_ADD_ELEMENT_TO_LIST =
            "let quickLinks = document.querySelector('#quick-links') ;" +
            "let list = quickLinks.querySelector('.dropdown-contents') ;" +
            "let settingsListItem = document.createElement('li') ; " +
                    "settingsListItem.classList.add('small-icon') ; " +
                    "let settings = document.createElement('a') ;" +
                    "settings.innerHTML = 'עפפ סעטטינגס' ;" +
                    "settings.setAttribute('href', './settings') ;" +
                    "settings.setAttribute('role', 'menuitem') ;" +
                    "settingsListItem.appendChild(settings) ;" +
                    "settingsListItem.setAttribute('style', 'background-image: url(./styles/prosilver_yidddish/theme/images/icon_topic_poll.gif)') ; " +
                    "list.append(settingsListItem)";
}
