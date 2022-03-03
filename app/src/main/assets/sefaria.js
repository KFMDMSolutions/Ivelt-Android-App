function addSefaria(){
    var script = document.createElement('script');
    script.setAttribute('type', 'text/javascript');
    script.setAttribute('src', "https://www.sefaria.org/linker.js");
    script.onload = function () {
        sefaria.link({
            contentLang: "hebrew",
            interfaceLang: "english",
        });

    };
    if (document && document.head){
        document.head.appendChild(script);
    }
}
addSefaria();