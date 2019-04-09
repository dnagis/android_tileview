TileView remasterisé pour passer dans l'aosp

adb uninstall tileview.demo
adb install out/target/product/mido/system/app/tv_vvnx/tv_vvnx.apk
pm grant tileview.demo android.permission.READ_EXTERNAL_STORAGE

j'ai enlevé les jpg de assets/tiles/ of course, je vais pas bloater mon github

HOWTO passer du projet tileview vers qq chose qui compile dans l'aosp/development/samples/

point de départ -> https://github.com/moagrius/TileView.git
le coeur fonctionnel de ce projet est dans tileview/src/main -> je passe son contenu dans un nouveau dossier de development/samples/
j'y copie l'Android.mk d'AlrmVvnx, je change LOCAL_PACKAGE_NAME  

make TileView -> erreur: ./development/samples/TileView/tileview/src/main/Android.mk:7: error: FindEmulator: find: development/samples/TileView/tileview/src/main/src: No such file or directory
Android.mk:7 --> LOCAL_SRC_FILES := $(call all-java-files-under, src) , je modifie src pour java (LOCAL_SRC_FILES := $(call all-java-files-under, java))

erreurs type import android.support.annotation.Nullable; ce sont des annotations java, je peux m'en passer: n'y est que 3 fois dans cette partie du projet
***commenter imports, enlever les annotations @Nullable et @NonNull***
concerne: TileView.java (import android.support.annotation.Nullable; et @Nullable)
concerne: plugins/MarkerPlugin.java (import android.support.annotation.NonNull; et @NonNull)

erreur dependency: il faut DiskLruCache https://github.com/JakeWharton/DiskLruCache
mkdir -p third_party/disklrucache/ et y copier le src/ de DiskLruCache (enlever le dir test/) 
ajouter LOCAL_SRC_FILES += $(call all-java-files-under, third_party/disklrucache/src) dans le Android.mk

pour cette erreur dependency ce qui m'a le plus aidé c'est un mgrep disklrucache dans les sources (external/glide a la même dep)
j'ai farfouillé l'ajout de .jar: https://mvnrepository.com/artifact/com.jakewharton/disklrucache/2.0.2 https://stackoverflow.com/questions/36745754/add-prebuilt-jar-to-aosp mais flop

--> à ce stade j'ai un premier succesfule build, mais sans main activity. il faut maintenant incorporer la partie affichage (demo/) de TileView



je copie demo/ dans TOPDIR/java/ à côté de com/

gérer les android.support.* @Nullable dans les .java
commenter deux lignes qui contiennent Sthetho dans DemoApplication.java

"R cannot be resolved to a variable" à chaque fois qu'il y a R dans un .java  AndroidManifest.xml assets/ et res/ qui sont tout en bas de la path de demo/--> passer dans TOPDIR/ 
vraisemblablement cette erreur vient à chaque fois que pb dans le manifest, dans res/

java/com/qozix/widget/ScrollView.java:23.8: The import com.qozix.tileview.R cannot be resolved --> entraîne des R cannot be resolved to a variable
	j'ai cherché dans out/ des R.java à un endroit qui pourrait correspondre. effectivement pas de com/qozix mais un out/target/common/R/tileview/demo/R.java
	du coup import tileview.demo.R;
	à mon avis le problème vient d'anciens builds qui laissent des R.java. Le problème c'est que je sais pas nettoyer out/

"No resource found that matches the given name: attr 'color{Accent,Primary,PrimaryDark}'. --> res/values/styles.xml --> <item name="android:colorPrimary">@color/colorPrimary</item> au lieu de <item name="colorPrimary">@color/colorPrimary</item>
un bon moment passé là dessus-> c'est aapt qui parse res/ la commande simplifiée et qui évite de régénérer à chaque make et donc de passer 5 minutes à chaque essai: 
aapt package -M development/samples/TileView/tileview/src/main/AndroidManifest.xml -P /initrd/mnt/dev_save/android/lineageOS/sources/out/target/common/obj/APPS/TileView_intermediates/public_resources.xml -S development/samples/TileView/tileview/src/main/res -A development/samples/TileView/tileview/src/main/assets -I /initrd/mnt/dev_save/android/lineageOS/sources/out/target/common/obj/APPS/framework-res_intermediates/package-export.apk -v
la solution est pas bien claire pour moi, surtout quand je vois les resources dans development/samples/IntentPlayground


res/layout/activity_demos_scalingscrollview_tiger.xml --> app:srcCompat à remplacer par android:src sinon erreur srcCompat

dans le manifest enlever android:name=".DemoApplication" sinon bloque au démarrage (Didn't find class "tileview.demo.DemoApplication" ils essaient de lancer leur truc stetho qui est du facebook qui est leur sponsor je suppose)

l'apk fait 15Mo (le contenu de assets pèse 28 Mo)


