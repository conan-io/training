#pragma once

#ifdef WIN32
  #define HELLO_EXPORT __declspec(dllexport)
#else
  #define HELLO_EXPORT
#endif

inline void hello(){
    #ifdef NDEBUG
    std::cout << "Hello World **** 1.1 **** Release!" <<std::endl;
    #else
    std::cout << "Hello World **** 1.1 **** Debug!" <<std::endl;
    #endif
}
