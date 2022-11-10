package ultraviolet

import ultraviolet.core.DebugAST
import ultraviolet.syntax.*

class WebGL2Tests extends munit.FunSuite {

  test("Can generate the simplest valid WebGL 2.0 fragment shader") {

    @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
    case class Env(var outColor: vec4)

    inline def fragment: Shader[Env, Unit] =
      Shader { env =>

        @out val foo: vec4 = vec4(1.0f)
        @in val bar: vec4 = vec4(1.0f)

        def main: Unit = 
          env.outColor = vec4(1.0f, 0.0f, 0.5f, 1.0f)
      }

    val actual =
      fragment.toGLSL

    // DebugAST.toAST(fragment)
    // println(actual)

    val expected: String =
      """
      |#version 300 es
      |precision highp float;
      |out vec4 outColor;
      |void main(){outColor = vec4(1, 0, 0.5, 1);}
      |""".stripMargin.trim

    assertEquals(actual, expected)
  }

}
