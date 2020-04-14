#include <iostream>
#include "hello.h"
#ifdef WITH_ZIP
    #include <zlib.h>
#endif
#include <stdio.h>
#include <string.h>

void hello(){
    std::cout << "Hello world!\n";
    
    char buffer_in [100] = {"Conan Package Manager is GREEEEEEEEEEEEEEAAAAAAAAAAT"};
    char buffer_out [100] = {0};

    #ifdef WITH_ZIP
        z_stream defstream;
        defstream.zalloc = Z_NULL;
        defstream.zfree = Z_NULL;
        defstream.opaque = Z_NULL;
        defstream.avail_in = (uInt) strlen(buffer_in);
        defstream.next_in = (Bytef *) buffer_in;
        defstream.avail_out = (uInt) sizeof(buffer_out);
        defstream.next_out = (Bytef *) buffer_out;

        deflateInit(&defstream, Z_BEST_COMPRESSION);
        deflate(&defstream, Z_FINISH);
        deflateEnd(&defstream);

        printf("Original size is: %lu\n", strlen(buffer_in));
        printf("Original string is: %s\n", buffer_in);
        printf("Compressed size is: %lu\n", strlen(buffer_out));
        printf("Compressed string is: %s\n", buffer_out);

        printf("ZLIB VERSION: %s\n", zlibVersion());
    #endif
}
