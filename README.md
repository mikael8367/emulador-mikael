# Emulador Mikael — LAVOICE Mobile

Projeto Android experimental para investigar a compatibilidade de jogos Windows no Android, com foco inicial no LAVOICE.

## Recursos da interface

- Criar, selecionar, editar e excluir perfis de container.
- Salvar configurações separadas para cada perfil.
- Preferência de versão do Windows (XP legado, 7, 10 ou 11).
- Resolução, orientação e tela cheia.
- Limite de RAM e quantidade de threads de CPU como preferências.
- Perfil Box64 e driver gráfico.
- Opção de tradutor DirectX (DXVK, WineD3D ou VKD3D-Proton).
- Preferências de áudio, controles, limite de FPS e ponteiro do mouse.
- Opções de ESYNC e logs de depuração.
- Campo para variáveis de ambiente e argumentos de inicialização.
- Seletor de arquivos para localizar um executável.
- GitHub Actions para compilar APK de depuração.

## Limitação importante

**Esses containers ainda são perfis de configuração, não ambientes Windows reais.** As configurações são armazenadas localmente, mas ainda não afetam a execução porque o motor Wine/Box64 e as camadas gráficas ainda não foram integrados. Selecionar um EXE não o executa.

Opções como Turnip dependem de hardware e drivers compatíveis; não são aceleradores universais. RAM, CPU, DirectX e FPS também precisam ser ligados ao motor real e não podem ser garantidos pela interface.

O alvo inicial de teste é um Samsung Galaxy A12 com Android 12 e GPU PowerVR. A compatibilidade com LAVOICE ou qualquer outro EXE precisa ser verificada na prática.

## Construir APK de teste

1. Abra a aba **Actions** do GitHub.
2. Entre na execução **Build Android APK** iniciada por um commit.
3. Se a execução terminar com sucesso, baixe o artefato `emulador-mikael-debug-apk`.
4. Extraia o APK do ZIP do artefato.

O workflow compila a interface Android; isso não significa que o motor Windows esteja implementado.

## Próximas etapas

1. Compilar e testar a interface no dispositivo Android.
2. Integrar um motor de compatibilidade Windows adequado a Android/ARM64.
3. Implementar containers reais isolados, gerenciamento de arquivos e instalação de dependências.
4. Conectar cada preferência ao motor e validar o LAVOICE com testes reais.

## Segurança

Use apenas executáveis de fontes confiáveis. Selecionar um arquivo não o executa nem verifica se é seguro.
