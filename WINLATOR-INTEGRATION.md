# Integração do Winlator

## Fonte vinculada

O projeto contém o código Android do Winlator como submódulo Git em `winlator-source/`:

- Repositório de origem: https://github.com/brunodev85/winlator-app
- Commit fixado: `8077f374ef2d54127b088de9e8cd7b4af14d9c8b`
- Licença do código-fonte: LGPL-2.1; consulte `winlator-source/LICENSE` e cumpra as obrigações aplicáveis antes de redistribuir versões modificadas.

A referência é fixada num commit para que o código usado no teste não mude inesperadamente.

## Teste de compilação

O workflow `.github/workflows/winlator-base.yml` faz checkout recursivo do submódulo, configura Java/Android SDK/NDK e tenta compilar o APK-base do Winlator. Se a compilação terminar com sucesso, o artefato chama-se `winlator-base-debug-apk`.

Esse APK é um **artefato de teste do Winlator original**, não uma versão final do Emulador Mikael. Ainda usa a identidade/pacote do Winlator e não deve ser apresentado como o aplicativo rebatizado.

## Próximas etapas para uma integração real

1. Confirmar que o código-fonte vinculado compila no CI.
2. Trazer o código para uma base modificável e definir uma estratégia sustentável para atualizar o upstream.
3. Adaptar nome, ícone, identificador do pacote e interface do Emulador Mikael sem quebrar a inicialização do ambiente.
4. Validar os componentes nativos e a instalação do ambiente Wine/rootfs.
5. Testar com executáveis não críticos no Android real; compilação sozinha não prova compatibilidade.

## Compatibilidade de GPU

O Galaxy A12 informado como dispositivo-alvo usa GPU PowerVR. O driver Turnip é destinado a GPUs Adreno compatíveis e não deve ser selecionado como se fosse universal. A compatibilidade gráfica precisa ser verificada com uma opção adequada, como VirGL/Vortek quando suportada pela combinação de componentes, sem garantia de desempenho.
