package com.kfmdmsolutions.ivelt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.webkit.JavascriptInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class IveltWebInterface {
    private static final int MAX_SIZE_KB = 400;
    private static final int INITIAL_QUALITY = 90;
    private static final int MIN_QUALITY = 30;
    private static final int QUALITY_DECREMENT = 5;
    private Context context;

    public IveltWebInterface(Context context) {
        this.context = context;
    }

    /**
     * Compress an image from a Uri if it exceeds MAX_SIZE_KB
     * @param originalUri The Uri of the original image
     * @return Uri of the compressed image or original if no compression needed
     */
    public Uri compressImageUri(Uri originalUri) {
        try {
            // Get the file size
            InputStream inputStream = context.getContentResolver().openInputStream(originalUri);
            byte[] imageBytes = readBytesFromInputStream(inputStream);
            int sizeKB = imageBytes.length / 1024;
            
            if (sizeKB <= MAX_SIZE_KB) {
                return originalUri;
            }
            
            // Compress the image
            byte[] compressedBytes = compressImageBytes(imageBytes);
            
            // Save to a temporary file
            File tempFile = new File(context.getCacheDir(), "compressed_image.jpg");
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(compressedBytes);
            fos.close();
            
            // Return Uri for the temporary file
            return Uri.fromFile(tempFile);
        } catch (Exception e) {
            Logger.e("Failed to compress image: " + e.getMessage());
            return originalUri; // Return original if compression fails
        }
    }

    /**
     * Read bytes from an InputStream
     */
    private byte[] readBytesFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    /**
     * Compress image bytes to ensure they're under the size limit
     */
    private byte[] compressImageBytes(byte[] imageBytes) {
        try {
            // Decode the image
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size());
            if (bitmap == null) {
                return imageBytes; // Unable to decode, return original
            }
            
            int quality = INITIAL_QUALITY;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // Calculate initial dimensions for scaling if image is very large
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            double scaleFactor = 1.0;
            int originalSizeKB = imageBytes.length / 1024;
            
            // If image is very large, do an initial resize
            if (originalSizeKB > MAX_SIZE_KB * 5) {
                scaleFactor = Math.sqrt((double) MAX_SIZE_KB / originalSizeKB);
                scaleFactor = Math.max(0.5, scaleFactor);
                
                width = (int) (bitmap.getWidth() * scaleFactor);
                height = (int) (bitmap.getHeight() * scaleFactor);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                byte[] compressedBytes = outputStream.toByteArray();
                int scaledSizeKB = compressedBytes.length / 1024;
                
                if (scaledSizeKB <= MAX_SIZE_KB) {
                    return compressedBytes;
                }
                
                bitmap.recycle();
                bitmap = scaledBitmap;
                outputStream.reset();
            }
            
            // Try compression with decreasing quality
            byte[] compressedBytes;
            do {
                outputStream.reset();
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                compressedBytes = outputStream.toByteArray();
                int compressedSizeKB = compressedBytes.length / 1024;
                
                if (compressedSizeKB <= MAX_SIZE_KB) {
                    break;
                }
                
                quality -= QUALITY_DECREMENT;
                
                if (quality <= MIN_QUALITY) {
                    width = (int) (width * 0.9);
                    height = (int) (height * 0.9);
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                    bitmap.recycle();
                    bitmap = scaledBitmap;
                    quality = INITIAL_QUALITY;
                }
            } while (quality >= MIN_QUALITY);
            
            return compressedBytes;
            
        } catch (Exception e) {
            Logger.e("Error compressing image bytes: " + e.getMessage());
            return imageBytes;
        }
    }

}

    @JavascriptInterface
    public String getHiddenElements() {
        String[] hiddenArray = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("hidden_buttons", new HashSet<>()).toArray(new String[]{});
        try {
            return new JSONArray(hiddenArray).toString();
        } catch (JSONException e) {
            return null;
        }
    }

    private int newposts;

    @JavascriptInterface
    public String checkForNewPostsTimes() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String new_posts = preferences.getString("new_posts", "Never");
        newposts = convertDurationStringToMilliSeconds(new_posts);
        if (newposts == 0) {
            return null;
        }
        return "" + newposts;
    }

    @JavascriptInterface
    public void copyToClipboard(String data) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("אייוועלט", data);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void saveDefaultPage(String defaultPage) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString("default_page", defaultPage).apply();
        Toast.makeText(context, "Default Page Updated", Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void saveCredentials(String username, String password) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(IVELT_USERNAME, username).putString(IVELT_PASSWORD, password).apply();
    }

    @JavascriptInterface
    public String getUsername() {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(IVELT_USERNAME, "");
    }

    @JavascriptInterface
    public String getPassword() {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(IVELT_PASSWORD, "");
    }

    @JavascriptInterface
    public String getVersionString() {
        return BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")";
    }

    @JavascriptInterface
    public void sharePost(String html) {
        String parsedHtml = parsePostHtml(html);
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        CharSequence text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
        shareIntent.putExtra(Intent.EXTRA_HTML_TEXT, html);
        shareIntent.putExtra(Intent.EXTRA_TEXT, parsedHtml);
        shareIntent.putExtra(Intent.EXTRA_TITLE, "Share");
        shareIntent.setType("text/*");
        context.startActivity(Intent.createChooser(shareIntent, "null"));
    }

    // NEW METHODS FOR IMAGE COMPRESSION

    /**
     * JavaScript interface method to compress an image from a Base64 string
     * @param base64Image The Base64 encoded image
     * @return The compressed Base64 encoded image if needed, otherwise the original
     */
    @JavascriptInterface
    public String compressImage(String base64Image) {
        try {
            // Remove data URL prefix if present
            String base64Data = base64Image;
            if (base64Image.contains(",")) {
                base64Data = base64Image.split(",")[1];
            }
            
            // Decode Base64 to byte array
            byte[] imageBytes = Base64.decode(base64Data, Base64.DEFAULT);
            
            // Check if compression is needed
            if (imageBytes.length / 1024 <= MAX_SIZE_KB) {
                return base64Image; // Already under the limit
            }
            
            // Get the image prefix (if any)
            String prefix = "";
            if (base64Image.contains(",")) {
                prefix = base64Image.split(",")[0] + ",";
            }
            
            // Compress the image
            byte[] compressedBytes = compressImageBytes(imageBytes);
            
            // Convert back to Base64
            String compressedBase64 = Base64.encodeToString(compressedBytes, Base64.NO_WRAP);
            return prefix + compressedBase64;
            
        } catch (Exception e) {
            Logger.e("Failed to compress image: " + e.getMessage());
            return base64Image; // Return original if compression fails
        }
    }
    
    /**
     * Compress image bytes to ensure they're under the size limit
     */
    private byte[] compressImageBytes(byte[] imageBytes) {
        try {
            // Decode the image
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size());
            if (bitmap == null) {
                return imageBytes; // Unable to decode, return original
            }
            
            int quality = INITIAL_QUALITY;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // Calculate initial dimensions for scaling if image is very large
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            double scaleFactor = 1.0;
            int originalSizeKB = imageBytes.length / 1024;
            
            // If image is very large, do an initial resize
            if (originalSizeKB > MAX_SIZE_KB * 5) {
                // Target scale based on size reduction needed
                scaleFactor = Math.sqrt((double) MAX_SIZE_KB / originalSizeKB);
                // Ensure we don't scale too aggressively initially
                scaleFactor = Math.max(0.5, scaleFactor);
                
                width = (int) (bitmap.getWidth() * scaleFactor);
                height = (int) (bitmap.getHeight() * scaleFactor);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                
                // If the scaled bitmap solves our problem
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                byte[] compressedBytes = outputStream.toByteArray();
                int scaledSizeKB = compressedBytes.length / 1024;
                
                if (scaledSizeKB <= MAX_SIZE_KB) {
                    return compressedBytes;
                }
                
                // Continue with the scaled bitmap
                bitmap.recycle();
                bitmap = scaledBitmap;
                outputStream.reset();
            }
            
            // Try compression with decreasing quality
            byte[] compressedBytes;
            do {
                outputStream.reset();
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                compressedBytes = outputStream.toByteArray();
                int compressedSizeKB = compressedBytes.length / 1024;
                
                if (compressedSizeKB <= MAX_SIZE_KB) {
                    break;
                }
                
                quality -= QUALITY_DECREMENT;
                
                // If quality is at minimum, scale down dimensions
                if (quality <= MIN_QUALITY) {
                    // Scale down by 10%
                    width = (int) (width * 0.9);
                    height = (int) (height * 0.9);
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                    bitmap.recycle();
                    bitmap = scaledBitmap;
                    quality = INITIAL_QUALITY; // Reset quality
                }
            } while (quality >= MIN_QUALITY);
            
            return compressedBytes;
            
        } catch (Exception e) {
            Logger.e("Error compressing image bytes: " + e.getMessage());
            return imageBytes; // Return original if compression fails
        }
    }
    
    /**
     * JavaScript interface method to get the auto-compression status
     * @return true if auto-compression is enabled (default), false otherwise
     */
    @JavascriptInterface
    public boolean isAutoCompressionEnabled() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean("auto_compress_images", true);
    }
    
    /**
     * JavaScript interface method to set the auto-compression status
     */
    @JavascriptInterface
    public void setAutoCompressionEnabled(boolean enabled) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean("auto_compress_images", enabled).apply();
        String message = enabled ? "Auto-compression enabled" : "Auto-compression disabled";
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    // EXISTING METHODS

    private String parsePostHtml(String html) {
        Document doc = Jsoup.parse(html);
        Element body = doc.body();
        body.select("img").forEach(element -> {
            String src = element.attr("src");
            if (src.startsWith("./")) {
                src = src.replace("./", "www.ivelt.com/forum/");
            }
            element.text("<br>" + src + "<br>");
        });
        body.select("a").forEach(element -> {
            if (!element.text().matches("^(https:|http:|www\\. |(./)?viewtopic)\\S*")) {
                element.text("<br>" + element.text() + " " + element.attr("href") + "<br>");
            }
        });
        body.select(".file dd").forEach(element -> element.text(""));
        body.select("cite").forEach(element -> element.text(""));
        body.select("blockquote").forEach(element -> {
            String text = element.text();
            element.text("[quote]<br> " + text + " <br>[end quote]<br>");
        });

        return Html.fromHtml(body.html(), Html.FROM_HTML_MODE_COMPACT)
                .toString()
                .replaceAll("<br>", "\n")
                .replaceAll("(?:^|\\s)viewtopic", "www.ivelt.com/forum/viewtopic")
                .replaceAll("\n+", "\n");
    }

    private static final int SECOND_IN_MILLIS = 1000;
    private static final int MINUTE_IN_MILLIS = 60 * SECOND_IN_MILLIS;

    private int convertDurationStringToMilliSeconds(String duration) {
        duration = duration == null ? "" : duration;
        switch (duration) {
            case "Every 20 seconds":
                return 20 * SECOND_IN_MILLIS;
            case "Every 40 seconds":
                return 40 * SECOND_IN_MILLIS;
            case "Every minute":
                return MINUTE_IN_MILLIS;
            case "Never":
            default:
                return 0;
        }
    }
}
