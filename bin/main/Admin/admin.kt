package Admin

import Login.User

fun showHome(usuario: User, listaUsuarios: MutableList<User>) {

    println()
    println("===============================================")
    println("                    HOME")
    println("===============================================")
    println("Bienvenido: ${usuario.email}")
    println("Rol: ${usuario.role}")
    println()

    when (usuario.role) {

        "ADMINISTRADOR" -> {
            println("1. Evaluacion de administradores")
            println("2. Historial de reportes")
            println("3. Reportes de Ventas")
            println("4. Asistente")
            println("5. Cerrar sesion")

            print("\nSelecciona una opcion: ")

            when (readln().trim()) {
                "1" -> evaluacionAdministradores(listaUsuarios)
                "2" -> historialReportes()
                "3" -> reportesVentas()
                "4" -> asistente()
                "5" -> println("Sesion cerrada.")
                else -> println("Opcion invalida")
            }
        }

        "DUENO" -> {
            println("1. ")
        }

        "EMPLEADO" -> {
            println("1. ")
        }
    }
}


fun evaluacionAdministradores(listaUsuarios: MutableList<User>) {

    println()
    println("===============================================")
    println("      EVALUACION DE ADMINISTRADORES")
    println("===============================================")

    println("1. Ver Administradores")
    println("2. Agregar nuevo administrador")
    println("3. Salir")

    print("Selecciona una opcion: ")

    when (readln().trim()) {
        "1" -> mostrarAdministradores(listaUsuarios)
        "2" -> nuevoAdministrador(listaUsuarios)
        "3" -> return
        else -> println("Opcion invalida")
    }
}


fun mostrarAdministradores(listaUsuarios: List<User>) {

    println()
    println("===============================================")
    println("          LISTA DE ADMINISTRADORES")
    println("===============================================")

    val administradores = listaUsuarios.filter {
        it.role == "ADMINISTRADOR"
    }

    if (administradores.isEmpty()) {
        println("No hay administradores registrados")
    } else {

        administradores.forEachIndexed { index, administrador ->

            println()
            println("${index + 1}. ${administrador.name}")
            println("Correo: ${administrador.email}")
            println("Telefono: ${administrador.phoneNumber}")

            println("Informacion de ventas: [pendiente]")
            println("---------------------------------------")
        }
    }

    println()
    println("Presiona ENTER para regresar.")
    readln()
}


fun nuevoAdministrador(listaUsuarios: MutableList<User>) {

    println()
    println("======================================")
    println("          NUEVO ADMINISTRADOR")
    println("======================================")

    println("Nombre del administrador: ")
    val nombre = readln().trim()

    println("Ingrese el correo electrónico: ")
    val email = readln().trim()

    val existe = listaUsuarios.any {
        it.email.equals(email, ignoreCase = true)
    }

    if (existe) {
        println()
        println("Ya existe un usuario con ese correo.")
        println("\nPresiona ENTER para regresar.")
        readln()
        return
    }

    println("Ingrese el Número de teléfono: ")
    val telefono = readln().trim()

    println(" Contraseña: ")
    val password = readln().trim()

    val nuevoAdministrador = User(
        name = nombre,
        email = email,
        phoneNumber = telefono,
        password = password,
        role = "ADMINISTRADOR"
    )

    listaUsuarios.add(nuevoAdministrador)

    println()
    println("Administrador creado correctamente.")
    println("Nombre: ${nuevoAdministrador.name}")
    println("Correo: ${nuevoAdministrador.email}")
    println("Rol: ${nuevoAdministrador.role}")

    println("\nPresiona ENTER para regresar.")
    readln()
}

fun reportesVentas() {
    println("ventas: pendiente")
}

fun historialReportes() {
    println("ventas: pendiente")
}


fun asistente() {
    println("Asistente: pendiente")
}