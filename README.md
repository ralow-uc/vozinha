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
| Check box | Login, Registro |
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
    └── Validaciones.kt      Reglas de validación de formularios
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
# vozinha
