# Dolmiş 0.4.0 — Material 3 + erişilebilirlik

Android arayüzü Jetpack Compose ve gerçek `androidx.compose.material3` bileşenleriyle yeniden yazıldı. shadcn M3E’nin Material 3 renk rolleri, yüzeyleri ve bileşen düzenleri tasarım referansı olarak kullanıldı. React/shadcn paketi Android uygulamasına eklenmez.

- `Scaffold`, `CenterAlignedTopAppBar`, `NavigationBar` / `NavigationBarItem`
- `SingleChoiceSegmentedButtonRow`, `SegmentedButton`, `AssistChip`, `FilterChip`
- `ElevatedCard`, `OutlinedCard`, `ListItem`, `OutlinedTextField`
- `ModalBottomSheet`, `SnackbarHost`, Material ilerleme göstergeleri
- Yerel Android klavyesi, geri hareketi ve erişilebilir bileşen adları

Harita tek başına yerel bir WebView içinde Leaflet ile çizilir; gezinme, arama, kartlar ve paneller Compose’tur. Önceki sürümün favorileri, aynı uygulamanın yerel depolamasından ilk harita açılışında aktarılır. Aynı paket adı ve imza anahtarı korunur.

## Derleme

JDK 17, Android SDK platform 36 / build-tools 35.0.0 ve Python gerekir. Gradle wrapper 8.13, Android Gradle Plugin 8.10.1, Kotlin 2.2.21 ve Compose BOM 2025.11.01 sabitlendi. Material 3 sürümü BOM ile 1.4.0.

```powershell
python -X utf8 build.py --sdk C:/path/to/Android/Sdk --signing-directory C:/private/dolmis-signing
```

İlk derlemede Gradle bağımlılıkları indirilir. Android 8+ hedeflenir; cihaz ve ağ doğrulaması derlemeden ayrıdır. İmza klasörünü koruyun; kaynak arşivine/parolaları eklemeyin. Farklı anahtarla imzalanan APK bu önizlemenin üzerine güncelleme olarak kurulamaz.

Kaynak yalnız bağımsız istemci kodu ve arındırılmış halka açık katalogları içerir; orijinal uygulamanın kodu/anahtarları içerilmez. Konum ve canlı araç verileri diske yazılmaz; belediye yanıtlarının yalnız seçili ulaşım alanları uygulama durumuna alınır.

## Referanslar

- [Android Compose Material 3](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Navigation bar](https://developer.android.com/develop/ui/compose/components/navigation-bar)
- [Chips](https://developer.android.com/develop/ui/compose/components/chip)
- [Bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets)
- [shadcn M3E kaynakları](https://github.com/Crysta1221/shadcn-m3e) — button, card, chip ve navigation örnekleri incelendi; web bileşenlerinin yerine Android karşılıkları kullanıldı.
- [Leaflet](https://leafletjs.com/) — BSD-2-Clause lisansı assets/vendor içinde.

