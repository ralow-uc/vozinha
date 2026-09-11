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
- TextToSpeech y Vibrator del sistema Android
- compileSdk 36, minSdk 24

## Views

| View | Descripción |
|---|---|
| Login | Acceso validado contra el arreglo de usuarios registrados. |
| Registro de usuario | Formulario que agrega un usuario nuevo al arreglo. |
| Recuperar contraseña | Verifica el correo y simula el envío de instrucciones. |
| Comunicación | Escribe y el teléfono habla, con frases rápidas que se pueden agregar y eliminar. |

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
