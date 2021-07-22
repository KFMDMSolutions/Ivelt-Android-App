package com.kfmdmsolutions.ivelt;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {
    private static Bundle webviewBundle;
    WebView mywebView;
    String sURL, sFileName, sUserAgent;
    SwipeRefreshLayout swipeRefreshLayout;
    String currentUrl = "https://www.ivelt.com/";
    String url = null;

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int INPUT_FILE_REQUEST_CODE = 1;
    public static final String EXTRA_URL = "com.kdmfs.mainactivity.url";

    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;

    public int  getBundleSizeInBytes(Bundle bundle  ) {
        Parcel parcel = Parcel.obtain();
        parcel.writeValue(bundle);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes.length;
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webviewBundle = new Bundle();
        mywebView.saveState(webviewBundle);
        android.util.Log.d("SaveState", getBundleSizeInBytes(webviewBundle) + " bytes");
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
//        handleIntent(getIntent());
//        NotificationService.checkNotifications(MainActivity.this);
    }


    private void handleIntent(Intent intent){
        if (intent == null || intent.getStringExtra(EXTRA_URL) == null){
            return;
        }
        String url = intent.getStringExtra(EXTRA_URL);
            loadUrl(url);
        android.util.Log.d("INTENTURL", "Url is " + intent.getStringExtra(EXTRA_URL));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationService.initChannels(getApplicationContext());
        mywebView = (WebView) findViewById(R.id.webview);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        swipeRefreshLayout.setNestedScrollingEnabled(true);
        mywebView.setWebViewClient(new CustomWebViewClient());
        mywebView.setWebChromeClient(new WebChromeClient());
        initListener();
        WebSettings webSettings = mywebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mywebView.getSettings().setSupportZoom(true);
        mywebView.getSettings().setBuiltInZoomControls(true);
        mywebView.getSettings().setDisplayZoomControls(false);
        mywebView.getSettings().setAppCacheEnabled(true);
        mywebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
//        String desktopuseragent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36";
//        mywebView.getSettings().setUserAgentString(desktopuseragent);
//        mywebView.getSettings().setLoadWithOverviewMode(true);
//        mywebView.getSettings().setUseWideViewPort(true);
        mywebView.setLongClickable(true);
        mywebView.setPadding(0, 0, 0, 0);
        registerForContextMenu(mywebView);
        url = getIntent().getDataString();

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE

        }, 0);

        if (webviewBundle != null) {
            mywebView.restoreState(webviewBundle);
            webviewBundle = null;
        } else if (url == null) {
            mywebView.loadUrl(currentUrl);
        } else {
           loadUrl(url);
        }

        mywebView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                WebView webView1 = (WebView) v;
                WebView.HitTestResult result = webView1.getHitTestResult();

                if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                    String linkToCopy = result.getExtra();
                    ClipboardManager clipboard = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("simple text", linkToCopy);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getApplicationContext(), "Link Copied!",
                            Toast.LENGTH_SHORT).show();
                }
                return false;
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
                            }


                } else {
                    downloadFile(fileName, url, userAgent);
                }

            }

        });

        if (getIntent() != null){
            handleIntent(getIntent());
        }
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {


            @Override
            public void onRefresh() {
                loadUrl(currentUrl);
            }

        });

    }

    private void loadUrl(String url2) {
        if (isIvelt(url2)) {
            if (!handleIvelt(url2, mywebView)){
                mywebView.loadUrl(url2);
            }
        } else {
            mywebView.loadUrl(url2);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void initListener() {
        mywebView.setWebChromeClient(new WebChromeClient() {

            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    WebChromeClient.FileChooserParams fileChooserParams) {
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        Log.e(TAG, "Unable to create Image File", ex);
                    }

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");

                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

                return true;
            }
        });
    }
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        if(requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        Uri[] results = null;

        // Check that the response is a good one
        if(resultCode == Activity.RESULT_OK) {
            if(data == null) {
                // If there is not data, then we may have taken a photo
                if(mCameraPhotoPath != null) {
                    results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                }
            } else {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }

        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
        return;
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

        MenuItem.OnMenuItemClickListener handler = new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getTitle() == "Copy image link") {
                    String linkToCopy = result.getExtra();
                    ClipboardManager clipboard = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("simple text", linkToCopy);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getApplicationContext(), "Link Copied!",
                            Toast.LENGTH_SHORT).show();
                } else if (item.getTitle() == "Save - Download Image") {

                    String DownloadImageURL = result.getExtra();


                    if (URLUtil.isValidUrl(DownloadImageURL)) {

                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(DownloadImageURL));
                        String cookie = CookieManager.getInstance().getCookie(DownloadImageURL);
                        request.allowScanningByMediaScanner();
                        request.addRequestHeader("coockie", cookie);
                        request.setMimeType(getFileType(DownloadImageURL));
                        request.setAllowedOverMetered(true);
                        request.setAllowedOverRoaming(true);
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE | DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "ivelt image.jpg");

                        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                        downloadManager.enqueue(request);

                        Toast.makeText(MainActivity.this, "Image Downloaded Successfully.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Sorry.. Something Went Wrong.", Toast.LENGTH_LONG).show();
                    }
                }

                return true;
            }
        };

        if (result.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            menu.setHeaderTitle("Image options");
            menu.add(0, 2, 1, "Copy image link").setOnMenuItemClickListener(handler);
            menu.add(0, 1, 0, "Save - Download Image").setOnMenuItemClickListener(handler);
        }
    }

    private boolean handleIvelt(String url, WebView view){
//        if (url.endsWith("unread")){
//            return false;
//        }
//        if (url.contains("ivelt")){
//            return false;
//        }
        if (url.contains("download")){
            return false;
        }
        if (url.contains("mark_notification")){
            int id = NotificationService.NotificationInfo.extractIDFromURL(url);
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
            notificationManagerCompat.cancel(id);
            return false;
        }
        if (url.endsWith("settings")){
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        swipeRefreshLayout.setRefreshing(true);
        android.util.Log.d("JSOUP", "is ivelt " + url);
        String useragent = view.getSettings().getUserAgentString();
        Utils.executeAsync(() -> {
            try {
                android.util.Log.d("JSOUP", "starting try");
                String cookies = CookieManager.getInstance().getCookie(url);
                android.util.Log.d("JSOUP", "starting with url " + url + " cookies " + cookies + " useragent " + useragent);
                Document doc = Jsoup.connect(url).
                        cookies(Utils.convertCookies(cookies)).
                        sslSocketFactory(Utils.socketFactory()).
                        userAgent(useragent).get();
                if(url.contains("ucp.php")){
                    handleUserControlPage(doc);
                }else {
                    handleGeneralPage(doc);
                }
//                android.util.Log.d("JSOUP", "linkslist " + quickLinksList);
                final String mime = "text/html";
                final String encoding = "utf-8";
                String html = doc.outerHtml();
                runOnUiThread(() -> {
                    view.loadDataWithBaseURL(url, html, mime, encoding, "");
                });
            } catch (IOException e) {
//                    return false;
                android.util.Log.d("JSOUP", "error", e);
            }
        });
        return true;
    }

    private void handleGeneralPage(Document doc) {
        Elements postButtons = doc.select(".post-buttons");
        addSettingsToQuickLinks(doc);
    }

    private void addSettingsToQuickLinks(Document doc) {

        Elements quickLinks = doc.select("#quick-links");
        Elements quickLinksList = quickLinks.select(".dropdown-contents");
        if (quickLinksList.size() > 0 && (quickLinksList.first().childrenSize() > 1)){
            Element settingListElement = new Element("li");
            settingListElement.addClass("small-icon");
            Element settingsElement = new Element("a");
            settingsElement.html("עפפ סעטטינגס");
            settingsElement.attr("href", "./settings");
            settingsElement.attr("role", "menuitem");
            settingListElement.appendChild(settingsElement);
            settingListElement.attr("style", "background-image: url(./styles/prosilver_yidddish/theme/images/icon_topic_poll.gif)");
//                    quickLinksList.first().appendChild(settingListElement);
            quickLinksList.first().insertChildren(quickLinksList.first().childrenSize(), settingListElement);
        }
    }

    private void handleUserControlPage(Document doc) {
        addSettingsToQuickLinks(doc);
        Element tabs = doc.select("#tabs").first();
        android.util.Log.d("UCP", "tabs " + tabs);
        if (tabs != null) {
            Element tabList = tabs.getElementsByTag("ul").first();
            if (tabList != null) {
                android.util.Log.d("UCP", "tabList " + tabList);
                Element appSettingElement = new Element("li");
                appSettingElement.addClass("tab");
                Element appSettingLink = new Element("a");
                appSettingLink.attr("href", "./settings");
                appSettingLink.html("עפפ סעטטינגס");
                appSettingElement.appendChild(appSettingLink);
                tabList.appendChild(appSettingElement);
//                tabList.insertChildren(2, appSettingElement);
            }
        }
    }




    private static boolean isIvelt(String url){
        boolean isIvelt =  (url != null && (
                url.startsWith("https://www.ivelt.com/") ||
                url.startsWith("http://www.ivelt.com/") ||
                url.startsWith("https://ivelt.com")||
                url.startsWith("http://ivelt.com")||
                        url.startsWith("https://yiddishworld.com/") ||
                        url.startsWith("http://yiddishworld.com/")||
                        url.startsWith("https://www.yiddishworld.com/") ||
                        url.startsWith("http://www.yiddishworld.com/")));
        android.util.Log.d("URLComp", "url is " + url + " is ivelt " + isIvelt);
        return isIvelt;
    }


    public class CustomWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            this.showProgress();

        }



        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

            String url = request.getUrl().toString();
            if (isIvelt(url)){
//                return  false;
                return handleIvelt(url, view);
            }

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(request.getUrl());
            try {
                startActivity(i);
            }catch (ActivityNotFoundException activityNotFoundException){
                if (
                        url.startsWith("https://drive.google.com/") ||
                                url.startsWith("https://accounts.google.com/") ||
                                url.startsWith("https://www.yiddish24.com/") ||
                                url.startsWith("https://www.dropbox.com/")) {
                    return false;
                }else{
                    Toast.makeText(MainActivity.this,"לינק געבלאקט, אשריכם ישראל" ,Toast.LENGTH_LONG).show();
                }
            }
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
