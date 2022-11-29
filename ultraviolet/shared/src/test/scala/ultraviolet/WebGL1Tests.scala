package ultraviolet

import ultraviolet.DebugAST
import ultraviolet.syntax.*

class WebGL1Tests extends munit.FunSuite {

  test("Can generate a simple valid WebGL 1.0 fragment shader") {

    @SuppressWarnings(Array("scalafix:DisableSyntax.null"))
    inline def fragment =
      Shader[WebGL1Env, Unit](
        GLSLHeader.PrecisionMediumPFloat
      ) { env =>
        @uniform val u_texture: sampler2D.type = sampler2D
        @in val v_texcoord: vec2               = null

        def main: Unit =
          env.gl_FragColor = texture2D(u_texture, v_texcoord)
      }

    val actual =
      fragment.toGLSL[WebGL1].code

    // DebugAST.toAST(fragment)
    // println(actual)

    val expected: String =
      """
      |precision mediump float;
      |uniform sampler2D u_texture;
      |varying vec2 v_texcoord;
      |void main(){
      |  gl_FragColor=texture2D(u_texture,v_texcoord);
      |}
      |""".stripMargin.trim

    assertEquals(actual, expected)
  }

}
