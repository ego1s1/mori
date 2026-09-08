# comic-ocr consumer ProGuard / R8 rules.
#
# Pins the pluggable OCR API surface under R8 full-mode shrinking. The Tesseract
# AAR ships its own native-library rules; nothing extra is needed for the JNI
# bridge here.

-keep public interface com.mori.comic.ocr.OcrEngine { *; }
-keep public class com.mori.comic.ocr.OcrEngines { *; }
-keep public class com.mori.comic.ocr.TesseractOcrEngine { *; }
-keep public class com.mori.comic.ocr.FakeOcrEngine { *; }
-keep public class com.mori.comic.ocr.Ocr* { *; }
