# Contrato de pacote de runtime Windows

O aplicativo não inclui Wine, Box64, Box86, PRoot ou uma rootfs Linux por padrão. O pacote de runtime é separado porque esses componentes são grandes, têm licenças distintas e precisam ser compilados e validados para a arquitetura/dispositivo.

## Estrutura exigida

Importe um ZIP que tenha este arquivo na raiz:

```
start-wine.sh
(opcionais: binários, bibliotecas e arquivos auxiliares do runtime)
```

O app extrai o pacote para o diretório privado `files/wine-runtime` e chama o script por meio de `/system/bin/sh`, passando:

1. argumento 1: caminho absoluto do EXE copiado para o diretório privado do app;
2. argumento 2: caminho absoluto do prefixo Wine persistente;
3. variável `MIKAEL_EXE_PATH`: o mesmo caminho do EXE;
4. variável `MIKAEL_WINEPREFIX`: prefixo Wine;
5. variável `MIKAEL_RUNTIME_DIR`: diretório extraído do pacote.

O script deve configurar o seu runtime de forma independente, iniciar o programa em primeiro plano e encaminhar stdout/stderr para o processo pai. Se precisar de PRoot, Box64, Wine, bibliotecas glibc, um servidor gráfico ou outras dependências, o pacote precisa fornecê-las e configurá-las.

## Estado e limitações

- O importador valida o caminho ZIP e exige `start-wine.sh`; isso **não valida** a integridade, a arquitetura ou o funcionamento de Wine/Box64.\n- A extração atual não preserva permissões POSIX, links simbólicos, hard links ou ownership de Linux. Por isso, uma rootfs Linux convencional pode não funcionar ao ser importada neste formato; ainda falta um instalador de runtime capaz de montar corretamente esses componentes.
- Este repositório ainda não fornece um pacote de runtime funcional. Não use um script de exemplo como se fosse Wine.
- O app chama o script se o pacote estiver presente, mas o status “processo iniciado” não garante que o jogo abriu. Consulte `files/windows-engine.log` para diagnóstico.
- Não há garantia de compatibilidade com jogos x86/x64, DirectX, DRM, anti-cheat, Vulkan ou drivers gráficos.
- Um pacote completo pode exigir vários gigabytes e o armazenamento disponível precisa ser conferido antes da importação.

## Segurança

Um pacote de runtime pode executar código nativo e scripts dentro do contexto do aplicativo. Importe apenas pacotes que você mesmo compilou ou cuja origem e integridade você verificou. O importador não verifica assinaturas criptográficas. O ZIP não deve conter executáveis maliciosos nem arquivos obtidos de fontes desconhecidas.

## Próxima etapa de engenharia

Produzir e validar um pacote real para Android ARM64 com Wine, camada x86/x86_64 adequada, rootfs/glibc, PRoot se necessário, bibliotecas gráficas e um caminho de renderização compatível com o dispositivo. Testar com um executável simples antes de tentar jogos.
