#include "picojson/picojson.h"

int main(void) {
  
  const char* json = "{\"a\":1}";
  picojson::value v;
  std::string err;
  const char* json_end = picojson::parse(v, json, json + strlen(json), &err);
  if (! err.empty()) {
    std::cerr << err << std::endl;
  }
  std::cout << "Json parsed ok!" << std::endl;

  return 0;
}

