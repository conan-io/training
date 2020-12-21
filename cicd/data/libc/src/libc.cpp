
#include "libc/libc.h"

#include <iostream>
#include "liba/liba.h"

///////


void hello_libc(int indent, const std::string& msg) {
    std::cout << std::string(indent, ' ') << "libc: " << msg << std::endl;
    std::cout << "libc version 1.0" << std::endl;
    hello_liba(indent+1, "called from libc");
}