package ultraviolet.acceptance

import ultraviolet.DebugAST
import ultraviolet.syntax.*

class GLSLLoopTests extends munit.FunSuite {

  @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
  case class FragEnv(UV: vec2, var COLOR: vec4)

  test("while loops") {

    @SuppressWarnings(Array("scalafix:DisableSyntax.var", "scalafix:DisableSyntax.while"))
    inline def fragment: Shader[FragEnv, Float] =
      Shader { _ =>
        var i = 0.0f
        while i < 4.0f do i += 1.0f
        i
      }

    val actual =
      fragment.toGLSL[WebGL2].code

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |float i=0.0;
      |while(i<4.0){
      |  i=i+1.0;
      |}
      |i;
      |""".stripMargin.trim
    )
  }

  test("for loops (cfor) - int") {

    @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
    inline def fragment: Shader[FragEnv, Float] =
      Shader { _ =>
        var i     = 0.0f
        val steps = 10
        cfor(0, _ < steps, _ + 1) { _ =>
          i += 1.0f
        }
        i
      }

    val actual =
      fragment.toGLSL[WebGL2].code

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |float i=0.0;
      |int steps=10;
      |for(int val0=0;val0<steps;val0=val0+1){
      |  i=i+1.0;
      |}
      |i;
      |""".stripMargin.trim
    )
  }

  test("for loops (cfor) - float") {

    @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
    inline def fragment: Shader[FragEnv, Float] =
      Shader { _ =>
        var i     = 0.0f
        val steps = 10.0f
        cfor(0.0f, _ < steps, _ + 1.5f) { _ =>
          i += 1.0f
        }
        i
      }

    val actual =
      fragment.toGLSL[WebGL2].code

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |float i=0.0;
      |float steps=10.0;
      |for(float val0=0.0;val0<steps;val0=val0+1.5){
      |  i=i+1.0;
      |}
      |i;
      |""".stripMargin.trim
    )
  }

  test("for loops (_for)") {

    @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
    inline def fragment: Shader[FragEnv, Float] =
      Shader { _ =>
        var i     = 0.0f
        val steps = 10
        _for(0, _ < steps, _ + 1) { _ =>
          i += 1.0f
        }
        i
      }

    val actual =
      fragment.toGLSL[WebGL2].code

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |float i=0.0;
      |int steps=10;
      |for(int val0=0;val0<steps;val0=val0+1){
      |  i=i+1.0;
      |}
      |i;
      |""".stripMargin.trim
    )
  }

}
