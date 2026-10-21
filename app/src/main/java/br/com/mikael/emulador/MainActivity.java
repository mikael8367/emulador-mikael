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
import android.graphics.drawable.GradientDrawable;
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
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    private static final int PICK_EXE = 41;
    private static final int PICK_RUNTIME = 42;
    private Uri selectedUri;
    private String selectedDisplayName = "";
    private TextView runtimeLabel;
    private volatile Process runningProcess;
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

    private LinearLayout rootLayout, page;
    private ScrollView contentScroll;
    private LinearLayout bottomNav;
    private int currentTab = 0;
    private LinearLayout[] navItems;

    private void buildUi() {
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(bg);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(14), dp(18), dp(10));
        header.setBackgroundColor(Color.rgb(13, 23, 18));
        header.addView(text("MIKAEL • WINDOWS ON ANDROID", 11, green, true));
        TextView title = text("Emulador Mikael", 25, Color.WHITE, true);
        title.setPadding(0, dp(3), 0, dp(2));
        header.addView(title);
        header.addView(text("Programas e jogos • interface experimental", 12, muted, false));
        rootLayout.addView(header);

        status = text("Pronto. Escolha uma aba para começar.", 12, green, false);
        status.setPadding(dp(18), dp(8), dp(18), dp(8));
        status.setBackgroundColor(Color.rgb(16, 27, 21));
        rootLayout.addView(status, new LinearLayout.LayoutParams(-1, -2));

        contentScroll = new ScrollView(this);
        contentScroll.setFillViewport(true);
        contentScroll.setBackgroundColor(bg);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(16), dp(16), dp(22));
        contentScroll.addView(page);
        rootLayout.addView(contentScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setPadding(dp(4), dp(5), dp(4), dp(5));
        bottomNav.setBackgroundColor(Color.rgb(13, 23, 18));
        String[] labels = {"Início", "Arquivos", "Containers", "Motores"};
        String[] symbols = {"⌂", "▣", "▦", "⚙"};
        navItems = new LinearLayout[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int tab = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(2), dp(5), dp(2), dp(4));
            item.setBackground(customShape(Color.TRANSPARENT, 15, Color.TRANSPARENT, 0));
            TextView glyph = text(symbols[i], 20, muted, true);
            glyph.setGravity(Gravity.CENTER);
            TextView caption = text(labels[i], 10, muted, true);
            caption.setGravity(Gravity.CENTER);
            item.addView(glyph);
            item.addView(caption);
            item.setContentDescription(labels[i]);
            item.setOnClickListener(v -> showTab(tab));
            navItems[i] = item;
            bottomNav.addView(item, new LinearLayout.LayoutParams(0, dp(58), 1));
        }
        rootLayout.addView(bottomNav);
        setContentView(rootLayout);
        showTab(0);
        refreshRuntimeStatus();
    }

    private void showTab(int tab) {
        currentTab = tab;
        page.removeAllViews();
        for (int i = 0; i < navItems.length; i++) {
            LinearLayout item = navItems[i];
            int color = i == tab ? green : muted;
            item.setBackground(customShape(i == tab ? Color.rgb(27, 55, 39) : Color.TRANSPARENT,
                    15, i == tab ? Color.rgb(45, 95, 64) : Color.TRANSPARENT, i == tab ? 1 : 0));
            for (int j = 0; j < item.getChildCount(); j++) {
                View child = item.getChildAt(j);
                if (child instanceof TextView) ((TextView) child).setTextColor(color);
            }
        }
        contentScroll.scrollTo(0, 0);
        if (tab == 0) buildHomeTab();
        else if (tab == 1) buildFilesTab();
        else if (tab == 2) buildContainersTab();
        else buildEnginesTab();
    }

    private void buildHomeTab() {
        TextView hero = text("Seu ambiente Windows no Android", 22, Color.WHITE, true);
        hero.setPadding(0, 0, 0, dp(6));
        page.addView(hero);
        page.addView(text("Acesse rapidamente os arquivos, perfis e componentes do emulador.", 14, muted, false));

        TextView device = text("DISPOSITIVO\nAndroid " + Build.VERSION.RELEASE + " • API " + Build.VERSION.SDK_INT
                + "\nArquitetura: " + Build.SUPPORTED_ABIS[0], 14, Color.WHITE, false);
        cardStyle(device); addTop(page, device, 16);

        addTop(page, text("ACESSO RÁPIDO", 12, green, true), 22);
        Button openFiles = button("Selecionar programa ou jogo");
        openFiles.setOnClickListener(v -> showTab(1)); addTop(page, openFiles, 8);
        Button manageContainers = button("Gerenciar containers");
        manageContainers.setOnClickListener(v -> showTab(2)); addTop(page, manageContainers, 8);
        Button manageEngines = button("Ver motores de execução");
        manageEngines.setOnClickListener(v -> showTab(3)); addTop(page, manageEngines, 8);

        TextView active = text("", 13, muted, false);
        ContainerProfile profile = getActive();
        active.setText(profile == null ? "Nenhum perfil selecionado" : "Perfil selecionado: " + profile.name
                + "\nAs opções do perfil ainda não configuram um motor real automaticamente.");
        cardStyle(active); addTop(page, active, 18);

        TextView note = text("Nota: a interface e os perfis não significam que todo EXE seja compatível. A execução depende dos motores instalados, da arquitetura do programa e do hardware.", 12, muted, false);
        note.setPadding(dp(12), dp(12), dp(12), dp(12));
        note.setBackgroundColor(panel); addTop(page, note, 14);
    }

    private void buildFilesTab() {
        page.addView(text("Arquivos e jogos", 22, Color.WHITE, true));
        page.addView(text("Selecione um arquivo do armazenamento. O Android abrirá formatos compatíveis; EXE precisa de um runtime Windows funcional.", 13, muted, false));
        Button choose = button("＋ Selecionar arquivo ou jogo");
        choose.setOnClickListener(v -> pickExe()); addTop(page, choose, 14);
        selectedFile = text("Nenhum arquivo selecionado", 14, Color.WHITE, false);
        cardStyle(selectedFile); addTop(page, selectedFile, 10);
        Button openSelected = button("Abrir / executar arquivo");
        openSelected.setOnClickListener(v -> openSelectedFile()); addTop(page, openSelected, 8);
        Button viewEngine = button("Configurar motor");
        viewEngine.setOnClickListener(v -> showTab(3)); addTop(page, viewEngine, 8);
    }

    private void openFullEngine() {
        new AlertDialog.Builder(this)
                .setTitle("Motor integrado")
                .setMessage("A interface personalizada do Emulador Mikael está ativa. A integração direta das funções do motor ainda está em desenvolvimento; abrir a tela original do motor foi desativado para manter a interface própria.")
                .setPositiveButton("OK", null)
                .show();
        status.setText("Interface Mikael ativa • integração direta do motor pendente.");
    }

    private void buildContainersTab() {
        page.addView(text("Containers", 22, Color.WHITE, true));
        page.addView(text("Organize perfis e preferências em cartões. Criar um perfil não instala nem inicia um sistema operacional.", 13, muted, false));
        activeLabel = text("", 13, green, true);
        addTop(page, activeLabel, 12);
        containerList = new LinearLayout(this);
        containerList.setOrientation(LinearLayout.VERTICAL);
        page.addView(containerList);
        Button add = button("＋ Criar container");
        add.setOnClickListener(v -> showContainerEditor(null)); addTop(page, add, 12);
        renderContainers();
    }

    private void buildEnginesTab() {
        page.addView(text("Motores de execução", 22, Color.WHITE, true));
        page.addView(text("Instale ou atualize um pacote de motor Windows a partir de um ZIP compatível. O instalador valida a estrutura e copia os arquivos para o armazenamento privado do app.", 13, muted, false));
        runtimeLabel = text("", 13, Color.WHITE, false);
        runtimeLabel.setPadding(dp(13), dp(13), dp(13), dp(13));
        runtimeLabel.setBackgroundColor(panel); addTop(page, runtimeLabel, 14);
        addTop(page, text("CHECKLIST DE COMPONENTES", 12, green, true), 16);
        TextView componentsChecklist = text("", 13, Color.WHITE, false);
        componentsChecklist.setPadding(dp(13), dp(13), dp(13), dp(13));
        componentsChecklist.setBackgroundColor(panel);
        componentsChecklist.setTag("component-checklist");
        addTop(page, componentsChecklist, 8);
        updateComponentChecklist(componentsChecklist);
        Button checkComponents = button("↻ Verificar componentes");
        checkComponents.setOnClickListener(v -> updateComponentChecklist(componentsChecklist));
        addTop(page, checkComponents, 8);
        Button installRuntime = button("⬇  Instalar motor Windows (.ZIP)");
        installRuntime.setOnClickListener(v -> showRuntimeInstallDialog()); addTop(page, installRuntime, 10);
        Button importRuntime = smallButton("Selecionar outro ZIP / reinstalar");
        importRuntime.setOnClickListener(v -> pickRuntime()); addTop(page, importRuntime, 8);
        Button showEngineGuide = button("Componentes necessários");
        showEngineGuide.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Motores e componentes")
                .setMessage("Wine fornece a camada de compatibilidade Windows. Box64/Box86 podem traduzir aplicações x86/x64, com bibliotecas e rootfs compatíveis. Gráficos, áudio e entrada precisam de integração nativa. Nem todos os jogos funcionam.\n\nO Galaxy A12 usa GPU PowerVR; drivers específicos de Adreno, como Turnip, não são universais. A importação de um pacote valida a estrutura, mas não garante que o runtime funcione.")
                .setPositiveButton("Entendi", null).show()); addTop(page, showEngineGuide, 8);
        TextView warning = text("A interface é do Emulador Mikael. A execução direta via motor ainda precisa ser integrada e testada no aparelho.", 13, Color.rgb(255, 220, 145), false);
        warning.setPadding(dp(13), dp(13), dp(13), dp(13));
        warning.setBackgroundColor(Color.rgb(48, 39, 22)); addTop(page, warning, 14);
        refreshRuntimeStatus();
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
            Button runContainer = button("▶  Executar container");
            runContainer.setOnClickListener(v -> launchContainer(p));
            addTop(card, runContainer, 4);
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
    private GradientDrawable customShape(int fill, int radiusDp, int stroke, int strokeDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) shape.setStroke(dp(strokeDp), stroke);
        return shape;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(10), dp(16), dp(10));
        b.setMinHeight(dp(48));
        b.setBackground(customShape(Color.rgb(25, 57, 39), 14, Color.rgb(54, 121, 80), 1));
        b.setStateListAnimator(null);
        if (Build.VERSION.SDK_INT >= 21) b.setElevation(dp(1));
        return b;
    }

    private Button smallButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(11);
        b.setTextColor(green);
        b.setAllCaps(false);
        b.setPadding(dp(4), dp(4), dp(4), dp(4));
        b.setBackground(customShape(Color.rgb(28, 42, 34), 10, Color.rgb(51, 79, 61), 1));
        b.setStateListAnimator(null);
        return b;
    }
    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
        p.setMargins(dp(2), 0, dp(2), 0); return p;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private void cardStyle(TextView v) {
        v.setPadding(dp(15), dp(14), dp(15), dp(14));
        v.setBackground(customShape(panel, 16, Color.rgb(37, 57, 45), 1));
    }
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

    private File runtimeDir() { return new File(getFilesDir(), "wine-runtime"); }

    private void refreshRuntimeStatus() {
        if (runtimeLabel == null) return;
        File wineScript = new File(runtimeDir(), "start-wine.sh");
        File containerScript = new File(runtimeDir(), "start-container.sh");
        boolean installed = wineScript.isFile() || containerScript.isFile();
        runtimeLabel.setText(installed
                ? "Pacote detectado • " + runtimeDir().getAbsolutePath()
                    + "\\nEXE: " + (wineScript.isFile() ? "script encontrado" : "script ausente")
                    + " • Container: " + (containerScript.isFile() ? "script encontrado" : "script ausente")
                : "Motor não instalado • importe um ZIP com start-wine.sh e/ou start-container.sh");
        runtimeLabel.setTextColor(installed ? green : muted);
        View checklist = page == null ? null : page.findViewWithTag("component-checklist");
        if (checklist instanceof TextView) updateComponentChecklist((TextView) checklist);
    }

    private void updateComponentChecklist(TextView target) {
        File root = runtimeDir();
        boolean packageDir = root.isDirectory();
        boolean wineScript = new File(root, "start-wine.sh").isFile();
        boolean containerScript = new File(root, "start-container.sh").isFile();
        File wine = findNamedFile(root, new String[]{"wine"});
        File box64 = findNamedFile(root, new String[]{"box64"});
        File box86 = findNamedFile(root, new String[]{"box86"});
        File proot = findNamedFile(root, new String[]{"proot"});
        File rootfs = findNamedDirectory(root, new String[]{"rootfs", "ubuntu", "debian", "alpine", "filesystem"});
        File vulkan = findNamedFile(root, new String[]{"libvulkan.so", "vulkan-1.dll"});
        File dxvk = findNamedFile(root, new String[]{"d3d11.dll", "dxgi.dll"});
        StringBuilder report = new StringBuilder();
        report.append(componentLine("Pacote de runtime", packageDir));
        report.append(componentLine("Inicializador de EXE (start-wine.sh)", wineScript));
        report.append(componentLine("Inicializador de container (start-container.sh)", containerScript));
        report.append(componentLine("Wine", wine != null));
        report.append(componentLine("Box64 (x86_64)", box64 != null));
        report.append(componentLine("Box86 (x86 32-bit, opcional)", box86 != null));
        report.append(componentLine("PRoot (opcional, conforme o runtime)", proot != null));
        report.append(componentLine("Root filesystem Linux", rootfs != null));
        report.append(componentLine("Biblioteca/arquivos Vulkan (heurística)", vulkan != null));
        report.append(componentLine("Arquivos DXVK (heurística)", dxvk != null));
        report.append("\\nA detecção verifica nomes e caminhos, não a integridade nem a execução real. Componentes opcionais dependem do runtime e do hardware; ausência não prova incompatibilidade.");
        target.setText(report.toString());
    }

    private String componentLine(String label, boolean found) {
        return (found ? "✓  " : "⚠  ") + label + (found ? " — encontrado" : " — não detectado") + "\\n";
    }

    private File findNamedFile(File dir, String[] names) {
        if (dir == null || !dir.isDirectory()) return null;
        File[] children = dir.listFiles();
        if (children == null) return null;
        for (File child : children) {
            if (child.isFile()) {
                for (String name : names) if (child.getName().equalsIgnoreCase(name)) return child;
            }
        }
        for (File child : children) {
            if (child.isDirectory()) {
                File found = findNamedFile(child, names);
                if (found != null) return found;
            }
        }
        return null;
    }

    private File findNamedDirectory(File dir, String[] names) {
        if (dir == null || !dir.isDirectory()) return null;
        File[] children = dir.listFiles();
        if (children == null) return null;
        for (File child : children) {
            if (child.isDirectory()) {
                for (String name : names) if (child.getName().equalsIgnoreCase(name)) return child;
            }
        }
        for (File child : children) {
            if (child.isDirectory()) {
                File found = findNamedDirectory(child, names);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void showRuntimeInstallDialog() {
        boolean installed = new File(runtimeDir(), "start-wine.sh").isFile()
                || new File(runtimeDir(), "start-container.sh").isFile();
        new AlertDialog.Builder(this)
                .setTitle(installed ? "Atualizar ou reinstalar motor" : "Instalar motor Windows")
                .setMessage("Escolha um arquivo ZIP de runtime compatível com Android ARM64. Para abrir programas Windows, o ZIP precisa ter start-wine.sh na raiz. Para iniciar containers, precisa ter start-container.sh na raiz.\n\nO app não baixa um motor pronto automaticamente: você precisa selecionar um pacote que já tenha Wine/Box64 e os arquivos necessários. A instalação extrai o ZIP no armazenamento privado do app e substitui o pacote anterior somente quando a estrutura básica é válida. Isso não garante compatibilidade ou execução real.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Escolher ZIP", (d, w) -> pickRuntime())
                .show();
    }

    private void pickRuntime() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        try { startActivityForResult(intent, PICK_RUNTIME); }
        catch (Exception e) { status.setText("Não foi possível abrir o seletor do pacote de motor."); }
    }

    private void importRuntime(Uri uri) {
        status.setText("Importando pacote do motor…");
        new Thread(() -> {
            File stage = new File(getFilesDir(), "wine-runtime-installing");
            deleteTree(stage);
            boolean ok = false;
            String message;
            try {
                if (!stage.mkdirs() && !stage.isDirectory()) throw new IOException("Não foi possível criar a pasta temporária.");
                long total = 0;
                InputStream raw = getContentResolver().openInputStream(uri);
                if (raw == null) throw new IOException("Não foi possível ler o ZIP.");
                try (InputStream source = raw; ZipInputStream zip = new ZipInputStream(source)) {
                    ZipEntry entry;
                    byte[] buffer = new byte[32768];
                    String root = stage.getCanonicalPath() + File.separator;
                    while ((entry = zip.getNextEntry()) != null) {
                        File out = new File(stage, entry.getName());
                        String path = out.getCanonicalPath();
                        if (!path.startsWith(root)) throw new IOException("O ZIP contém um caminho inválido.");
                        if (entry.isDirectory()) {
                            if (!out.mkdirs() && !out.isDirectory()) throw new IOException("Falha ao criar diretório do pacote.");
                        } else {
                            File parent = out.getParentFile();
                            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("Falha ao criar diretório.");
                            try (FileOutputStream file = new FileOutputStream(out)) {
                                int count;
                                while ((count = zip.read(buffer)) != -1) {
                                    total += count;
                                    if (total > 8L * 1024L * 1024L * 1024L) throw new IOException("O pacote descompactado excede 8 GB.");
                                    file.write(buffer, 0, count);
                                }
                            }
                        }
                        zip.closeEntry();
                    }
                }
                File wineScript = new File(stage, "start-wine.sh");
                File containerScript = new File(stage, "start-container.sh");
                if (!wineScript.isFile() && !containerScript.isFile()) {
                    throw new IOException("Pacote incompatível: precisa conter start-wine.sh (executar EXE) ou start-container.sh (iniciar container) na raiz do ZIP.");
                }
                File destination = runtimeDir();
                File backup = new File(getFilesDir(), "wine-runtime-backup");
                deleteTree(backup);
                if (destination.exists() && !destination.renameTo(backup)) throw new IOException("Não foi possível substituir o motor atual.");
                if (!stage.renameTo(destination)) {
                    if (backup.exists()) backup.renameTo(destination);
                    throw new IOException("Não foi possível ativar o pacote importado.");
                }
                deleteTree(backup);
                ok = true;
                message = "Pacote importado. Isso valida apenas a estrutura; a execução depende do conteúdo do motor.";
            } catch (Exception e) {
                message = "Falha ao importar o motor: " + e.getMessage();
                deleteTree(stage);
            }
            final boolean success = ok;
            final String result = message;
            runOnUiThread(() -> {
                refreshRuntimeStatus();
                status.setText(result);
                Toast.makeText(MainActivity.this, success ? "Pacote importado" : "Importação falhou", Toast.LENGTH_LONG).show();
            });
        }, "runtime-import").start();
    }

    private void launchContainer(ContainerProfile profile) {
        File script = new File(runtimeDir(), "start-container.sh");
        if (!script.isFile()) {
            new AlertDialog.Builder(this)
                    .setTitle("Inicializador de container ausente")
                    .setMessage("O perfil foi salvo, mas ainda não é um sistema operacional instalado. Para iniciar um container, importe na aba Motores um pacote compatível que contenha start-container.sh na raiz. Um pacote que tenha somente start-wine.sh serve para tentar abrir EXEs, não para iniciar um ambiente de container completo.")
                    .setPositiveButton("Abrir Motores", (d, w) -> showTab(3))
                    .setNegativeButton("Fechar", null).show();
            status.setText("Container não iniciado: falta start-container.sh no runtime.");
            return;
        }
        status.setText("Iniciando container: " + profile.name + "…");
        new Thread(() -> {
            try {
                File containerRoot = new File(getFilesDir(), "containers/" + profile.id);
                if (!containerRoot.exists() && !containerRoot.mkdirs()) {
                    throw new IOException("Não foi possível criar a pasta de dados do container.");
                }
                File log = new File(getFilesDir(), "container-" + profile.id + ".log");
                ProcessBuilder builder = new ProcessBuilder("/system/bin/sh", script.getAbsolutePath(),
                        containerRoot.getAbsolutePath());
                builder.directory(runtimeDir());
                builder.redirectErrorStream(true);
                builder.redirectOutput(log);
                builder.environment().put("MIKAEL_CONTAINER_ID", String.valueOf(profile.id));
                builder.environment().put("MIKAEL_CONTAINER_NAME", profile.name);
                builder.environment().put("MIKAEL_CONTAINER_OS", profile.osName);
                builder.environment().put("MIKAEL_CONTAINER_ROOT", containerRoot.getAbsolutePath());
                builder.environment().put("MIKAEL_CONTAINER_RESOLUTION", profile.resolution);
                builder.environment().put("MIKAEL_CONTAINER_RAM_MB", String.valueOf(profile.ram));
                builder.environment().put("MIKAEL_CONTAINER_CPU_THREADS", String.valueOf(profile.cpu));
                builder.environment().put("MIKAEL_CONTAINER_GRAPHICS", profile.graphics);
                builder.environment().put("MIKAEL_CONTAINER_DIRECTX", profile.dx);
                Process process = builder.start();
                runningProcess = process;
                runOnUiThread(() -> status.setText("Inicializador executado para " + profile.name
                        + ". Isso não confirma que o sistema abriu; log: " + log.getAbsolutePath()));
                new Thread(() -> {
                    try {
                        int code = process.waitFor();
                        runOnUiThread(() -> status.setText("Inicializador do container terminou (código "
                                + code + "). Consulte " + log.getAbsolutePath()));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, "container-watch").start();
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Falha ao iniciar container: " + e.getMessage()));
            }
        }, "container-launch").start();
    }

    private void launchWindowsFile() {
        File script = new File(runtimeDir(), "start-wine.sh");
        if (!script.isFile()) {
            new AlertDialog.Builder(this)
                    .setTitle("Motor Windows não instalado")
                    .setMessage("Para tentar executar um EXE sem criar container manualmente, importe um pacote de runtime compatível. O ZIP deve conter start-wine.sh na raiz; veja RUNTIME-BUNDLE.md no repositório. O app ainda não inclui Wine/Box64 por padrão.")
                    .setPositiveButton("OK", null).show();
            status.setText("Execução não iniciada: falta o pacote do motor.");
            return;
        }
        status.setText("Preparando arquivo para o motor…");
        new Thread(() -> {
            File launchDir = new File(getFilesDir(), "launches");
            if (!launchDir.exists() && !launchDir.mkdirs()) {
                runOnUiThread(() -> status.setText("Não foi possível criar a pasta temporária."));
                return;
            }
            String safeName = selectedDisplayName.replaceAll("[^A-Za-z0-9._-]", "_");
            if (safeName.isEmpty()) safeName = "program.exe";
            File exe = new File(launchDir, safeName);
            try (InputStream in = getContentResolver().openInputStream(selectedUri);
                 FileOutputStream out = new FileOutputStream(exe)) {
                if (in == null) throw new IOException("Não foi possível ler o arquivo selecionado.");
                byte[] buffer = new byte[32768]; int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Falha ao preparar EXE: " + e.getMessage()));
                return;
            }
            File prefix = new File(getFilesDir(), "wine-prefix");
            if (!prefix.exists()) prefix.mkdirs();
            try {
                ProcessBuilder builder = new ProcessBuilder("/system/bin/sh", script.getAbsolutePath(),
                        exe.getAbsolutePath(), prefix.getAbsolutePath());
                builder.directory(runtimeDir());
                builder.redirectErrorStream(true);
                File log = new File(getFilesDir(), "windows-engine.log");
                builder.redirectOutput(log);
                builder.environment().put("MIKAEL_EXE_PATH", exe.getAbsolutePath());
                builder.environment().put("MIKAEL_WINEPREFIX", prefix.getAbsolutePath());
                builder.environment().put("MIKAEL_RUNTIME_DIR", runtimeDir().getAbsolutePath());
                Process process = builder.start();
                runningProcess = process;
                runOnUiThread(() -> status.setText("Motor iniciado como processo. O jogo ainda não foi confirmado; log: " + log.getAbsolutePath()));
                new Thread(() -> {
                    try {
                        int exitCode = process.waitFor();
                        runOnUiThread(() -> status.setText("Processo do motor terminou (código " + exitCode + "). Consulte windows-engine.log."));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, "windows-engine-watch").start();
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Falha ao iniciar o motor: " + e.getMessage()));
            }
        }, "windows-engine-launch").start();
    }

    private void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteTree(child);
        file.delete();
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
            launchWindowsFile();
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
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == PICK_RUNTIME) {
            importRuntime(uri);
        } else if (requestCode == PICK_EXE) {
            selectedUri = uri;
            try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
            catch (Exception ignored) { }
            selectedDisplayName = queryDisplayName(uri);
            selectedFile.setText("Arquivo: " + selectedDisplayName + "\nTipo: " + fileKind(selectedDisplayName));
            status.setText("Selecionado sem container. Toque em Executar / abrir arquivo.");
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
