// In the test file
#include <gtest/gtest.h>
#include "hello.h"

TEST(SalutationTest, Static) {
  EXPECT_EQ(string("Hello World!"), message());
}
