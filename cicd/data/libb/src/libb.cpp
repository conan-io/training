
#include "libb/libb.h"

#include <iostream>
#include "liba/liba.h"


void hello_libb(int indent, const std::string& msg) {
    std::cout << std::string(indent, ' ') << "libb: " << msg << std::endl;    
    hello_liba(indent+1, "called from libb");
}