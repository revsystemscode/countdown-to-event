# Publicar Countdown to Event en Google Play

Guía paso a paso. Todo lo que se puede hacer desde el proyecto ya está hecho; lo que queda
necesita tu cuenta de Google Play Console.

## 0. Antes de nada: guarda la clave de subida

La app se firma con una **clave de subida** creada para este proyecto:

- `keystore/upload-keystore.jks` (la clave)
- `keystore.properties` (su contraseña)

Ninguno de los dos está en git. **Copia ambos a un lugar seguro** (gestor de contraseñas o
disco cifrado). Si los pierdes, Google puede restablecer la clave de subida (gracias a la firma
de apps de Play), pero tarda unos días.

Huella SHA-256 de la clave de subida (por si Play Console la pide):
`71:D8:A7:36:61:2C:A4:EB:26:E2:AD:ED:84:32:11:21:37:17:59:4F:2C:C3:0B:9C:B3:A3:6F:D1:4E:DD:F4:B3`

## 1. Generar el paquete

```sh
./gradlew bundleRelease
```

Resultado: `app/build/outputs/bundle/release/app-release.aab`.

Para cada actualización, sube `versionCode` (y normalmente `versionName`) en `app/build.gradle.kts`.
Play rechaza un `versionCode` repetido.

## 2. Publicar la política de privacidad

Play exige una URL pública. El archivo está en `docs/privacy/index.html` (español e inglés).

1. Sustituye `[TU_EMAIL_DE_CONTACTO]` por tu correo de contacto (aparece dos veces).
2. Súbelo a cualquier hosting estático. Con GitHub Pages: sube el proyecto a GitHub →
   *Settings → Pages → Branch: main, carpeta `/docs`*. La URL será
   `https://<usuario>.github.io/<repositorio>/privacy/`.

## 3. Crear la app en Play Console

1. Cuenta de desarrollador en <https://play.google.com/console> (pago único de 25 USD y
   verificación de identidad).
2. *Crear app* → nombre **Countdown to Event**, idioma predeterminado **Español (España)**,
   tipo **App**, **Gratis**.
3. La firma de apps de Play se activa sola al subir el primer `.aab`: Google genera y guarda la
   clave de firma definitiva; tú solo usas la clave de subida.

## 4. Contenido de la app (*Política → Contenido de la app*)

| Sección | Respuesta |
|---|---|
| Política de privacidad | La URL del paso 2 |
| Anuncios | No contiene anuncios |
| Acceso a la app | Todas las funciones están disponibles sin restricciones (no hay inicio de sesión) |
| Clasificación de contenido | Categoría *Utilidad / Productividad*; responde «No» a todo el contenido sensible → resultado esperado: PEGI 3 / Todos |
| Público objetivo | 13 años o más (evita las reglas de apps para niños; la app no está dirigida a menores) |
| Seguridad de los datos | **No recopila ni comparte datos.** La app no tiene permiso de internet; las copias de seguridad las crea el usuario en el lugar que elige y no se envían a nosotros |
| App de noticias / gobierno / finanzas / salud | No |

Permisos que Play puede preguntar:

- `SCHEDULE_EXACT_ALARM`: no requiere declaración (solo `USE_EXACT_ALARM` la requiere). La app
  funciona si el usuario no lo concede.
- `FOREGROUND_SERVICE`: lo añade WorkManager, que usan los widgets. Si Play pide declarar el uso
  de servicios en primer plano, indica que la app no inicia servicios en primer plano propios.

## 5. Ficha de la tienda (*Presencia en la tienda → Ficha principal*)

Textos y recursos en `docs/play-store/listing.md`:

- Icono 512×512: `docs/play-store/graphics/icon-512.png`
- Gráfico de funciones: `docs/play-store/graphics/feature-graphic-es.png` (y `-en` en la traducción inglesa)
- Capturas: `docs/play-store/screenshots/es-*.png`

## 6. Pruebas y lanzamiento

1. **Prueba interna**: *Probar y publicar → Pruebas → Prueba interna* → crear versión → subir
   el `.aab`. Añade tu correo como tester e instálala desde el enlace de inscripción.
2. **Prueba cerrada (obligatoria en cuentas personales nuevas)**: las cuentas personales creadas
   después de noviembre de 2023 deben tener **al menos 12 testers inscritos durante 14 días
   seguidos** antes de poder pedir acceso a producción.
3. **Producción**: cuando Play lo permita, promueve la versión a producción. Recomendado:
   lanzamiento escalonado (por ejemplo, 20 % → 100 %).

## 7. Comprobaciones hechas antes de esta versión

- Versión de lanzamiento (R8 activo) instalada en un emulador con Android 16: crear y editar
  eventos, detalle con cuenta regresiva en vivo, exportar e importar copias con fotos, compartir
  imagen, widgets (incluido el modo en vivo), recordatorios y reinicio del teléfono.
- 77 pruebas unitarias y 4 pruebas en el emulador, lint de Android y ktlint sin errores.
- `targetSdk 36`, `minSdk 26` (Android 8.0 o superior).
