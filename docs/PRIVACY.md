# Veri ve gizlilik

Dolmiş bağımsız, salt okunur bir ulaşım istemcisidir. Belediye veya mevcut uygulamanın resmî ürünü değildir.

- Hesap, reklam veya analiz SDK’sı kullanılmaz.
- Seçilen hat ve favoriler yalnız uygulamanın bu telefondaki yerel alanında saklanır.
- Konum yalnız kullanıcı istediğinde, ön planda alınır. Yakındaki durak sıralaması telefonda hesaplanır; konum belediyenin ulaşım API’sine gönderilmez.
- Haritayı konumuna merkezlersen OpenStreetMap döşeme istekleri görüntülenen bölgeyi harita sağlayıcısına gösterebilir. Her ağ bağlantısında sağlayıcı IP adresini de görebilir.
- Belediye yanıtlarından yalnız gerekli ulaşım alanları seçilir. Şoför, gelir ve yolcu sayaçları gibi gereksiz `raw` alanlar uygulama durumuna veya dosyaya kaydedilmez.
- Canlı konumlar diske kaydedilmez. İnternetsiz veya veri servisi kapalıyken canlı takip çalışmaz; uygulama bunu açıkça bildirir.
- İki dakikadan eski konum eski olarak işaretlenir; on dakikadan eski/zamanı olmayan konum haritada gösterilmez.

Katalog, belediyenin halka açık servisinden 24 Ekim 2026’da alınan 115 hat ve 1060 durak kaydını içerir. Kaynak: [Diyarbakır Büyükşehir Belediyesi ulaşım hizmetleri](https://www.diyarbakir.bel.tr/hizmetler/ulastirma-hizmetleri). Bu veriler için ayrıca açık bir yeniden dağıtım lisansı doğrulanmadı; belediye verisi ve üçüncü taraf harita/lisansları uygulama kodundan ayrıdır.

Harita [OpenStreetMap](https://www.openstreetmap.org/copyright) ve [Leaflet](https://leafletjs.com/) kullanır. Leaflet lisansı `app/src/main/assets/vendor/LEAFLET-LICENSE` içinde korunur. AndroidX/Jetpack Compose ve Kotlin kendi üçüncü taraf lisanslarına tabidir.
