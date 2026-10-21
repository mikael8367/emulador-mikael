package br.com.mikael.emulador;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int PICK_EXE = 41;
    private static final String PREFS = "mikael_emulator";
    private static final String KEY_CONTAINERS = "containers_v1";
    private static final String KEY_ACTIVE = "active_container";
    private final int bg = Color.rgb(9, 15, 12);
    private final int panel = Color.rgb(22, 32, 27);
    private final int green = Color.rgb(70, 230, 135);
    private final int muted = Color.rgb(176, 190, 181);
    private final List<ContainerProfile> containers = new ArrayList<>();
    private LinearLayout containerList;
    private TextView activeLabel;
    private TextView selectedFile;
    private TextView status;
    private int activeId = 1;
    private int nextId = 2;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        loadContainers();
        buildUi();
    }

    private void loadContainers() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        activeId = prefs.getInt(KEY_ACTIVE, 1);
        String raw = prefs.getString(KEY_CONTAINERS, "");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                ContainerProfile p = ContainerProfile.fromJson(obj);
                containers.add(p);
                nextId = Math.max(nextId, p.id + 1);
            }
        } catch (Exception ignored) { }
        if (containers.isEmpty()) {
            containers.add(new ContainerProfile(1, "Padrão", "1280 × 720", "Equilibrado", "Automático"));
            activeId = 1;
        }
        if (getActive() == null) activeId = containers.get(0).id;
        saveContainers();
    }

    private void saveContainers() {
        JSONArray arr = new JSONArray();
        for (ContainerProfile p : containers) arr.put(p.toJson());
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_CONTAINERS, arr.toString())
                .putInt(KEY_ACTIVE, activeId).apply();
    }

    private ContainerProfile getActive() {
        for (ContainerProfile p : containers) if (p.id == activeId) return p;
        return null;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(24), dp(18), dp(28));
        scroll.addView(page);

        TextView brand = text("MIKAEL • WINDOWS ON ANDROID", 12, green, true);
        page.addView(brand);
        TextView title = text("Emulador Mikael", 28, Color.WHITE, true);
        title.setPadding(0, dp(7), 0, dp(5));
        page.addView(title);
        TextView subtitle = text("Organize ambientes separados para testar configurações diferentes.", 14, muted, false);
        page.addView(subtitle);

        TextView device = text("DISPOSITIVO\nAndroid " + Build.VERSION.RELEASE + " • API " + Build.VERSION.SDK_INT
                + "\nArquitetura: " + Build.SUPPORTED_ABIS[0], 14, Color.WHITE, false);
        cardStyle(device);
        addTop(page, device, 18);

        TextView section = text("SEUS CONTAINERS", 13, green, true);
        section.setPadding(0, dp(22), 0, dp(8));
        page.addView(section);
        activeLabel = text("", 13, muted, false);
        page.addView(activeLabel);
        containerList = new LinearLayout(this);
        containerList.setOrientation(LinearLayout.VERTICAL);
        page.addView(containerList);

        Button add = button("+ Criar container");
        add.setOnClickListener(v -> showContainerEditor(null));
        addTop(page, add, 10);

        TextView warning = text("MOTOR WINDOWS AINDA NÃO INTEGRADO\nOs containers desta versão guardam nome e preferências. Eles ainda não criam um prefixo Wine nem executam EXE.", 13, Color.rgb(255, 220, 145), false);
        warning.setPadding(dp(14), dp(14), dp(14), dp(14));
        warning.setBackgroundColor(Color.rgb(48, 39, 22));
        addTop(page, warning, 18);

        Button choose = button("Selecionar arquivo .EXE");
        choose.setOnClickListener(v -> pickExe());
        addTop(page, choose, 18);
        selectedFile = text("Nenhum arquivo selecionado", 13, muted, false);
        page.addView(selectedFile);

        status = text("Estado: interface pronta • execução de EXE indisponível", 13, green, false);
        status.setPadding(0, dp(20), 0, dp(5));
        page.addView(status);
        TextView footer = text("Projeto experimental • compatibilidade não garantida", 12, Color.GRAY, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(18), 0, 0);
        page.addView(footer);

        setContentView(scroll);
        renderContainers();
    }

    private void renderContainers() {
        if (containerList == null) return;
        containerList.removeAllViews();
        ContainerProfile active = getActive();
        activeLabel.setText(active == null ? "Nenhum container ativo" : "Selecionado: " + active.name);
        for (ContainerProfile p : containers) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(13), dp(14), dp(12));
            card.setBackgroundColor(p.id == activeId ? Color.rgb(24, 54, 38) : panel);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.topMargin = dp(8);
            containerList.addView(card, cp);

            TextView name = text((p.id == activeId ? "✓  " : "") + p.name, 17, Color.WHITE, true);
            card.addView(name);
            TextView details = text("Resolução: " + p.resolution + "\nPerfil Box64: " + p.preset + "\nGráficos: " + p.graphics, 12, muted, false);
            details.setPadding(0, dp(5), 0, dp(8));
            card.addView(details);

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button select = smallButton(p.id == activeId ? "Selecionado" : "Usar");
            select.setEnabled(p.id != activeId);
            select.setOnClickListener(v -> {
                activeId = p.id;
                saveContainers();
                renderContainers();
                status.setText("Container ativo: " + p.name + " (perfil salvo; motor ainda não integrado).");
            });
            Button edit = smallButton("Editar");
            edit.setOnClickListener(v -> showContainerEditor(p));
            Button delete = smallButton("Excluir");
            delete.setOnClickListener(v -> confirmDelete(p));
            actions.addView(select, weightParams());
            actions.addView(edit, weightParams());
            actions.addView(delete, weightParams());
            card.addView(actions);
        }
    }

    private void showContainerEditor(ContainerProfile existing) {
        boolean editing = existing != null;
        ContainerProfile original = existing;
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(6), dp(4), dp(6), dp(4));

        EditText name = editText(editing ? original.name : "", "Nome do container");
        form.addView(name);
        Spinner resolution = spinner(new String[]{"800 × 600", "960 × 540", "1280 × 720", "1600 × 900", "1920 × 1080"},
                editing ? original.resolution : "1280 × 720");
        addField(form, "Resolução preferida", resolution);
        Spinner preset = spinner(new String[]{"Economia", "Equilibrado", "Desempenho", "Estabilidade"},
                editing ? original.preset : "Equilibrado");
        addField(form, "Perfil Box64 (preferência)", preset);
        Spinner graphics = spinner(new String[]{"Automático", "OpenGL", "Vulkan / DXVK", "VirGL (experimental)"},
                editing ? original.graphics : "Automático");
        addField(form, "Modo gráfico (preferência)", graphics);

        TextView note = text("Essas opções são salvas no perfil. Elas só terão efeito no jogo depois que o motor de compatibilidade for integrado.", 12, muted, false);
        note.setPadding(0, dp(8), 0, 0);
        form.addView(note);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing ? "Editar container" : "Novo container")
                .setView(form)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(editing ? "Salvar" : "Criar", null)
                .create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String newName = name.getText().toString().trim();
            if (newName.isEmpty()) {
                name.setError("Digite um nome");
                return;
            }
            for (ContainerProfile p : containers) {
                if (p != original && p.name.equalsIgnoreCase(newName)) {
                    name.setError("Já existe um container com esse nome");
                    return;
                }
            }
            if (editing) {
                original.name = newName;
                original.resolution = resolution.getSelectedItem().toString();
                original.preset = preset.getSelectedItem().toString();
                original.graphics = graphics.getSelectedItem().toString();
            } else {
                ContainerProfile created = new ContainerProfile(nextId++, newName,
                        resolution.getSelectedItem().toString(),
                        preset.getSelectedItem().toString(),
                        graphics.getSelectedItem().toString());
                containers.add(created);
                activeId = created.id;
            }
            saveContainers();
            renderContainers();
            dialog.dismiss();
            Toast.makeText(this, editing ? "Container atualizado" : "Container criado e selecionado", Toast.LENGTH_SHORT).show();
        }));
        dialog.show();
    }

    private void confirmDelete(ContainerProfile p) {
        if (containers.size() == 1) {
            Toast.makeText(this, "Mantenha pelo menos um container", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Excluir " + p.name + "?")
                .setMessage("Isso remove apenas o perfil salvo no aplicativo. Não há ambiente Windows instalado nesta versão.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (d, w) -> {
                    containers.remove(p);
                    if (activeId == p.id) activeId = containers.get(0).id;
                    saveContainers();
                    renderContainers();
                }).show();
    }

    private void addField(LinearLayout parent, String label, View field) {
        TextView title = text(label, 12, muted, true);
        title.setPadding(0, dp(12), 0, dp(2));
        parent.addView(title);
        parent.addView(field);
    }

    private Spinner spinner(String[] values, String selected) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        for (int i = 0; i < values.length; i++) if (values[i].equals(selected)) spinner.setSelection(i);
        return spinner;
    }

    private EditText editText(String value, String hint) {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setText(value);
        edit.setHint(hint);
        edit.setTextColor(Color.WHITE);
        edit.setHintTextColor(Color.GRAY);
        return edit;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        return b;
    }

    private Button smallButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(11);
        b.setPadding(dp(2), 0, dp(2), 0);
        return b;
    }

    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
        p.setMargins(dp(2), 0, dp(2), 0);
        return p;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void cardStyle(TextView view) {
        view.setPadding(dp(15), dp(14), dp(15), dp(14));
        view.setBackgroundColor(panel);
    }

    private void addTop(LinearLayout parent, View view, int margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(margin);
        parent.addView(view, p);
    }

    private void pickExe() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        try { startActivityForResult(intent, PICK_EXE); }
        catch (Exception e) { status.setText("Não foi possível abrir o seletor de arquivos."); }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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

    private static class ContainerProfile {
        int id;
        String name, resolution, preset, graphics;
        ContainerProfile(int id, String name, String resolution, String preset, String graphics) {
            this.id = id; this.name = name; this.resolution = resolution; this.preset = preset; this.graphics = graphics;
        }
        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id); o.put("name", name); o.put("resolution", resolution);
                o.put("preset", preset); o.put("graphics", graphics);
            } catch (Exception ignored) { }
            return o;
        }
        static ContainerProfile fromJson(JSONObject o) {
            return new ContainerProfile(o.optInt("id", 1), o.optString("name", "Container"),
                    o.optString("resolution", "1280 × 720"),
                    o.optString("preset", "Equilibrado"),
                    o.optString("graphics", "Automático"));
        }
    }
}
