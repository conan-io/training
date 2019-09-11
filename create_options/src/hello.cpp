#include <iostream>
#include "hello.h"

void hello(){
    #if GREET_LANGUAGE == 1
        #ifdef NDEBUG
        std::cout << "Hello World Release!" <<std::endl;
        #else
        std::cout << "Hello World Debug!" <<std::endl;
        #endif
    #else
        #ifdef NDEBUG
        std::cout << "HOLA MUNDO Release!" <<std::endl;
        #else
        std::cout << "HOLA MUNDO Debug!" <<std::endl;
        #endif
    #endif
}
