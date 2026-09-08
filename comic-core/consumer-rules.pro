# comic-core consumer ProGuard / R8 rules.
#
# The public entry points are referenced directly by consumers, but these rules
# protect the API surface (and the junrar bridge it delegates to) under R8's
# aggressive full-mode shrinking, where entry points reached only through the
# ComicArchive interface could otherwise be renamed or removed.

# Public API surface.
-keep public class com.mori.comic.ComicFactory { *; }
-keep public interface com.mori.comic.ComicArchive { *; }
-keep public class com.mori.comic.DecodingComicArchive { *; }
-keep public class com.mori.comic.model.* { *; }
-keep public class com.mori.comic.decode.* { *; }
-keep public class com.mori.comic.metadata.* { *; }
-keep public class com.mori.comic.*Exception { *; }

# junrar classes touched by the CBR backend. They are plain Java with no reflection,
# but the rules below pin the small subset the library links against so full-mode
# R8 cannot break the bridge.
-keep class com.github.junrar.Archive { *; }
-keep class com.github.junrar.rarfile.FileHeader { *; }
-keep class com.github.junrar.exception.* { *; }
