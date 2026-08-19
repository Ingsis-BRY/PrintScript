Las interfaces viven donde esta el que las consume. Dependency inversion.

Los errores viajan como datos (Diagnostic), no como texto. El unico que abre el ADT completo
es ErrorRenderer: ahi vive el texto de todos los mensajes, y agregar un caso rompe la compilacion ahi.
