package ultraviolet.acceptance

import ultraviolet.DebugAST
import ultraviolet.syntax.*

import scala.annotation.nowarn

@nowarn("msg=unused")
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
      fragment.toGLSL[WebGL2].toOutput.code

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
      fragment.toGLSL[WebGL2].toOutput.code

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

  test("for loops (cfor) tuple 2 - int") {

    @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
    inline def fragment: Shader[FragEnv, Int] =
      Shader { _ =>
        val steps = 10
        var acc1  = 0
        var acc2  = 0
        cfor((0, -1), (i, _) => i < steps, (i, _) => (i + 1, i)) { (i, j) =>
          acc1 += i
          acc2 += j
        }
        acc1 + acc2
      }

    val actual =
      fragment.toGLSL[WebGL2].toOutput.code

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
         |int steps=10;
         |int acc1=0;
         |int acc2=0;
         |for(int i=0,j=-1;i<steps;i=i+1,j=i){
         |  acc1=acc1+i;
         |  acc2=acc2+j;
         |}
         |acc1+acc2;
         |""".stripMargin.trim
    )
  }

  test("for loops (cfor) tuple 3 - int") {

    @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
    inline def fragment: Shader[FragEnv, Int] =
      Shader { _ =>
        val steps = 10
        var acc1  = 0
        var acc2  = 0
        var acc3  = 0
        cfor((1, 2, 3), (i, _, _) => i < steps, (i, _, _) => (i + 1, i, i * 2)) { (i, j, k) =>
          acc1 += i
          acc2 += j
          acc3 += k
        }
        acc1 + acc2 + acc3
      }

    val actual =
      fragment.toGLSL[WebGL2].toOutput.code

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
         |int steps=10;
         |int acc1=0;
         |int acc2=0;
         |int acc3=0;
         |for(int i=1,j=2,k=3;i<steps;i=i+1,j=i,k=i*2){
         |  acc1=acc1+i;
         |  acc2=acc2+j;
         |  acc3=acc3+k;
         |}
         |(acc1+acc2)+acc3;
         |""".stripMargin.trim
    )
  }

  test("for loops (cfor) tuple 4 - int") {

    @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
    inline def fragment: Shader[FragEnv, Int] =
      Shader { _ =>
        val steps = 10
        var acc1  = 0
        var acc2  = 0
        var acc3  = 0
        var acc4  = 0
        cfor((1, 2, 3, 4), (i, _, _, _) => i < steps, (i, _, k, _) => (i + 1, i, k * 2, i * i)) { (i, j, k, l) =>
          acc1 += i
          acc2 += j
          acc3 += k
          acc4 += l
        }
        acc1 + acc2 + acc3 + acc4
      }

    val actual =
      fragment.toGLSL[WebGL2].toOutput.code

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
         |int steps=10;
         |int acc1=0;
         |int acc2=0;
         |int acc3=0;
         |int acc4=0;
         |for(int i=1,j=2,k=3,l=4;i<steps;i=i+1,j=i,k=k*2,l=i*i){
         |  acc1=acc1+i;
         |  acc2=acc2+j;
         |  acc3=acc3+k;
         |  acc4=acc4+l;
         |}
         |((acc1+acc2)+acc3)+acc4;
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
      fragment.toGLSL[WebGL2].toOutput.code

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
      fragment.toGLSL[WebGL2].toOutput.code

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

  test("for loops (_for) - use the value.") {

    @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
    inline def fragment: Shader[FragEnv, Float] =
      Shader { _ =>
        val v: array[3, vec2] = array[3, vec2](vec2(1.0f), vec2(2.0f), vec2(3.0f))
        var xs: Float         = 0.0f
        _for(0, _ < 3, _ + 1) { i =>
          xs = xs + v(i).x
        }
        xs
      }

    val actual =
      fragment.toGLSL[WebGL2].toOutput.code

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |vec2 v[3]=vec2[3](vec2(1.0),vec2(2.0),vec2(3.0));
      |float xs=0.0;
      |for(int i=0;i<3;i=i+1){
      |  xs=xs+v[i].x;
      |}
      |xs;
      |""".stripMargin.trim
    )
  }

}
