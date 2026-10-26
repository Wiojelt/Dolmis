# 0.3.0 doğrulama sınırı

27 Ekim 2026’da Android 15 emülatöründe bu imzalı APK kurulup çalıştırıldı. canlı ekranda araçlar haritada ve listede görüldü. Arama, yön filtresi, favorilerin önceki sürümden aktarımı/yeniden açılışta korunması ve Havalimanı durağının hat paneli kontrol edildi.

Release derlemesi, lintVital, v2/v3 imza ve ZIP/16 KB hizalama doğrulandı. Cihazdan geri alınan APK’nin SHA-256 değeri dağıtılan dosyayla aynıdır. Makine okunur kayıt: [verification.json](verification.json).

Fiziksel telefon, bütün 115 hat, farklı font ölçekleri, küçük ekranlar ve bütün şebekeler doğrulanmadı. Telefonunda test etmek için bu sürüm bir önizlemedir. Veri hizmetinin gelecekteki sürekliliği garanti edilmez.

Diyarkart/bakiye ekranı eklendi; gerçek ödeme son adımı işletmeci ekranına yönlendirilir. QR/NFC ve hesap işlemleri için işletmeci entegrasyonu gerekir. Sefer saati servisinden kullanılabilir bir saat tablosu doğrulanamadı, bu nedenle resmî sayfaya bağlantı sunulur. Varış tahmini yalnız canlı araç, durak ve hız verisi varsa hesaplanır. Orijinal uygulamanın sunucu tarafı kök nedeni için kesin çözüm iddiası yoktur.
