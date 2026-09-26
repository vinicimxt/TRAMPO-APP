# CI Android — Sprint 7

Workflow: .github/workflows/android.yml, pronto para a raiz de um repositório
GitHub deste projeto. Em 21/09/2026 o diretório ainda não tinha .git nem remoto.
Não houve publicação ou execução hospedada nesta sessão.

Ambiente: JDK 25, Gradle Wrapper 9.6.0, AGP 9.4.1, SDK 37 e Build Tools 36.0.0.
O daemon já exige Java 25 em gradle/gradle-daemon-jvm.properties.
Não é necessário Gradle global nem emulador.

Comando Linux/CI: bash ./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon --console=plain.
No Windows: configurar JAVA_HOME para o JBR do Android Studio e executar
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --console=plain.

Resultados locais: 23 testes aprovados, zero falhas; LiveApiTest ignorado na
execução sem fixtures. O runner Tests/Sprint6/Run.ps1 do backend executou esse
teste separadamente contra a API e SQL reais, com sucesso.
Lint: zero erros, 10 avisos preexistentes. APK em app/build/outputs/apk/debug.
Relatórios em app/build/reports e app/build/test-results; estes últimos são
substituídos a cada execução filtrada, por exemplo pela integração LiveApiTest.

Debug mantém http://10.0.2.2:5165/. Release exige -PtrampoApiUrl com a URL HTTPS
real terminada em /. Nenhuma URL de produção padrão foi definida. Sem URL ou com
HTTP, o build Release falha. A política de rede principal proíbe cleartext.
APK Debug não serve como artefato de distribuição da versão Release.

Versionar fontes, arquivos Gradle e todo o Wrapper, incluindo o JAR e checksum.
Não versionar local.properties, build/, .gradle/, .kotlin/, .idea/, logs,
keystore.properties, secrets.properties ou chaves de assinatura.
O workflow não exige secrets reais e não executa testes instrumentados.

Relatório completo no backend TRAMPO: docs/SPRINT-7-DEVOPS.md,
docs/SPRINT-7-BASELINE.md e docs/SPRINT-7-VALIDACAO.md.

