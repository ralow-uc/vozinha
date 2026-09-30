package com.vozinha.app.data.servicios

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vozinha.app.data.MedioComunicacion
import com.vozinha.app.data.Mensaje
import com.vozinha.app.data.TipoUsuario
import com.vozinha.app.data.Ubicacion
import com.vozinha.app.data.Usuario
import com.vozinha.app.util.normalizado
import kotlinx.coroutines.tasks.await

/*
 * Implementación del backend sobre Google Firebase.
 *
 * Firebase Authentication resuelve el registro, el inicio de sesión y el correo
 * de recuperación. Cloud Firestore guarda la ficha de cada persona, sus
 * mensajes y sus ubicaciones.
 *
 * Todas las llamadas usan await(), la extensión KTX de corrutinas que convierte
 * la Task de Google Play Services en una función de suspensión: el código queda
 * lineal, sin devoluciones de llamada anidadas.
 */

private const val USUARIOS = "usuarios"
private const val MENSAJES = "mensajes"
private const val UBICACIONES = "ubicaciones"

/** Envuelve la llamada a Firebase y traduce la excepción a un [Resultado]. */
private suspend fun <T> consultar(bloque: suspend () -> T): Resultado<T> = try {
    Resultado.Exito(bloque())
} catch (error: Exception) {
    Resultado.Fallo(error.message ?: "No pudimos conectarnos con el servidor")
}

/** Autenticación de las personas registradas con Firebase Authentication. */
class AutenticacionFirebase(private val auth: FirebaseAuth) : ServicioAutenticacion {

    override val correoActual: String?
        get() = auth.currentUser?.email

    override suspend fun registrar(correo: String, password: String): Resultado<String> =
        consultar {
            auth.createUserWithEmailAndPassword(correo.normalizado(), password).await()
            correo.normalizado()
        }

    override suspend fun iniciarSesion(correo: String, password: String): Resultado<String> =
        consultar {
            auth.signInWithEmailAndPassword(correo.normalizado(), password).await()
            correo.normalizado()
        }

    override suspend fun enviarCorreoDeRecuperacion(correo: String): Resultado<Unit> =
        consultar { auth.sendPasswordResetEmail(correo.normalizado()).await() }

    override fun cerrarSesion() = auth.signOut()
}

/** Ficha de cada persona usuaria en la colección usuarios de Firestore. */
class UsuariosFirestore(private val db: FirebaseFirestore) : ServicioUsuarios {

    private fun documento(correo: String) =
        db.collection(USUARIOS).document(correo.normalizado().lowercase())

    override suspend fun crear(usuario: Usuario): Resultado<Unit> = consultar {
        documento(usuario.correo).set(usuario.aMapa()).await()
        Unit
    }

    override suspend fun obtener(correo: String): Resultado<Usuario?> = consultar {
        documento(correo).get().await().datos()?.aUsuario()
    }

    override suspend fun listar(): Resultado<List<Usuario>> = consultar {
        db.collection(USUARIOS).get().await().documents.mapNotNull { it.data?.aUsuario() }
    }

    override suspend fun actualizar(usuario: Usuario): Resultado<Unit> = consultar {
        documento(usuario.correo).update(usuario.aMapa()).await()
        Unit
    }

    override suspend fun eliminar(correo: String): Resultado<Unit> = consultar {
        documento(correo).delete().await()
        Unit
    }
}

/** Mensajes de las views Escribir y Hablar, bajo el documento de cada persona. */
class MensajesFirestore(private val db: FirebaseFirestore) : ServicioMensajes {

    private fun coleccion(correo: String) =
        db.collection(USUARIOS).document(correo.normalizado().lowercase()).collection(MENSAJES)

    /**
     * Guarda un mensaje nuevo.
     *
     * Antes de escribir comprueba que el texto no esté repetido, igual que la
     * implementación local: las dos cumplen el mismo contrato, así la persona
     * ve el mismo comportamiento con o sin conexión.
     */
    override suspend fun guardar(correo: String, texto: String): Resultado<Mensaje> {
        val limpio = texto.normalizado()
        if (limpio.isEmpty()) return Resultado.Fallo("Escribe un mensaje antes de guardarlo")
        return consultar {
            val repetido = coleccion(correo).get().await()
                .toObjects(Mensaje::class.java)
                .any { it.texto.equals(limpio, ignoreCase = true) }
            if (repetido) error("Ese mensaje ya está guardado")

            val documento = coleccion(correo).document()
            val mensaje = Mensaje(
                id = documento.id,
                texto = limpio,
                creadoEn = System.currentTimeMillis()
            )
            documento.set(mensaje).await()
            mensaje
        }
    }

    override suspend fun listar(correo: String): Resultado<List<Mensaje>> = consultar {
        coleccion(correo).get().await()
            .toObjects(Mensaje::class.java)
            .sortedByDescending { it.creadoEn }
    }

    override suspend fun registrarReproduccion(correo: String, id: String): Resultado<Unit> =
        consultar {
            val documento = coleccion(correo).document(id)
            val actual = documento.get().await().toObject(Mensaje::class.java)
                ?: error("No encontramos ese mensaje")
            documento.update("vecesDicho", actual.vecesDicho + 1).await()
            Unit
        }

    override suspend fun cambiarFavorito(correo: String, id: String): Resultado<Unit> =
        consultar {
            val documento = coleccion(correo).document(id)
            val actual = documento.get().await().toObject(Mensaje::class.java)
                ?: error("No encontramos ese mensaje")
            documento.update("favorito", !actual.favorito).await()
            Unit
        }

    override suspend fun eliminar(correo: String, id: String): Resultado<Unit> = consultar {
        coleccion(correo).document(id).delete().await()
        Unit
    }
}

/** Ubicaciones de la view BuscarDispositivo. */
class UbicacionesFirestore(private val db: FirebaseFirestore) : ServicioUbicaciones {

    private fun coleccion(correo: String) =
        db.collection(USUARIOS).document(correo.normalizado().lowercase()).collection(UBICACIONES)

    override suspend fun guardar(correo: String, ubicacion: Ubicacion): Resultado<Ubicacion> =
        consultar {
            val documento = coleccion(correo).document()
            val guardada = ubicacion.copy(id = documento.id)
            documento.set(guardada).await()
            guardada
        }

    override suspend fun listar(correo: String): Resultado<List<Ubicacion>> = consultar {
        coleccion(correo).get().await()
            .toObjects(Ubicacion::class.java)
            .sortedByDescending { it.registradaEn }
    }

    override suspend fun ultima(correo: String): Resultado<Ubicacion?> = consultar {
        coleccion(correo).get().await()
            .toObjects(Ubicacion::class.java)
            .maxByOrNull { it.registradaEn }
    }

    override suspend fun borrarHistorial(correo: String): Resultado<Unit> = consultar {
        coleccion(correo).get().await().documents.forEach { it.reference.delete().await() }
        Unit
    }
}

// ---------------------------------------------------------------------------
// Conversión entre el modelo y los documentos de Firestore
// ---------------------------------------------------------------------------

/**
 * La contraseña no viaja a Firestore: de eso se encarga Firebase
 * Authentication, que guarda solo el hash.
 */
private fun Usuario.aMapa(): Map<String, Any> = mapOf(
    "nombre" to nombre,
    "correo" to correo.normalizado(),
    "tipoUsuario" to tipoUsuario.name,
    "medioPreferido" to medioPreferido.name
)

private fun com.google.firebase.firestore.DocumentSnapshot.datos(): Map<String, Any>? =
    if (exists()) data else null

private fun Map<String, Any>.aUsuario(): Usuario = Usuario(
    nombre = this["nombre"] as? String ?: "",
    correo = this["correo"] as? String ?: "",
    password = "",
    tipoUsuario = TipoUsuario.entries
        .firstOrNull { it.name == this["tipoUsuario"] } ?: TipoUsuario.PERSONA_SORDA,
    medioPreferido = MedioComunicacion.entries
        .firstOrNull { it.name == this["medioPreferido"] } ?: MedioComunicacion.TEXTO_ESCRITO
)

/**
 * Backend sobre Firebase.
 *
 * Devuelve null cuando Firebase no está inicializado, es decir cuando el
 * proyecto todavía no tiene el archivo google-services.json, para que la
 * aplicación pueda seguir con el backend local en lugar de caerse.
 */
fun backendFirebase(): Backend? = try {
    Backend(
        autenticacion = AutenticacionFirebase(FirebaseAuth.getInstance()),
        usuarios = UsuariosFirestore(FirebaseFirestore.getInstance()),
        mensajes = MensajesFirestore(FirebaseFirestore.getInstance()),
        ubicaciones = UbicacionesFirestore(FirebaseFirestore.getInstance()),
        nombre = "Firebase"
    )
} catch (error: Exception) {
    null
}
