# Vozinha

Aplicación móvil Android de accesibilidad para personas con discapacidad
sensorial auditiva. Permite escribir un mensaje y reproducirlo en voz alta,
para que la persona pueda comunicarse en situaciones cotidianas sin depender
de un intérprete.

Toda la retroalimentación de la aplicación es visual y táctil: cada acción se
confirma con un aviso en pantalla y con vibración, nunca con sonido.

## Tecnologías

- Kotlin
- Jetpack Compose con Material Design 3
- Navigation Compose
- Extensiones KTX de AndroidX, Play Services y Firebase
- Firebase Authentication y Cloud Firestore
- TextToSpeech, Vibrator y ubicación del sistema Android
- JUnit, Mockito, Robolectric y Espresso
- compileSdk 36, minSdk 24, versión 3.0

## Views

| View | Descripción |
|---|---|
| Login | Acceso validado contra el servicio de autenticación. |
| Registro de usuario | Crea la cuenta y su ficha en la base de datos. |
| Recuperar contraseña | Verifica el correo y pide el envío de las instrucciones. |
| HomeMenú | Punto de entrada a las tres funciones, con el perfil de la persona. |
| Escribir | Redacta mensajes y los guarda para tenerlos listos. |
| Hablar | Reproduce en voz alta los mensajes guardados, con los favoritos arriba. |
| Buscar dispositivo | Lee la ubicación del teléfono y guarda el historial. |

## Componentes de interfaz

| Componente | Dónde aparece |
|---|---|
| Input | Login, Registro, Recuperar, Comunicación |
| Botones | Las cuatro views |
| Vínculos | Login y Registro |
| Textos | Las cuatro views |
| Combo box | Registro, tipo de usuario |
| Radio buttons | Registro y Recuperar, en grilla |
| Check list | Login y Registro, casillas de recordar correo y aceptar términos |
| Grilla | Login, Registro, Recuperar, Comunicación |
| Tabla | Las cuatro views |

## Arreglo de usuarios

`UsuariosRepository` mantiene un `Array<Usuario>` con cinco usuarios y sus
contraseñas. La view de Registro agrega usuarios nuevos, que pueden iniciar
sesión de inmediato. Los datos viven en memoria: al cerrar la aplicación el
arreglo vuelve a su estado inicial.

## Cuentas de prueba

La view de Login lista las cinco cuentas iniciales con su contraseña, así que
se puede entrar con cualquiera de ellas sin buscarlas en el código. La primera
es `camila@gmail.com`. Las cinco usan la misma contraseña: `hola1234`.

## Frases rápidas

La view de Comunicación parte con seis frases de uso frecuente. Cada una se
puede eliminar con el botón de su tarjeta, que pide confirmación en un diálogo
antes de borrar, y se pueden agregar frases propias desde el formulario del
final. La aplicación rechaza las frases vacías y las repetidas. Al igual que los usuarios, las frases viven en memoria y vuelven a
su estado inicial al cerrar la aplicación.

## Cómo se reproduce la voz

La reproducción la hace el motor de texto a voz del sistema. La aplicación
comprueba que el motor se haya inicializado y que la voz en español esté
instalada, y muestra ambos estados en pantalla: si algo falla, la persona
usuaria no puede oírlo, así que tiene que verlo.

Cada llamada al motor y al vibrador va dentro de `intentar`, que captura la
excepción y la deja escrita en la tarjeta de estado en lugar de dejar caer la
aplicación. Un teléfono sin motor de voz instalado, o que niegue el permiso de
vibración, no rompe el resto de las funciones.

## Sintaxis de Kotlin aplicada

Estos son los elementos del lenguaje que usa el proyecto y dónde encontrarlos.

| Elemento de Kotlin | Dónde se usa |
|---|---|
| `Array` | `UsuariosRepository`, arreglo de los cinco usuarios con sus contraseñas |
| `data class` | `Usuario` |
| `enum class` | `TipoUsuario`, `MedioComunicacion` y `ExigenciaCorreo` |
| `sealed interface` | `ResultadoRegistro`, con `data class` y `data object` |
| Funciones de extensión | `normalizado()`, `comoFrase()`, `conCorreo()` y `mensajeLegible()` |
| Propiedades de extensión | `String.esCorreo` y `Usuario.resumen`, declaradas con `get()` |
| Funciones de orden superior | `reglaDeCorreo()` devuelve una función; `primerError()` recibe funciones |
| `typealias` | `ReglaDeCampo`, el tipo de función que valida un campo |
| Lambdas | Las reglas de los formularios y las operaciones de colección |
| Funciones inline | `intentar()`, que además recibe una lambda |
| `try` / `catch` | Dentro de `intentar()`, alrededor del motor de voz y del vibrador |
| Seguridad nula | `Usuario?`, `String?` y los operadores `?.`, `?:` y `takeIf` |
| Genéricos | `GrillaSeleccionUnica<T>` e `intentar<T>` |

### Funciones de orden superior en los formularios

Las tres views de acceso validan el mismo correo, pero cada una le exige algo
distinto. En lugar de repetir la validación, `reglaDeCorreo` construye y
devuelve la regla que cada view necesita:

```kotlin
// Login: basta con que tenga formato de correo
val reglaCorreo = reglaDeCorreo(ExigenciaCorreo.SOLO_FORMATO)

// Registro: además, nadie más puede tenerlo
val reglaCorreo = reglaDeCorreo(ExigenciaCorreo.DEBE_SER_NUEVO, onCorreoRegistrado)

// Recuperar contraseña: además, tiene que existir
val reglaCorreo = reglaDeCorreo(ExigenciaCorreo.DEBE_EXISTIR, onCorreoRegistrado)
```

Gracias a eso el registro avisa que el correo ya está tomado mientras la
persona escribe, y no recién al enviar el formulario.

`primerError` recibe las reglas como funciones y devuelve la primera que falla,
sin evaluar las siguientes, de modo que el aviso nombre el problema real:

```kotlin
val error = primerError(
    { Validaciones.errorNombre(nombre) },
    { reglaCorreo(correo) },
    { Validaciones.errorPassword(password) },
    { Validaciones.errorConfirmacion(password, confirmacion) },
    { if (aceptaTerminos) null else "Debes aceptar los términos para continuar" }
)
```

### Manejo de errores

```kotlin
inline fun <T> intentar(alFallar: (Exception) -> Unit = {}, bloque: () -> T): T? =
    try {
        bloque()
    } catch (error: Exception) {
        alFallar(error)
        null
    }
```

## Estructura

Sigue la organización por responsabilidades que usa el material de la
asignatura: los datos por un lado, las views por otro y el tema aparte.

```
app/src/main/java/com/vozinha/app/
├── MainActivity.kt          Actividad única, aplica el tema
├── VozinhaApp.kt            Rutas y grafo de navegación
├── data/
│   ├── Usuario.kt           Modelo y enums de tipo y medio
│   ├── UsuariosRepository.kt Arreglo de cinco usuarios
│   └── Frases.kt            Frases rápidas iniciales
├── ui/
│   ├── AppViewModel.kt      Estado de usuarios, sesión y frases
│   ├── components/          Componentes reutilizables y motor de voz
│   ├── screens/             Las cuatro views
│   └── theme/               Colores y tipografía
└── util/
    ├── Validaciones.kt      Reglas de validación de formularios
    └── Extensiones.kt      Extensiones, orden superior y manejo de errores
```

## Ejecutar y probar

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Documentación

El informe técnico, los mockups y el diagrama EDT de la entrega se mantienen
fuera de este repositorio, junto a la carpeta del proyecto, porque son
material de la evaluación y no del código.

## Arquitectura de datos

La aplicación no habla con Firebase directamente. Entre las views y el backend
hay cuatro interfaces de servicio:

| Servicio | Responsabilidad |
|---|---|
| `ServicioAutenticacion` | Registro, inicio de sesión y correo de recuperación |
| `ServicioUsuarios` | CRUD de la ficha de cada persona |
| `ServicioMensajes` | CRUD de los mensajes de las views Escribir y Hablar |
| `ServicioUbicaciones` | CRUD del historial de la view Buscar dispositivo |

Cada una tiene dos implementaciones: una sobre Firebase y otra local, que
trabaja en memoria. Eso permite tres cosas: que la aplicación siga funcionando
si el teléfono no tiene conexión, que las pruebas unitarias corran sin red ni
cuenta del servicio, y que Mockito pueda reemplazar los servicios por dobles
para verificar cómo se comunican las capas.

El menú principal indica en qué backend se están guardando los datos.

## Firebase

El proyecto está conectado al proyecto de Firebase `vozinha-1b610`, con
Authentication por correo y contraseña y Cloud Firestore. El archivo
`app/google-services.json` viaja en el repositorio, así que el proyecto
compila y funciona sin configuración adicional.

Los datos se organizan de modo que cada persona sea dueña de lo suyo:

```
usuarios/{correo}                    ficha de la persona
usuarios/{correo}/mensajes/{id}      mensajes de Escribir y Hablar
usuarios/{correo}/ubicaciones/{id}   historial de Buscar dispositivo
```

Las reglas de seguridad publicadas solo permiten leer y escribir bajo el
documento cuyo identificador coincide con el correo de la sesión:

```
match /usuarios/{correo} {
  allow read, write: if request.auth != null
    && request.auth.token.email.lower() == correo;

  match /{documento=**} {
    allow read, write: if request.auth != null
      && request.auth.token.email.lower() == correo;
  }
}
```

Por eso la aplicación no puede listar todas las cuentas, y la tabla de cuentas
de prueba de la view de Login se arma con el arreglo que vive en el proyecto.

Si el teléfono no tiene conexión o Firebase no responde, `backendActivo()`
devuelve la implementación local y la aplicación sigue operando. El menú
principal indica en qué backend se están guardando los datos.

## Extensiones KTX

Las extensiones KTX agregan a las APIs de Android una versión idiomática de
Kotlin: funciones de extensión, lambdas y corrutinas en lugar de devoluciones
de llamada.

| Extensión | Dónde se usa |
|---|---|
| `core-ktx` | `ContextCompat` en `Localizador`, `toUri()` al abrir el mapa |
| `lifecycle-viewmodel-ktx` | `viewModelScope` lanza todas las operaciones del `AppViewModel` |
| `lifecycle-runtime-compose` | `collectAsStateWithLifecycle` observa el correo recordado |
| `activity-compose` | `rememberLauncherForActivityResult` pide el permiso de ubicación |
| `datastore-preferences` | `preferencesDataStore` y `edit {}` guardan el correo |
| `kotlinx-coroutines-play-services` | `await()` convierte las Task de Google en suspensión |
| Firebase | Desde el BOM 33 las extensiones vienen en los artefactos principales |

## Pruebas

```bash
./gradlew testDebugUnitTest            # 69 pruebas locales
./gradlew connectedDebugAndroidTest    # 6 pruebas instrumentadas
```

Las pruebas instrumentadas necesitan un emulador o un teléfono con las
animaciones desactivadas, que es lo que exige Espresso:

```bash
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
```

| Herramienta | Qué cubre |
|---|---|
| JUnit | Validaciones, extensiones y CRUD de los servicios |
| kotlinx-coroutines-test | Operaciones asíncronas del ViewModel |
| Mockito | Que el ViewModel llame al servicio correcto y no siga tras un fallo |
| Robolectric | Permisos de ubicación y vibración, dentro de la JVM |
| Espresso y Compose UI Test | Recorrido entre las siete views en el dispositivo |

## Generar el APK firmado

```bash
./gradlew assembleRelease
```

La firma se configura en `keystore.properties`, que apunta al almacén de
claves de `keystore/`. Si el archivo no existe, el proyecto igual compila y la
variante de release queda sin firmar.

En un proyecto real ni el almacén ni sus contraseñas se suben al repositorio:
se guardan en un gestor de secretos y se inyectan al compilar. Aquí se
incluyen a propósito para que la evaluación pueda reconstruir y verificar el
mismo APK firmado.

## Publicación

La carpeta `docs/` contiene la página de descarga del APK, pensada para
publicarse con GitHub Pages. Simula la ficha de una tienda: datos de la
aplicación, capturas, requisitos, la huella SHA-256 del archivo y el botón de
descarga.
