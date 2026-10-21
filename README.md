# Emulador Mikael — LAVOICE Mobile

Projeto Android experimental para investigar a compatibilidade de jogos Windows no Android, com foco inicial no LAVOICE.

## O que já existe

- Interface Android verde/preta em Java.
- Lista de perfis de container.
- Criar, selecionar, editar e excluir containers (mantém pelo menos um).
- Preferências por container: nome, resolução, perfil Box64 e modo gráfico.
- Perfis salvos localmente no aplicativo.
- Seletor de arquivos para localizar um executável.
- GitHub Actions para compilar um APK de depuração.

## Limitação importante

**Os containers atuais são perfis de configuração da interface; ainda não são ambientes Windows reais.** Eles não criam prefixos Wine, não instalam Wine/Box64 e não executam arquivos .exe. As opções de resolução, perfil Box64 e gráficos são armazenadas para a futura integração do motor, mas ainda não alteram a execução.

A compatibilidade depende da arquitetura do programa, APIs do Windows, bibliotecas e suporte gráfico. O dispositivo inicial de teste é um Samsung Galaxy A12 com Android 12 e GPU PowerVR, então não há garantia de que o LAVOICE ou qualquer outro EXE funcione nele.

## Construir APK de teste

1. Abra a aba **Actions** do GitHub.
2. Entre na execução **Build Android APK** iniciada por um commit.
3. Se a execução terminar com sucesso, baixe o artefato `emulador-mikael-debug-apk`.
4. Extraia o APK do ZIP do artefato.

O workflow compila a interface Android; isso não significa que o motor Windows esteja implementado.

## Próximas etapas

1. Compilar e testar a interface no dispositivo Android.
2. Integrar e empacotar um motor de compatibilidade Wine e uma camada de tradução x86_64 adequada para ARM64.
3. Implementar a criação real e isolada dos ambientes Windows, armazenamento, instalação de componentes e execução.
4. Testar o LAVOICE e registrar resultados reais de compatibilidade e desempenho.

## Segurança

Use apenas executáveis de fontes confiáveis. Selecionar um arquivo não o executa nem verifica se é seguro.
