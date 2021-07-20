package com.android.cts.ivelt;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Utils {
    static final Executor executor = Executors.newSingleThreadExecutor();
    static void executeAsync(Runnable runnable){
        executor.execute(runnable);
    }

    public static Map<String, String> convertCookies(String cookie){
        Map<String,String> cookies = new HashMap<>();
        if (cookie == null){
            return cookies;
        }
        String[] cookieArray = cookie.split(";");
        for (String singleCookie : cookieArray){
            String[] cookieParts = singleCookie.split("=");
            if (cookieParts.length == 2){
                cookies.put(cookieParts[0], cookieParts[1]);
            }
        }
        return cookies;
    }

    public static SSLSocketFactory socketFactory(){

        TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }};
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());
            return sslContext.getSocketFactory();
        }catch (NoSuchAlgorithmException | KeyManagementException e){
            android.util.Log.d("SSL", "FAILED");
            throw new RuntimeException("Failed to create Factory", e);
        }
    }

}
