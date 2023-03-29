package com.android.cts.kfmdmsolutions.ivelt;

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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Message;
import android.os.Parcel;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;

import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.BaseInputConnection;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.android.cts.kfmdmsolutions.ivelt.BuildConfig;
import com.android.cts.kfmdmsolutions.ivelt.R;
import com.android.cts.kfmdmsolutions.ivelt.Utilities.Logger;
import com.android.cts.kfmdmsolutions.ivelt.Utilities.Utils;
import com.android.cts.kfmdmsolutions.ivelt.Utilities.WebkitCookieManagerProxy;
import com.kfmdmsolutions.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.kfmdmsolutions.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE;
import static java.security.AccessController.getContext;


public class MainActivity extends AppCompatActivity implements SwipyRefreshLayout.OnRefreshListener {
    private static Bundle webviewBundle;
    WebView mywebView;
    String sURL, sFileName, sUserAgent;
    private SwipyRefreshLayout mSwipyRefreshLayout;
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

    private static CountDownTimer testTimer;

//    private ActivityMainBinding mBinding;

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
        Logger.getInstance(this).logWithFirebase("Bundle size " + getBundleSizeInBytes(webviewBundle));

        android.util.Log.d("SaveState", getBundleSizeInBytes(webviewBundle) + " bytes");
    }
    @Override
    protected void onResume(){
        if(testTimer != null)
        {
            testTimer.cancel();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        android.util.Log.d("SNQ", "Stopping Activity");
        try {
            NotificationService.startNotificationService(this, NotificationService.ACTION_SAVE_NOTIFICATION_LIST);
        }catch (IllegalStateException ise){
            // can only happen on samsung when starting from android studio
        }
        testTimer = new CountDownTimer(60000, 1000) {

            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                showDialog();
            }
        }.start();
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        if(testTimer != null)
        {
            testTimer.cancel();
        }
        
        super.onDestroy();
    }
    
    private boolean shouldLogout = false;
    private void handleIntent(Intent intent) {

        Logger.getInstance(this).logWithFirebase("Handling Intent");
        if (intent == null || (intent.getStringExtra(EXTRA_URL) == null && intent.getDataString() == null)) {
            if (intent.getAction() != null && intent.getAction().equals("com.kfmdm.ivelt.shortcut.logout")){
                mywebView.setVisibility(View.INVISIBLE);
                shouldLogout = true;
            }
            return;
        }

        String url = intent.getStringExtra(EXTRA_URL);
        Logger.getInstance(this).logWithFirebase("Intent URL " + url);
        url = url == null ? intent.getDataString() : url;
        if (url != null && !url.isEmpty()) {
            Logger.getInstance(this).logWithFirebase("Intent Loading URL " + url);
            url = url.replace("://ivelt.com", "://www.ivelt.com");
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
        mSwipyRefreshLayout = (SwipyRefreshLayout) findViewById(R.id.swipeContainer);

//        SwipyRefreshLayoutDirection = findViewById(R.id.swipeContainer);
//        SwipyRefreshLayoutDirection.setNestedScrollingEnabled(true);
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
        webSettings.setSupportMultipleWindows(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        mywebView.getSettings().setSupportZoom(true);
        mywebView.getSettings().setBuiltInZoomControls(true);
        mywebView.getSettings().setDisplayZoomControls(false);
        mywebView.getSettings().setAppCacheEnabled(true);
        mywebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mywebView.getSettings().setMixedContentMode(MIXED_CONTENT_COMPATIBILITY_MODE);
        mywebView.getSettings().setAllowFileAccess(true);
        mywebView.getSettings().setDomStorageEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(mywebView,true);
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
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE

            }, 0);
        }else {
            requestPermissions();
        }

        showDialog();

        Logger.getInstance(this).logWithFirebase("Preparing to start with bundle " + (webviewBundle == null));
        if (webviewBundle != null) {
            mywebView.restoreState(webviewBundle);
            webviewBundle = null;
        }else if (currentUrl != null) {
            Logger.getInstance(this).logWithFirebase("Preparing to start with currentURL " + currentUrl);
            String url = PreferenceManager.getDefaultSharedPreferences(this).getString("default_page", currentUrl);

            Logger.getInstance(this).logWithFirebase("Preparing to start with default/current url " + url);
            url = url.replace("://ivelt.com", "://www.ivelt.com");
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


//            tryDownload(url, userAgent, contentDisposition, mimetype, contentLength);
            handleDownload(url, userAgent, mimetype, contentDisposition);


        });


        mSwipyRefreshLayout.setOnRefreshListener(this);

    }
    private void requestPermissions(){
        String[]  permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        for (String permission: permissions) {
            //Do your stuff here
            if (ContextCompat.checkSelfPermission(this, String.valueOf(permission))  != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{
                        permission
                }, 0);

            }
        }
    }


    private void handleDownload(String url, String userAgent, String mimeType, String contentDisposition) {

        String filename = parseContentDisposition(contentDisposition);
        if (!filename.contains(".")){
            filename = filename + "." + getExtension(mimeType);
        }
        sFileName = filename;
        sURL = url;
        sUserAgent = userAgent;
        downloadFile(filename, url, userAgent);

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
                    RuntimeException exception = new RuntimeException(logMessage);
                    String sourceID = consoleMessage.sourceId();
                    if (sourceID.contains("/kfmdm/")){
                        sourceID = sourceID;
                        exception.setStackTrace(new StackTraceElement[]{new StackTraceElement(consoleMessage.sourceId(), "javascript", consoleMessage.sourceId(), consoleMessage.lineNumber())});
                    }else{
                        sourceID = "Webpage";
                        exception.setStackTrace(
                                new StackTraceElement[]{
                                        new StackTraceElement(sourceID, "javascript", sourceID, consoleMessage.lineNumber()),
                                        new StackTraceElement(consoleMessage.sourceId(), "javascript", consoleMessage.sourceId(), consoleMessage.lineNumber()),
                                });

                    }
                    exception.setStackTrace(new StackTraceElement[]{new StackTraceElement(consoleMessage.sourceId(), "javascript", consoleMessage.sourceId(), consoleMessage.lineNumber())});
                    FirebaseCrashlytics.getInstance().recordException(exception);
                }
                return super.onConsoleMessage(consoleMessage);
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {

                android.util.Log.d("OCW", "Resultmsg = " + resultMsg);
                if (resultMsg == null){
                    return false;
                }


                Message href = view.getHandler().obtainMessage();
                view.requestFocusNodeHref(href);
                String url = href.getData().getString("url");
                android.util.Log.d("OCW", "href " + href + " data " + href.getData());
                android.util.Log.d("OCW", "is dialog " + isDialog);
                view.getOriginalUrl();
                url = view.getHitTestResult().getExtra();


                android.util.Log.d("OCW", "url " + view.getOriginalUrl());
                if(url != null && href.getData() != null && shouldOverrideUrlLoading(mywebView, Uri.parse(url))){
                    return false;
                }
                if (url != null && !(url.contains("ivelt.com") || url.contains("sefaria.com"))){
                    mywebView.loadUrl(url);
                    return false;
                }

                if (resultMsg.obj !=null && resultMsg.obj instanceof WebView.WebViewTransport) {
                    WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                    WebView windowWebView = new WebView(MainActivity.this);
                    windowWebView.setLayoutParams(new ViewGroup.LayoutParams(200, 200));
                    windowWebView.getSettings().setJavaScriptEnabled(true);
                    windowWebView.getSettings().setDomStorageEnabled(true);
                    windowWebView.getSettings().setSupportZoom(true);
                    windowWebView.getSettings().setBuiltInZoomControls(true);
                    windowWebView.getSettings().setDisplayZoomControls(false);
                    CookieManager.getInstance().setAcceptThirdPartyCookies(windowWebView, true);
                    windowWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                    windowWebView.getSettings().setSupportMultipleWindows(true);
                    windowWebView.setWebViewClient(new WebViewClient(){
                        @Override
                        public void onPageStarted(WebView view, String url, Bitmap favicon) {
                            super.onPageStarted(view, url, favicon);
                        }

                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                            android.util.Log.d("OCW", "override " + request.getUrl());
                            if (request.getUrl().toString().startsWith("https://drive.google.com") || request.getUrl().toString().startsWith("https://www.drive.google.com")){
                                mywebView.loadUrl(request.getUrl().toString());
                                windowWebView.loadUrl("javascript:window.close()");
                                return true;
                            }
                            boolean override = MainActivity.this.shouldOverrideUrlLoading(view, request.getUrl());
                            if (override){
                                windowWebView.loadUrl("javascript:window.close()");
                            }
                            return override;
                        }

                        @Override
                        public void onPageFinished(WebView view, String url) {
                            mSwipyRefreshLayout.setRefreshing(false);
                            super.onPageFinished(view, url);
                        }
                    });
                    transport.setWebView(windowWebView);

                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                            .setNegativeButton(android.R.string.cancel, (dialog, which) -> {windowWebView.loadUrl("javascript:window.close()");})
                            .setView(windowWebView).create();

                    windowWebView.setWebChromeClient(new WebChromeClient() {
                        @Override
                        public void onCloseWindow(WebView window) {
                            alertDialog.dismiss();
                        }

                        @Override
                        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                            android.util.Log.d("WindowWV", "ConsoleMEssage " + consoleMessage );
                            return super.onConsoleMessage(consoleMessage);
                        }
                    });
//                    windowWebView.setDownloadListener((url2, userAgent, contentDisposition, mimetype, contentLength) -> {
//                        handleDownload(url2, userAgent, mimetype,contentDisposition);
//                    });

                    resultMsg.sendToTarget();

                    mSwipyRefreshLayout.setRefreshing(true);
                    alertDialog.show();
                    return true;
                }
                return false;
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
            logger.log("mimatype = " +getMimeType(fileName));
            String mimeType = getMimeType(fileName);
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            String cookie = CookieManager.getInstance().getCookie(url);
            request.allowScanningByMediaScanner();
            request.setTitle(fileName)
                    .setDescription("Downloading")
                    .addRequestHeader("cookie", cookie)
                    .addRequestHeader("User-Agent", userAgent)
                    .setMimeType(mimeType)
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

    private String getExtension(String mimeType){
        return  MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
    }
    public String getMimeType(String filename){
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getMimeTypeFromExtension(filename.substring(filename.lastIndexOf(".") + 1));
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
                    request.addRequestHeader("cookie", cookie);
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
            } else if (item.getTitle() == "View Image") {
                mywebView.loadUrl("javascript:window.open('"+result.getExtra()+"');");
            }

            return true;
        };

        if (result.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            menu.setHeaderTitle("Image options");
            menu.add(0, 1, 0, "View Image").setOnMenuItemClickListener(handler);
            menu.add(0, 2, 1, "Save - Download Image").setOnMenuItemClickListener(handler);
            menu.add(0, 3, 2, "Copy image link").setOnMenuItemClickListener(handler);
            menu.add(0, 4, 3, "Share Link").setOnMenuItemClickListener(handler);
        }
    }

    private boolean handleIvelt(String url, WebView view){

        if (url.contains("://ivelt")){
            android.util.Log.d("URL","Redirecting to www");
            mywebView.loadUrl(url.replace("://ivelt.com", "://www.ivelt.com"));
            return true;
        }
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
//        swipeRefreshLayout.setRefreshing(true);
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
                        url.startsWith("ivelt.com") ||
                        url.startsWith("http://www.ivelt.com/") ||
                        url.startsWith("https://ivelt.com") ||
                        url.startsWith("http://ivelt.com") ||
                        url.startsWith("https://yiddishworld.com/") ||
                        url.startsWith("http://yiddishworld.com/") ||
                        url.startsWith("https://www.yiddishworld.com/") ||
                        url.startsWith("http://www.yiddishworld.com/")));
        return isIvelt;
    }


    @Override
    public void onRefresh(SwipyRefreshLayoutDirection direction) {
        mywebView.reload();

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

        @SuppressWarnings("deprecation")
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return MainActivity.this.shouldOverrideUrlLoading(view, Uri.parse(url));
        }
        @RequiresApi(api = Build.VERSION_CODES.N)
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return MainActivity.this.shouldOverrideUrlLoading(view, request.getUrl());
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
            Log.d("test",iveltWebInterface.getUsername());
            mSwipyRefreshLayout.setRefreshing(false);

            FirebaseCrashlytics.getInstance().log("current url " + currentUrl);
            logger.log("Page finished for url " + url);
            if (url == null){
                FirebaseCrashlytics.getInstance().recordException(new Exception("Null URL"));
            }else if (url.equals("about:blank")){
                FirebaseCrashlytics.getInstance().recordException(new Exception("Blank URL returned."));
            }else{
                currentUrl = url;
            }
            super.onPageFinished(view, url);
            this.hideProgress();
            DisplayMetrics metrics = getDisplayMetrics();

            metrics.widthPixels /= metrics.density;

            mywebView.loadUrl("javascript:var scale = " + metrics.widthPixels + " / document.body.scrollWidth; document.body.style.zoom = scale;");

            if (!shouldLogout){
                mywebView.setVisibility(View.VISIBLE);
            }
            try {

                mywebView.loadUrl("javascript:" + Utils.readTextFile(MainActivity.this, R.raw.add_style));
                if (shouldLogout){
                    shouldLogout = false;
                    mywebView.loadUrl("javascript:" +
                            " let logoutElement = document.querySelector(\".icon-logout a\");\n" +
                            "    if (logoutElement){\n" +
                            "        logoutElement.click();\n" +
                            "    }else{\n" +
                            "        console.info(\"logout null\");\n" +
                            "    }");
                    Toast.makeText(MainActivity.this, "Logging Out", Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (serviceNeedsStarting){
                NotificationService.startNotificationService(MainActivity.this, NotificationService.ACTION_UPDATE_DELAY_TIME);
                serviceNeedsStarting = false;
            }
            if (url.contains("https://docs.google.com/")) {
                mywebView.zoomBy(1.1f);
                String desktopuseragent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/605.1.15";
                mywebView.getSettings().setUserAgentString(desktopuseragent);
            }else if (url.contains("https://accounts.google.com/")) {
                String androidua = "Linux; Android 11; Android SDK built for x86 Build/RSR1.210210.001.A1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/83.0.4103.106 Safari/537.36";
                mywebView.getSettings().setUserAgentString(androidua);
            }

        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
//            FirebaseCrashlytics.getInstance().recordException(new Exception("Received error " + error.getErrorCode() + ": "+ error.getDescription() + " while loading URL " + request.getUrl()));
            super.onReceivedError(view, request, error);
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            if (errorResponse.getStatusCode() == 404){
                Toast.makeText(MainActivity.this, request.getUrl().toString() + " not found", Toast.LENGTH_LONG).show();
            }else {
                FirebaseCrashlytics.getInstance().recordException(new Exception("Received HTTP error " + errorResponse.getStatusCode() + ": " + errorResponse.getReasonPhrase() + " while loading URL " + request.getUrl()));
            }
            super.onReceivedHttpError(view, request, errorResponse);
        }

        private void showProgress() {           
            mSwipyRefreshLayout.setRefreshing(true);
        }

        private void hideProgress() {
            mSwipyRefreshLayout.setRefreshing(false);

        }

        @Override
        public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
            if(event.getKeyCode() == 1) {
                if(event.getAction() == KeyEvent.ACTION_UP){
                    mywebView.performLongClick();
                }
            }
            if(event.getKeyCode() == 67) {
                if(event.getAction() == KeyEvent.ACTION_UP){
                    if (!mywebView.onCheckIsTextEditor()) {
                        if (mywebView.canGoBack()) {
                            mywebView.goBack();
                        } else {
                            finish();
                        }

                    }
                }
            }
            return;
        }
    }

    private boolean shouldOverrideUrlLoading(WebView view, Uri request) {
        logger.log("Should override url? " + request);
        String url = request.toString();
        if (url.startsWith("http://www.ivelt.com/forum/ucp.php?mode=logout") || url.startsWith("https://www.ivelt.com/forum/ucp.php?mode=logout")){
            iveltWebInterface.saveCredentials("","");
        }
        if (isIvelt(url)){
//                return  false;
            return handleIvelt(url, view);
        }


        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(request);
        try {
//            return !isAllowedWebsite(url);
            startActivity(i);
        }catch (ActivityNotFoundException activityNotFoundException){
            logger.log("No browser found, trying to open URL = " + request);
            if (isAllowedWebsite(url)) {
                return false;
            } else {
                logger.log("Url blocked. URL = " + request);
                Toast.makeText(MainActivity.this,"לינק געבלאקט, אשריכם ישראל" ,Toast.LENGTH_LONG).show();
            }
        }
        return true;
    }

    private boolean isAllowedWebsite(String url) {
        return url.startsWith("https://drive.google.com/") ||
                // Should change this to regex
                url.contains("docs.googleusercontent.com") ||
                url.contains("dropboxusercontent.com") ||
                url.startsWith("https://www.sefaria.org/") ||
                url.startsWith("https://accounts.google.com/") ||
                url.startsWith("https://www.yiddish24.com/") ||
                url.contains("https://docs.google.com/") ||
                url.startsWith("https://www.dropbox.com/");
    }
    private String parseContentDisposition(String contentDisposition){
        String[] headers = contentDisposition.split(";");
        String filename = null;
        String utf8Filename = null;
        for (String header : headers){
            if (header.trim().startsWith("filename=")){
                filename = header;
            }
            if (header.trim().startsWith("filename*=UTF-8")){
                utf8Filename = header;
            }
        }
        utf8Filename = utf8Filename == null ? filename : utf8Filename;
        utf8Filename = utf8Filename.substring(utf8Filename.lastIndexOf("=") + 1).trim().replace("UTF-8''", "");

        return URLDecoder.decode(utf8Filename);
    }
    @NonNull
    private DisplayMetrics getDisplayMetrics() {
        WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(metrics);
        return metrics;
    }

    private class SwipeDetector implements View.OnTouchListener {
        private float startX = 0.0f;
        private float startY = 0.0f;
        private float startX2 = 0.0f;
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
                    if (event.getPointerCount() > 1){
                        startX2 = event.getX(1);
                    }
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float endX = event.getX(0);
                    float endY = event.getY(0);
                    boolean oneFingerSwipe = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("one_finger_swipe", true);
                    boolean twoFingerSwipe = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("two_finger_swipe", true);
                    boolean isHorizontalSwipe = isHorizontalSwipe(startX, startY, endX, endY);
                    boolean isForward = startX < endX;
                    android.util.Log.d("SWIPE", "oneFinger " + oneFingerSwipe + " two finger " +  twoFingerSwipe);
                    if (!zoomed && isHorizontalSwipe && oneFingerSwipe && swiping && event.getPointerCount() == 1){
                        singleFingerSwipe(isForward);
                    }
                    if (isHorizontalSwipe && twoFingerSwipe && swiping && event.getPointerCount() > 1){
                        boolean isSecondForward = startX2 < event.getX(1);
                        if (isSecondForward == isForward) {
                            multiFingerSwipe(isForward);
                        }
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
            return (Math.abs(startX - endX) - Math.abs(startY - endY)) > (100 * getDisplayMetrics().density);
        }


    }
    public void showDialog() {

        String password = PreferenceManager.getDefaultSharedPreferences(this).getString("password", "");
        if (!password.equals("")) {
            mywebView.setVisibility(View.INVISIBLE);
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            final EditText input = new EditText(this);
            input.setSingleLine();
            input.setTransformationMethod(PasswordTransformationMethod.getInstance());
            alert.setView(input);
            alert.setCancelable(false)
                    .setTitle("Login")
                    .setMessage("Enter Your Password");
            alert.setPositiveButton("Login", (dialog,  whichButton)-> {
                String value = input.getText().toString();
                if (!value.equals(password)) {
                    AlertDialog.Builder alert2 = new AlertDialog.Builder(MainActivity.this);
                    alert2.setTitle("Login")
                            .setMessage("The password you have entered is incorrect.\n Please try again!");
                    alert2.setPositiveButton("Retry", (dialog1, id) -> {
                            showDialog();
                    });
                    alert2.show();
                }
                else {
                    mywebView.setVisibility(View.VISIBLE);
                }

            });
            alert.show();
        }
    }
}
