
#include <iostream>

#include "libc/libc.h"

int main() {
    std::cout << "app2: v1.0" << std::endl;
    hello_libc(1, "called from app2");    
    return 0;
}