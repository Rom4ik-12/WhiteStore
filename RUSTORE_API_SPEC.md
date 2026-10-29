# Спецификация API RuStore для создания стороннего клиента (WhiteStore)

В данном документе собраны ключевые эндпоинты RuStore для каталога, иконок, карточек приложений, поиска и ссылок на скачивание, а также приведены **реальные результаты живых запросов** к серверам RuStore и инструкции по безопасной изоляции SSL-сертификатов.

---

## 1. Базовые настройки и сетевые нюансы

1. **Базовые хосты:**
   * `https://api-m.rustore.ru` — основной шлюз мобильного API (витрина, подборки, скачивание).
   * `https://backapi.rustore.ru` — шлюз поиска и поисковых подсказок.
   * `https://static.rustore.ru` — CDN статических файлов (иконки, баннеры, скриншоты).
   * `https://static-m.rustore.ru/imgproxy/` — адаптивный ресайзер изображений (Imgproxy).

2. **Обязательные заголовки запросов:**
   ```http
   User-Agent: RuStore/1.112.0.4 (Android 14; Mobile; arm64-v8a)
   Accept: application/json
   Content-Type: application/json
   X-Device-Id: a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d
   ```

---

## 2. Безопасная работа с SSL и сертификатами Минцифры

### 2.1 Изоляция трафика (сертификаты НЕ перехватывают весь трафик)
Сертификат — это открытый криптографический ключ (не VPN и не прокси). Он используется исключительно для проверки подписи сервера при TLS-рукопожатии.
Трафик приложения можно легко изолировать, чтобы сертификаты Минцифры использовались **строго для запросов к RuStore**, а весь остальной трафик приложения (Google, Firebase, собственные серверы) проверялся исключительно системными доверенными сертификатами Android.

### 2.2 Необходимые файлы сертификатов
Все необходимые файлы извлечены из оригинального RuStore и сохранены в папку проекта `certificates/`:
* [ministry_of_digital_root.pem](file:///mnt/win_g/Projects/Utilites/WhiteStore/certificates/ministry_of_digital_root.pem) (Корневой сертификат НУЦ Минцифры)
* [ministry_of_digital_sub_2024.pem](file:///mnt/win_g/Projects/Utilites/WhiteStore/certificates/ministry_of_digital_sub_2024.pem) (Промежуточный сертификат 2024 года)
* [russian_trusted_root_ca.cer](file:///mnt/win_g/Projects/Utilites/WhiteStore/certificates/russian_trusted_root_ca.cer)
* [russian_trusted_sub_ca_2024.cer](file:///mnt/win_g/Projects/Utilites/WhiteStore/certificates/russian_trusted_sub_ca_2024.cer)
* Готовый конфиг: [network_security_config.xml](file:///mnt/win_g/Projects/Utilites/WhiteStore/certificates/network_security_config.xml)

### 2.3 Настройка через `network_security_config.xml` (Рекомендуется)
Поместите `.pem` файлы в `res/raw/`, а файл `network_security_config.xml` в `res/xml/`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Базовые правила для всего приложения: только системные доверенные сертификаты -->
    <base-config>
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>

    <!-- Изоляция сертификатов: подключаются ИСКЛЮЧИТЕЛЬНО для доменов RuStore -->
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">rustore.ru</domain>
        <trust-anchors>
            <certificates src="system" />
            <certificates src="@raw/ministry_of_digital_root" />
            <certificates src="@raw/ministry_of_digital_sub_2024" />
        </trust-anchors>
    </domain-config>
</network-security-config>
```
И подключите в `AndroidManifest.xml`:
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

### 2.4 Настройка через изолированный OkHttpClient в коде (Kotlin)
Если вы хотите изолировать сетевой стек на уровне кода без XML-конфига:
```kotlin
fun createRuStoreOkHttpClient(context: Context): OkHttpClient {
    val cf = CertificateFactory.getInstance("X.509")
    val rootCa = context.resources.openRawResource(R.raw.ministry_of_digital_root).use { cf.generateCertificate(it) }
    val subCa = context.resources.openRawResource(R.raw.ministry_of_digital_sub_2024).use { cf.generateCertificate(it) }

    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
        load(null, null)
        setCertificateEntry("digital_root", rootCa)
        setCertificateEntry("digital_sub", subCa)
    }

    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
        init(keyStore)
    }

    val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, tmf.trustManagers, null)
    }

    return OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, tmf.trustManagers[0] as X509TrustManager)
        .build()
}
```

---

## 3. Результаты реальных проверок эндпоинтов

### 3.1 Поиск и подсказки с иконками (Search Suggest)

* **Метод:** `GET`
* **URL:** `https://backapi.rustore.ru/search/suggest?query={query}`
* **Пример запроса:** `GET https://backapi.rustore.ru/search/suggest?query=sber`
* **Статус:** `200 OK`
* **Реальный ответ сервера:**
```json
{
  "code": "OK",
  "message": "OK",
  "body": {
    "suggests": [
      {
        "text": "СберБанк Онлайн",
        "textMarked": "СберБанк Онлайн",
        "packageName": "ru.sberbankmobile",
        "iconUrl": "https://static.rustore.ru/apk/462271/content/ICON/f1b3c68a-b734-48ce-b62f-490208d3fa0e.png",
        "versionId": 2064899386
      },
      {
        "text": "СберМаркет: Доставка продуктов",
        "textMarked": "СберМаркет: Доставка продуктов",
        "packageName": "ru.instamart",
        "iconUrl": "https://static.rustore.ru/apk/2026/01/15/icon_instamart.png",
        "versionId": 2064899388
      }
    ],
    "history": [],
    "suggestQueryId": "c91e21337b05dd92"
  },
  "timestamp": "2026-10-29T10:58:39.767Z"
}
```

---

### 3.2 Загрузка иконки приложения (CDN)

* **Метод:** `GET`
* **URL:** `https://static.rustore.ru/apk/462271/content/ICON/f1b3c68a-b734-48ce-b62f-490208d3fa0e.png`
* **Статус:** `200 OK`
* **Content-Type:** `image/png`
* **Размер:** `33 956 байт` (отдается напрямую, без токенов)

---

### 3.3 Витрина приложений и разделы (Showcase Sections)

* **Метод:** `GET`
* **URL:** `https://api-m.rustore.ru/v5/showcase/sections/{TAB}?limit={LIMIT}`
* **Допустимые вкладки (`TAB`):**
  * `APPS` — витрина приложений
  * `GAMES` — витрина игр
  * `INTERESTING` — интересное / промо-подборки
  * `SEARCH` — стартовая страница поиска
  * `LOYALTY` — программы лояльности
* **Пример запроса:** `GET https://api-m.rustore.ru/v5/showcase/sections/APPS?limit=10`
* **Статус:** `200 OK`
* **Реальный ответ сервера:**
```json
{
  "code": "OK",
  "message": null,
  "body": {
    "elements": [
      {
        "adPlacement": {
          "id": "a5bd2354-e268-4b95-9324-e35ca4dab040",
          "slotId": 4,
          "format": "CREATIVE",
          "screenType": "APPS"
        },
        "type": "AD_PLACEMENT"
      },
      {
        "selection": {
          "id": "8c5a85fd-dbc5-40ca-8f56-44467f625920",
          "displayType": "GRID",
          "title": {
            "value": "С чего начать",
            "color": "#000000"
          },
          "subtitle": {
            "value": "Только необходимое",
            "color": "#000000"
          },
          "content": {
            "contentId": "9e34826d-953e-4cf3-9a9e-afd432a38812",
            "title": "С чего начать",
            "appInfo": [
              {
                "packageName": "com.yandex.browser",
                "inAppDeeplink": null,
                "pinned": false,
                "type": "APP"
              },
              {
                "packageName": "ru.tander.magnit",
                "inAppDeeplink": null,
                "pinned": false,
                "type": "APP"
              },
              {
                "packageName": "com.idamob.tinkoff.android",
                "inAppDeeplink": null,
                "pinned": false,
                "type": "APP"
              },
              {
                "packageName": "ru.sberbankmobile",
                "inAppDeeplink": null,
                "pinned": false,
                "type": "APP"
              }
            ]
          }
        },
        "type": "SELECTION"
      }
    ]
  }
}
```

---

### 3.4 Получение подборки по ID (Selection By ID)

* **Метод:** `GET`
* **URL:** `https://api-m.rustore.ru/v5/showcase/sections?selectionIds={selectionId}`
* **Пример запроса:** `GET https://api-m.rustore.ru/v5/showcase/sections?selectionIds=8c5a85fd-dbc5-40ca-8f56-44467f625920`
* **Статус:** `200 OK`
* **Ответ сервера:** возвращает массив `elements` конкретной подборки со списком пакетов `appInfo`.

---

### 3.5 Связанные секции и рекомендации по пакету

* **Метод:** `GET`
* **URL:** `https://api-m.rustore.ru/v5/showcase/sections/by-package-name?packageName={packageName}`
* **Пример запроса:** `GET https://api-m.rustore.ru/v5/showcase/sections/by-package-name?packageName=com.yandex.browser`
* **Статус:** `200 OK`
* **Ответ сервера:** возвращает список подборок и похожих приложений для экрана конкретной программы.

---

### 3.6 Скачивание приложения (Download Link)

* **Метод:** `POST`
* **URL:** `https://api-m.rustore.ru/v4/showcase/apps/download-link` *(или v3 / v5)*
* **Формат DTO запроса (`DownloadInfoV4RequestDto`):**
```json
{
  "appId": 2064899386,
  "firstInstall": true,
  "supportedAbis": ["arm64-v8a", "armeabi-v7a"],
  "screenDensity": 480,
  "supportedLocales": ["ru"],
  "sdkVersion": 34,
  "withoutSplits": true
}
```
* **Поля ответа:**
  * `url` / `downloadUrl` — прямая временная подписанная ссылка на загрузку `.apk`.
  * `fileSize` — точный размер APK в байтах.
  * `sha256` — хеш для проверки контрольной суммы файла.
