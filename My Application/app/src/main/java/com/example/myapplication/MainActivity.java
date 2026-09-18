package com.anagastudio.gf;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private WebView webView;

    private static final int PERMISSAO_STORAGE = 100;

    private String csvPendente = null;
    private String pdfPendente = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        // Necessário para confirm(), alert(), etc.
        webView.setWebChromeClient(
                new WebChromeClient()
        );

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.addJavascriptInterface(
                new AndroidInterface(),
                "Android"
        );

        webView.loadUrl(
                "file:///android_asset/index.html"
        );
    }

    // =========================================================
    // INTERFACE JAVASCRIPT
    // =========================================================

    private class AndroidInterface {

        @JavascriptInterface
        public void exportarCSV(String csv) {

            csvPendente = csv;

            verificarPermissaoEContinuar(
                    "CSV"
            );
        }

        @JavascriptInterface
        public void exportarPDF(String dados) {

            pdfPendente = dados;

            verificarPermissaoEContinuar(
                    "PDF"
            );
        }
    }

    // =========================================================
    // PERMISSÃO
    // =========================================================

    private void verificarPermissaoEContinuar(
            String tipo
    ) {

        /*
         * Android 10 (API 29) ou superior:
         * usamos MediaStore e não precisamos
         * pedir WRITE_EXTERNAL_STORAGE.
         */

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            if (tipo.equals("CSV")) {

                salvarCSV(csvPendente);
                csvPendente = null;

            } else {

                salvarPDF(pdfPendente);
                pdfPendente = null;
            }

            return;
        }

        /*
         * Android 8 / 9:
         * precisamos da permissão de armazenamento.
         */

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    PERMISSAO_STORAGE
            );

            return;
        }

        if (tipo.equals("CSV")) {

            salvarCSV(csvPendente);
            csvPendente = null;

        } else {

            salvarPDF(pdfPendente);
            pdfPendente = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode != PERMISSAO_STORAGE) {
            return;
        }

        if (grantResults.length > 0 &&
                grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {

            if (csvPendente != null) {

                salvarCSV(csvPendente);
                csvPendente = null;

            } else if (pdfPendente != null) {

                salvarPDF(pdfPendente);
                pdfPendente = null;
            }

        } else {

            Toast.makeText(
                    MainActivity.this,
                    "Permissão de armazenamento negada.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // PASTA DOWNLOADS
    // =========================================================

    private File obterPastaDownloads() {

        File pasta =
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                );

        if (!pasta.exists()) {

            if (!pasta.mkdirs() &&
                    !pasta.exists()) {

                throw new RuntimeException(
                        "Não foi possível criar a pasta Downloads"
                );
            }
        }

        return pasta;
    }

    // =========================================================
    // CSV
    // =========================================================

    private void salvarCSV(String csv) {

        if (csv == null || csv.trim().isEmpty()) {

            Toast.makeText(
                    MainActivity.this,
                    "Não existem dados para exportar.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try {

            String nomeArquivo =
                    "transacoes_" +
                    new SimpleDateFormat(
                            "yyyyMMdd_HHmmss",
                            Locale.getDefault()
                    ).format(new Date()) +
                    ".csv";

            byte[] dados =
                    ("\uFEFF" + csv)
                            .getBytes(
                                    StandardCharsets.UTF_8
                            );

            // =================================================
            // ANDROID 10+
            // =================================================

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q) {

                ContentResolver resolver =
                        getContentResolver();

                ContentValues values =
                        new ContentValues();

                values.put(
                        MediaStore.Downloads.DISPLAY_NAME,
                        nomeArquivo
                );

                values.put(
                        MediaStore.Downloads.MIME_TYPE,
                        "text/csv"
                );

                values.put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS
                );

                Uri uri =
                        resolver.insert(
                                MediaStore.Downloads
                                        .EXTERNAL_CONTENT_URI,
                                values
                        );

                if (uri == null) {

                    throw new Exception(
                            "Não foi possível criar o arquivo."
                    );
                }

                OutputStream output =
                        resolver.openOutputStream(uri);

                if (output == null) {

                    throw new Exception(
                            "Não foi possível abrir o arquivo."
                    );
                }

                output.write(dados);
                output.flush();
                output.close();

            }

            // =================================================
            // ANDROID 8 / 9
            // =================================================

            else {

                File pasta =
                        obterPastaDownloads();

                File arquivo =
                        new File(
                                pasta,
                                nomeArquivo
                        );

                FileOutputStream output =
                        new FileOutputStream(
                                arquivo
                        );

                output.write(dados);
                output.flush();
                output.close();
            }

            Toast.makeText(
                    MainActivity.this,
                    "✅ CSV salvo em Downloads",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    MainActivity.this,
                    "Erro ao exportar CSV:\n" +
                            e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // PDF
    // =========================================================

    private void salvarPDF(String dados) {

        if (dados == null || dados.trim().isEmpty()) {

            Toast.makeText(
                    MainActivity.this,
                    "Não existem dados para gerar o PDF.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        PdfDocument documento =
                new PdfDocument();

        try {

            int numeroPagina = 1;

            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(
                            595,
                            842,
                            numeroPagina
                    ).create();

            PdfDocument.Page pagina =
                    documento.startPage(
                            pageInfo
                    );

            Canvas canvas =
                    pagina.getCanvas();

            Paint titulo =
                    new Paint();

            titulo.setTypeface(
                    Typeface.create(
                            Typeface.DEFAULT,
                            Typeface.BOLD
                    )
            );

            titulo.setTextSize(24);

            Paint normal =
                    new Paint();

            normal.setTextSize(12);

            Paint pequeno =
                    new Paint();

            pequeno.setTextSize(10);

            int x = 40;
            int y = 50;

            canvas.drawText(
                    "RELATÓRIO FINANCEIRO",
                    x,
                    y,
                    titulo
            );

            y += 25;

            String dataAtual =
                    new SimpleDateFormat(
                            "dd/MM/yyyy HH:mm",
                            Locale.getDefault()
                    ).format(
                            new Date()
                    );

            canvas.drawText(
                    "Gerado em: " + dataAtual,
                    x,
                    y,
                    pequeno
            );

            y += 35;

            String[] linhas =
                    dados.split("\\r?\\n");

            for (String linha : linhas) {

                /*
                 * Evita problemas com linhas muito grandes.
                 */

                if (linha.length() > 85) {

                    linha =
                            linha.substring(
                                    0,
                                    85
                            );
                }

                if (y > 800) {

                    documento.finishPage(
                            pagina
                    );

                    numeroPagina++;

                    pageInfo =
                            new PdfDocument.PageInfo.Builder(
                                    595,
                                    842,
                                    numeroPagina
                            ).create();

                    pagina =
                            documento.startPage(
                                    pageInfo
                            );

                    canvas =
                            pagina.getCanvas();

                    y = 50;
                }

                canvas.drawText(
                        linha,
                        x,
                        y,
                        normal
                );

                y += 22;
            }

            documento.finishPage(
                    pagina
            );

            String nomeArquivo =
                    "relatorio_financeiro_" +
                    new SimpleDateFormat(
                            "yyyyMMdd_HHmmss",
                            Locale.getDefault()
                    ).format(
                            new Date()
                    ) +
                    ".pdf";

            // =================================================
            // ANDROID 10+
            // =================================================

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q) {

                ContentResolver resolver =
                        getContentResolver();

                ContentValues values =
                        new ContentValues();

                values.put(
                        MediaStore.Downloads.DISPLAY_NAME,
                        nomeArquivo
                );

                values.put(
                        MediaStore.Downloads.MIME_TYPE,
                        "application/pdf"
                );

                values.put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS
                );

                Uri uri =
                        resolver.insert(
                                MediaStore.Downloads
                                        .EXTERNAL_CONTENT_URI,
                                values
                        );

                if (uri == null) {

                    throw new Exception(
                            "Não foi possível criar o PDF."
                    );
                }

                OutputStream output =
                        resolver.openOutputStream(uri);

                if (output == null) {

                    throw new Exception(
                            "Não foi possível abrir o PDF."
                    );
                }

                documento.writeTo(output);

                output.flush();
                output.close();
            }

            // =================================================
            // ANDROID 8 / 9
            // =================================================

            else {

                File pasta =
                        obterPastaDownloads();

                File arquivo =
                        new File(
                                pasta,
                                nomeArquivo
                        );

                FileOutputStream output =
                        new FileOutputStream(
                                arquivo
                        );

                documento.writeTo(
                        output
                );

                output.flush();
                output.close();
            }

            Toast.makeText(
                    MainActivity.this,
                    "✅ PDF salvo em Downloads",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    MainActivity.this,
                    "Erro ao criar PDF:\n" +
                            e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

        } finally {

            documento.close();
        }
    }

    // =========================================================
    // CICLO DE VIDA
    // =========================================================

    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.destroy();
        }

        super.onDestroy();
    }
}