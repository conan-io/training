
#include <iostream>

#include "libb/libb.h"
#include "libc/libc.h"
/////

int main() {
    std::cout << "app1: v1.0" << std::endl;
    hello_libb(1, "called from app1");
    hello_libc(1, "called from app1");    
    hello_libd(1, "called from app1");    
    return 0;
}