package com.android.cts.kfmdmsolutions.ivelt.javascript;

public class UnhideContactButton {
    public static String JS_UNHIDE_CONTACT_BUTTON = " " +
            "    var parent = document.getElementsByTagName('head').item(0);" +
            "    var style = document.createElement('style');" +
            "    style.type = 'text/css';" +
            "    style.innerHTML = \"@media (max-width:700px){\\n\" +\n" +
            "        \"\\t.profile-contact {\\n\" +\n" +
            "        \"\\tdisplay: inline-block !important;\\n\" +\n" +
            "        \"    \\tmargin-left: 0 !important;\\n\" +\n" +
            "        \"\\tposition:absolute;\\n\" +\n" +
            "        \"\\t}\\n\" +\n" +
            "        \"\\t.profile-contact .contact-icons a {\\n\" +\n" +
            "        \"    \\tpadding: 5px;\\n\" +\n" +
            "        \"}\\n\" +\n" +
            "        \"}\";" +
            "    parent.appendChild(style)";
}
