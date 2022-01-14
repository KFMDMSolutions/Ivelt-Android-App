 var parent = document.getElementsByTagName('head').item(0);
    var style = document.createElement('style');
    style.type = 'text/css';
    style.innerHTML = "@media (max-width:700px){\n" +
        "\t.profile-contact {\n" +
        "\tdisplay: inline-block !important;\n" +
        "    \tmargin-right: 0 !important;\n" +
        "\tposition:absolute;\n" +
        "\t}\n" +
        "\t.profile-contact .contact-icons a {\n" +
        "    \tpadding: 5px;\n" +
        "}\n" +
        "}";
    parent.appendChild(style);