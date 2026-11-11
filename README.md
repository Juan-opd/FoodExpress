# FoodExpress — Login + SQLite

Proyecto Android en Kotlin y Jetpack Compose. La base de datos local SQLite se crea automáticamente como `foodexpress.db` en el almacenamiento privado de la aplicación.

## Tablas
- `users`: cuentas de usuario (correo único, nombre y hash SHA-256 de contraseña).
- `categories`: categorías iniciales.
- `products`: catálogo inicial cargado automáticamente.
- `favorites`: favoritos por correo de usuario.
- `cart_items`: estructura preparada para persistir el carrito.
- `orders`: cabecera de pedidos por usuario.
- `order_items`: líneas de cada pedido.

## Funcionalidad conectada a SQLite
- Registro e inicio de sesión consultan la tabla `users`.
- Favoritos se guardan por usuario en `favorites`.
- Historial y detalle de pedidos se guardan en `orders` y `order_items`.
- El catálogo se inserta/actualiza al iniciar en `products`.

## Limitaciones importantes
Esta es una base de datos local, propia de cada instalación de la app. No sincroniza cuentas entre teléfonos ni sustituye un servidor. El hash SHA-256 es una mejora básica para un prototipo, no un sistema de autenticación de producción. La tabla `cart_items` está creada como estructura, pero el carrito actual sigue viviendo en memoria mientras la app está abierta.

## Abrir en Android Studio
1. Extrae el ZIP.
2. Abre la carpeta `FoodExpress`, la que contiene `settings.gradle.kts`.
3. Espera a que termine la sincronización de Gradle.
4. Selecciona un emulador y ejecuta el módulo `app`.

Requisitos configurados: JDK 17, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, compileSdk 35, minSdk 24.
