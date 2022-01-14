function addAutoLoginOption(){
    let autoLogin = document.querySelectorAll('label[for="autologin"]').item(0);
    let checkboxes = autoLogin.parentElement.parentElement;
    let dd = document.createElement("dd");
    let label = document.createElement("label");
    let input = document.createElement("input");
    label.setAttribute("for", "applogin");
    input.setAttribute("type", "checkbox");
    input.setAttribute("name", "applogin");
    input.setAttribute("id", "applogin");
    input.setAttribute("tabindex", 7);
    dd.appendChild(label);
    label.appendChild(input);
    label.innerHTML = label.getInnerHTML() + "געדענק אין עפפ";
    checkboxes.appendChild(dd);

    /* Will only be visible in case of error. */
    let errorDiv = document.querySelectorAll(".error").item(0);
    /* Check for autoLogin, to make sure that we are on login page, and check for errors.*/
    if(autoLogin && errorDiv){
        android.saveCredentials("","");
    }
    let username = document.querySelectorAll("#username").item(0);
    let password = document.querySelectorAll("#password").item(0);
    let savedUsername = android.getUsername();
    let savedPassword = android.getPassword();
    username.value = savedUsername;
    password.value = savedPassword;
    let form = document.querySelectorAll("#login").item(0);
    if (savedUsername && savedPassword){
        let autoLoginCB = document.querySelectorAll("#autologin").item(0);
        let appLogin = document.querySelectorAll("#applogin").item(0);
        appLogin.checked = true;
        autoLoginCB.checked = true;
        document.querySelectorAll('input[type="submit"]').item(0).click();
    }
    form.setAttribute("onsubmit", "onClickLogin()");
}

function onClickLogin() {
    let appLogin = document.querySelectorAll("#applogin").item(0);
    let username = document.querySelectorAll("#username").item(0);
    let password = document.querySelectorAll("#password").item(0);
    if (appLogin.checked) {
        android.saveCredentials(username.value, password.value);
    }else{
        android.saveCredentials("", "");
    }
}

addAutoLoginOption();