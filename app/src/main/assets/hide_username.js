function hideUsername(){
    let userName = document.querySelector('.header-avatar .username');
    if (userName && android.shouldHideUsername())
    	userName.innerText = 'הא?';
    	let avatar = document.querySelector('.header-avatar .avatar');
    if (avatar)
    	avatar.style.display = "none";
}
hideUsername();