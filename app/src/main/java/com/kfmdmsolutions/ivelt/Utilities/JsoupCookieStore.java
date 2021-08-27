package com.kfmdmsolutions.ivelt.Utilities;

import android.content.Context;
import android.webkit.CookieManager;

import java.lang.ref.WeakReference;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class JsoupCookieStore implements java.net.CookieStore{

    WeakReference<Context> contextWeakReference;

    public JsoupCookieStore(Context context){
        contextWeakReference = new WeakReference<>(context);
    }
    @Override
    public void add(URI uri, HttpCookie cookie) {

        String value = cookie.getValue();
        Logger.getInstance(contextWeakReference.get()).log("Cookies, value is " + cookie.getValue() + " setting ");
        try {
            CookieManager.getInstance().setCookie(uri.toURL().toString(), cookie.toString());
            CookieManager.getInstance().flush();
        } catch (MalformedURLException e) {
            Logger.getInstance(contextWeakReference.get()).log("Unable to add cookie", e);
            e.printStackTrace();
        }
    }

    @Override
    public List<HttpCookie> get(URI uri) {

        Logger.getInstance(contextWeakReference.get()).log("Cookies getting cookies");
        try {
            String cookieString = CookieManager.getInstance().getCookie(uri.toURL().toString());
            if (cookieString == null){
                return new ArrayList<>();
            }
            return  HttpCookie.parse(cookieString);
//            String[] cookies = cookieString.split(";");
//            List<HttpCookie> list = new ArrayList<>();
//            for(String str : cookies){
//                list.addAll(HttpCookie.parse(str));
//            }
//
//            Logger.getInstance(contextWeakReference.get()).log("Cookie String is " + cookieString + " list size is " + list.size());
//            list.forEach(cookie -> android.util.Log.d("Cookies", cookie.toString()));
//            return list;

        } catch (MalformedURLException e) {

            Logger.getInstance(contextWeakReference.get()).log("Unable to get cookies", e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<HttpCookie> getCookies() {
        android.util.Log.d("COOKIE", "getting cookies");
        return new ArrayList<>();
    }

    @Override
    public List<URI> getURIs() {
        return null;
    }

    @Override
    public boolean remove(URI uri, HttpCookie cookie) {
        return false;
    }

    @Override
    public boolean removeAll() {
        return false;
    }
}
