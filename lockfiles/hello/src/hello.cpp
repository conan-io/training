#include <iostream>
#include "hello.h"

void hello(){
    #ifdef NDEBUG
    std::cout << "Hello World **** 0.2 **** Release!" <<std::endl;
    #else
    std::cout << "Hello World **** 0.2 **** Debug!" <<std::endl;
    #endif
}
