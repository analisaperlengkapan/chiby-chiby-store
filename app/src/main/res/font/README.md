# Custom Fonts

This directory is intended for custom font files (e.g., `.ttf`, `.otf`, or XML font resources).

To use a custom font in the application:

1.  Add your font files to this directory.
    *   Ensure filenames contain only lowercase letters, numbers, and underscores (e.g., `my_custom_font.ttf`).
2.  Update `app/src/main/java/com/chibychibystore/ui/theme/Type.kt`:
    *   Uncomment the `ChibyFontFamily` definition.
    *   Reference your new font resources (e.g., `R.font.my_custom_font`).
