# Emulador Mikael — ambientes Windows e Linux no Android

Projeto Android experimental para investigar a execução de software de desktop no Android.

## Recursos da interface

- Criar, selecionar, editar e excluir perfis de container.
- Escolher sistema operacional/distribuição por perfil:
  - **Windows:** 11, 10, 8.1, 7 e XP legado.
  - **Linux:** Ubuntu, Debian, Linux Mint, Fedora, Arch Linux, Manjaro, Kali Linux, openSUSE, Alpine Linux e opção genérica para outra distribuição.
  - **Outros:** FreeBSD, ReactOS experimental e FreeDOS.
- Preferências por container: resolução, orientação, tela cheia, limite de RAM, threads de CPU, perfil Box64, driver gráfico, tradutor DirectX, áudio, controles, limite de FPS, ESYNC, logs, variáveis de ambiente e argumentos de inicialização.
- Salvar configurações localmente por perfil.
- Selecionar arquivos diretamente, sem criar container, e identificar tipos comuns (EXE/MSI, APK, ZIP, JAR, PDF, scripts, binários e dados de jogos).\n- Tentar abrir formatos compatíveis com aplicativos já instalados no Android.\n- Importar um ZIP de runtime que contenha `start-wine.sh` na raiz.\n- Encaminhar um EXE ao script do runtime, sem exigir a criação manual de um perfil de container.\n- Manter o acesso ao arquivo escolhido usando a permissão concedida pelo seletor do Android.
- GitHub Actions para compilar APK de depuração.

## Limitação importante

**A lista de sistemas operacionais é, por enquanto, uma seleção de perfil na interface. A abertura direta não executa arquivos Windows.** O aplicativo ainda não baixa nem instala imagens de Windows, distribuições Linux ou outros sistemas. Também não cria containers reais nem inicia máquinas virtuais. A tela permite importar um pacote de runtime externo e chamar seu script de inicialização para um EXE sem criar um perfil manualmente, mas o repositório ainda não inclui um runtime funcional Wine/Box64/rootfs. O importador verifica somente a estrutura do ZIP, não a integridade ou o funcionamento do motor; sem um pacote compatível, os EXEs não serão executados.

Windows em ARM/PC x86, Linux ARM/x86, BSD e DOS têm requisitos diferentes. O caminho mais adequado pode ser Wine + camada de tradução para programas Windows, um ambiente Linux user-space como proot para ferramentas Linux, ou virtualização real quando houver suporte de hardware e desempenho suficiente. A seleção de um nome não instala automaticamente esse sistema.

O alvo inicial de teste é um Samsung Galaxy A12 com Android 12 e GPU PowerVR. Compatibilidade e desempenho dependem do motor escolhido, arquitetura do programa, bibliotecas e suporte gráfico; não há garantia de que LAVOICE ou qualquer outro executável funcione.

## Construir APK de teste

1. Abra a aba **Actions** do GitHub.
2. Entre na execução **Build Android APK** iniciada por um commit.
3. Se a execução terminar com sucesso, baixe o artefato `emulador-mikael-debug-apk`.
4. Extraia o APK do ZIP do artefato.

O workflow compila a interface Android; isso não significa que os sistemas operacionais já estejam instalados ou funcionais.

## Próximas etapas

1. Compilar e testar a interface no Android.
2. Escolher uma arquitetura de execução apropriada para cada família de sistema.
3. Integrar instalações e ambientes isolados, armazenamento e gestão de dependências.
4. Testar cada ambiente e documentar as limitações reais.

## Segurança

Instale imagens e executáveis somente de fontes confiáveis. Selecionar um arquivo não o executa nem verifica se é seguro.
