# Edge Detection Viewer

Android app with Camera2, JNI (C++), OpenCV (optional), and OpenGL ES. Minimal web viewer in TypeScript.

## Structure
- `app/` Android app (Kotlin)
- `app/src/main/cpp/` C++ (NDK, JNI, OpenCV optional)
- `web/` TypeScript web viewer

## Android setup
1) Android Studio + SDK + NDK + CMake
2) (Optional) Download OpenCV Android SDK and note `OpenCV-android-sdk/sdk/native/jni`
3) Build with OpenCV:
   - In Android Studio: add Gradle arg: `-PopencvDir=PATH_TO_OpenCV_android_sdk\\sdk\\native\\jni`
   - Or in `local.properties` add: `opencv.dir=PATH` and pass to Gradle as `-PopencvDir=%opencv.dir%`

Without OpenCV, native falls back to gray/passthrough.

## Run
- Build and run on device (minSdk 24)
- Toggle button switches edges/gray
- FPS shows simple frame count per second

## Web viewer
```
cd web
npm i
npm run build
```
Open `web/public/index.html` (served) to see dummy frame and stats.

## Notes
- OpenGL ES 2.0 textured quad
- JNI method `processEdgesRgba` (RGBA in/out)
- Simple YUV_420_888 → RGBA converter (CPU)

## Screenshots / GIF
- Add your device screenshot or GIF here (app in edges and gray modes).
- Example placeholders:
  - `docs/screenshot_edges.png`
  - `docs/screenshot_gray.png`

## Web viewer note
- Built with TypeScript. Output goes to `web/dist/`.
- Use any static server to open `web/public/index.html` (it references `../dist/main.js`).