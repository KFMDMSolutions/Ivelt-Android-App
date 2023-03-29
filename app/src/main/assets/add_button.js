'use strict';
function createButton(reference, customClass, title, text, onclick, itext) {
    if (itext === undefined) {
        itext = '';
    }
    let li = document.createElement('li');
    let a = document.createElement('a');
    let span = document.createElement('span');
    let i = document.createElement('i');
    //let img = document.createElement('img');
    //img.setAttribute('src', `https://www.ivelt.com/kfmdm/resources/drawable/${icon}`)
    if (reference){
        a.setAttribute('href', reference);
    }
    if (onclick){
        a.setAttribute('onClick', onclick)
    }
    a.setAttribute('class', 'button button-icon-only custom-btn ');
    a.setAttribute('title',title );
    i.setAttribute('class', `icon ${customClass} fa-fw`);
    i.setAttribute('aria-hidden', 'true');
    i.innerText = itext;
    span.innerText = text;
    a.appendChild(i);
    a.appendChild(span);
    li.appendChild(a);

    return {li, a, span, i}
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

    Array.prototype.forEach.call(btns,function(btn) {
        // Check if custom buttons have been added already, if yes ignore.
        if(btn.getElementsByClassName('custom-btn').length > 0){
            return;
        }
        needUpdating = true
        Array.prototype.forEach.call(btn.querySelectorAll('li.hidden:not(.responsive-menu)'),function(b) {
            if (b.getAttribute('class') == "hidden"){
                b.removeAttribute('class')
            }
        })

        let contentElement = btn.parentElement.getElementsByClassName("content").item(0)
        let id = btn.parentElement.getAttribute("id") || ""
        let strippedId = id.replace("post_content", "")
        strippedId = strippedId.replace("pr", "")
        if (!isPosting){
            if (android.isSharePostAvailable()){
                addSimpleButton(btn, '#', 'fa-share-alt', 'טייל מיט די תגובה','טייל מיט', `sharePost(${id})`)
            }
            addCopyQuoteButton(btn, id.replace("post_content", ""))
            let pm_href = getPMHref(id);
            if (pm_href){
                addSimpleButton(btn, pm_href, "fa-commenting", 'שיק א פריוואטע מעסעדזש', 'שיק א פריוואטע מעסעדזש')
            }
        }
        let pingOnClick = `ping_user(${strippedId})`
        addSimpleButton(btn, '#', 'fa-at', 'דערמאן תגובה', 'דערמאן תגובה', pingOnClick)
        if (contentElement.innerHTML.includes("blockquote")) {
            addQuoteLastButton(btn, isPosting);
        }

        let responsiveMenu = btn.getElementsByClassName('responsive-menu').item(0);
        try {
            btn.removeChild(btn.getElementsByClassName('responsive-menu').item(0))
        }catch (e) {

        }
    });
    let navUpdate = addDefaultPage();
    if (needUpdating || navUpdate){
        let navBar = document.querySelector('#nav-footer');
        Array.prototype.forEach.call(navBar.querySelectorAll('li.hidden:not(.responsive-menu)'),function(si) {
            si.setAttribute('class', si.getAttribute('class').replace('hidden', ''))
        })
        navBar.removeChild(navBar.getElementsByClassName('responsive-menu').item(0))
        parseDocument($('body'));
    }
}

function addSimpleButton(btn, href, customClass, title, text, onclick){
    let button = createButton(href, customClass, title, text, onclick);
    btn.appendChild(button.li);
}

function getQuoteURL(btn){

    try{
        let quoteButton = btn.querySelector('i.icon.fa-quote-left.fa-fw');
        let quoteUrl = quoteButton.parentElement;
            if (!quoteUrl){
                return null;
            }
            let href = quoteUrl.getAttribute('href');
            return href;
    }catch (e){

    }

}
function addCopyQuoteButton(btn, postID){
    let href = getPMHref(postID) || getQuoteURL(btn)
    if (!href){
        addSimpleButton(btn, '#', 'fa-copy', 'ציטיר אין אנדערע אשכול', 'ציטיר אין אנדערע אשכול', `copyQuoteParse("${postID}")`)
        return;
    }
    addSimpleButton(btn, '#', 'fa-copy', 'ציטיר אין אנדערע אשכול', 'ציטיר אין אנדערע אשכול', `copyQuote("${href}", "${postID}")`)
}
function addQuoteLastButton(btn, isPosting) {

    let href = getQuoteURL(btn)
    if (!href){

        return;
    }
    var onclick = null;
    if (isPosting){
       onclick = "last" + btn.querySelector('i.icon.fa-quote-left.fa-fw').parentElement.getAttribute('onclick');
//       button.a.setAttribute("onclick", "last" + onclick);
    }
    let button = createButton(href + '&last=true', 'fa-quote-left last', 'ציטיר בלויז די לעצטע תגובה', 'ציטיר לעצטע', onclick, '1');

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
    Array.prototype.forEach.call(buttons,function(button) {
        if(selector == '.profile-contact'){
            button.classList.add('app-hidden');
        }else{
            if(button.parentElement.parentElement.classList.contains('clone-first')){
                button.parentElement.parentElement.classList.add('app-hidden')
            }
            else{
                button.parentElement.classList.add('app-hidden');
            }
        }
    })
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

function sharePost(id){

    console.log("Sharing ID " + id)
    console.log("selector " + `#${id}`)

    let content = id.getElementsByClassName("content").item(0);
    let a = document.createElement("a")
    let postID = id.id.replace("post_content", "")
    let link = getPostLink(postID)
    a.setAttribute("href",link )
    a.innerText = " זעה אויף אייוועלט: "
    content.appendChild(a)
    let html = content.innerHTML;
    content.removeChild(a)
    android.sharePost(html)
}

function getPostLink(postID){
    return `https://www.IVELT.com/forum/viewtopic.php?p=${postID}#p${postID}`;
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

function getPostDetails(post_id, prefix){
    if (prefix === undefined) {
        prefix = 'p';
    }
    var usernameE = document.querySelector(`#${prefix}${post_id} .username`)
    if (!usernameE){
       var usernameE = document.querySelector(`#p${prefix}${post_id} .username-coloured`)
    }
    var username = ""
    if(usernameE){
       username = usernameE.innerText
    }

    var idE = document.querySelector(`#${prefix}${post_id} .username`);
    if (!idE){
       var usernameE = document.querySelector(`#p${prefix}${post_id} .username-coloured`)
    }
    var usernameLink = ""
    let id =""
    if(idE){
       usernameLink = idE.href;
       id = usernameLink.split("u=")[1];
    }
    var tsE = document.querySelector(`#${prefix}${post_id} time`)
    if (!tsE){
        var tsE = document.querySelector(`#${prefix}${post_id} [href='#postingbox']`).getAttribute('onclick')
        if(tsE)
            var time = tsE.match('(?<=time:)(.*)(?=,user)')[0];
    }else{
        var ts = Date.parse(tsE.dateTime)
        var time = ts / 1000
    }



    return {
        "username":username,
        "id":id,
        "time":time
    };
}

function ping_user(post_id){

    if (window.location.href.includes("posting.php")){
        let PostDetails = getPostDetails(post_id,"pr")
        let text = `[quote="${PostDetails.username}" user_id=${PostDetails.id} time=${PostDetails.time} post_id=${post_id}]\n[/quote]`
        insert_text(text)
    }else{
        let PostDetails = getPostDetails(post_id)
        let text = `[quote="${PostDetails.username}" user_id=${PostDetails.id} time=${PostDetails.time} post_id=${post_id}]\n[/quote]`

        try {
            addText(text)
        }catch (exception_var) {
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

function addFocus(){
    var element = document.querySelectorAll(".topic-tools ,.tools-icon, .spoilbtn");
    for (let i = 0; i < element.length; i++) {
        //x.setAttribute("style", "outline-style: inset;");
        element[i].setAttribute("tabindex", "0");
        element[i].addEventListener("focus", "hover", function () {
          this.style.outline = "10px","dotted","yellow";
        });
    }
    var imageElement = document.querySelectorAll(".postimage");
    for (let i = 0; i < imageElement.length; i++) {
        var parent = imageElement[i].parentNode;
        var wrapper = document.createElement('a');
        wrapper.setAttribute("href","");
        wrapper.setAttribute("onclick","console.log('image clicked')");
        wrapper.setAttribute("target","blank");
        wrapper.setAttribute("rel","nofollow");
        parent.replaceChild(wrapper, imageElement[i]);
        wrapper.appendChild(imageElement[i]);


    }
}

addBtn();
hideButtons();
addFocus();







