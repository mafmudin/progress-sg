<link rel="shortcut icon" type="image/x-icon" href="favicon.ico">

# Progress-svg-gif
android progress dialog dengan menggunakan file svg atau gif

## Kompatibilitas / Migrasi ke 2.0.0

Mulai `2.0.0` library ditulis dalam **Kotlin** dan rendering memakai **Coil**. `ProgressSvg`
dan `ProgressGif` digabung menjadi satu kelas **`FoProgressDialog`** (breaking change):

| Lama (1.0.0)                                            | Baru (2.0.0)                                                  |
|---------------------------------------------------------|---------------------------------------------------------------|
| `new ProgressSvg(ctx)` + `setSvgAssets("x.svg")`        | `new FoProgressDialog.Builder(ctx).svg("x.svg").build()`      |
| `new ProgressGif(ctx)` + `setGifResource(R.drawable.x)` | `new FoProgressDialog.Builder(ctx).gif(R.drawable.x).build()` |

- SVG tetap dari folder `assets/`; GIF tetap dari `res/drawable`.
- `dismiss()`, `setMessage`, `setCancelable`, dst. tetap tersedia (lewat properti/Builder).
- Jika belum bisa migrasi, pin versi lama: `com.github.mafmudin:progress-sg:1.0.0`.

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
  implementation 'com.github.mafmudin:progress-sg:2.0.0'
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
  <version>2.0.0</version>
</dependency>
```

## cara menggunakan progress-svg-gif
* pastikan sudah disiapkan file svg di dalam folder assets

<img src='https://github.com/Mafmudin/myassets/blob/master/images/assets.png?raw=true' alt="contoh tampilan susuna folder assets"/>

*contoh tampilan lokasi file svg dalam folder assets, buatlah folder assets jika belum tersedia* (<a href='https://stackoverflow.com/questions/26706843/adding-an-assets-folder-in-android-studio?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa'>Link cara menambahkan folder assets</a>)

* gunakan progress-svg-gif seperti penggunaan progress dialog, contoh penggunaannya yaitu:

```
// Java — Builder
FoProgressDialog p = new FoProgressDialog.Builder(MainActivity.this)
        .svg("loading_circle.svg")
        .message(getResources().getString(R.string.please_wait))
        .textColor(ContextCompat.getColor(MainActivity.this, R.color.colorAccent))
        .textSize(11f)
        .backgroundColor(Color.GRAY)
        .build();
p.show();
p.dismiss();
```

```
// Kotlin — DSL
val p = foProgressDialog(this) {
    svg("loading_circle.svg")
    message = getString(R.string.please_wait)
    textColor = Color.BLACK
    cancelable = false
}
p.show()
p.dismiss()
```

yap, *enjoy* penggunaan progress-svg-gif :)


## Cara menggunakan file gif
* siapkan file gif dan simpan di dalam folder drawable (res/drawable)

<img src='https://github.com/Mafmudin/myassets/blob/master/images/gif.png?raw=true' alt="simpan file gif di dalam folder drawable"/>

*contoh tampilan susuan folder drawable*

* contoh pennggunaan progress-svg-gif yaitu: 

```
// Java
FoProgressDialog g = new FoProgressDialog.Builder(MainActivity.this)
        .gif(R.drawable.mag)
        .message(getResources().getString(R.string.searching))
        .build();
g.show();

// Kotlin
foProgressDialog(this) {
    gif(R.drawable.mag)
    message = getString(R.string.searching)
}.show()
```

#### Download loading / progress svg, gif atau apng file di link berikut
([Loading.io](https://loading.io/))
