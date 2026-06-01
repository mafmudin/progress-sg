<link rel="shortcut icon" type="image/x-icon" href="favicon.ico">

# Progress-svg-gif
android progress dialog dengan menggunakan file svg atau gif

## Kompatibilitas / Migrasi ke 1.0.0

Versi `1.0.0` membenahi penamaan method yang sebelumnya salah eja (breaking change):

| 0.0.1 (lama)                   | 1.0.0 (baru)                   |
|--------------------------------|--------------------------------|
| `dissmis()`                    | `dismiss()`                    |
| `setCancleable(...)`           | `setCancelable(...)`           |
| `setCancleOnTouchOutside(...)` | `setCancelOnTouchOutside(...)` |

Selain itu kini tersedia **Builder** yang fluent. Jika belum bisa migrasi, pin versi lama:
`com.github.mafmudin:progress-sg:0.0.1`.

### cara menambahkan progress-svg-gif ke project android studio dengan menggunakan gradle
* tambahkan kode di bawah kedalam file ```build.gradle``` (root project)

```
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
   }
}
```

* lalu, tambahkan dependesi berikut kedalam file ```build.gradle``` (module aplikasi)

```
dependencies {
  implementation 'com.github.mafmudin:progress-sg:1.0.0'
}
```

### cara menambahkan progress-svg-gif ke project android studio dengan menggunakan maven
* tambahkan *jitpack repository* ke dalam build file maven

```
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```

* tambahkan dependesinya

```
<dependency>
  <groupId>com.github.mafmudin</groupId>
  <artifactId>progress-sg</artifactId>
  <version>1.0.0</version>
</dependency>
```

## cara menggunakan progress-svg-gif
* pastikan sudah disiapkan file svg di dalam folder assets

<img src='https://github.com/Mafmudin/myassets/blob/master/images/assets.png?raw=true' alt="contoh tampilan susuna folder assets"/>

*contoh tampilan lokasi file svg dalam folder assets, buatlah folder assets jika belum tersedia* (<a href='https://stackoverflow.com/questions/26706843/adding-an-assets-folder-in-android-studio?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa'>Link cara menambahkan folder assets</a>)

* gunakan progress-svg-gif seperti penggunaan progress dialog, contoh penggunaannya yaitu:

```
// Cara 1 — setter (nama method sudah benar di 1.0.0)
ProgressSvg progressSvg = new ProgressSvg(MainActivity.this);
progressSvg.setSvgAssets("loading_circle.svg");
progressSvg.setMessage(getResources().getString(R.string.please_wait));
progressSvg.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccent));
progressSvg.setTextSize(11.0f);
progressSvg.setBackgroundColor(Color.GRAY);
progressSvg.setCancelable(false);
progressSvg.setCancelOnTouchOutside(false);
progressSvg.show();

// menyembunyikan progress:
progressSvg.dismiss();

// Cara 2 — Builder
ProgressSvg p = new ProgressSvg.Builder(MainActivity.this)
        .svg("loading_circle.svg")
        .message(getResources().getString(R.string.please_wait))
        .textColor(Color.BLACK)
        .cancelable(false)
        .build();
p.show();
```

yap, *enjoy* penggunaan progress-svg-gif :)


## Cara menggunakan file gif
* siapkan file gif dan simpan di dalam folder drawable (res/drawable)

<img src='https://github.com/Mafmudin/myassets/blob/master/images/gif.png?raw=true' alt="simpan file gif di dalam folder drawable"/>

*contoh tampilan susuan folder drawable*

* contoh pennggunaan progress-svg-gif yaitu: 

```
// setter
ProgressGif progressGif = new ProgressGif(MainActivity.this);
progressGif.setGifResource(R.drawable.mag);
progressGif.setMessage(getResources().getString(R.string.searching));
progressGif.show();

// Builder
ProgressGif g = new ProgressGif.Builder(MainActivity.this)
        .gif(R.drawable.mag)
        .message(getResources().getString(R.string.searching))
        .build();
g.show();
```

#### Download loading / progress svg, gif atau apng file di link berikut
([Loading.io](https://loading.io/))
