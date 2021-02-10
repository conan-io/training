
#include "liba/liba.h"

#include <iostream>

void hello_liba(int indent, const std::string& msg) {
    std::cout << std::string(indent, ' ') << "liba: " << msg << std::endl;
}