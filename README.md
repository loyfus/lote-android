# Loyfus — Android

App Android do Loyfus: **Kotlin + Jetpack Compose**, Material 3,MVVM com `StateFlow`.
Consome a API do repositório `loyfus-backend` (Spring Boot + PostgreSQL).

## Rodar

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export ANDROID_HOME=~/Library/Android/sdk

# aponta o loopback do dispositivo para a API da maquina
~/Library/Android/sdk/platform-tools/adb reverse tcp:8080 tcp:8080

./gradlew :app:installDebug
```

A API precisa estar no ar: suba o `loyfus-backend` (`docker compose up -d` + `./gradlew :api:bootRun`).

## Apontar para outro host

Para dispositivo fisico em vez de emulador, passe a base na compilação:

```bash
./gradlew :app:assembleDebug -PloyfusApiBaseUrl=http://192.168.0.10:8080
```

## Testes

```bash
./gradlew :app:testDebugUnitTest
```

Cobrem formatação de preço em pt-BR, contagem regressiva e a forma do JSON que a API devolve.
As mesmas regras existem espelhadas em `loyfus-ios` de proposito: uma divergencia entre os
dois clientes vira teste vermelho em vez de surpresa em producao.

## Deep link

`loyfus://lot/<id>` abre o lote, `loyfus://explore` abre a busca. O manifesto declara o scheme.

## Documentacao

Fica no repositorio `loyfus-backend`, em `docs/`:

- `DESIGN_SYSTEM.md` — tokens, componentes, telas e o contrato de dados dos dois clientes
- `ARCHITECTURE.md` — como o app consome a API

## Estrutura

```
app/src/main/java/br/com/loyfus/
├── data/      cliente HTTP, repositório, stores (favoritos, buscas recentes, banner, settings)
├── core/      formatação de preço, titulo de lote, buscas recentes — espelhado no iOS
└── ui/        composables, telas (home, explorar, lote, salvos, ajustes) e o tema
```
