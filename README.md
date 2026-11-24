# TRAMPO Android — Cliente e Profissional

Aplicativo Android em Kotlin para **Clientes e Profissionais** da plataforma TRAMPO.

O Cliente pode buscar serviços, consultar disponibilidade, realizar e acompanhar agendamentos, confirmar a conclusão de atendimentos, avaliar profissionais, editar seu perfil, receber notificações e utilizar o suporte.

O Profissional possui uma área própria para gerenciamento de serviços, locais, disponibilidade semanal e agendamentos recebidos.

Este aplicativo integra o ecossistema TRAMPO:

- **Web/API:** https://github.com/vinicimxt/Trampo
- **Desktop:** https://github.com/vinicimxt/Trampo-Desktop

O aplicativo Android consome exclusivamente a **API REST do TRAMPO** e não possui acesso direto ao SQL Server.

---

## Perfis suportados

| Perfil | Android |
| --- | --- |
| Cliente | Sim |
| Profissional | Sim |
| Administrador | Não — utiliza a aplicação Web |

Após o login, o aplicativo consulta `/api/v1/auth/me` para identificar o perfil autenticado e direcionar o usuário para sua respectiva Home.

A autorização real das operações permanece sob responsabilidade do backend.

---

## Funcionalidades

| Área | Cliente | Profissional |
| --- | --- | --- |
| Conta | Cadastro, login, edição de perfil, alteração de senha e logout | Login, logout, edição de nome/telefone e alteração de senha pela API |
| Serviços | Explorar catálogo, buscar, filtrar e visualizar detalhes | Listar, cadastrar, editar e remover/desativar serviços |
| Categorias | Consultar categorias e subcategorias | Utilizadas no cadastro de serviços |
| Disponibilidade | Consultar horários disponíveis | Configurar disponibilidade semanal por serviço |
| Locais | Consultar informações do atendimento quando aplicável | Listar, cadastrar, editar e excluir locais |
| CEP | Utilizado por meio do backend | Consulta e preenchimento de endereço por meio do backend |
| Agendamentos | Solicitar, consultar, cancelar e confirmar conclusão | Consultar recebidos, confirmar, recusar, cancelar e finalizar |
| Avaliações | Enviar avaliação após atendimento permitido | Visualização de avaliações recebidas pela API |
| Notificações | Disponível | Tela integrada à API para o usuário autenticado |
| Suporte | Disponível | Tela integrada à API para o usuário autenticado |

As telas de avaliações, notificações, suporte e perfil usam os contratos REST liberados na estabilização final. Os testes de API e o build Android foram executados; a validação visual no emulador depende de aceleração de hardware neste ambiente.

---

## Limitações conhecidas

Os contratos abaixo foram liberados no backend. A validação visual no emulador permanece pendente neste ambiente.

### Avaliações

As rotas:

```text
GET /api/v1/servicos/{id}/avaliacoes
GET /api/v1/profissionais/me/avaliacoes
```

retornam, respectivamente, avaliações do serviço ativo e avaliações recebidas pelo profissional autenticado, com média, total, nota, comentário e data. O nome do serviço também é retornado quando disponível.

### Notificações e suporte do Profissional

As rotas de notificações e suporte aceitam Cliente e Profissional autenticados. As listas e a leitura de notificações usam a identidade do JWT; suporte permite envio e consulta dos próprios chamados.

### Perfil Profissional

O contrato existente de perfil aceita Cliente e Profissional para consulta e edição de nome/telefone. A alteração de senha reutiliza a regra existente, que exige a senha atual.

---

## Arquitetura

```text
Android
Activities / Views XML
        ↓
    ViewModels
        ↓
   Repositories
        ↓
 Retrofit / OkHttp
        ↓
 HTTP + JWT Bearer
        ↓
 API REST TRAMPO
        ↓
Application Services
        ↓
   DAO / ADO.NET
        ↓
    SQL Server
```

O Android nunca acessa diretamente o banco de dados.

As regras de negócio e autorização permanecem centralizadas no backend.

---

## Autenticação

O fluxo de autenticação utiliza:

```text
POST /api/v1/auth/login
```

seguido de:

```text
GET /api/v1/auth/me
```

O segundo endpoint identifica o perfil autenticado.

A partir disso:

```text
Cliente
   ↓
Home Cliente

Profissional
   ↓
Home Profissional
```

Contas Administrativas continuam utilizando exclusivamente a aplicação Web.

---

## Sessão

O aplicativo mantém localmente apenas os dados necessários para a sessão.

O JWT é enviado nas chamadas autenticadas utilizando:

```text
Authorization: Bearer <token>
```

O aplicativo:

- não armazena a senha do usuário;
- utiliza preferências privadas para dados mínimos da sessão;
- limpa a sessão no logout;
- redireciona para o login quando uma chamada autenticada retorna `401`;
- possui backup dos dados locais desabilitado.

A API continua sendo responsável pela autorização e ownership dos recursos.

---

## Pré-requisitos

Para executar o projeto:

- Android Studio;
- Android SDK Platform 37;
- Android Build Tools 36.0.0;
- JDK 25 para o Gradle;
- Android Emulator com API 24 ou superior;
- Backend TRAMPO em execução;
- SQL Server configurado pelo projeto Web/API;
- conta Cliente ou Profissional.

O projeto inclui o **Gradle Wrapper**, portanto não é necessário instalar Gradle separadamente.

O JDK utilizado pelo projeto também pode ser o JBR distribuído com o Android Studio, desde que configurado corretamente no ambiente.

---

## Executar em desenvolvimento

### 1. Inicie o backend

Prepare o projeto:

https://github.com/vinicimxt/Trampo

Na configuração atual de desenvolvimento, a API atende em:

```text
http://localhost:5165/
```

Valide:

```text
GET /health
```

### 2. Abra o projeto Android

Abra a pasta do projeto no Android Studio.

Aguarde:

- sincronização do Gradle;
- download dos componentes;
- indexação do projeto.

### 3. Inicie um emulador

Utilize um dispositivo virtual compatível.

Para usar o Debug padrão no emulador, execute `adb -e reverse tcp:5165 tcp:5165`. Para acessar o host diretamente pelo endereço do emulador, compile com a propriedade:

```text
-PtrampoDebugApiUrl=http://10.0.2.2:5165/
```

No Android Emulator, `10.0.2.2` representa o `localhost` da máquina host.

### 4. Execute

Selecione:

```text
app
```

e execute em modo:

```text
Debug
```

Depois, entre com uma conta Cliente ou Profissional.

---

## Dispositivo físico — modo recomendado

O Debug padrão usa `http://127.0.0.1:5165/`. Os scripts configuram o túnel USB e instalam o APK Debug no celular.

1. Ative a Depuração USB, conecte o celular e autorize o computador.
2. Inicie o backend em `http://localhost:5165/`.
3. Na raiz deste repositório, execute:

Linux/macOS:

```bash
./scripts/run-device-debug.sh
```

Windows PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-device-debug.ps1
```

4. Abra o TRAMPO no celular.

O túnel leva `127.0.0.1:5165` do celular à porta 5165 do computador via ADB. O script verifica `adb reverse --list` antes da instalação. Se `adb devices` listar USB e Wi-Fi ao mesmo tempo, selecione o serial USB: `./scripts/run-device-debug.sh SERIAL` ou `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-device-debug.ps1 SERIAL`. Nesse caso, o APK é instalado somente no serial escolhido. Se o Linux não permitir executar o script diretamente, use `bash scripts/run-device-debug.sh`.

O Debug permite HTTP sem criptografia somente para `10.0.2.2`, `localhost` e `127.0.0.1`. Release continua bloqueando HTTP e exigindo URL HTTPS.

### Override manual da URL

No Debug, use `-PtrampoDebugApiUrl=...` para substituir o padrão `http://127.0.0.1:5165/`. O script do celular passa `-PtrampoDebugApiUrl=http://127.0.0.1:5165/`. Use propriedades manuais para testes LAN, outros computadores, servidores remotos ou CI; a URL deve terminar em `/`. Uma URL HTTP para outro host também exige adicioná-lo explicitamente a `app/src/debug/res/xml/network_security_config.xml`. Em Release, `-PtrampoApiUrl=...` deve continuar sendo HTTPS.

---

## Ambiente online

O [APK online de previa](https://github.com/vinicimxt/TRAMPO-APP/releases/tag/v2026.11.23-online-preview) esta disponivel para download na release do projeto. Ele e um build Debug assinado com chave de desenvolvimento; leia as ressalvas antes de instalar.

A API publicada esta em `https://trampo.tryasp.net/` (com barra final). Para gerar um Debug online, na raiz do repositorio:

Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug -PtrampoApiUrl=https://trampo.tryasp.net/
```

Linux/macOS:

```bash
./gradlew assembleDebug -PtrampoApiUrl=https://trampo.tryasp.net/
```

Para instalar em um dispositivo conectado, substitua `assembleDebug` por `installDebug`. O modo online nao precisa de `adb reverse` nem do IP do computador. O Debug sem propriedades continua usando `http://127.0.0.1:5165/` com ADB reverse; para o Emulator sem reverse, use `-PtrampoDebugApiUrl=http://10.0.2.2:5165/`. A propriedade `trampoDebugApiUrl` tem prioridade sobre `trampoApiUrl` no Debug. Release usa `trampoApiUrl` e aceita somente HTTPS.

---

## Compilar e testar

No PowerShell, com o JDK 25 disponível no ambiente:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Esse comando executa:

- testes unitários;
- Android Lint;
- compilação do APK Debug.

O APK é gerado em:

```text
app/build/outputs/apk/debug/
```

### Resultado da homologação mais recente

Na homologação local de 07/11/2026:

```text
Testes encontrados: 36
Aprovados: 35
Ignorados: 1
Falhas: 0

Lint:
0 erros
21 avisos

assembleDebug:
APROVADO
```

Os testes unitários utilizam fixtures e respostas simuladas.

Testes de integração real dependem do backend em execução.

Testes instrumentados exigem emulador ou dispositivo.

---

## Observação sobre homologação visual

Na homologação automatizada mais recente, o emulador disponível no ambiente não iniciou por falta de aceleração de hardware.

Por isso, apesar do build, testes unitários e lint terem passado, a execução completa das telas Android ainda requer validação manual em um emulador ou dispositivo funcional.

Isso inclui especialmente:

- Cliente;
- Profissional;
- TalkBack;
- fontes ampliadas;
- navegação por foco;
- estados vazios;
- erros de API;
- rotação;
- fluxo visual de agendamento;
- exibição visual do nome do serviço.

---

## Acessibilidade

Os layouts contêm rótulos visíveis, usos de `contentDescription` e associações `labelFor`. O Android Lint terminou sem erros na homologação de 07/11/2026.

A experiência com TalkBack, fonte ampliada, ordem de foco, áreas de toque e contraste ainda precisa ser verificada em dispositivo ou emulador funcional. A presença desses atributos no XML não comprova a acessibilidade de todas as telas.

---

## Configuracao de Release e assinatura

A API publicada e `https://trampo.tryasp.net/`. Release exige HTTPS e bloqueia trafego HTTP em `app/src/main/res/xml/network_security_config.xml`.

Para produzir um APK Release **assinado com sua propria chave**, crie uma keystore em local privado. O comando abaixo pede a senha interativamente; nao informe senha na linha de comando:

```powershell
keytool -genkeypair -v -keystore release.jks -alias trampo -keyalg RSA -keysize 3072 -validity 10000
```

Copie `keystore.properties.example` para `keystore.properties` na raiz e preencha `storeFile`, `storePassword`, `keyAlias` e `keyPassword` localmente. Esses arquivos, assim como `*.jks`, sao ignorados pelo Git. Guarde a chave e suas senhas com seguranca; futuras atualizacoes do app precisam da mesma chave. Depois execute:

```powershell
.\gradlew.bat assembleRelease -PtrampoApiUrl=https://trampo.tryasp.net/
```

Sem `keystore.properties`, o Gradle pode gerar um Release **nao assinado**, que nao deve ser entregue para instalacao. Um Debug gerado por `assembleDebug` e assinado pela chave Debug local e pode ser instalado diretamente, mas fica marcado como Debug e nao substitui o Release assinado para distribuicao definitiva.

---

## Estrutura do projeto

```text
app/src/main/java/com/example/trampo/
├── data/
│   ├── api/          Contratos REST, Retrofit e autenticação HTTP
│   ├── model/        Modelos de dados
│   ├── repository/   Acesso à API
│   └── SessionManager.kt
├── ui/
│   ├── login/
│   ├── home/
│   ├── servicos/
│   ├── agendamentos/
│   ├── conta/
│   └── profissional/
└── MainActivity.kt

app/src/main/res/         Layouts, textos e demais recursos
app/src/test/             Testes unitários
app/src/androidTest/      Testes instrumentados
.github/workflows/        Pipeline Android
```

Activities, ViewModels e Repositories organizam as chamadas à API e o estado das telas.

---

## CI

O workflow:

```text
.github/workflows/android.yml
```

é responsável pela automação do projeto Android.

Ele contempla:

- testes unitários;
- lint;
- build Debug;
- geração de artefatos.

A situação do workflow remoto deve ser verificada diretamente no GitHub antes de afirmar que a execução mais recente está verde.

---

## Segurança

O aplicativo segue estas regras:

- senha não armazenada;
- JWT utilizado para autenticação REST;
- autorização validada pelo backend;
- nenhum acesso direto ao SQL Server;
- secrets não devem ser versionados;
- `local.properties` permanece fora do Git;
- keystores e chaves de assinatura permanecem fora do repositório;
- dados de sessão são limpos no logout.

Arquivos como os seguintes não devem ser versionados:

```text
local.properties
*.jks
*.keystore
key.properties
signing.properties
*.apk
*.aab
```

---

## Backend e documentação técnica

A aplicação Android depende do projeto principal:

https://github.com/vinicimxt/Trampo

Consulte nesse repositório:

```text
docs/API-FINAL.md
docs/ARQUITETURA-FINAL.md
docs/BANCO-DE-DADOS.md
docs/MATRIZ-WEB-API-MOBILE.md
docs/SEGURANCA-FINAL.md
docs/TESTES-FINAIS.md
```

A matriz Web/API/Mobile documenta quais funcionalidades estão disponíveis em cada interface e quais dependem de endpoints adicionais.

---

## Outros clientes TRAMPO

O ecossistema atual possui três interfaces principais:

```text
Web
Cliente + Profissional + Administrador

Android
Cliente + Profissional

Desktop
Cliente + Profissional
```

As três aplicações compartilham regras e dados por meio do backend/API.

Android e Desktop nunca acessam diretamente o SQL Server.

---

## Estado atual

O Android possui suporte estrutural para Cliente e Profissional.

As áreas Cliente e Profissional têm contratos REST para agendamentos, serviços, locais, disponibilidade, perfil, avaliações, notificações e suporte conforme cada perfil. O build, os testes unitários e os contratos da API foram validados. A navegação visual Android ainda não foi homologada neste ambiente por ausência de aceleração do emulador.
