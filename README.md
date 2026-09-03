# Expedicion Android

App Android nativa (Kotlin + Jetpack Compose) que reemplaza el cliente Delphi/FireMonkey,
hablando contra el API Node.js en `../api`. Ver el plan de migracion completo para el
contexto funcional (reglas de negocio, mensajes, casos especiales).

## Requisitos

- JDK 17 o superior (probado con JDK 21). AGP 8.6 no corre con JDK 8/11.
- Android SDK con `compileSdk 35` / `build-tools 35.0.0` instalados.
- `local.properties` con `sdk.dir` apuntando al SDK (no se versiona; crear uno propio).

## Abrir el proyecto

Abrir la carpeta `android/` (no la raiz del repo) en Android Studio, o compilar por linea de
comandos:

```bash
cd android
./gradlew assembleDebug
```

Si `JAVA_HOME` no apunta a un JDK 17+, exportarlo antes de invocar Gradle:

```bash
export JAVA_HOME=/ruta/a/jdk-17-o-superior
```

## Configurar la URL del API

La URL base se inyecta via `BuildConfig.API_BASE_URL`, default
`http://10.0.2.2:3000/` (alias del host desde el emulador Android, asumiendo el API
corriendo en `localhost:3000`). Para apuntar a otra URL sin tocar codigo:

```bash
./gradlew assembleDebug -PAPI_BASE_URL=http://192.168.10.50:3000/
```

o agregar `API_BASE_URL=http://...` a un `gradle.properties` local (no versionado).

Ese valor es sólo el default que se precarga en la pantalla de configuracion: la URL efectiva es
la que el usuario guarda en runtime (`ApiConfigStore`, DataStore).

### Verificacion de la URL al guardarla

`ApiConfigScreen` no persiste una URL sin antes sondearla (`ApiProbe`): consulta
`GET /health/ready` y `GET /version` contra la direccion tipeada y **bloquea el guardado** si el
API no responde, si responde 503 (alguna base caida, se nombra cual) o si contesta algo que no es
el API de Expedicion. Recien con `/health/ready` en 200 guarda la URL junto con la version que
reporto `/version`, y muestra un aviso con esa version antes de reiniciar el proceso.

`ApiProbe` usa su propio `OkHttpClient` y NO el `ApiService` inyectado: el `Retrofit` singleton
fija su `baseUrl` al construirse el grafo de Hilt, mientras que aca hay que consultar una URL que
todavia no se persistio.

La version del API queda guardada y se muestra en el pie de Configuracion y de Login junto a la
version de la app (`versionesTexto` en `ui/common/Versiones.kt`). Es la version verificada en la
ultima configuracion: si el servidor se actualiza sin reconfigurar la app, queda desactualizada.
El API devuelve `"dev"` cuando corre sin `APP_VERSION` inyectada (`npm run dev`), y ese caso se
muestra como "API dev", sin prefijo `v`.

## Correr los tests

```bash
./gradlew testDebugUnitTest
```

Cubre los ViewModel (JUnit + MockK + kotlinx-coroutines-test) con fakes/mocks del repository:
contador ciclico 1-8 y reset, escaneo duplicado (sin dialogo, limpia el campo), error 503
(no resetea remito/etiqueta/contador, permite reintentar), confirmar despacho con diferencias
(no cierra) vs sin diferencias (cierra), confirmar devolucion (siempre cierra sin validar) y
login con ambos campos vacios (no llama al repository).

`ApiProbeTest` levanta un `MockWebServer` real (no mockea OkHttp) y cubre los cuatro desenlaces
del sondeo: `/health/ready` 200 con version, 503 identificando la base caida, un servidor que
responde otra cosa, y puerto cerrado.

## Estructura

```
app/src/main/java/com/expedicion/app/
  data/remote        ApiService (Retrofit) + DTOs (kotlinx.serialization) + AuthInterceptor
  data/repository    AuthRepository, RemitoRepository, EscaneoRepository, EstadoRepository
  di                 Modulos Hilt (NetworkModule)
  session            SessionManager (usuario/password solo en memoria de proceso)
  sound              SoundFeedbackPlayer (feedback sonoro de exito/error)
  ui/login|menu|despacho|devolucion|estado   Pantallas 1:1 con los forms Delphi
  ui/buscarremito    BuscarRemitoBottomSheet, reusado por Despacho y Devolucion
  ui/common          Dialogos (error / confirmacion irreversible)
  ui/navigation      NavHost Compose
```

## Notas de diseno

- **Credenciales**: viven solo en `SessionManager` (memoria de proceso), nunca en disco. Se
  revalidan en el servidor en cada request (no hay JWT ni sesion cacheada localmente).
- **Auth por request**: `AuthInterceptor` (OkHttp) agrega `Authorization: Basic` a cada
  request; ademas los endpoints con body (escaneo, eliminar, confirmar, borrar transaccion,
  login) mandan las credenciales tambien en el JSON, porque el API acepta ambos modos y le da
  prioridad al body si esta presente (`resolveCredentials` en el API) — mandar los dos evita
  depender de cual gana, ya que siempre son las mismas credenciales de la sesion activa.
- **Mensajes de error**: se muestran tal cual `error.message` del API, sin reprocesar texto.
  Unico texto que no viene del backend: fallas de transporte puras (sin respuesta del
  servidor — sin red, timeout, DNS), que muestran un texto generico client-side
  (`NETWORK_ERROR_MESSAGE` en `data/ApiResult.kt`), ya que ahi no hay mensaje de negocio que
  mostrar.
- **Error de negocio (4xx) vs falla de servidor (5xx)** en escaneo: se distinguen por
  `httpStatus` (ver `ApiResult.Error.isServerFault()`), replicando el comportamiento del
  Delphi original (`UnitFunciones.pas`): los errores de negocio (etiqueta invalida, cupo
  completo, etc.) limpian el campo etiqueta y devuelven el foco; una falla de infraestructura
  (503 `DB_UNAVAILABLE`, 500 `SCAN_ERROR`) solo muestra el dialogo y preserva remito, items,
  contador y el contenido del campo para poder reintentar sin perder contexto.
- **Feedback sonoro**: `SoundFeedbackPlayer` usa `android.media.ToneGenerator` (tonos DTMF
  generados en tiempo real) en vez de `SoundPool` + archivos `.wav` embebidos, porque el repo
  no trae assets de audio y generar PCM a mano para `SoundPool` no agrega valor funcional.
  Decision explicitamente habilitada por el plan de migracion para este caso.
- **Catalogo de TIPO** en la pantalla Estado: fijo a `COCINA` / `TERMOTANQUE` (unicos valores
  observados en el sistema legado), ya que el plan deja pendiente la confirmacion formal del
  catalogo cerrado por parte del usuario (bloqueante #3).
- **Confirmar devolucion**: cierra la pantalla siempre, incluso si la llamada al API falla,
  replicando fielmente `UnitDevolucion.pas` (el bloque de validacion esta comentado en el
  original y ni siquiera revisa el resultado).

## Limitaciones conocidas

- No se pudo verificar en este entorno la ejecucion contra un emulador/dispositivo real
  (no hay AVD levantado ni dispositivo conectado) ni el flujo end-to-end con lector fisico de
  codigo de barras: se valido compilacion (`assembleDebug`) y tests unitarios de ViewModel
  (`testDebugUnitTest`), ambos con SDK/build-tools/JDK reales instalados en la maquina.
- No hay tests de UI (Compose) ni instrumentados; el alcance de testing automatizado de este
  ciclo es a nivel ViewModel, como pide la especificacion.
