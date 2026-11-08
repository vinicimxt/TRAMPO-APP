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
| Conta | Cadastro, login, edição de perfil, alteração de senha e logout | Login e logout; edição de perfil/senha ainda depende de rotas REST não liberadas ao perfil Profissional |
| Serviços | Explorar catálogo, buscar, filtrar e visualizar detalhes | Listar, cadastrar, editar e remover/desativar serviços |
| Categorias | Consultar categorias e subcategorias | Utilizadas no cadastro de serviços |
| Disponibilidade | Consultar horários disponíveis | Configurar disponibilidade semanal por serviço |
| Locais | Consultar informações do atendimento quando aplicável | Listar, cadastrar, editar e excluir locais |
| CEP | Utilizado por meio do backend | Consulta e preenchimento de endereço por meio do backend |
| Agendamentos | Solicitar, consultar, cancelar e confirmar conclusão | Consultar recebidos, confirmar, recusar, cancelar e finalizar |
| Avaliações | Enviar avaliação após atendimento permitido | Visualização de avaliações recebidas ainda depende de contrato REST ausente |
| Notificações | Disponível | Tela existente, porém a rota atual da API restringe o acesso ao perfil Cliente |
| Suporte | Disponível | Tela existente, porém a rota atual da API restringe o acesso ao perfil Cliente |

Algumas telas profissionais e de avaliações já existem no aplicativo, mas dependem de endpoints que ainda não estão disponíveis ou que atualmente possuem restrição de autorização no backend.

A existência de uma tela no aplicativo não significa que sua integração REST esteja concluída.

---

## Limitações conhecidas

Na versão atual existem algumas limitações conhecidas de integração:

### Avaliações

As rotas:

```text
GET /api/v1/servicos/{id}/avaliacoes
GET /api/v1/profissionais/me/avaliacoes
```

ainda não estão implementadas no backend atual.

Por isso:

- o Cliente pode enviar uma avaliação quando permitido;
- média e quantidade de avaliações podem aparecer quando fornecidas pelo serviço;
- comentários individuais ainda não possuem endpoint público;
- a tela de avaliações recebidas do Profissional ainda não possui integração completa.

### Notificações e suporte do Profissional

As rotas atuais de notificações e suporte estão autorizadas apenas para Cliente.

Quando utilizadas com uma conta Profissional, a API retorna `403 Forbidden`.

As telas profissionais foram mantidas para evolução futura, mas sua integração não deve ser considerada concluída.

### Perfil Profissional

A edição de perfil e alteração de senha pelo aplicativo dependem de contratos REST que ainda não estão liberados ao perfil Profissional.

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

O Debug do aplicativo utiliza:

```text
http://10.0.2.2:5165/
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

## Dispositivo físico

A URL:

```text
http://10.0.2.2:5165/
```

funciona apenas no Android Emulator.

Para utilizar dispositivo físico, configure uma URL que possa ser acessada pela rede do aparelho.

Também será necessário revisar a política de tráfego HTTP caso o ambiente não utilize HTTPS.

Para builds de produção, utilize sempre HTTPS.

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

## Configuração de Release

O build Release exige uma URL HTTPS válida para a API.

Exemplo:

```powershell
.\gradlew.bat assembleRelease -PtrampoApiUrl=https://api.seu-dominio.com/
```

Substitua o domínio por uma API realmente implantada.

O projeto atualmente não possui uma URL pública de produção definida.

O Manifest principal bloqueia tráfego HTTP não criptografado para produção.

Para distribuição real também será necessário configurar:

- assinatura;
- keystore;
- versão;
- política de publicação;
- requisitos da Google Play.

O APK Debug não deve ser tratado como build de distribuição.

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

A área Cliente possui maior cobertura dos contratos REST atuais.

A área Profissional já possui telas e integração para:

- agendamentos;
- serviços;
- locais;
- disponibilidade.

Ainda existem lacunas REST para:

- perfil profissional;
- avaliações recebidas;
- notificações profissionais;
- suporte profissional.

Essas limitações estão documentadas e devem ser consideradas antes de classificar o aplicativo como totalmente homologado para ambos os perfis.
