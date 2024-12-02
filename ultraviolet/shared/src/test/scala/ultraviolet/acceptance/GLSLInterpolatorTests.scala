package ultraviolet.acceptance

import ultraviolet.syntax.*

class GLSLInterpolatorTests extends munit.FunSuite {
  test("interpolators") {
    inline def fragment: Shader[Unit, vec4] =
      Shader { _ =>
        vec4(hex"ff00ff",0f)
      }

    val actual = fragment.toGLSL[WebGL2].toOutput.code

    assertEquals(
      actual,
      s"""
      |vec4 v4=vec4(vec3(1.0,0.0,1.0),0.0)
      |""".stripMargin.trim
    )
  }
}
