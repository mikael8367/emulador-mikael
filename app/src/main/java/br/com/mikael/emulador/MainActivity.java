package br.com.mikael.emulador;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;

public class MainActivity extends Activity {
    private static final int PICK_EXE = 41;
    private final int bg = Color.rgb(10, 16, 13);
    private final int panel = Color.rgb(22, 32, 27);
    private final int green = Color.rgb(70, 230, 135);
    private TextView selectedFile;
    private TextView status;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bg);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(28), dp(20), dp(28));
        scroll.addView(page);

        TextView brand = new TextView(this);
        brand.setText("MIKAEL");
        brand.setTextColor(green);
        brand.setTextSize(14);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        page.addView(brand);

        TextView title = new TextView(this);
        title.setText("Emulador Mikael");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(0, dp(8), 0, dp(6));
        page.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Ambiente experimental para compatibilidade de jogos Windows no Android.");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(15);
        page.addView(subtitle);

        TextView device = new TextView(this);
        device.setText("DISPOSITIVO\nAndroid " + Build.VERSION.RELEASE + " • API " + Build.VERSION.SDK_INT
                + "\nArquitetura: " + Build.SUPPORTED_ABIS[0]);
        device.setTextColor(Color.WHITE);
        device.setTextSize(14);
        device.setPadding(dp(16), dp(16), dp(16), dp(16));
        device.setBackgroundColor(panel);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(22);
        page.addView(device, cardParams);

        TextView warning = new TextView(this);
        warning.setText("MOTOR WINDOWS AINDA NÃO INSTALADO\nEsta versão é somente a interface inicial. Escolher um EXE não executa o arquivo.");
        warning.setTextColor(Color.rgb(255, 220, 145));
        warning.setTextSize(14);
        warning.setPadding(dp(16), dp(16), dp(16), dp(16));
        warning.setBackgroundColor(Color.rgb(48, 39, 22));
        LinearLayout.LayoutParams warningParams = new LinearLayout.LayoutParams(-1, -2);
        warningParams.topMargin = dp(14);
        page.addView(warning, warningParams);

        Button choose = new Button(this);
        choose.setText("Selecionar arquivo .EXE");
        choose.setOnClickListener(v -> pickExe());
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(-1, -2);
        btnParams.topMargin = dp(20);
        page.addView(choose, btnParams);

        selectedFile = new TextView(this);
        selectedFile.setText("Nenhum arquivo selecionado");
        selectedFile.setTextColor(Color.LTGRAY);
        selectedFile.setTextSize(13);
        selectedFile.setPadding(0, dp(10), 0, dp(10));
        page.addView(selectedFile);

        TextView optimizationTitle = new TextView(this);
        optimizationTitle.setText("PERFIL DE DESEMPENHO");
        optimizationTitle.setTextColor(green);
        optimizationTitle.setTextSize(13);
        optimizationTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        optimizationTitle.setPadding(0, dp(18), 0, dp(8));
        page.addView(optimizationTitle);

        TextView optimization = new TextView(this);
        optimization.setText("• Perfil econômico: prioriza menor uso de recursos\n• Perfil equilibrado: opção recomendada para testes\n• Perfil de desempenho: pode aumentar consumo e aquecimento");
        optimization.setTextColor(Color.LTGRAY);
        optimization.setTextSize(14);
        optimization.setPadding(dp(16), dp(14), dp(16), dp(14));
        optimization.setBackgroundColor(panel);
        page.addView(optimization);

        status = new TextView(this);
        status.setText("Estado: interface carregada • execução de EXE indisponível");
        status.setTextColor(green);
        status.setTextSize(13);
        status.setPadding(0, dp(20), 0, dp(6));
        page.addView(status);

        TextView footer = new TextView(this);
        footer.setText("Projeto experimental • Compatibilidade não garantida");
        footer.setTextColor(Color.GRAY);
        footer.setTextSize(12);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(22), 0, 0);
        page.addView(footer);

        setContentView(scroll);
    }

    private void pickExe() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        try {
            startActivityForResult(intent, PICK_EXE);
        } catch (Exception e) {
            status.setText("Não foi possível abrir o seletor de arquivos.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_EXE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                selectedFile.setText("Arquivo escolhido: " + uri.getLastPathSegment());
                status.setText("Arquivo selecionado. Ainda não pode ser executado nesta versão.");
            }
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
