package ultraviolet

import syntax.*

class SyntaxTests extends munit.FunSuite {

  // TODO: Should the input be prefixed by '#'?
  // TODO: Do we want shorthand notation (i.e. #000 to mean #000000)?
  test("hex interpolator") {
    assertEquals(hex"000000", vec3(0f, 0f, 0f))
    assertEquals(hex"FFFFFF", vec3(1f, 1f, 1f))
    assertEquals(hex"ffffff", vec3(1f, 1f, 1f))
  }

  test("hexa interpolator") {
    assertEquals(hexa"00000000", vec4(0f, 0f, 0f, 0f))
    assertEquals(hexa"FFFFFFFF", vec4(1f, 1f, 1f, 1f))
    assertEquals(hexa"ffffffff", vec4(1f, 1f, 1f, 1f))
  }
  // TODO: rgb/rgba tests
}
