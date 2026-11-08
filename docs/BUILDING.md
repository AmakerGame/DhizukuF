# Building DhizukuF

## Requirements

- JDK 17+ (CI uses 21)
- Android SDK matching the project `compileSdk`
- Network access for Gradle dependencies

## Release APK

```sh
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/`.

## dish client

```sh
./dish-client/build.sh
```

See also [DISH.md](DISH.md).

## Modules

| Module | Role |
| --- | --- |
| `app` | Main DhizukuF application |
| `hidden-api` | Compile-only hidden API stubs |
| `hub` | Compatibility hub APK packaged as asset |


## Tips

- First clone: let Gradle download the wrapper distribution.
- Prefer a release keystore for Play / sideload builds; debug signing is fine for local tests.
