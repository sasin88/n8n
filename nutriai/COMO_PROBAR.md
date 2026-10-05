# Cómo probar NutriAI (sin saber programar)

NutriAI es una app de **Android**. Un iPhone no puede instalar apps de Android (archivos `.apk`).

## Paso 1 · Descargar la app

1. Abre en el navegador: **https://github.com/sasin88/n8n/releases/tag/nutriai-latest**
2. En "Assets", toca **NutriAI.apk** para descargarla.

## Opción A · En un teléfono Android (la mejor)

1. Desde el teléfono Android, abre el enlace de arriba y descarga `NutriAI.apk`.
2. Ábrela. Android te pedirá permitir "Instalar apps desconocidas" para el navegador: acéptalo.
3. Toca **Instalar** y luego **Abrir**.

> Android puede avisar de que la app no viene de Google Play. Es normal: es una versión de prueba.

## Opción B · Desde tu iPhone, en el navegador

Hay servicios que ejecutan apps de Android en una página web, por ejemplo **Appetize.io**
(no he verificado sus precios ni límites actuales: revísalos en su web).

1. Descarga `NutriAI.apk` en el iPhone (se guarda en la app Archivos) o en una computadora.
2. Entra en appetize.io, crea una cuenta y sube el archivo.
3. Abre el enlace que te dan: verás la app funcionando en un teléfono virtual.

La cámara no funciona en estos simuladores; usa "Elegir de la galería" o "Escribir lo que comí".

## Opción C · En una computadora

Instala **Android Studio** (gratis), abre *Device Manager*, crea un teléfono virtual y arrastra
`NutriAI.apk` encima de la ventana del emulador.

## Qué esperar

- **Modo demostración:** mientras no se conecte la IA real, el análisis de fotos devuelve un
  resultado **simulado** (siempre arroz, frijoles, pollo y ensalada) y la app lo indica en pantalla.
  Todo lo demás funciona de verdad: perfiles, registro manual, cálculos, historial, peso, privacidad.
- Para activar la IA real hay que desplegar el backend (carpeta `backend/`) y poner su URL en
  GitHub → *Settings → Secrets and variables → Actions → Variables* con el nombre
  `NUTRIAI_AI_BACKEND_URL`. El siguiente APK ya usará la IA real.
