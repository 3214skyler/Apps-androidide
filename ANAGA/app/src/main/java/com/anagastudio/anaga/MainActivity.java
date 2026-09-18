package com.anagastudio.anaga;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;

import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;

    private ValueCallback<Uri[]> fileChooserCallback;

    private static final int FILE_CHOOSER_REQUEST = 1001;

    private static final String APP_URL =
            "file:///android_asset/index.html";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        setContentView(webView);

        configurarWebView();

        abrirApp(getIntent());
    }


    private void configurarWebView() {

        WebSettings settings = webView.getSettings();

        // JavaScript
        settings.setJavaScriptEnabled(true);

        // LocalStorage / IndexedDB / Supabase
        settings.setDomStorageEnabled(true);

        // Permitir acesso aos ficheiros
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // WebView
        webView.setWebViewClient(new WebViewClient());


        /*
         * IMPORTANTE:
         *
         * O WebChromeClient é necessário para que:
         *
         * <input type="file">
         *
         * funcione dentro do Android WebView.
         */
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {

                // Cancela callback anterior, se existir
                if (fileChooserCallback != null) {
                    fileChooserCallback.onReceiveValue(null);
                }

                fileChooserCallback = filePathCallback;

                try {

                    Intent intent =
                            fileChooserParams.createIntent();

                    startActivityForResult(
                            intent,
                            FILE_CHOOSER_REQUEST
                    );

                    return true;

                } catch (Exception e) {

                    fileChooserCallback = null;

                    return false;
                }
            }
        });
    }


    /*
     * Abre o index.html.
     *
     * Se o Android recebeu:
     *
     * anaga://auth/callback#access_token=...
     *
     * o fragmento é colocado no próprio URL do WebView
     * ANTES do carregamento do HTML.
     *
     * Isso é mais seguro para o Supabase do que carregar
     * primeiro e tentar inserir o hash depois.
     */
    private void abrirApp(Intent intent) {

        String url = APP_URL;

        if (intent != null) {

            Uri data = intent.getData();

            if (data != null
                    && "anaga".equals(data.getScheme())
                    && "auth".equals(data.getHost())
                    && "/callback".equals(data.getPath())) {

                /*
                 * Query:
                 *
                 * ?code=...
                 */
                String query = data.getEncodedQuery();

                if (query != null && !query.isEmpty()) {
                    url += "?" + query;
                }


                /*
                 * Fragment:
                 *
                 * #access_token=...
                 */
                String fragment = data.getEncodedFragment();

                if (fragment != null && !fragment.isEmpty()) {
                    url += "#" + fragment;
                }
            }
        }

        webView.loadUrl(url);
    }


    /*
     * Quando o APK já está aberto e recebe novamente
     * um Deep Link.
     */
    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);

        setIntent(intent);

        abrirApp(intent);
    }


    /*
     * Resultado do seletor de ficheiros Android.
     */
    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );


        if (requestCode != FILE_CHOOSER_REQUEST) {
            return;
        }


        if (fileChooserCallback == null) {
            return;
        }


        Uri[] results = null;


        if (resultCode == RESULT_OK && data != null) {

            /*
             * Seleção de um único ficheiro.
             */
            if (data.getData() != null) {

                results = new Uri[]{
                        data.getData()
                };
            }

            /*
             * Seleção múltipla.
             *
             * Necessário para adicionar várias páginas
             * de mangá de uma vez.
             */
            else if (data.getClipData() != null) {

                int count =
                        data.getClipData().getItemCount();

                results = new Uri[count];


                for (int i = 0; i < count; i++) {

                    results[i] =
                            data.getClipData()
                                    .getItemAt(i)
                                    .getUri();
                }
            }
        }


        /*
         * Entrega os ficheiros selecionados ao
         * <input type="file"> do HTML.
         */
        fileChooserCallback.onReceiveValue(results);

        fileChooserCallback = null;
    }


    /*
     * Botão "Voltar" do Android.
     */
    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }


    /*
     * Limpeza quando a Activity é destruída.
     */
    @Override
    protected void onDestroy() {

        if (fileChooserCallback != null) {

            fileChooserCallback.onReceiveValue(null);

            fileChooserCallback = null;
        }


        if (webView != null) {

            webView.stopLoading();

            webView.destroy();

            webView = null;
        }


        super.onDestroy();
    }
}