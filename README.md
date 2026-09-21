# Krono Corp

Proyecto de la materia Desarrollo de Software para Móviles, Universidad Don Bosco.

Krono Corp es un asistente para negocios que sirve para guardar, consultar y analizar datos de ventas. La idea salió de un conocido que llevaba las ventas de sus empleados en Excel desde el teléfono y, cuando no tenía computadora, se le quedaban días sin registrar y perdía el historial. Queremos una aplicación fácil de usar donde se puedan ver las ventas, sacar reportes y gráficos, y tener un asistente con IA que ayude a analizar el negocio.

## Integrantes

- Gustavo Ismael Serrano Rivera - SR251873
- Fernando Antonio Paz López - LP251570
- Katherine Estefany Beltran Lopez - BL233081
- Erick Vladimir Barrientos Lopez - BL230303

Mockups en Figma: https://www.figma.com/design/65EZucVsWODzITBkJ1r8Ws/Sin-t%C3%ADtulo?node-id=27-446&t=QzvNpZ6f4plfeEeR-1

## En qué punto va el proyecto

Por ahora es un prototipo que corre en la consola, hecho en Kotlin. Lo hicimos así para probar primero la lógica (usuarios, roles, reportes, asistente) antes de armar la app móvil.

Esto es lo que teníamos planeado en el documento y cómo va cada cosa:

- Kotlin: ya lo estamos usando.
- App Android con Jetpack Compose: todavía no, por ahora es de consola.
- Appwrite para guardar los datos: todavía no, por ahora guardamos todo en archivos JSON.
- Groq para el chat: sí está, aunque usamos el modelo `openai/gpt-oss-120b` y no Llama 3.3 como decía el documento.
- Gemini para los reportes: no se pudo. El modelo que teníamos planeado (Gemini 1.5 Flash) ya fue dado de baja por Google y dejó de funcionar en 2025. Por eso los reportes en PDF los hacemos con OpenPDF y JFreeChart.

## Tecnologías

- Kotlin 2.4.10 con JDK 21
- Gradle 9.2.0 (el repositorio ya trae el `gradlew`, no hay que instalar Gradle)
- kotlinx-serialization-json 1.6.3 para leer y guardar los JSON
- OpenPDF 3.0.0 para crear los PDF
- JFreeChart 1.5.5 para el gráfico de pastel que va dentro del PDF
- `java.net.http.HttpClient` para conectarnos a la API de Groq

En `gradle/libs.versions.toml` también quedaron declaradas Guava y JUnit, pero todavía no las usamos y no tenemos pruebas automáticas.

## Cómo correrlo

Se necesita internet la primera vez porque Gradle descarga las dependencias. Si no tienes el JDK 21, el proyecto intenta descargarlo solo.

```
git clone https://github.com/ROJO1495/Krono-Corp.git
cd Krono-Corp
.\gradlew run --console=plain
```

En Linux o Mac es `./gradlew run --console=plain`.

Hay que poner el `--console=plain`, si no la barra de progreso de Gradle se mezcla con lo que el programa pregunta y no se entiende lo que uno escribe.

Si en Windows los acentos salen con símbolos raros, se puede correr `chcp 65001` antes.

### Usuarios de prueba

Se puede entrar con el correo o con el alias.

| Alias | Correo | Contraseña | Rol |
|---|---|---|---|
| Gustavo | gustavoisr14@gmail.com | 123456 | GERENTE |
| Ismael | ismael@gmail.com | 123456 | SUPERVISOR |
| clau | clau@gmail.com | 12345678 | SUPERVISOR |
| coco | gatoGordo@gmail.com | gato | DUENO |

También se puede crear un usuario nuevo con la opción 2 del menú de inicio.

## Estructura del proyecto

```
Krono-Corp/
  build.gradle.kts          dependencias y clase principal
  settings.gradle.kts       nombre del proyecto
  gradle/                   catálogo de versiones y el wrapper
  gradlew, gradlew.bat      para correr el proyecto sin instalar Gradle
  .gitignore                archivos que no se suben (build, .gradle, *.log, etc.)
  src/main/kotlin/
    Login/login.kt          inicio de sesión y registro (aquí empieza el programa)
    Gerente/admin.kt        menú por roles y menú del Gerente General
    Negocio/negocio.kt      home del negocio, sucursales y configuración
    AsistenteIA/
      asistente_de_gerenete_general.kt   asistente del Gerente (supervisores, PDF y chat)
      asistenteAdmin.kt                  asistente del negocio, historiales y nueva cuenta
    IA/
      GestorIA.kt           envía las preguntas a la IA
      GroqService.kt        conexión con la API de Groq
  usuario.json, negocios.json, supervisores.json
  datosNorte.json, datosCentro.json, datosSur.json
  historial_reportes.json, historial_graficos.json
```

Los paquetes son `Login`, `Gerente`, `Negocio`, `AsistenteIA`, `AsistenteAdmin` e `IA`. Ojo que `asistenteAdmin.kt` está dentro de la carpeta `AsistenteIA` pero su paquete es `AsistenteAdmin`.

## Cómo funciona

El programa empieza en `login.kt`. Se puede iniciar sesión o registrarse, y según el rol del usuario se abre un menú distinto:

- GERENTE: va al menú del Gerente General.
- SUPERVISOR y DUENO: van al home del negocio.

Al registrarse se elige el rol: 1 Gerente, 2 Supervisor y 3 Dueño.

### Login

Cuando arranca lee `usuario.json`. Si no lo puede leer, el programa se detiene, porque si seguía iba a guardar una lista vacía encima del archivo y se perdían los usuarios.

Para iniciar sesión se revisa el correo o alias junto con la contraseña. La contraseña tiene que ser exacta, el correo y el alias no distinguen mayúsculas. Se validan los dos al mismo tiempo porque puede haber varias cuentas con el mismo correo pero con contraseñas diferentes (ver "nueva cuenta de administración" más abajo).

En el registro el alias y el correo no se pueden repetir.

### Gerente General

Su menú tiene evaluación de supervisores, historial de reportes, reportes de ventas y el asistente.

- Evaluación de supervisores: muestra cada supervisor con su equipo, cuántos integrantes tiene, el total de ventas y el promedio por integrante. También se pueden agregar y eliminar supervisores.
- Hay tres equipos (Norte, Centro y Sur) y cada uno tiene un solo supervisor. Un supervisor nuevo solo puede tomar un equipo que esté libre. Al crearlo se guarda como usuario SUPERVISOR en `usuario.json` y en `supervisores.json`.
- Reportes de ventas: genera un PDF con el nombre `Reporte_ventas_AAAA-MM-DD_HH-mm-ss.pdf` en la carpeta donde se ejecuta el programa.
- Historial de reportes: lista los PDF que se han generado, con la fecha y el tamaño.

### Asistente del Gerente

Muestra un menú fijo:

1. Consultar datos de los supervisores
2. Buscar un supervisor (por nombre, id, correo o teléfono)
3. Generar el PDF de promedios de ventas (`Promedios_de_ventas.pdf`)
4. Quitar o agregar supervisores
5. Salir

Si el usuario escribe algo que no es un número, se toma como una pregunta para la IA y se abre un chat. El chat se acuerda de los últimos 10 mensajes y se cierra escribiendo `salir`.

El PDF trae la fecha, una tabla con el resumen de cada equipo, un gráfico de pastel con los promedios y el detalle de ventas por integrante. El gráfico se dibuja con JFreeChart y se mete en el PDF con OpenPDF.

### Home del negocio

Es para los usuarios SUPERVISOR y DUENO. Si el usuario no tiene un negocio en `negocios.json`, se le crea uno llamado "Mi Negocio" con dos preguntas de ejemplo para la IA.

Opciones del home:

1. Mi negocio: cambiar el nombre y administrar las preguntas para la IA (agregar, editar y borrar)
2. Administración de sucursales: agregar (con código de acceso, por defecto `1234`), editar el nombre, activar o desactivar y eliminar
3. Asistente (ChatBot)
4. Historial de reportes
5. Historial de gráficos
6. Nueva cuenta de administración
7. Configuración: mi cuenta, reportar un fallo y cerrar sesión. Las opciones de seguridad, notificaciones e idioma por ahora solo muestran un mensaje.
8. Cerrar sesión

Cuando algo falla (por ejemplo al leer un JSON) o un usuario reporta un fallo desde Configuración, se guarda en el archivo `errores.log`.

### Asistente del negocio

Esta parte es la versión beta. Las respuestas son simuladas y usan datos de ejemplo, todavía no llama a ninguna IA.

Al abrirlo muestra primero las preguntas que el dueño configuró para su negocio (`P1`, `P2`...) y luego las opciones generales: resumen de ventas, producto más vendido, inventario, productos con poco stock (5 unidades o menos), generar un reporte, generar un gráfico de barras, ver el historial de reportes, ver el historial de gráficos y salir.

Si se escribe una pregunta libre, el asistente busca palabras clave como mes, producto, venta, inventario, reporte o gráfico y responde con los datos de ejemplo. Si no entiende, da una respuesta general.

Los reportes y gráficos que se generan se guardan solos en `historial_reportes.json` y `historial_graficos.json`. Cada usuario solo ve los suyos, según el correo del dueño del negocio, y puede abrir uno para verlo completo.

### Nueva cuenta de administración

Sirve para crear otra cuenta que administre el mismo negocio sin tener que poner otro correo. La cuenta nueva usa el mismo correo, teléfono y rol del usuario que la crea. Solo se piden un nombre (que no lo tenga otro usuario) y una contraseña (mínimo 4 caracteres y distinta a la de otras cuentas con ese correo). Después se puede iniciar sesión con esa cuenta usando el correo o el nombre y su contraseña, y entra al mismo negocio.

## La IA

`GestorIA` recibe la pregunta y se la pasa a `GroqService`, que hace una petición POST a `https://api.groq.com/openai/v1/chat/completions` con el modelo `openai/gpt-oss-120b`. Le mandamos un mensaje de sistema para que se presente como Krono, el asistente de KronoCorp, y responda de forma clara y profesional.

La clave se lee de la variable `GROQ_API_KEY`. Si no existe o si la API da error, devuelve un mensaje de error y el programa sigue funcionando.

## Archivos de datos

Todos son JSON y se guardan en la carpeta donde se ejecuta el programa.

- `usuario.json`: alias, email, phoneNumber, password y role de cada usuario.
- `negocios.json`: nombre del negocio, correo del dueño, tipo (PROPIO o SUCURSAL), las preguntas para la IA y las sucursales (id, nombre, activa y código de acceso).
- `supervisores.json`: id, nombre, correo, teléfono y equipo de cada supervisor.
- `datosNorte.json`, `datosCentro.json` y `datosSur.json`: las ventas realizadas por cada integrante de cada equipo.
- `historial_reportes.json` y `historial_graficos.json`: id, autor, título, fecha y contenido de cada reporte o gráfico.

Al ejecutar el programa también se generan los PDF de los reportes y el archivo `errores.log` (los `.log` están en el `.gitignore`).

## Lo que falta o no está bien todavía

- No es la app Android todavía y no usa Appwrite.
- Las contraseñas se guardan en texto plano en `usuario.json`. Por ahora son solo datos de prueba, pero hay que cambiarlo.
- El asistente del negocio usa datos de ejemplo fijos y respuestas simuladas, no analiza las ventas reales.
- Los datos de ventas de los equipos (`datos*.json`) son inventados.
- Gemini no está integrado porque Gemini 1.5 Flash, el modelo que habíamos planeado, ya no está disponible. Si más adelante queremos usar Gemini, habría que cambiarlo a un modelo más nuevo.
- No tenemos pruebas automáticas.

Lo que sigue sería pasar la interfaz a Android con Jetpack Compose siguiendo el diseño de Figma, cambiar los JSON por Appwrite, conectar el asistente del negocio a la IA con los datos reales, guardar las contraseñas cifradas y agregar pruebas.

## Para trabajar en equipo

- Cada quien trabaja en su propia rama y luego se hace un pull request.
- Antes de mezclar, revisar que el proyecto corra con `.\gradlew run --console=plain` desde una copia limpia del repositorio.
- No subir claves ni archivos generados (PDF y `errores.log`).
