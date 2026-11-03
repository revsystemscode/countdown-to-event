# Plan: app de cuenta regresiva para Android

> Nombre: **Countdown to Event** (`countdown-to-event`) · Paquete: `com.revsystems.countdowntoevent`
> Stack: **Kotlin + Jetpack Compose**, **solo local** (sin backend) en esta primera versión.

## 1. Análisis de la app de referencia (reachus.app)

**Qué es:** app gratis y sin anuncios para crear cuentas regresivas a eventos (cumpleaños, viajes,
conciertos, reencuentros de pareja a distancia) y compartirlas con amigos. Sus widgets son lo que más destaca.

**Pantallas que se ven en las capturas:**
- **Inicio:** dos pestañas, `Próximos` y `Alcanzados`, y un icono de perfil arriba a la derecha.
- **Tarjeta de evento:** imagen de fondo a ancho completo con capa oscura. A la izquierda, título con emoji
  y rango de fechas `20/11/2025 → 20/11/2025`. A la derecha, número grande de días y la palabra "días".
  Abajo a la izquierda, avatares de los participantes; abajo a la derecha, un botón para compartir.
- **Tarjeta vacía** gris con un `+` al final de la lista.
- **Barra inferior:** Inicio · botón `+` en color turquesa · Amigos.

**Funciones (según la web y el FAQ):**
| Función | Detalle | ¿En v1 local? |
|---|---|---|
| Crear evento | título, fecha (y fecha de fin), imagen o color de fondo | ✅ |
| Repetición | diaria, semanal, quincenal, mensual, anual | ✅ |
| Próximos / Alcanzados | los eventos pasados se mueven de pestaña | ✅ |
| Widget de pantalla de inicio | varios tamaños; modo "Live" que cuenta días, horas, minutos y segundos | ✅ |
| Recordatorios | uno global: de 2 días a 15 minutos antes | ✅ |
| Compartir | enlace o imagen | ✅ (imagen/texto) · enlace en v2 |
| Amigos, eventos compartidos en sincronía | requiere backend | ❌ v2 |
| Inicio de sesión Google/Apple, sincronización | requiere backend | ❌ v2 |
| Premium | compra única para subir imágenes propias sin límite | ❌ v3 (en v1 imágenes propias gratis) |
| 24 idiomas | | ES + EN en v1 |

**Estilo visual:** turquesa `#42C0CC` (principal), `#2DA6B2` (presionado), `#7ADFE8` (claro),
`#0B343B` (texto oscuro), `#F2F9FA` (fondo suave). Esquinas muy redondeadas, tipografía sans geométrica en negrita.

**Lo que NO copiamos** (para poder publicar en Google Play): el nombre "Reach", el eslogan
"Countdown to Connect", el logo, la fuente Pangram Sans (es de pago; usaremos una alternativa libre como
*Outfit* o *Plus Jakarta Sans*), las fotos y los textos de la tienda. La paleta la ajustamos un poco
para que la marca sea propia.

## 2. Arquitectura

```
app/
 ├─ ui/            Compose: pantallas, componentes, tema (Material 3)
 ├─ domain/        modelos + casos de uso (cálculo de cuenta regresiva, repetición)
 ├─ data/          Room (eventos), DataStore (ajustes), almacenamiento de imágenes
 ├─ widget/        Jetpack Glance (widgets de pantalla de inicio)
 └─ notifications/ AlarmManager + WorkManager
```
- **Patrón:** MVVM + flujo de datos en una sola dirección (`StateFlow`), Hilt para inyección de dependencias.
- **Librerías:** Compose BOM, Navigation Compose, Room, DataStore, Glance, WorkManager, Coil (imágenes),
  kotlinx-datetime o `java.time`.
- **minSdk 26**, targetSdk = la última versión estable.
- **Modelo `Event`:** `id, title, emoji?, startAt, endAt?, allDay, timeZone, repeat(NONE|DAILY|WEEKLY|BIWEEKLY|MONTHLY|YEARLY),
  background(Color|Preset|LocalImage), createdAt, updatedAt`. Dejamos `remoteId` y `ownerId` vacíos,
  listos para la v2.

## 3. Tareas

### Fase 0: Base del proyecto
- [x] T0.1 Crear proyecto Gradle (Kotlin DSL, catálogo de versiones), módulo `app`, configurar Hilt
- [x] T0.2 Tema Material 3: colores, tipografía libre, formas y modo oscuro
- [x] T0.3 Navegación: Inicio, Crear/Editar, Detalle, Ajustes; barra inferior
- [x] T0.4 Configurar ktlint/detekt y pruebas unitarias; CI con GitHub Actions (opcional)

### Fase 1: Datos y lógica
- [x] T1.1 Entidad Room `EventEntity`, DAO, migraciones y repositorio
- [x] T1.2 `CountdownCalculator`: días, horas, minutos y segundos restantes, respetando zona horaria y eventos de todo el día
- [x] T1.3 `RecurrenceResolver`: calcular la siguiente ocurrencia para cada tipo de repetición (cuidado con el 29 de febrero y el día 31)
- [x] T1.4 Clasificar eventos en Próximos y Alcanzados (los que se repiten nunca pasan a Alcanzados)
- [x] T1.5 Pruebas unitarias de T1.2 a T1.4

### Fase 2: Pantallas principales
- [x] T2.1 Inicio: pestañas Próximos/Alcanzados con lista de tarjetas
- [x] T2.2 Componente `EventCard`: fondo, capa oscura, título, fechas, número grande y botón compartir
- [x] T2.3 Tarjeta vacía `+` y estado vacío para la primera vez
- [x] T2.4 Crear/Editar evento: título, emoji, fecha/hora, fecha de fin, repetición, fondo
- [x] T2.5 Selector de fondo: 8 colores, 6 degradados (en lugar de fotos incluidas, sin licencias) e imagen propia (Photo Picker, reducida a 1600 px y comprimida). Sin pantalla de recorte: la imagen se centra y recorta al mostrarse
- [x] T2.6 Detalle del evento: cuenta regresiva en vivo con segundos, editar, borrar (con confirmación) y compartir como texto
- [x] T2.7 Gestos: deslizar para borrar con opción de deshacer; mantener presionado para editar

### Fase 3: Widgets (función estrella)
- [x] T3.1 Widget pequeño 2x2 con un evento: días restantes y título (un único widget que se adapta al tamaño)
- [x] T3.2 Versión ancha 4x2 del mismo widget, con fondo de color, degradado o foto
- [x] T3.3 Widget de lista con los próximos 3 a 5 eventos
- [x] T3.4 Pantalla de configuración del widget: elegir evento o "el próximo"
- [x] T3.5 Modo en vivo: `Chronometer` en cuenta regresiva dentro de RemoteViews (Glance no refresca cada segundo)
- [x] T3.6 Actualización: a medianoche, al empezar o terminar un evento, al cambiar un evento, al reiniciar y al cambiar hora o zona horaria. Sin permiso de alarmas exactas puede retrasarse hasta 10 min (se resuelve en la Fase 4)

### Fase 4: Notificaciones
- [x] T4.1 Ajuste global de recordatorio: ninguno, 15 min, 1 h, 1 día o 2 días antes (por defecto 1 día). En eventos de todo el día los avisos son a las 9:00
- [x] T4.2 Una sola alarma con el próximo aviso. Exacta si el usuario concede `SCHEDULE_EXACT_ALARM` (desde Ajustes); si no, Android puede retrasarla unos minutos
- [x] T4.3 Volver a programar tras reiniciar (`BOOT_COMPLETED`), al editar o al repetirse un evento
- [x] T4.4 Pedir permiso `POST_NOTIFICATIONS` (Android 13+) con una tarjeta explicativa en Inicio y desde Ajustes, nunca al abrir la app
- [x] T4.5 Notificación cuando el evento llega: "¡Llegó el día!"

### Fase 5: Compartir y extras
- [x] T5.1 Compartir la tarjeta como imagen de 1080×1350, dibujada con Canvas (fondo, número, título y fecha), junto con el texto
- [x] T5.2 Compartir como texto: "Faltan 10 días para…"
- [x] T5.3 Ajustes: tema (sistema, claro u oscuro), exportar e importar una copia en JSON con fotos incluidas. El formato de fecha sigue el idioma del teléfono
- [x] T5.4 Traducciones en ES y EN, con idioma por app en Android 13+
- [x] T5.5 Accesibilidad: descripciones para TalkBack, contraste mínimo 4.5:1 comprobado en pruebas y texto al 200 % sin cortes

### Fase 6: Publicación
- [x] T6.1 Icono original (cronómetro, `docs/brand/icon.svg`) adaptable y monocromo, pantalla de inicio y nombre final
- [x] T6.2 Clave de subida (firma de apps de Play), R8 probado en el emulador y AAB generado
- [x] T6.3 Política de privacidad (`docs/privacy`), ficha en ES/EN, gráficos y capturas (`docs/play-store`)
- [ ] T6.4 Pruebas internas y cerradas en Play Console (requiere tu cuenta; pasos en `docs/RELEASE.md`)

### v2 (después): versión social
Backend (Supabase o Firebase) · inicio de sesión con Google · amigos por nombre de usuario ·
eventos compartidos con enlace profundo · sincronización en tiempo real · avatares en la tarjeta ·
FCM · Premium con Google Play Billing.

## 4. Orden sugerido y entregables
1. **Hito 1 (Fases 0 a 2):** app usable para crear y ver eventos con imágenes
2. **Hito 2 (Fases 3 y 4):** widgets y recordatorios, que hacen la app competitiva
3. **Hito 3 (Fases 5 y 6):** pulido y publicación en Google Play

## 5. Decisiones pendientes
- ~~Nombre final~~ → Countdown to Event
- ~~Fuente y paleta~~ → Outfit (OFL) y turquesa propio `#1A9AA6`
- ¿Imágenes incluidas? Necesitamos fotos libres de derechos (Unsplash o Pexels) o ilustraciones propias
