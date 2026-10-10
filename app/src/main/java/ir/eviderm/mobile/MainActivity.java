package ir.eviderm.mobile;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String BASE = "https://hooshangi68.github.io/Eviderm/";
    private static final int OLIVE = Color.rgb(50, 63, 45);
    private static final int CREAM = Color.rgb(248, 245, 239);
    private static final int BEIGE = Color.rgb(232, 224, 212);
    private static final int PICK_FILE = 401;

    private WebView webView;
    private ProgressBar progress;
    private View errorView;
    private ValueCallback<Uri[]> fileCallback;
    private TextView[] tabLabels = new TextView[3];
    private final String[] routes = { BASE, BASE + "products.html", BASE + "order.html" };

    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radius));
        return bg;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(size);
        tv.setGravity(Gravity.CENTER);
        if (bold) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return tv;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(CREAM);
        getWindow().setNavigationBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= 26) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(CREAM);
        setContentView(page);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(7), dp(14), dp(7));
        header.setBackgroundColor(CREAM);
        page.addView(header, new LinearLayout.LayoutParams(-1, dp(67)));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(brand, new LinearLayout.LayoutParams(0, -1, 1));
        TextView title = label("EVIDERM", 22, OLIVE, false);
        title.setGravity(Gravity.START | Gravity.BOTTOM);
        title.setLetterSpacing(0.20f);
        brand.addView(title, new LinearLayout.LayoutParams(-1, dp(36)));
        TextView subtitle = label("DERMOCOSMETIC", 8, OLIVE, false);
        subtitle.setGravity(Gravity.START | Gravity.TOP);
        subtitle.setLetterSpacing(0.26f);
        brand.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(16)));

        TextView reload = label("↻", 32, OLIVE, false);
        reload.setContentDescription("بارگذاری دوباره صفحه");
        header.addView(reload, new LinearLayout.LayoutParams(dp(44), dp(48)));
        reload.setOnClickListener(v -> { errorView.setVisibility(View.GONE); webView.reload(); });

        View rule = new View(this);
        rule.setBackgroundColor(BEIGE);
        page.addView(rule, new LinearLayout.LayoutParams(-1, dp(1)));

        FrameLayout content = new FrameLayout(this);
        page.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        webView = new WebView(this);
        webView.setBackgroundColor(CREAM);
        content.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);  // Required by the existing website.
        settings.setDomStorageEnabled(true);   // Website session/cart storage.
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);  // Android file picker content URIs.
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        CookieManager.getInstance().setAcceptCookie(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri == null) return true;
                if ("https".equalsIgnoreCase(uri.getScheme()) && "hooshangi68.github.io".equalsIgnoreCase(uri.getHost()) &&
                    (uri.getPath() != null && uri.getPath().startsWith("/Eviderm"))) {
                    return false;
                }
                openExternal(uri);
                return true;
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                progress.setVisibility(View.VISIBLE);
                errorView.setVisibility(View.GONE);
                updateNav(url);
            }
            @Override public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE);
                updateNav(url);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) { progress.setVisibility(View.GONE); errorView.setVisibility(View.VISIBLE); }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try { startActivityForResult(params.createIntent(), PICK_FILE); return true; }
                catch (ActivityNotFoundException e) { fileCallback = null; Toast.makeText(MainActivity.this,"برنامه انتخاب فایل یافت نشد",Toast.LENGTH_SHORT).show(); return false; }
            }
        });
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> openExternal(Uri.parse(url)));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(OLIVE));
        FrameLayout.LayoutParams progressLp = new FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP);
        content.addView(progress, progressLp);

        LinearLayout err = new LinearLayout(this);
        err.setGravity(Gravity.CENTER);
        err.setOrientation(LinearLayout.VERTICAL);
        err.setPadding(dp(32), dp(24), dp(32), dp(24));
        err.setBackgroundColor(CREAM);
        TextView errTitle = label("ارتباط با سایت برقرار نشد", 18, OLIVE, true);
        TextView errDetail = label("اتصال اینترنت را بررسی کن و دوباره تلاش کن.", 14, 0xff777568, false);
        TextView retry = label("تلاش دوباره", 15, Color.WHITE, true);
        retry.setBackground(shape(OLIVE, 15));
        LinearLayout.LayoutParams retryLp = new LinearLayout.LayoutParams(dp(160), dp(48));
        retryLp.topMargin = dp(24);
        err.addView(errTitle, new LinearLayout.LayoutParams(-1, dp(46)));
        err.addView(errDetail, new LinearLayout.LayoutParams(-1, dp(48)));
        err.addView(retry, retryLp);
        retry.setOnClickListener(v -> { err.setVisibility(View.GONE); webView.reload(); });
        errorView = err;
        content.addView(err, new FrameLayout.LayoutParams(-1, -1));
        err.setVisibility(View.GONE);

        View divider = new View(this);
        divider.setBackgroundColor(BEIGE);
        page.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        LinearLayout nav = new LinearLayout(this);
        nav.setBackgroundColor(Color.WHITE);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(10), dp(7), dp(10), dp(6));
        page.addView(nav, new LinearLayout.LayoutParams(-1, dp(69)));
        String[] icons = { "⌂", "▦", "▣" };
        String[] names = { "خانه", "محصولات", "ثبت سفارش" };
        for (int i = 0; i < 3; i++) {
            final int index = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(0, -1, 1);
            itemLp.setMargins(dp(4), 0, dp(4), 0);
            nav.addView(item, itemLp);
            TextView symbol = label(icons[i], 24, OLIVE, true);
            TextView name = label(names[i], 12, OLIVE, true);
            item.addView(symbol, new LinearLayout.LayoutParams(-1, dp(30)));
            item.addView(name, new LinearLayout.LayoutParams(-1, dp(24)));
            tabLabels[i] = name;
            item.setOnClickListener(v -> { if (!routes[index].equals(webView.getUrl())) webView.loadUrl(routes[index]); });
        }
        updateNav(BASE);
        if (state != null) webView.restoreState(state);
        else webView.loadUrl(BASE);
    }

    private void updateNav(String url) {
        if (tabLabels[0] == null) return;
        int current = url != null && url.contains("order.html") ? 2 :
            (url != null && url.contains("products.html") ? 1 : 0);
        for (int i = 0; i < tabLabels.length; i++) {
            tabLabels[i].setTextColor(i == current ? OLIVE : 0xff989A8F);
            tabLabels[i].setAlpha(i == current ? 1f : 0.75f);
        }
    }

    private void openExternal(Uri uri) {
        try {
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http") ||
                scheme.equalsIgnoreCase("mailto") || scheme.equalsIgnoreCase("tel") || scheme.equalsIgnoreCase("sms"))) return;
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception e) { Toast.makeText(this,"برنامه‌ای برای بازکردن لینک یافت نشد",Toast.LENGTH_SHORT).show(); }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        }
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        webView.saveState(out);
        super.onSaveInstanceState(out);
    }
    @Override public void onBackPressed() {
        if (errorView.getVisibility() == View.VISIBLE) { errorView.setVisibility(View.GONE); webView.loadUrl(BASE); }
        else if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }
}
