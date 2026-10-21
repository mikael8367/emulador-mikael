# Integração do Winlator no Emulador Mikael

## Estratégia atual

O projeto mantém a interface personalizada do Emulador Mikael e constrói um APK separado, reidentificado como **Emulador Mikael - Motor**, a partir do código completo do projeto upstream Winlator. Essa separação permite usar o motor real sem fingir que o protótipo de perfis e scripts já contém Wine e rootfs.

- Código upstream: https://github.com/brunodev85/winlator
- Licença upstream: LGPL-2.1. Consulte e preserve a licença, avisos e fontes correspondentes ao distribuir modificações.
- O workflow é `.github/workflows/winlator-base.yml`.
- O artefato de compilação esperado é `emulador-mikael-winlator-engine-apk`, contendo o APK do motor e informações/licença.
- A interface principal tem os botões **Abrir motor completo** e **Obter APK do motor**. O APK principal e o APK do motor continuam sendo dois aplicativos separados nesta fase.

## Instalação e primeiro início

1. Abra o workflow de motor no GitHub Actions e baixe o artefato após uma compilação bem-sucedida.
2. Extraia o ZIP do artefato e instale `Emulador-Mikael-Motor-debug.apk`.
3. Abra o motor e mantenha conexão à internet enquanto ele instala/prepara os componentes exigidos pela implementação upstream.
4. Reserve vários GB livres. O espaço exato depende dos componentes e versões baixados.
5. Depois, abra o Emulador Mikael e toque em **Abrir motor completo** para acessar o motor instalado.

O motor upstream é responsável pelo fluxo real de preparação do ambiente Wine/Box86/Box64/rootfs e pelos componentes gráficos que ele suporta. O APK principal ainda não incorpora as classes nativas do motor dentro do mesmo APK; por isso, o botão abre o app de motor separado.

## Limitações importantes

- Compilar o APK não comprova que o motor iniciará ou que qualquer arquivo EXE funcionará.
- O Galaxy A12 de referência usa GPU PowerVR, não Adreno. Turnip é voltado a GPUs Adreno compatíveis e não deve ser tratado como driver universal. A compatibilidade gráfica e o desempenho podem ser limitados.
- Alguns jogos e programas dependem de arquitetura, bibliotecas, DirectX, memória e recursos não disponíveis em todos os aparelhos.
- Não é seguro declarar compatibilidade universal. Teste primeiro com um executável simples e consulte os logs se houver falha.
- O APK do motor é uma build de desenvolvimento; antes de redistribuir, revise a licença LGPL-2.1, as modificações de identidade e os avisos de terceiros.

## Runtime ZIP experimental do app principal

O importador `start-wine.sh`/`start-container.sh` continua sendo uma ponte experimental. Ele não substitui o motor upstream completo e não deve ser apresentado como um runtime autossuficiente.
