# Emulador Mikael — LAVOICE Mobile

Projeto Android em desenvolvimento para investigar a execução de jogos Windows no Android, com foco inicial em avaliar a compatibilidade do LAVOICE.

## Estado atual

Esta é a base inicial do aplicativo Android. **Ainda não executa arquivos .exe.** O seletor permite escolher um arquivo, mas o motor de compatibilidade Windows (por exemplo, Wine e uma camada de tradução x86_64 compatível) ainda precisa ser integrado e validado.

Não existe garantia de que todo executável funcione. Compatibilidade depende da arquitetura, APIs do Windows, bibliotecas, DirectX/Vulkan e capacidade do dispositivo. O alvo inicial é um Samsung Galaxy A12 com Android 12 e GPU PowerVR, portanto o desempenho e o suporte gráfico precisam ser testados no aparelho real.

## Construir APK de teste

1. Abra a aba **Actions** do GitHub.
2. Execute **Build Android APK** ou envie um commit.
3. Quando terminar, abra a execução concluída e baixe o artefato `emulador-mikael-debug-apk`.
4. O APK de depuração estará dentro do ZIP do artefato.

O workflow compila a base Android; isso não significa que o motor Windows esteja implementado.

## Desenvolvimento

- Android nativo em Java
- Interface inicial verde/preta
- Seleção de arquivos pelo seletor seguro do Android
- Compilação de APK de depuração via GitHub Actions

## Próximas etapas

1. Compilar e testar a interface em um dispositivo Android.
2. Definir uma integração compatível de Wine e camada x86_64 para Android/ARM64.
3. Implementar instalação isolada do ambiente Windows e gestão de dependências.
4. Testar o LAVOICE e documentar resultados reais de compatibilidade e desempenho.

## Segurança

Use apenas executáveis de fontes confiáveis. A seleção de um arquivo não o executa nem verifica se ele é seguro.
