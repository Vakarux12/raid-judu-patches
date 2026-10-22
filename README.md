# Raid for JUDU

Morphe patch that brings raid.lt ticket inspector reports into the JUDU app.

[Add to Morphe](https://morphe.software/add-source?github=Vakarux12/raid-judu-patches)

Reports show on JUDU's own map as red dots labelled with the report time, from zoom 11 in.
JUDU's map is Dart (flutter_map_vector_tiles), so the patch points the style url in libapp.so at a small
server inside the app. It serves JUDU's style plus a raid layer, and builds the raid vector tiles from raid.lt.
The dots are fetched each time the map loads its style, a map left open does not pick up new reports.

On top there is a pill with the count of active reports in your city and the nearest one, red when one is
within 1 km, live every minute. Tap it for the full list sorted by distance, drag it anywhere, the spot is remembered.

Also spoofs JUDU's Play signing cert in-app for its Google keys. Google sign-in still won't work on a re-signed build, use email login.

<!-- PATCHES_START EXPANDED -->
<!-- PATCHES_END -->

## Building

    echo "sdk.dir=$HOME/Android/Sdk" > local.properties
    GITHUB_ACTOR=Vakarux12 GITHUB_TOKEN=$(gh auth token) ./gradlew buildAndroid
    java21 -jar morphe-cli.jar patch -p patches/build/libs/patches-*.mpp -o judu-raid.apk com.judu.xapk

## License

GPLv3
