package br.com.mikael.emulador;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ActivityNotFoundException;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
    private Uri selectedUri;
    private String selectedDisplayName = "";
    private static final String PREFS = "mikael_emulator";
    private static final String KEY_CONTAINERS = "containers_v1";
    private static final String KEY_ACTIVE = "active_container";
    private final int bg = Color.rgb(9, 15, 12);
    private final int panel = Color.rgb(22, 32, 27);
    private final int green = Color.rgb(70, 230, 135);
    private final int muted = Color.rgb(176, 190, 181);
    private final List<ContainerProfile> containers = new ArrayList<>();
    private LinearLayout containerList;
    private TextView activeLabel, selectedFile, status;
    private int activeId = 1, nextId = 2;

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
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_CONTAINERS, ""));
            for (int i = 0; i < arr.length(); i++) {
                ContainerProfile p = ContainerProfile.fromJson(arr.getJSONObject(i));
                containers.add(p);
                nextId = Math.max(nextId, p.id + 1);
            }
        } catch (Exception ignored) { }
        if (containers.isEmpty()) {
            containers.add(new ContainerProfile(1, "Padrão"));
            activeId = 1;
        }
        if (getActive() == null) activeId = containers.get(0).id;
        saveContainers();
    }

    private void saveContainers() {
        JSONArray arr = new JSONArray();
        for (ContainerProfile p : containers) arr.put(p.toJson());
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_CONTAINERS, arr.toString()).putInt(KEY_ACTIVE, activeId).apply();
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

        page.addView(text("MIKAEL • WINDOWS ON ANDROID", 12, green, true));
        TextView title = text("Emulador Mikael", 28, Color.WHITE, true);
        title.setPadding(0, dp(7), 0, dp(5)); page.addView(title);
        page.addView(text("Abra arquivos diretamente, sem precisar criar um container.", 14, muted, false));

        TextView device = text("DISPOSITIVO\nAndroid " + Build.VERSION.RELEASE + " • API " + Build.VERSION.SDK_INT
                + "\nArquitetura: " + Build.SUPPORTED_ABIS[0], 14, Color.WHITE, false);
        cardStyle(device); addTop(page, device, 18);

        TextView launchSection = text("EXECUÇÃO DIRETA • SEM CONTAINER", 13, green, true);
        launchSection.setPadding(0, dp(22), 0, dp(8)); page.addView(launchSection);
        page.addView(text("Selecione jogos, instaladores, documentos ou outros arquivos. O app identificará o tipo e tentará abrir formatos que o Android ou outro aplicativo instalado suporte.", 13, muted, false));
        Button choose = button("Selecionar arquivo ou jogo");
        choose.setOnClickListener(v -> pickExe()); addTop(page, choose, 10);
        selectedFile = text("Nenhum arquivo selecionado", 13, muted, false); addTop(page, selectedFile, 6);
        Button openSelected = button("Abrir arquivo selecionado");
        openSelected.setOnClickListener(v -> openSelectedFile()); addTop(page, openSelected, 6);
        status = text("Pronto para selecionar arquivos • sem container", 13, green, false);
        status.setPadding(0, dp(12), 0, dp(5)); page.addView(status);

        TextView section = text("SEUS CONTAINERS (OPCIONAL)", 13, green, true);
        section.setPadding(0, dp(22), 0, dp(8)); page.addView(section);
        activeLabel = text("", 13, muted, false); page.addView(activeLabel);
        containerList = new LinearLayout(this);
        containerList.setOrientation(LinearLayout.VERTICAL); page.addView(containerList);
        Button add = button("+ Criar container");
        add.setOnClickListener(v -> showContainerEditor(null)); addTop(page, add, 10);

        TextView warning = text("MOTOR WINDOWS AINDA NÃO INTEGRADO\nA seleção de arquivos funciona sem container. Para executar jogos e programas EXE, ainda é necessário integrar Wine/Box64 ou outro motor compatível; o Android não executa EXE nativamente.", 13, Color.rgb(255, 220, 145), false);
        warning.setPadding(dp(14), dp(14), dp(14), dp(14));
        warning.setBackgroundColor(Color.rgb(48, 39, 22)); addTop(page, warning, 18);
        TextView footer = text("Projeto experimental • compatibilidade não garantida", 12, Color.GRAY, false);
        footer.setGravity(Gravity.CENTER); footer.setPadding(0, dp(18), 0, 0); page.addView(footer);
        setContentView(scroll);
        renderContainers();
    }

    private void renderContainers() {
        if (containerList == null) return;
        containerList.removeAllViews();
        ContainerProfile active = getActive();
        activeLabel.setText(active == null ? "Nenhum container ativo" : "Ativo: " + active.name);
        for (ContainerProfile p : containers) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14), dp(13), dp(14), dp(12));
            card.setBackgroundColor(p.id == activeId ? Color.rgb(24, 54, 38) : panel);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.topMargin = dp(8); containerList.addView(card, cp);
            card.addView(text((p.id == activeId ? "✓  " : "") + p.name, 17, Color.WHITE, true));
            TextView details = text(p.osName + " • " + p.resolution + " • " + p.ram + " MB RAM\n"
                    + p.graphics + " • " + p.dx + " • " + p.cpu + " threads\n"
                    + "Box64: " + p.box64 + " • FPS: " + p.fps + " • " + (p.fullscreen ? "Tela cheia" : "Janela"), 12, muted, false);
            details.setPadding(0, dp(5), 0, dp(8)); card.addView(details);
            LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
            Button select = smallButton(p.id == activeId ? "Ativo" : "Selecionar");
            select.setEnabled(p.id != activeId);
            select.setOnClickListener(v -> {
                activeId = p.id; saveContainers(); renderContainers();
                status.setText("Container ativo: " + p.name + ". Preferências selecionadas; motor não integrado.");
            });
            Button edit = smallButton("Configurar");
            edit.setOnClickListener(v -> showContainerEditor(p));
            Button delete = smallButton("Excluir");
            delete.setOnClickListener(v -> confirmDelete(p));
            actions.addView(select, weightParams()); actions.addView(edit, weightParams()); actions.addView(delete, weightParams());
            card.addView(actions);
        }
    }

    private void showContainerEditor(ContainerProfile existing) {
        final ContainerProfile original = existing;
        final boolean editing = existing != null;
        final ContainerProfile values = editing ? existing.copy() : new ContainerProfile(nextId, "Novo container");
        ScrollView formScroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(8), dp(4), dp(8), dp(10));
        formScroll.addView(form);

        EditText name = editText(values.name, "Nome do container");
        addField(form, "IDENTIFICAÇÃO", name);

        Spinner os = spinner(new String[]{
                "Windows 11", "Windows 10", "Windows 8.1", "Windows 7", "Windows XP (legado)",
                "Ubuntu", "Debian", "Linux Mint", "Fedora", "Arch Linux", "Manjaro",
                "Kali Linux", "openSUSE", "Alpine Linux", "Linux (outra distribuição)",
                "FreeBSD", "ReactOS (experimental)", "FreeDOS"
        }, values.osName);
        addField(form, "Sistema operacional / distribuição", os);
        Spinner resolution = spinner(new String[]{"640 × 480", "800 × 600", "960 × 540", "1280 × 720", "1600 × 900", "1920 × 1080"}, values.resolution);
        addField(form, "Resolução", resolution);
        Spinner orientation = spinner(new String[]{"Paisagem", "Retrato", "Automático"}, values.orientation);
        addField(form, "Orientação da tela", orientation);
        Spinner ram = spinner(new String[]{"512", "768", "1024", "1536", "2048", "3072"}, String.valueOf(values.ram));
        addField(form, "Limite de RAM (MB, preferência)", ram);
        Spinner cpu = spinner(new String[]{"1", "2", "3", "4", "6", "8"}, String.valueOf(values.cpu));
        addField(form, "Threads de CPU (preferência)", cpu);
        Spinner box64 = spinner(new String[]{"Economia", "Compatibilidade", "Equilibrado", "Desempenho", "Desempenho máximo"}, values.box64);
        addField(form, "Perfil Box64", box64);
        Spinner graphics = spinner(new String[]{"Automático", "VirGL", "Turnip (Adreno)", "Zink", "OpenGL nativo", "Vulkan / DXVK"}, values.graphics);
        addField(form, "Driver gráfico", graphics);
        Spinner dx = spinner(new String[]{"Desativado", "DXVK", "WineD3D", "VKD3D-Proton"}, values.dx);
        addField(form, "Tradutor DirectX", dx);
        Spinner audio = spinner(new String[]{"Padrão", "PulseAudio", "ALSA (experimental)", "Desativado"}, values.audio);
        addField(form, "Áudio", audio);
        Spinner input = spinner(new String[]{"Toque na tela", "Mouse e teclado", "Controle externo"}, values.input);
        addField(form, "Controles", input);
        Spinner fps = spinner(new String[]{"30", "45", "60", "90", "Ilimitado"}, values.fps);
        addField(form, "Limite de FPS (preferência)", fps);

        CheckBox fullscreen = check("Iniciar em tela cheia", values.fullscreen); form.addView(fullscreen);
        CheckBox showPointer = check("Mostrar ponteiro do mouse", values.pointer); form.addView(showPointer);
        CheckBox audioEnabled = check("Ativar áudio", values.audioEnabled); form.addView(audioEnabled);
        CheckBox esync = check("Ativar ESYNC (se suportado)", values.esync); form.addView(esync);
        CheckBox debug = check("Ativar logs de depuração", values.debug); form.addView(debug);

        EditText env = editText(values.env, "Ex.: MESA_GL_VERSION_OVERRIDE=4.0");
        env.setSingleLine(false); env.setMinLines(2);
        addField(form, "Variáveis de ambiente (uma por linha)", env);
        EditText args = editText(values.args, "Argumentos opcionais do programa");
        addField(form, "Argumentos de inicialização", args);

        TextView note = text("Estas são preferências guardadas no perfil. Driver gráfico, DirectX, RAM, CPU e variáveis só terão efeito depois da integração real do motor Windows; opções como Turnip dependem do hardware compatível.", 12, muted, false);
        note.setPadding(0, dp(12), 0, dp(4)); form.addView(note);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing ? "Configurar container" : "Criar container")
                .setView(formScroll)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String newName = name.getText().toString().trim();
            if (newName.isEmpty()) { name.setError("Digite um nome"); return; }
            for (ContainerProfile p : containers) {
                if ((!editing || p.id != original.id) && p.name.equalsIgnoreCase(newName)) {
                    name.setError("Já existe um container com esse nome"); return;
                }
            }
            values.name = newName;
            values.osName = os.getSelectedItem().toString();
            values.resolution = resolution.getSelectedItem().toString();
            values.orientation = orientation.getSelectedItem().toString();
            values.ram = parseInt(ram.getSelectedItem().toString(), 1024);
            values.cpu = parseInt(cpu.getSelectedItem().toString(), 2);
            values.box64 = box64.getSelectedItem().toString();
            values.graphics = graphics.getSelectedItem().toString();
            values.dx = dx.getSelectedItem().toString();
            values.audio = audio.getSelectedItem().toString();
            values.input = input.getSelectedItem().toString();
            values.fps = fps.getSelectedItem().toString();
            values.fullscreen = fullscreen.isChecked();
            values.pointer = showPointer.isChecked();
            values.audioEnabled = audioEnabled.isChecked();
            values.esync = esync.isChecked();
            values.debug = debug.isChecked();
            values.env = env.getText().toString();
            values.args = args.getText().toString();
            if (editing) {
                int idx = containers.indexOf(original);
                containers.set(idx, values);
            } else {
                containers.add(values); nextId++; activeId = values.id;
            }
            saveContainers(); renderContainers(); dialog.dismiss();
            Toast.makeText(this, editing ? "Configurações salvas" : "Container criado e selecionado", Toast.LENGTH_SHORT).show();
        }));
        dialog.show();
    }

    private void confirmDelete(ContainerProfile p) {
        if (containers.size() == 1) {
            Toast.makeText(this, "Mantenha pelo menos um container", Toast.LENGTH_SHORT).show(); return;
        }
        new AlertDialog.Builder(this).setTitle("Excluir " + p.name + "?")
                .setMessage("Isso apaga o perfil e suas preferências salvas neste aplicativo.")
                .setNegativeButton("Cancelar", null).setPositiveButton("Excluir", (d, w) -> {
                    containers.remove(p);
                    if (activeId == p.id) activeId = containers.get(0).id;
                    saveContainers(); renderContainers();
                }).show();
    }

    private int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (Exception e) { return fallback; }
    }
    private CheckBox check(String label, boolean checked) {
        CheckBox c = new CheckBox(this); c.setText(label); c.setTextColor(Color.WHITE); c.setChecked(checked); return c;
    }
    private void addField(LinearLayout parent, String label, View field) {
        TextView title = text(label, 12, muted, true); title.setPadding(0, dp(12), 0, dp(2));
        parent.addView(title); parent.addView(field);
    }
    private Spinner spinner(String[] values, String selected) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); s.setAdapter(a);
        for (int i = 0; i < values.length; i++) if (values[i].equals(selected)) s.setSelection(i);
        return s;
    }
    private EditText editText(String value, String hint) {
        EditText e = new EditText(this); e.setSingleLine(true); e.setText(value); e.setHint(hint);
        e.setTextColor(Color.WHITE); e.setHintTextColor(Color.GRAY); return e;
    }
    private Button button(String label) { Button b = new Button(this); b.setText(label); return b; }
    private Button smallButton(String label) {
        Button b = new Button(this); b.setText(label); b.setTextSize(11); b.setPadding(dp(2), 0, dp(2), 0); return b;
    }
    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
        p.setMargins(dp(2), 0, dp(2), 0); return p;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private void cardStyle(TextView v) { v.setPadding(dp(15), dp(14), dp(15), dp(14)); v.setBackgroundColor(panel); }
    private void addTop(LinearLayout parent, View view, int margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(margin); parent.addView(view, p);
    }
    private void pickExe() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        try { startActivityForResult(intent, PICK_EXE); }
        catch (Exception e) { status.setText("Não foi possível abrir o seletor de arquivos."); }
    }

    private String queryDisplayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) return cursor.getString(column);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        String segment = uri.getLastPathSegment();
        return segment == null ? "Arquivo selecionado" : segment;
    }

    private String fileKind(String name) {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".exe") || lower.endsWith(".msi") || lower.endsWith(".bat") || lower.endsWith(".com"))
            return "Executável Windows";
        if (lower.endsWith(".apk")) return "Pacote de aplicativo Android";
        if (lower.endsWith(".sh")) return "Script shell";
        if (lower.endsWith(".elf") || lower.endsWith(".so")) return "Binário Linux/Unix";
        if (lower.endsWith(".zip") || lower.endsWith(".7z") || lower.endsWith(".rar")) return "Arquivo compactado";
        if (lower.endsWith(".jar")) return "Aplicativo Java";
        if (lower.endsWith(".pdf")) return "Documento PDF";
        if (lower.endsWith(".txt") || lower.endsWith(".log")) return "Arquivo de texto/log";
        if (lower.endsWith(".iso")) return "Imagem de disco";
        if (lower.endsWith(".obb") || lower.endsWith(".pak") || lower.endsWith(".dat")) return "Dados de jogo";
        return "Arquivo genérico";
    }

    private void openSelectedFile() {
        if (selectedUri == null) {
            status.setText("Selecione um arquivo primeiro.");
            return;
        }
        String lower = selectedDisplayName.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".exe") || lower.endsWith(".msi") || lower.endsWith(".bat") || lower.endsWith(".com")) {
            new AlertDialog.Builder(this)
                    .setTitle("Motor Windows necessário")
                    .setMessage("O arquivo foi selecionado sem precisar criar um container, mas este app ainda não possui Wine/Box64 integrado. O Android não executa arquivos EXE nativamente. Para iniciar este jogo ou programa, primeiro é necessário integrar um motor compatível.")
                    .setPositiveButton("OK", null).show();
            status.setText("Arquivo Windows reconhecido; execução indisponível até integrar Wine/Box64.");
            return;
        }
        String mime = getContentResolver().getType(selectedUri);
        if (mime == null) {
            if (lower.endsWith(".pdf")) mime = "application/pdf";
            else if (lower.endsWith(".txt") || lower.endsWith(".log")) mime = "text/plain";
            else if (lower.endsWith(".apk")) mime = "application/vnd.android.package-archive";
            else if (lower.endsWith(".zip")) mime = "application/zip";
            else mime = "*/*";
        }
        try {
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(selectedUri, mime);
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(view, "Abrir arquivo com"));
            status.setText("Solicitada abertura do arquivo em um app compatível.");
        } catch (ActivityNotFoundException e) {
            status.setText("Nenhum aplicativo instalado consegue abrir este tipo de arquivo.");
            Toast.makeText(this, "Nenhum app compatível encontrado", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            status.setText("Não foi possível abrir este arquivo: " + e.getMessage());
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_EXE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                selectedUri = uri;
                try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch (Exception ignored) { }
                selectedDisplayName = queryDisplayName(uri);
                selectedFile.setText("Arquivo: " + selectedDisplayName + "\nTipo: " + fileKind(selectedDisplayName));
                status.setText("Selecionado sem container. Toque em Abrir arquivo selecionado para tentar abrir.");
            }
        }
    }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }

    private static class ContainerProfile {
        int id, ram = 1024, cpu = 2;
        String name, osName = "Windows 10", windows = "Windows 10", resolution = "1280 × 720", orientation = "Paisagem";
        String box64 = "Equilibrado", graphics = "Automático", dx = "Desativado", audio = "Padrão";
        String input = "Toque na tela", fps = "60", env = "", args = "";
        boolean fullscreen = true, pointer = true, audioEnabled = true, esync = false, debug = false;
        ContainerProfile(int id, String name) { this.id = id; this.name = name; }
        ContainerProfile copy() {
            ContainerProfile p = new ContainerProfile(id, name);
            p.ram=ram; p.cpu=cpu; p.windows=windows; p.osName=osName; p.resolution=resolution; p.orientation=orientation;
            p.box64=box64; p.graphics=graphics; p.dx=dx; p.audio=audio; p.input=input; p.fps=fps;
            p.env=env; p.args=args; p.fullscreen=fullscreen; p.pointer=pointer; p.audioEnabled=audioEnabled;
            p.esync=esync; p.debug=debug; return p;
        }
        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id",id); o.put("name",name); o.put("windows",windows); o.put("osName",osName); o.put("resolution",resolution);
                o.put("orientation",orientation); o.put("ram",ram); o.put("cpu",cpu); o.put("box64",box64);
                o.put("graphics",graphics); o.put("dx",dx); o.put("audio",audio); o.put("input",input);
                o.put("fps",fps); o.put("env",env); o.put("args",args); o.put("fullscreen",fullscreen);
                o.put("pointer",pointer); o.put("audioEnabled",audioEnabled); o.put("esync",esync); o.put("debug",debug);
            } catch (Exception ignored) { }
            return o;
        }
        static ContainerProfile fromJson(JSONObject o) {
            ContainerProfile p = new ContainerProfile(o.optInt("id",1), o.optString("name","Container"));
            p.windows=o.optString("windows","Windows 10"); p.osName=o.optString("osName", p.windows); p.resolution=o.optString("resolution","1280 × 720");
            p.orientation=o.optString("orientation","Paisagem"); p.ram=o.optInt("ram",1024); p.cpu=o.optInt("cpu",2);
            p.box64=o.optString("box64",o.optString("preset","Equilibrado"));
            p.graphics=o.optString("graphics",o.optString("graphicsOld","Automático"));
            p.dx=o.optString("dx","Desativado"); p.audio=o.optString("audio","Padrão");
            p.input=o.optString("input","Toque na tela"); p.fps=o.optString("fps","60");
            p.env=o.optString("env",""); p.args=o.optString("args","");
            p.fullscreen=o.optBoolean("fullscreen",true); p.pointer=o.optBoolean("pointer",true);
            p.audioEnabled=o.optBoolean("audioEnabled",true); p.esync=o.optBoolean("esync",false);
            p.debug=o.optBoolean("debug",false);
            return p;
        }
    }
}
