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

function addBtn(){
    let btns = document.querySelectorAll('.post-buttons');
    let isPosting = (window.location.href.includes("posting.php"));
    var needUpdating = false;
    btns.forEach(btn => {

        needUpdating = true
        // Check if custom buttons have been added already, if yes ignore.
        if(btn.getElementsByClassName('custom-button').length > 0){
            return;
        }
        btn.querySelectorAll('li.hidden:not(.responsive-menu)').forEach(b => {
            if (b.getAttribute('class') == "hidden"){
                b.removeAttribute('class')
            }
        })

        let contentElement = btn.parentElement.getElementsByClassName("content").item(0)
//        addSimpleButton(btn, 'ivelt_logo48.png', null, "logo-class", 'logo', 'logo')
        if (!isPosting){
            let id = btn.parentElement.getAttribute("id")
            addSimpleButton(btn, 'share.png', null, 'share-icon', 'טייל מיט די תגובה','טייל מיט', `sharePost(${id})`)
            addCopyQuoteButton(btn, id.replace("post_content", ""))

        }
        if (contentElement.innerHTML.includes("blockquote")) {
            addQuoteLastButton(btn, isPosting);
        }
        let responsiveMenu = btn.getElementsByClassName('responsive-menu').item(0);
        btn.removeChild(btn.getElementsByClassName('responsive-menu').item(0))
    });
    if (needUpdating){
        let navBar = document.querySelector('#nav-footer');
        navBar.querySelectorAll('li.hidden:not(.responsive-menu)').forEach(si => {
            si.setAttribute('class', si.getAttribute('class').replace('hidden', ''))
        })
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
    let href = getQuoteURL(btn)
    if (!href){
        addSimpleButton(btn, 'ivelt_logo48.png', null, 'copy-quote', 'ציטיר אין אנדערע אשכול', 'ציטיר אין אנדערע אשכול', `copyQuoteParse("${postID}")`)
        return;
    }
//    console.log(`copyQuote(${href})`)
    console.log(`copyQuote("${href}", "${postID}")`)
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
    let button = createButton('quote_last.png', href + '&last=true', 'quote-last', 'ציטיר בלויז די לעצטע תגובה', 'ציטור לעצטע', onclick);

    btn.appendChild(button.li);
}

function hideButtons(){
    let hidden = android.getHiddenElements()
    let parsed = JSON.parse(hidden);
    for (button of parsed){
        hideButton(button)
    }
//    hideButton('.logo-class')
}

function hideButton(selector){
    let buttons = document.querySelectorAll(selector);
    console.log("reportButtons " + buttons.length)
    buttons.forEach(button => {
        button.classList.add('app-hidden');
        if(button.parentElement.classList.contains('clone-first')){
            button.parentElement.classList.add('app-hidden')
        }
    })
}

function addDefaultPage(){

    let a = document.createElement('a');
    a.setAttribute('onClick', "saveDefaultPage()")
    a.innerText = "מאך די בלאט די דיפאולט בלאט •"
    let pagination = document.querySelectorAll(".action-bar.top .pagination").item(0)
    if (pagination){
        pagination.insertBefore(a, pagination.firstChild);
    }
}

function saveDefaultPage(){
    let page = window.location.href;
    android.saveDefaultPage(page)
}

function addCopyright(){
    let br = document.createElement("br");
    let span = document.createElement("span")
    span.innerText = "App by KF MDM v" + android.getVersionString();
    let copyright = document.querySelectorAll('.copyright').item(0);
    copyright.appendChild(br);
    copyright.appendChild(span);
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
    return `https://www.ivelt.com/forum/viewtopic.php?p=${postID}#p${postID}`;
}

//theSelection = divArea.innerHTML.replace(/<br>/ig, '\n');
//              theSelection = theSelection.replace(/<br\/>/ig, '\n');
function copyQuoteParse(post_id){
    var html = document.querySelector(`#post_content${post_id} .content`).innerHTML
    let post_url = getPostLink(post_id)
    var usernameE = document.querySelector(`#p${post_id} .username`)
    if (!usernameE){
        var usernameE = document.querySelector(`#p${post_id} .username-coloured`)
    }
    var username = ""
    if(usernameE){
        username = usernameE.innerText
    }
    android.copyToClipboard(`[quote="${username}"]${bbcodeParser.htmlToBBCode(html).replaceAll('<br>', '')} [/quote] [url=${post_url}]מקור[/url]`)
}

addBtn();
hideButtons();
addCopyright();
addDefaultPage();







