"use strict";
function createButton(icon, reference, customClass, title, text, onclick) {
    let li = document.createElement('li');
    let a = document.createElement('a');
    let span = document.createElement('span');
    let img = document.createElement('img');
    img.setAttribute('src', `https://www.ivelt.com/kfmdm/resources/drawable/${icon}`)
    if (reference){
        a.setAttribute('href', reference);
    }
    if (onclick){
        a.setAttribute('onClick', onclick)
    }
    a.setAttribute('class', `button custom-button ${customClass}`);
    a.setAttribute('title',title );
    span.innerText = text;
    a.appendChild(span);
    a.appendChild(img);
    li.appendChild(a);
    return {li, a, span, img}
}

function getPMHref(id) {
    let pm_button = document.querySelector(`#profile${id.replace("post_content", "")} .pm-icon`)
    if (pm_button){
        return pm_button.parentElement.getAttribute("href")
    }
    return null;
}

function addBtn(){
    let btns = document.querySelectorAll('.post-buttons');
    let isPosting = (window.location.href.includes("posting.php"));
    var needUpdating = false;
    for (let i = 0; i < btns.length; i++) {
    //btns.forEach(btn => {
        // Check if custom buttons have been added already, if yes ignore.
        if(btns[i].getElementsByClassName('custom-button').length > 0){
            return;
        }
        needUpdating = true
        //btns[i].querySelectorAll('li.hidden:not(.responsive-menu)').forEach(b => {
        for (let x = 0; x < btns[i].querySelectorAll('li.hidden:not(.responsive-menu)').length; x++) {

            if (btns[i].querySelectorAll('li.hidden:not(.responsive-menu)')[x].getAttribute('class') == "hidden"){
                btns[i].querySelectorAll('li.hidden:not(.responsive-menu)')[x].removeAttribute('class')
            }
        }

        let contentElement = btns[i].parentElement.getElementsByClassName("content").item(0)
        let id = btns[i].parentElement.getAttribute("id") || ""
        let strippedId = id.replace("post_content", "")
        strippedId = strippedId.replace("pr", "")
        if (!isPosting){
            addCopyQuoteButton(btns[i], id.replace("post_content", ""))
            let pm_href = getPMHref(id);
            if (pm_href){
                addSimpleButton(btns[i], 'pm_icon.png', pm_href, "app-pm-icon", 'שיק א פריוואטע מעסעדזש', 'שיק א פריוואטע מעסעדזש')
            }
        }
        let pingOnClick = `ping_user(${strippedId})`
        addSimpleButton(btns[i], 'baseline_alternate_email_black_24dp.png', null, 'ping-icon', 'דערמאן תגובה', 'דערמאן תגובה', pingOnClick)

        if (contentElement.innerHTML.includes("blockquote")) {
            addQuoteLastButton(btns[i], isPosting);
        }
        let responsiveMenu = btns[i].getElementsByClassName('responsive-menu').item(0);
        try {
            btns[i].removeChild(btns[i].getElementsByClassName('responsive-menu').item(0))
        }catch (e) {

        }
        }
    //});
    let navUpdate = addDefaultPage();
    if (needUpdating || navUpdate){
        let navBar = document.querySelector('#nav-footer');
        for (let x = 0; x < navBar.querySelectorAll('li.hidden:not(.responsive-menu)').length; x++) {
        //navBar.querySelectorAll('li.hidden:not(.responsive-menu)').forEach(si => {
            navBar.querySelectorAll('li.hidden:not(.responsive-menu)')[x].setAttribute('class', navBar.querySelectorAll('li.hidden:not(.responsive-menu)')[x].getAttribute('class').replace('hidden', ''))
        }
        //})
        navBar.removeChild(navBar.getElementsByClassName('responsive-menu').item(0))
        parseDocument($('body'));
    }
}

function addSimpleButton(btn, icon, href, customClass, title, text, onclick){
    let button = createButton(icon, href, customClass, title, text, onclick);
    btn.appendChild(button.li);
}

function getQuoteURL(btn){
    let quoteButton = btn.querySelector('a.button.icon-button.quote-icon');
    if (!quoteButton){
        return null;
    }
    let href = quoteButton.getAttribute('href');
    return href;
}
function addCopyQuoteButton(btn, postID){
    let href = getPMHref(postID) || getQuoteURL(btn)
    if (!href){
        addSimpleButton(btn, 'ivelt_logo48.png', null, 'copy-quote', 'ציטיר אין אנדערע אשכול', 'ציטיר אין אנדערע אשכול', `copyQuoteParse("${postID}")`)
        return;
    }
    addSimpleButton(btn, 'ivelt_logo48.png', null, 'copy-quote', 'ציטיר אין אנדערע אשכול', 'ציטיר אין אנדערע אשכול', `copyQuote("${href}", "${postID}")`)
}
function addQuoteLastButton(btn, isPosting) {

    let href = getQuoteURL(btn)
    if (!href){
        return;
    }
    var onclick = null;
    if (isPosting){
       onclick = "last" + btn.querySelector('a.button.icon-button.quote-icon').getAttribute('onclick');
//       button.a.setAttribute("onclick", "last" + onclick);
    }
    let button = createButton('quote_last.png', href + '&last=true', 'quote-last', 'ציטיר בלויז די לעצטע תגובה', 'ציטיר לעצטע', onclick);

    btn.appendChild(button.li);
}

function hideButtons(){
    let hidden = android.getHiddenElements()
    let parsed = JSON.parse(hidden);
    for (let button of parsed){
        hideButton(button)
    }
}

function hideButton(selector){
    let buttons = document.querySelectorAll(selector);
    for (let i = 0; i < buttons.length; i++) {
//    buttons.forEach(button => {
        buttons[i].classList.add('app-hidden');
        if(buttons[i].parentElement.classList.contains('clone-first')){
            buttons[i].parentElement.classList.add('app-hidden')
        }
    }
    //})
}
function addDefaultPage(){
    if (document.querySelector(`#kf-app-default-page`)){
        return false
    }
    let li = document.createElement("li")
    let a = document.createElement('a');
    li.appendChild(a)
    li.setAttribute("class", "rightside")
    li.setAttribute("id", "kf-app-default-page")
    a.setAttribute('onClick', "saveDefaultPage()")
    a.innerText = "מאך די בלאט די דיפאולט בלאט"
    let pagination = document.querySelectorAll("#nav-footer").item(0)
    if (pagination){
        pagination.insertBefore(li, pagination.firstChild);
        return true;
    }
    return false;
}

function saveDefaultPage(){
    let page = window.location.href;
    android.saveDefaultPage(page)
}



function getPostLink(postID){
    return `https://www.ivelt.com/forum/viewtopic.php?p=${postID}#p${postID}`;
}

function copyQuoteParse(post_id){
    var html = document.querySelector(`#post_content${post_id} .content`).innerHTML
    let post_url = getPostLink(post_id)
    var username = getUsername(post_id)
    let converter = new HTML2BBCode();
    html = html.replaceAll("./download", "www.ivelt.com/forum/download")
//    android.copyToClipboard(`[quote="${username}"]${bbcodeParser.htmlToBBCode(html).replaceAll('<br>', '')} [/quote] [url=${post_url}]מקור[/url]`)
    android.copyToClipboard(`[quote="${username}"]${converter.feed(html)} [/quote] [url=${post_url}]מקור[/url]`)
}
function getUsername(post_id, prefix){
    prefix = "p";
    var usernameE = document.querySelector(`#${prefix}${post_id} .username`)
    if (!usernameE){
       var usernameE = document.querySelector(`#p${prefix}${post_id} .username-coloured`)
    }
    var username = ""
    if(usernameE){
       username = usernameE.innerText
    }
    return username
}

function ping_user(post_id){
    let link = getPostLink(post_id)
    if (window.location.href.includes("posting.php")){
        let username = getUsername(post_id,"pr")
        let text = `[url=${link}][quote="${username}"]\n[/quote][/url]`
        insert_text(text)
    }else{
        let username = getUsername(post_id)
        let text = `[url=${link}][quote="${username}"]\n[/quote][/url]`
        try {
              addText(text)
        }
        catch (exception_var) {
              android.copyToClipboard(text)
        }


    }
}
function addText(text){
    var textarea = document.querySelector("#message-box textarea");

    if (!isNaN(textarea.selectionStart)) {
    	var sel_start = textarea.selectionStart;
    	var sel_end = textarea.selectionEnd;
    	mozWrapApp(textarea, text, '');
    	textarea.selectionStart = sel_start + text.length;
    	textarea.selectionEnd = sel_end + text.length;
    } else if (textarea.createTextRange && textarea.caretPos) {
    	if (baseHeight !== textarea.caretPos.boundingHeight) {
    		textarea.focus();
    		storeCaret(textarea);
    	}
    	var caret_pos = textarea.caretPos;
    	caret_pos.text = caret_pos.text.charAt(caret_pos.text.length - 1) === ' ' ? caret_pos.text + text + ' ' : caret_pos.text + text;
    } else {
    	textarea.value = textarea.value + text;
    }
    textarea.focus();
}
function mozWrapApp(txtarea, open, close) {
	var selLength = (typeof(txtarea.textLength) === 'undefined') ? txtarea.value.length : txtarea.textLength;
	var selStart = txtarea.selectionStart;
	var selEnd = txtarea.selectionEnd;
	var scrollTop = txtarea.scrollTop;

	var s1 = (txtarea.value).substring(0,selStart);
	var s2 = (txtarea.value).substring(selStart, selEnd);
	var s3 = (txtarea.value).substring(selEnd, selLength);

	txtarea.value = s1 + open + s2 + close + s3;
	txtarea.selectionStart = selStart + open.length;
	txtarea.selectionEnd = selEnd + open.length;
	txtarea.focus();
	txtarea.scrollTop = scrollTop;

	return;
}

addBtn();
hideButtons();







