package com.android.cts.ivelt;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static android.media.MediaRecorder.VideoSource.CAMERA;


public class MainActivity extends AppCompatActivity {
    WebView mywebView;
    String sURL, sFileName, sUserAgent;
    SwipeRefreshLayout swipeRefreshLayout;
    String currentUrl = "https://www.ivelt.com/";
    String url = null;
    private static final int REQUEST_CODE_ALBUM = 1;
    private static final int REQUEST_CODE_CAMERA = 2;
    private ValueCallback<Uri[]> mFilePathCallback;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mywebView.saveState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mywebView = (WebView) findViewById(R.id.webview);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        swipeRefreshLayout.setNestedScrollingEnabled(true);
        mywebView.setWebViewClient(new CustomWebViewClient());
        mywebView.setWebChromeClient(new WebChromeClient());
        if (BuildConfig.DEBUG){
            WebView.setWebContentsDebuggingEnabled(true);
        }
        initListener();
        WebSettings webSettings = mywebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mywebView.getSettings().setAppCacheEnabled(true);
        mywebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mywebView.setLongClickable(true);
        mywebView.setPadding(0, 0, 0, 0);
        registerForContextMenu(mywebView);
        url = getIntent().getDataString();

        if (savedInstanceState != null) {
            mywebView.restoreState(savedInstanceState);
        } else if (url == null) {
            mywebView.loadUrl(currentUrl);
        } else {
            mywebView.loadUrl(url);
        }

        mywebView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                WebView webView1 = (WebView) v;
                WebView.HitTestResult result = webView1.getHitTestResult();

                if (result != null) {


                    return false;

                }else
                    return true;
            }


        });
        mywebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {

                String fileName = URLUtil.guessFileName(url, contentDisposition, getFileType(url));
                sFileName = fileName;
                sURL = url;
                sUserAgent = userAgent;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED)
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                                == PackageManager.PERMISSION_GRANTED)
                            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    == PackageManager.PERMISSION_GRANTED) {
                                downloadFile(fileName, url, userAgent);
                            } else {
                                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.CAMERA,}, 1);
                            }

                } else {
                    downloadFile(fileName, url, userAgent);
                }

            }


        });



        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {


            @Override
            public void onRefresh() {
                if(isIvelt(currentUrl)){
                    handleIvelt(currentUrl, mywebView);
                }else{
                    mywebView.loadUrl(currentUrl);
                }
            }

        });
    }

    private void initListener() {
        mywebView.setWebChromeClient(new WebChromeClient() {

            //For Android5.0+
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;

                showChooserDialog();
                return true;
            }
        });

    }
    private Dialog dialog;
    private boolean resetCallback = true;

    private void showChooserDialog() {
        if (dialog == null) {
            dialog = new Dialog(this);
            dialog.setTitle(R.string.file_chooser);
            dialog.setContentView(R.layout.dialog_chooser_layout);


            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (resetCallback && mFilePathCallback != null) {
                        mFilePathCallback.onReceiveValue(null);
                        mFilePathCallback = null;
                    }
                    resetCallback = true;
                }
            });

            dialog.findViewById(R.id.text_album).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetCallback = false;
                    dialog.dismiss();
                    Intent albumIntent = new Intent(Intent.ACTION_PICK);
                    albumIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                    startActivityForResult(albumIntent, REQUEST_CODE_ALBUM);
                }
            });
            dialog.findViewById(R.id.text_camera).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetCallback = false;
                    dialog.dismiss();

                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);


                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }

                        if (photoFile != null) {


                            ContentValues contentValues = new ContentValues(1);
                            contentValues.put(MediaStore.Images.Media.DATA, photoFile.getAbsolutePath());
                            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                            startActivityForResult(takePictureIntent, CAMERA);

                        }
                    }
                }
            });
        }
        dialog.show();
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }


    private String mCameraPhotoPath;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_ALBUM:
                    if (data != null) {
                        String dataString = data.getDataString();
                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                            Log.d("CustomChooserActivity", dataString);
                        }
                    }
                    break;
                case REQUEST_CODE_CAMERA:
                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                        Log.d("CustomChooserActivity", mCameraPhotoPath);
                    }
                    break;
            }
        }
        if (mFilePathCallback != null) {
            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
        }
    }







    private void downloadFile(String fileName, String url, String userAgent) {
        try {
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            String cookie = CookieManager.getInstance().getCookie(url);
            request.allowScanningByMediaScanner();
            request.setTitle(fileName)
                    .setDescription("Downloading")
                    .addRequestHeader("coockie", cookie)
                    .addRequestHeader("User-Agent", userAgent)
                    .setMimeType(getFileType(url))
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE | DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);


            downloadManager.enqueue(request);
            sURL = "";
            sFileName = "";
            sUserAgent = "";
            Toast.makeText(this, "Download Started", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
            Toast.makeText(this, "error" + ignored, Toast.LENGTH_SHORT).show();


        }
    }

    public String getFileType(String url) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(Uri.parse(url)));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!sURL.equals("") && !sFileName.equals("") && !sUserAgent.equals("")) {
                    downloadFile(sFileName, sURL, sUserAgent);
                }
            }
        }
    }


    @Override
    public void onBackPressed() {
        if (mywebView.canGoBack()) {
            mywebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        WebView webView = (WebView) v;
        WebView.HitTestResult result = webView.getHitTestResult();

        if (result != null) {
            if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                String linkToCopy = result.getExtra();
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("simple text", linkToCopy);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), "Link Copied!",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleIvelt(String url, WebView view){
        android.util.Log.d("JSOUP", "is ivelt");
        String useragent = view.getSettings().getUserAgentString();
        Utils.executeAsync(() -> {
            try {
                android.util.Log.d("JSOUP", "starting try");
                String cookies = CookieManager.getInstance().getCookie(url);
                android.util.Log.d("JSOUP", "starting with url " + url + " cookies " + cookies + " useragent " + useragent);
                Document doc = Jsoup.connect(url).cookies(getCookies(cookies)).userAgent(useragent).get();
                Elements h1 = doc.select("h1");
                h1.first().html("Ivelt Android App");
                Elements postButtons = doc.select(".post-buttons");

                android.util.Log.d("JSOUP", "h1 " + h1);
                android.util.Log.d("JSOUP", "ending try");
                final String mime = "text/html";
                final String encoding = "utf-8";
                String html = doc.outerHtml();
                runOnUiThread(() -> {
                    view.loadDataWithBaseURL(url, html, mime, encoding, null);
                });
            } catch (IOException e) {
//                    return false;
                android.util.Log.d("JSOUP", "error", e);
            }
        });
    }

    private Map<String, String> getCookies (String cookie){
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

    private static boolean isIvelt(String url){
        return  (url != null && (url.startsWith("https://www.ivelt.com/")
                || url.startsWith("http://www.ivelt.com/")
                || url.startsWith("https://www.yiddishworld.com/")
                || url.startsWith("http://www.yiddishworld.com/")));
    }


    public class CustomWebViewClient extends WebViewClient {


        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            this.showProgress();
        }



        @Override

        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

            String url = request.getUrl().toString();
            if (isIvelt(url)){
                handleIvelt(url, view);
                return true;
                    || url.startsWith("https://www.yiddish24.com/")
            }
            if (url != null && ( url.startsWith("https://drive.google.com/")
                    || url.startsWith("https://accounts.google.com/")
                    || url.startsWith("https://www.dropbox.com/"))) {
                return false;
            }


            // reject anything other
            return true;
        }



        @Override
        public void onPageFinished(WebView view, String url) {
            swipeRefreshLayout.setRefreshing(false);
            currentUrl = url;
            super.onPageFinished(view, url);
            this.hideProgress();

            WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

            DisplayMetrics metrics = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(metrics);

            metrics.widthPixels /= metrics.density;

            mywebView.loadUrl("javascript:var scale = " + metrics.widthPixels + " / document.body.scrollWidth; document.body.style.zoom = scale;");


        }

        private void showProgress() {

            swipeRefreshLayout.setRefreshing(true);
        }


        private void hideProgress() {
            swipeRefreshLayout.setRefreshing(false);


        }

    }

}
