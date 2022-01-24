package com.kfmdmsolutions.ivelt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.unusedapprestrictions.IUnusedAppRestrictionsBackportCallback;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.kfmdmsolutions.ivelt.Utilities.Logger;
import com.kfmdmsolutions.ivelt.Utilities.Utils;
import com.kfmdmsolutions.ivelt.Utilities.WebkitCookieManagerProxy;
import com.kfmdmsolutions.ivelt.javascript.AddSettingsElement;
import com.kfmdmsolutions.ivelt.javascript.UnhideContactButton;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE;


public class MainActivity extends AppCompatActivity {
    private static Bundle webviewBundle;
    WebView mywebView;
    String sURL, sFileName, sUserAgent;
    SwipeRefreshLayout swipeRefreshLayout;
    String currentUrl = "https://www.ivelt.com/";
    String url = null;
    Logger logger;
    private boolean serviceNeedsStarting = true;
    public static final WebkitCookieManagerProxy coreCookieManager = new WebkitCookieManagerProxy(null, java.net.CookiePolicy.ACCEPT_ALL);
    private boolean zoomed = false;
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int INPUT_FILE_REQUEST_CODE = 1;
    public static final String EXTRA_URL = "com.kdmfs.mainactivity.url";
    public static final String WEBVIEW_BUNDLE = "com.kdmfs.ivelt.MainActivity.webview.bundle";
    private IveltWebInterface iveltWebInterface;

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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        webviewBundle = new Bundle();
        mywebView.saveState(webviewBundle);
        outState.putBundle(WEBVIEW_BUNDLE, webviewBundle);
        android.util.Log.d("SaveState", getBundleSizeInBytes(webviewBundle) + " bytes");
    }

    @Override
    protected void onPause() {
        android.util.Log.d("SNQ", "Stopping Activity");
        try {
            NotificationService.startNotificationService(this, NotificationService.ACTION_SAVE_NOTIFICATION_LIST);
        }catch (IllegalStateException ise){
            // can only happen on samsung when starting from android studio
        }
        super.onPause();
    }

    private void handleIntent(Intent intent) {
        if (intent == null || (intent.getStringExtra(EXTRA_URL) == null && intent.getDataString() == null)) {
            return;
        }

        String url = intent.getStringExtra(EXTRA_URL);
        url = url == null ? intent.getDataString() : url;
        if (url != null && !url.isEmpty()) {
            mywebView.loadUrl(url);
        }
        logger.log("URL Intent Url is " + url);
    }
    @SuppressLint({"ClickableViewAccessibility", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            Logger.getInstance(this).log("Fatal Error", exception);
            if (defaultUncaughtExceptionHandler != null){
                defaultUncaughtExceptionHandler.uncaughtException(thread, exception);
            }
            System.exit(2);
        });
        boolean firebase = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("firebase",false);
        boolean askFirebase = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("ask_firebase", true);
        if (askFirebase) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.crash_collection_dialog_title)
                    .setMessage(R.string.crash_collection_dialog_message)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("firebase", true).apply();
                        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
                        dialog.dismiss(); })
                    .setNegativeButton(R.string.no, (dialog, which) -> {
                        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("firebase", false).apply();
                        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);
                        dialog.dismiss(); })
                    .show();
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("ask_firebase", false).apply();
        }
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(firebase);
        logger = Logger.getInstance(getApplicationContext());
        setContentView(R.layout.activity_main);

        NotificationService.initChannels(getApplicationContext());

        serviceNeedsStarting = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            for (NotificationChannel channel : manager.getNotificationChannels()){
                logger.log(channel.toString());
            }

        }
        mywebView = findViewById(R.id.webview);
        swipeRefreshLayout = findViewById(R.id.swipeContainer);
        swipeRefreshLayout.setNestedScrollingEnabled(true);
        WebViewAssetLoader.AssetsPathHandler assetsHandler = new WebViewAssetLoader.AssetsPathHandler(this);
        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .setDomain("www.ivelt.com")
                .setHttpAllowed(true)
                .addPathHandler("/kfmdm/assets/", assetsHandler)
                .addPathHandler("/kfmdm/resources/", new WebViewAssetLoader.ResourcesPathHandler(this))

                .build();
        mywebView.setWebViewClient(new CustomWebViewClient(loader));
        mywebView.setWebChromeClient(new WebChromeClient());
        initListener();
        WebSettings webSettings = mywebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mywebView.getSettings().setSupportZoom(true);
        mywebView.getSettings().setBuiltInZoomControls(true);
        mywebView.getSettings().setDisplayZoomControls(false);
        mywebView.getSettings().setAppCacheEnabled(true);
        mywebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mywebView.getSettings().setMixedContentMode(MIXED_CONTENT_COMPATIBILITY_MODE);
        mywebView.getSettings().setAllowFileAccess(true);
        mywebView.getSettings().setDomStorageEnabled(true);
//        String desktopuseragent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36";
//        mywebView.getSettings().setUserAgentString(desktopuseragent);
//        mywebView.getSettings().setLoadWithOverviewMode(true);
//        mywebView.getSettings().setUseWideViewPort(true);
        mywebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mywebView.setLongClickable(true);
        mywebView.setOnTouchListener(new SwipeDetector());
        iveltWebInterface = new IveltWebInterface(this);
        mywebView.addJavascriptInterface(iveltWebInterface, "android");
        // Run only in debug mode. BuildConfig should be generated by gradle.
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        mywebView.setPadding(0, 0, 0, 0);
        registerForContextMenu(mywebView);
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE

        }, 0);

        if (webviewBundle != null) {
            mywebView.restoreState(webviewBundle);
            webviewBundle = null;
        }else if (currentUrl != null) {
            String url = PreferenceManager.getDefaultSharedPreferences(this).getString("default_page", currentUrl);
            mywebView.loadUrl(url);

        }
        if (getIntent() != null){
            handleIntent(getIntent());
            getIntent().setData(null);
        }

        java.net.CookieHandler.setDefault(coreCookieManager);


        mywebView.setOnLongClickListener(v -> {
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
        });

        mywebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {

            String fileName = URLUtil.guessFileName(url, contentDisposition, getFileType(url));
            sFileName = fileName;
            sURL = url;
            sUserAgent = userAgent;

            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED)
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED)
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED) {
                        downloadFile(fileName, url, userAgent);
                    }


        });

        swipeRefreshLayout.setOnRefreshListener(() -> mywebView.reload());

    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        Bundle webViewBundle = savedInstanceState.getBundle(WEBVIEW_BUNDLE);
        if (webViewBundle != null){
            mywebView.restoreState(webViewBundle);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }


    @SuppressLint("DeprecatedApi")
    private void initListener() {
        mywebView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String logMessage = "Console " + consoleMessage.messageLevel().toString().toLowerCase() + ": " + consoleMessage.message() +
                        ". source: " + consoleMessage.sourceId() + " (" + consoleMessage.lineNumber() + ")";
                Logger.getInstance(getApplicationContext()).log(logMessage);
                if(consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR){
                    FirebaseCrashlytics.getInstance().recordException(new RuntimeException(logMessage));
                    android.util.Log.d("Non-Fatal", "recording non fatal crash");
                }
                return super.onConsoleMessage(consoleMessage);
            }

            @SuppressLint("QueryPermissionsNeeded")
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
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
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
        } catch (Exception error) {
            Toast.makeText(this, "error" + error, Toast.LENGTH_SHORT).show();


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

        MenuItem.OnMenuItemClickListener handler = item -> {
            if (item.getTitle() == "Copy image link") {
                String linkToCopy = result.getExtra();
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("simple text", linkToCopy);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), "Link Copied!",
                        Toast.LENGTH_SHORT).show();
            } else if (item.getTitle() == "Share Link") {
                String linkToShare = result.getExtra();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT,linkToShare); // your above url
                startActivity(Intent.createChooser(shareIntent, "Share..."));
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
        };

        if (result.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            menu.setHeaderTitle("Image options");
            menu.add(0, 2, 1, "Copy image link").setOnMenuItemClickListener(handler);
            menu.add(0, 1, 0, "Save - Download Image").setOnMenuItemClickListener(handler);
            menu.add(0, 3, 2, "Share Link").setOnMenuItemClickListener(handler);
        }
    }

    private boolean handleIvelt(String url, WebView view){

        if (url.contains("download")){
            return false;
        }
        if (url.contains("mark_notification")){
            int id = (int) NotificationService.NotificationInfo.extractIDFromURL(url);
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
        return false;
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
            Element first = quickLinksList.first();
            if (first != null) {
                first.insertChildren(first.childrenSize(), settingListElement);
            }
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
                        url.startsWith("www.ivelt.com/") ||
                        url.startsWith("ivelt.com")||
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

        private final WebViewAssetLoader loader;

        CustomWebViewClient(WebViewAssetLoader loader){
            this.loader = loader;
        }
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            this.showProgress();

        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            try{

                WebResourceResponse resourceResponse = loader.shouldInterceptRequest(request.getUrl());
                if (resourceResponse == null || resourceResponse.getData() == null || resourceResponse.getStatusCode() > 299){
                    return super.shouldInterceptRequest(view, request);
                }
                return  resourceResponse;
            }catch (Exception e){
                return super.shouldInterceptRequest(view, request);
            }
        }

        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

            logger.log("Should override url? " + request.getUrl());
            String url = request.getUrl().toString();
            if (url.startsWith("http://www.ivelt.com/forum/ucp.php?mode=logout") || url.startsWith("https://www.ivelt.com/forum/ucp.php?mode=logout")){
                iveltWebInterface.saveCredentials("","");
            }
            if (isIvelt(url)){
//                return  false;
                return handleIvelt(url, view);
            }


            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(request.getUrl());
            try {
                startActivity(i);
            }catch (ActivityNotFoundException activityNotFoundException){
                logger.log("No browser found, trying to open URL = " + request.getUrl());
                if (
                        url.startsWith("https://drive.google.com/") ||
                                // Should change this to regex
                                url.contains("docs.googleusercontent.com") ||
                                url.startsWith("https://accounts.google.com/") ||
                                url.startsWith("https://www.yiddish24.com/") ||
                                url.startsWith("https://www.dropbox.com/")) {
                    return false;
                } else {
                    logger.log("Url blocked. URL = " + request.getUrl());
                    Toast.makeText(MainActivity.this,"לינק געבלאקט, אשריכם ישראל" ,Toast.LENGTH_LONG).show();
                }
            }
            return true;
        }
        private boolean firstRun = true;
        private float initScale = 0.0f;

        @Override
        public void onScaleChanged(WebView view, float oldScale,
                                   float newScale) {
            if (firstRun) {
                initScale = oldScale;
                firstRun = false;
                zoomed = false;
            }
            else {
                if (newScale>oldScale) {
                    zoomed = true;
                }
                else {
                    if ((newScale * 0.95f) < initScale) {
                        zoomed = false;
                    }
                }
            }
            android.util.Log.d("ZoomCheck", "zoomed " + zoomed + " first run " + firstRun + " initScale " + initScale + " oldScale " + oldScale + " newScale " + newScale);
            super.onScaleChanged(view, oldScale, newScale);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            swipeRefreshLayout.setRefreshing(false);
            currentUrl = url;
            logger.log("Page finished for url " + url);
            super.onPageFinished(view, url);
            this.hideProgress();
            WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

            DisplayMetrics metrics = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(metrics);

            metrics.widthPixels /= metrics.density;

            mywebView.loadUrl("javascript:var scale = " + metrics.widthPixels + " / document.body.scrollWidth; document.body.style.zoom = scale;");
//            mywebView.loadUrl("javascript:" + AddSettingsElement.JS_ADD_ELEMENT_TO_LIST);
            try {
//                mywebView.loadUrl("javascript:" + Utils.readTextFile(MainActivity.this, R.raw.add_settings_element));
                mywebView.loadUrl("javascript:" + Utils.readTextFile(MainActivity.this, R.raw.add_style));
//                mywebView.loadUrl("javascript:" + Utils.readTextFile(MainActivity.this, R.raw.login));
            } catch (IOException e) {
                e.printStackTrace();
            }
//            try {
//                mywebView.loadUrl("javascript:" + Utils.readTextFile(MainActivity.this, R.raw.add_contact));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            if (serviceNeedsStarting){
                NotificationService.startNotificationService(MainActivity.this, NotificationService.ACTION_UPDATE_DELAY_TIME);
                serviceNeedsStarting = false;
            }

        }

        private void showProgress() {
            swipeRefreshLayout.setRefreshing(true);
        }

        private void hideProgress() {
            swipeRefreshLayout.setRefreshing(false);

        }
    }

    private class SwipeDetector implements View.OnTouchListener {
        private float startX = 0.0f;
        private float startY = 0.0f;
        private boolean swiping = false;
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            switch (action){
                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_DOWN:
                    swiping = true;
                    startX = event.getX(0);
                    startY = event.getY(0);
//                    Log.d("ONTOUCH", "action: " + action + " count: " + event.getPointerCount());
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float endX = event.getX(0);
                    float endY = event.getY(0);
                    boolean isHorizontalSwipe = isHorizontalSwipe(startX, startY, endX, endY);
                    boolean isForward = startX < endX;
                    if (!zoomed && isHorizontalSwipe && swiping && event.getPointerCount() == 1){
                        singleFingerSwipe(isForward);
                    }
                    if (isHorizontalSwipe && swiping && event.getPointerCount() > 1){
                        multiFingerSwipe(isForward);
                    }
                    swiping = false;
//                    Log.d("ONTOUCH", "action: " + action + " count: " + event.getPointerCount() + " start x: " + startX + " end x: "
//                            + endX + " is horizontal " + isHorizontalSwipe + " is forward " + isForward);
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                default:
                    Log.d("ONTOUCH", "Other event " + action);

            }
            return v.onTouchEvent(event);
        }

        private void multiFingerSwipe(boolean forward){
            if (forward){
                String javaScript =
                        "let next = document.getElementsByClassName('next'); " +
                        "let a = next[0].getElementsByTagName('a') ;" +
                        "a[0].click()";
                mywebView.loadUrl("javascript:" + javaScript);
            }else{
                String javaScript =
                        "let next = document.getElementsByClassName('previous'); " +
                        "let a = next[0].getElementsByTagName('a');" +
                        "a[0].click()";
                mywebView.loadUrl("javascript:" + javaScript);
            }
        }

        private void singleFingerSwipe(boolean forward){
            if (forward && mywebView.canGoForward()){
                mywebView.goForward();
            }
            if (!forward && mywebView.canGoBack()){
                mywebView.goBack();
            }
        }
        private boolean isHorizontalSwipe(float startX, float startY, float endX, float endY){
            return (Math.abs(startX - endX) - Math.abs(startY - endY)) > 100;
        }
    }
}
