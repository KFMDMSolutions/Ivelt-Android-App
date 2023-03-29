package com.android.cts.kfmdmsolutions.ivelt.Utilities;

import android.content.Context;

import androidx.annotation.NonNull;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Utils {
    static final Executor executor = Executors.newSingleThreadExecutor();
    public static void executeAsync(Runnable runnable){
        executor.execute(runnable);
    }

    public static void executeAsync(Runnable runnable, int delay){
        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutor.schedule(runnable, delay, TimeUnit.SECONDS);
        scheduledExecutor.shutdown();
        try {
            scheduledExecutor.awaitTermination(delay + 1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

    public static File getSNQFile(Context context){
        File file = new File(context.getFilesDir().getAbsoluteFile() + File.separator + "SNQ");
        if (!file.exists()){
            file.mkdirs();
        }
        return file;
    }

    public static String readTextFile(Context context, int res) throws IOException {
        String string = "";
        StringBuilder stringBuilder = new StringBuilder();
        InputStream is = context.getResources().openRawResource(res);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        while (true) {
            try {
                if ((string = reader.readLine()) == null) break;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            stringBuilder.append(string).append("\n");
        }
        is.close();
        return stringBuilder.toString();
    }

    @NonNull
    public static Connection getConnection(String url, String useragent, Context context) {
        Connection connection = Jsoup.connect(url)
                .cookieStore(new JsoupCookieStore(context))
                .header("Connection", "keep-alive")
                .header("Host", "www.ivelt.com")
                .ignoreHttpErrors(true)
                .sslSocketFactory(socketFactory());
        if (useragent != null && !useragent.isEmpty()) {
            connection = connection.userAgent(useragent);
        }

        return connection;
    }
}
