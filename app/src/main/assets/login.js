function addAutoLoginOption(){
    let autoLogin = document.querySelectorAll('label[for="autologin"]').item(0);
    console.log("autologin is " + autoLogin)
    if (!autoLogin){
        console.log("returning")
        return;
    }

    console.log("after if")
    let checkboxes = autoLogin.parentElement.parentElement;
    let dd = document.createElement("dd");
    let label = document.createElement("label");
    let input = document.createElement("input");
    label.setAttribute("for", "applogin");
    input.setAttribute("type", "checkbox");
    input.setAttribute("name", "applogin");
    input.setAttribute("id", "applogin");
    input.setAttribute("tabindex", 7);
    dd.appendChild(input);
    dd.appendChild(label);
    label.innerHTML = "געדענק אין עפפ";
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
        console.log("logging in automatically");
        let autoLoginCB = document.querySelectorAll("#autologin").item(0);
        let appLogin = document.querySelectorAll("#applogin").item(0);
        appLogin.checked = true;
        autoLoginCB.checked = true;
        document.querySelectorAll('input[type="submit"]').item(0).click();
    }else{
        console.log("username and/or password not saved")
    }
    form.setAttribute("onsubmit", "onClickLogin()");
}

function onClickLogin() {
    let appLogin = document.querySelectorAll("#applogin").item(0);
    if (appLogin.checked) {
        console.log("user saved in app")
        let username = document.querySelectorAll("#username").item(0);
        let password = document.querySelectorAll("#password").item(0);
        android.saveCredentials(username.value, password.value);
    }else{
        console.log("user did not save in app")
        android.saveCredentials("", "");
    }
}

addAutoLoginOption();