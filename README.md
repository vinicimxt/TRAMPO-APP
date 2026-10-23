# TRAMPO Android — Área do Cliente

Aplicativo Android em Kotlin para clientes da plataforma TRAMPO. Pelo celular, o cliente pode encontrar serviços, solicitar atendimentos e acompanhar seus agendamentos.

Este aplicativo é uma extensão do [TRAMPO Web/API](https://github.com/vinicimxt/Trampo), onde ficam a aplicação web, a API e as regras de negócio. O [TRAMPO Desktop](https://github.com/vinicimxt/Trampo-Desktop) é outra interface do ecossistema, voltada ao profissional. O Android usa a API REST; não acessa diretamente o SQL Server.

## Funcionalidades

| Área | O que o cliente pode fazer |
| --- | --- |
| Conta | Cadastrar-se, entrar, consultar e editar o perfil, alterar a senha e sair. |
| Serviços | Explorar o catálogo, buscar, filtrar por modalidade e consultar detalhes, profissional e disponibilidade. |
| Agendamentos | Solicitar, consultar e cancelar agendamentos; confirmar a conclusão e avaliar o atendimento quando aplicável. |
| Comunicação | Consultar notificações, marcá-las como lidas e enviar uma solicitação ao suporte. |

Esta versão aceita somente contas com perfil **Cliente**. Profissionais e administradores usam as interfaces Web ou Desktop conforme seu papel.

## Como funciona

```text
Telas Android (Activities e Views XML)
             ↓
          ViewModels
             ↓
         Repositories
             ↓
       Retrofit / OkHttp
             ↓ HTTP + JWT Bearer
       API REST do TRAMPO
             ↓
     Backend e SQL Server
```

O app valida a conta após o login com `/api/v1/auth/me`. O JWT é guardado nas preferências privadas do aplicativo e enviado como Bearer Token nas chamadas autenticadas. A API controla autorização, dados do usuário e regras de negócio. A configuração do app desabilita backup dos dados locais.

## Pré-requisitos

- Android Studio com Android SDK 37 e Build Tools 36.0.0;
- JDK 25 para o Gradle (o projeto configura o daemon em `gradle/gradle-daemon-jvm.properties`);
- Android Emulator com Android 7.0/API 24 ou superior;
- [backend TRAMPO](https://github.com/vinicimxt/Trampo) em execução e configurado com seu banco;
- uma conta **Cliente**, que também pode ser criada no aplicativo.

O projeto inclui o Gradle Wrapper; não é necessário instalar Gradle separadamente.

## Executar em desenvolvimento

1. Prepare e inicie o backend conforme o [README do TRAMPO Web/API](https://github.com/vinicimxt/Trampo). Para a configuração Debug atual, ele deve atender em `http://localhost:5165/` na máquina de desenvolvimento.
2. Abra esta pasta no Android Studio e aguarde a sincronização do Gradle e a instalação dos componentes do SDK solicitados.
3. Inicie um Android Emulator compatível e execute a configuração `app` em **Debug**.
4. Cadastre uma conta Cliente ou entre com uma conta existente.

O Debug aponta para `http://10.0.2.2:5165/`, endereço pelo qual o **Android Emulator** alcança o `localhost` do computador. A exceção de HTTP sem criptografia vale apenas para `10.0.2.2` no Debug. Para usar um dispositivo físico, é preciso ajustar o endereço Debug em `app/build.gradle.kts` para uma API acessível pelo dispositivo e configurar a política de rede correspondente.

## Compilar e testar

No terminal do Android Studio ou em PowerShell com o JDK e o SDK configurados:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

O APK Debug gerado fica em `app/build/outputs/apk/debug/`. Os testes unitários usam fixtures e respostas simuladas; os testes de integração com API real exigem configuração própria. Testes instrumentados precisam de emulador ou dispositivo e não fazem parte do comando acima.

O workflow `.github/workflows/android.yml` executa testes unitários, lint e build Debug no GitHub Actions, além de armazenar relatórios e o APK Debug como artefatos.

## Configuração de Release

O build Release exige uma URL real da API em HTTPS, terminada em `/`. Passe-a como propriedade Gradle, por exemplo:

```powershell
.\gradlew.bat assembleRelease -PtrampoApiUrl=https://api.seu-dominio.com/
```

Substitua o domínio do exemplo pela API implantada. O projeto não define uma URL de produção padrão. O Manifest principal bloqueia tráfego HTTP sem criptografia. Para distribuir o aplicativo, configure também a assinatura e os demais requisitos de publicação Android; o APK Debug não é uma versão de distribuição.

## Estrutura do projeto

```text
app/src/main/java/com/example/trampo/
  data/api/          Contratos REST, Retrofit e autenticação HTTP
  data/repository/   Acesso aos dados da API
  data/              Modelos e sessão
  ui/                Telas e ViewModels
app/src/main/res/    Layouts, textos e identidade visual
app/src/test/        Testes unitários
app/src/androidTest/ Testes instrumentados
.github/workflows/   Pipeline Android
```

Arquivos locais como `local.properties`, credenciais, chaves de assinatura e pacotes gerados devem permanecer fora do Git. Para arquitetura, API, banco e configuração do backend, consulte o [repositório principal TRAMPO](https://github.com/vinicimxt/Trampo).
