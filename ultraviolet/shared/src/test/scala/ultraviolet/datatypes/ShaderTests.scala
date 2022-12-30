package ultraviolet.datatypes

import ultraviolet.DebugAST
import ultraviolet.syntax.*

class ShaderTests extends munit.FunSuite {

  test("Shader's can be run (and so, tested)") {

    case class UBO1(UV: vec2)

    def shader =
      Shader[UBO1, vec4] { env =>
        vec4(env.UV, 2.0f, 1.0f)
      }

    val actual =
      shader.run(UBO1(vec2(4.0f, 3.0f)))

    val expected =
      vec4(4.0f, 3.0f, 2.0f, 1.0f)

    assertEquals(actual, expected)

  }

  test("Shader's can be run with an intersection type") {

    trait UBO1:
      def UV: vec2
    trait UBO2:
      def TIME: Float

    case class UBO(UV: vec2, TIME: Float) extends UBO1, UBO2

    def shader =
      Shader[UBO1 & UBO2, vec4] { env =>
        vec4(env.UV, 2.0f, 1.0f)
      }

    val actual =
      shader.run(UBO(vec2(4.0f, 3.0f), 2.0f))

    val expected =
      vec4(4.0f, 3.0f, 2.0f, 1.0f)

    assertEquals(actual, expected)

  }

  test("Shader's can be mapped in a simple case") {

    case class UBO1(UV: vec2)

    inline def shader: Shader[UBO1, vec2] =
      Shader[UBO1, vec4] { env =>
        ubo[UBO1]
        vec4(env.UV, 2.0f, 1.0f)
      }.map(_.xy)

    val actual =
      shader.run(UBO1(vec2(4.0f, 3.0f)))

    val expected =
      vec2(4.0f, 3.0f)

    assertEquals(actual, expected)

    val actualCode =
      shader.toGLSL[WebGL2].code

    // DebugAST.toAST(shader)
    // println(actualCode)

    assertEquals(
      actualCode,
      s"""
      |vec2 def0(in vec4 val0){
      |  return val0.xy;
      |}
      |layout (std140) uniform UBO1 {
      |  vec2 UV;
      |};
      |def0(vec4(UV,2.0,1.0));
      |""".stripMargin.trim
    )
  }

}
