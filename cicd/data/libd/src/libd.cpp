
#include "libd/libd.h"

#include <iostream>
#include "libb/libb.h"
#include "libc/libc.h"


void hello_libd(int indent, const std::string& msg) {
    std::cout << std::string(indent, ' ') << "libd: " << msg << std::endl;    
    hello_libb(indent+1, "called from libd");
    hello_libc(indent+1, "called from libd");
}