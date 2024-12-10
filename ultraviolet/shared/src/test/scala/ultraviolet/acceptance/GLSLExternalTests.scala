package ultraviolet.acceptance

import ultraviolet.DebugAST
import ultraviolet.syntax.*

import scala.annotation.nowarn

@nowarn("msg=unused")
class GLSLExternalTests extends munit.FunSuite {

  @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
  case class FragEnv(UV: vec2, var COLOR: vec4)

  test("Inlined external def") {

    inline def alpha: Float = 1.0f

    inline def fragment: Shader[FragEnv, vec4] =
      Shader { _ =>
        vec4(1.0f, 1.0f, 0.0f, alpha)
      }

    val actual =
      fragment.toGLSL[WebGL2].toOutput.code

    // DebugAST.toAST(fragment)
    // println(actual)
    // println(ShaderMacros.toAST(fragment))

    assert(clue(actual) == clue("vec4(1.0,1.0,0.0,1.0);"))
  }

  test("Inlined external non-primitive (as def)") {

    inline def fn2: vec2 = vec2(0.0f, 1.0f)

    inline def fragment: Shader[FragEnv, vec4] =
      Shader { _ =>
        vec4(1.0f, 1.0f, fn2)
      }

    val actual =
      fragment.toGLSL[WebGL2].toOutput.code

    assert(clue(actual) == clue("vec4(1.0,1.0,vec2(0.0,1.0));"))
  }

  test("Inlined external function no proxy") {
    // The argument here will be ignored and inlined. Inlines are weird.
    inline def fn1(x: Float, y: Float): vec2 =
      vec2(x, y)

    inline def fragment: Shader[FragEnv, vec4] =
      Shader { env =>
        val y = 2.0f
        vec4(fn1(1.0f, y), 0.0f, 1.0f)
      }

    val actual =
      fragment.toGLSL[WebGL2].toOutput.code

    // println(DebugAST.toAST(fragment)) 
    // println(actual)

    assertEquals(
      actual,
      s"""
      |vec2 fn1(in float val0,in float y){
      |  return vec2(1.0,y);
      |}
      |float y=2.0;
      |vec4(fn1(1.0,y),0.0,1.0);
      |""".stripMargin.trim
    )
  }

  // Test disabled / commented out because of inlining issues with, e.g. inline def f: Float => Float = x => x
  // test("Inlined external function".only) {
  //   inline def fn1(v: Float): vec2 =
  //     vec2(v)

  //   inline def fn2: Float => vec2 =
  //     (alpha: Float) => vec2(0.0f, alpha)

  //   inline def fragment: Shader[FragEnv, vec4] =
  //     Shader { env =>
  //       val y = 2.0f
  //       vec4(fn1(1.0f), fn2(y))
  //     }

  //   val actual =
  //     fragment.toGLSL[WebGL2](false).toOutput.code

  //   println(DebugAST.toAST(fragment))
  //   println(actual)

  //   assertEquals(
  //     actual,
  //     s"""
  //     |vec2 fn1(in float val0){
  //     |  return vec2(1.0);
  //     |}
  //     |vec2 fn2(in float alpha){
  //     |  return vec2(0.0,alpha);
  //     |}
  //     |vec4(fn1(1.0),fn2(1.0));
  //     |""".stripMargin.trim
  //   )
  // }

  test("Inlined external function N args") {

    // The argument here will be ignored and inlined. Inlines are weird.
    inline def fn1(red: Float, green: Float): vec2 =
      vec2(red, green)

    // Is treated like a function
    inline def fn2: (Float, Float) => vec2 =
      (blue, alpha) => vec2(blue, alpha)

    inline def fragment: Shader[FragEnv, vec4] =
      Shader { env =>
        val proxy: (Float, Float) => vec2 = fn2
        vec4(fn1(1.0f, 0.25f), proxy(0.5f, 1.0f))
      }

    val actual =
      fragment.toGLSL[WebGL2].toOutput.code

    assertEquals(
      actual,
      s"""
      |vec2 def0(in float blue,in float alpha){
      |  return vec2(blue,alpha);
      |}
      |vec2 fn1(in float val0,in float val1){
      |  return vec2(1.0,0.25);
      |}
      |vec4(fn1(1.0,0.25),def0(0.5,1.0));
      |""".stripMargin.trim
    )
  }

  test("should correctly render tile code") {

    inline def fragment =
      Shader {
        import TileAndStretch.*

        val uv: vec2          = vec2(1.0)
        val channelPos: vec2  = vec2(2.0)
        val channelSize: vec2 = vec2(3.0)
        val entitySize: vec2  = vec2(4.0)
        val textureSize: vec2 = vec2(5.0)

        tiledUVs(
          uv,          // env.UV,
          channelPos,  // env.CHANNEL_0_POSITION,
          channelSize, // env.CHANNEL_0_SIZE,
          entitySize,  // env.SIZE,
          textureSize  // env.TEXTURE_SIZE
        )
      }

    val actual =
      fragment.toGLSL[WebGL2].toOutput.code

    // println(actual)

    assertEquals(
      actual,
      s"""
      |vec2 tiledUVs(in vec2 uv,in vec2 channelPos,in vec2 channelSize,in vec2 entitySize,in vec2 textureSize){
      |  return channelPos+((fract(uv*(entitySize/textureSize)))*channelSize);
      |}
      |vec2 uv=vec2(1.0);
      |vec2 channelPos=vec2(2.0);
      |vec2 channelSize=vec2(3.0);
      |vec2 entitySize=vec2(4.0);
      |vec2 textureSize=vec2(5.0);
      |tiledUVs(uv,channelPos,channelSize,entitySize,textureSize);
      |""".stripMargin.trim
    )

  }

  test("should correctly render stretch code") {

    inline def fragment =
      Shader {
        import TileAndStretch.*

        val uv: vec2          = vec2(1.0)
        val channelPos: vec2  = vec2(2.0)
        val channelSize: vec2 = vec2(3.0)

        stretchedUVs(
          uv,         // env.UV,
          channelPos, // env.CHANNEL_0_POSITION,
          channelSize // env.CHANNEL_0_SIZE,
        )
      }

    val actual =
      fragment.toGLSL[WebGL2].toOutput.code

    // println(actual)

    assertEquals(
      actual,
      s"""
      |vec2 stretchedUVs(in vec2 uv,in vec2 channelPos,in vec2 channelSize){
      |  return channelPos+(uv*channelSize);
      |}
      |vec2 uv=vec2(1.0);
      |vec2 channelPos=vec2(2.0);
      |vec2 channelSize=vec2(3.0);
      |stretchedUVs(uv,channelPos,channelSize);
      |""".stripMargin.trim
    )

  }

  test("should correctly render tile and stretch code (def)") {

    inline def fragment =
      Shader {
        import TileAndStretch.*

        val fillType: Int              = 0
        val fallback: vec4             = vec4(1.0)
        val srcChannel: sampler2D.type = sampler2D
        val channelPos: vec2           = vec2(2.0)
        val channelSize: vec2          = vec2(3.0)
        val uv: vec2                   = vec2(4.0)
        val entitySize: vec2           = vec2(5.0)
        val textureSize: vec2          = vec2(6.0)

        tileAndStretchChannelDef(
          fillType,    // env.FILLTYPE.toInt,
          fallback,    // env.CHANNEL_0,
          srcChannel,  // env.SRC_CHANNEL,
          channelPos,  // env.CHANNEL_0_POSITION,
          channelSize, // env.CHANNEL_0_SIZE,
          uv,          // env.UV,
          entitySize,  // env.SIZE,
          textureSize  // env.TEXTURE_SIZE
        )
      }

    val actual =
      fragment.toGLSL[WebGL2].toOutput.code

    // println(DebugAST.toAST(fragment))
    // println(actual)

    assertEquals(
      actual,
      s"""
      |vec2 stretchedUVs(in vec2 uv,in vec2 channelPos,in vec2 channelSize){
      |  return channelPos+(uv*channelSize);
      |}
      |vec2 tiledUVs(in vec2 uv,in vec2 channelPos,in vec2 channelSize,in vec2 entitySize,in vec2 textureSize){
      |  return channelPos+((fract(uv*(entitySize/textureSize)))*channelSize);
      |}
      |vec4 tileAndStretchChannelDef(in int fillType,in vec4 fallback,in sampler2D srcChannel,in vec2 channelPos,in vec2 channelSize,in vec2 uv,in vec2 entitySize,in vec2 textureSize){
      |  vec4 val0;
      |  switch(fillType){
      |    case 1:
      |      val0=texture(srcChannel,stretchedUVs(uv,channelPos,channelSize));
      |      break;
      |    case 2:
      |      val0=texture(srcChannel,tiledUVs(uv,channelPos,channelSize,entitySize,textureSize));
      |      break;
      |    default:
      |      val0=fallback;
      |      break;
      |  }
      |  return val0;
      |}
      |int fillType=0;
      |vec4 fallback=vec4(1.0);
      |sampler2D srcChannel=sampler2D;
      |vec2 channelPos=vec2(2.0);
      |vec2 channelSize=vec2(3.0);
      |vec2 uv=vec2(4.0);
      |vec2 entitySize=vec2(5.0);
      |vec2 textureSize=vec2(6.0);
      |tileAndStretchChannelDef(fillType,fallback,srcChannel,channelPos,channelSize,uv,entitySize,textureSize);
      |""".stripMargin.trim
    )

  }

  // TODO: Bring back
  // test("should correctly render tile and stretch code (fn)") {

  //   inline def fragment =
  //     Shader {
  //       import TileAndStretch.*

  //       val fillType: Int                       = 0
  //       val fallback: vec4                      = vec4(1.0)
  //       @uniform val srcChannel: sampler2D.type = sampler2D
  //       val channelPos: vec2                    = vec2(2.0)
  //       val channelSize: vec2                   = vec2(3.0)
  //       val uv: vec2                            = vec2(4.0)
  //       val entitySize: vec2                    = vec2(5.0)
  //       val textureSize: vec2                   = vec2(6.0)

  //       val proxy: (Int, vec4, sampler2D.type, vec2, vec2, vec2, vec2, vec2) => vec4 =
  //         tileAndStretchChannelFn

  //       proxy(
  //         fillType,    // env.FILLTYPE.toInt,
  //         fallback,    // env.CHANNEL_0,
  //         srcChannel,  // env.SRC_CHANNEL,
  //         channelPos,  // env.CHANNEL_0_POSITION,
  //         channelSize, // env.CHANNEL_0_SIZE,
  //         uv,          // env.UV,
  //         entitySize,  // env.SIZE,
  //         textureSize  // env.TEXTURE_SIZE
  //       )
  //     }

  //   val actual =
  //     fragment.toGLSL[WebGL2].toOutput.code

  //   // DebugAST.toAST(fragment)
  //   // println(actual)

  //   assertEquals(
  //     actual,
  //     s"""
  //     |uniform sampler2D srcChannel;
  //     |vec4 def0(in int _fillType,in vec4 _fallback,in sampler2D _srcChannel,in vec2 _channelPos,in vec2 _channelSize,in vec2 _uv,in vec2 _entitySize,in vec2 _textureSize){
  //     |  vec4 val0;
  //     |  switch(_fillType){
  //     |    case 1:
  //     |      val0=texture(_srcChannel,_channelPos+(_uv*_channelSize));
  //     |      break;
  //     |    case 2:
  //     |      val0=texture(_srcChannel,_channelPos+((fract(_uv*(_entitySize/_textureSize)))*_channelSize));
  //     |      break;
  //     |    default:
  //     |      val0=_fallback;
  //     |      break;
  //     |  }
  //     |  return val0;
  //     |}
  //     |int fillType=0;
  //     |vec4 fallback=vec4(1.0);
  //     |vec2 channelPos=vec2(2.0);
  //     |vec2 channelSize=vec2(3.0);
  //     |vec2 uv=vec2(4.0);
  //     |vec2 entitySize=vec2(5.0);
  //     |vec2 textureSize=vec2(6.0);
  //     |def0(fillType,fallback,srcChannel,channelPos,channelSize,uv,entitySize,textureSize);
  //     |""".stripMargin.trim
  //   )

  // }

  test("Inlined external def function with ignored argument") {
    inline def modifyColorNamed: vec4 => Shader[Unit, vec4] =
      inputColor =>
        Shader[Unit, vec4] { env =>
          vec4(2.0)
        }

    inline def fragmentNamed =
      Shader {
        modifyColorNamed(vec4(1.0)).run(())
      }

    val actualNamed =
      fragmentNamed.toGLSL[WebGL2].toOutput.code

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actualNamed,
      s"""
      |vec4 inputColor=vec4(1.0);
      |vec4(2.0);
      |""".stripMargin.trim
    )

    inline def modifyColorAnon: vec4 => Shader[Unit, vec4] =
      _ =>
        Shader[Unit, vec4] { env =>
          vec4(2.0)
        }

    inline def fragmentAnon =
      Shader {
        modifyColorAnon(vec4(1.0)).run(())
      }

    val actualAnon =
      fragmentAnon.toGLSL[WebGL2].toOutput.code

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actualAnon,
      s"""
      |vec4 def0(){
      |  return vec4(2.0);
      |}
      |def0();
      |""".stripMargin.trim
    )

  }

}

object TileAndStretch:

  inline def stretchedUVs(uv: vec2, channelPos: vec2, channelSize: vec2): vec2 =
    channelPos + uv * channelSize

  inline def tiledUVs(uv: vec2, channelPos: vec2, channelSize: vec2, entitySize: vec2, textureSize: vec2): vec2 =
    channelPos + (fract(uv * (entitySize / textureSize)) * channelSize)

  inline def tileAndStretchChannelDef(
      _fillType: Int,
      _fallback: vec4,
      _srcChannel: sampler2D.type,
      _channelPos: vec2,
      _channelSize: vec2,
      _uv: vec2,
      _entitySize: vec2,
      _textureSize: vec2
  ): vec4 =
    _fillType match
      case 1 =>
        texture2D(
          _srcChannel,
          stretchedUVs(_uv, _channelPos, _channelSize)
        )

      case 2 =>
        texture2D(
          _srcChannel,
          tiledUVs(_uv, _channelPos, _channelSize, _entitySize, _textureSize)
        )

      case _ =>
        _fallback

  inline def tileAndStretchChannelFn =
    (
        _fillType: Int,
        _fallback: vec4,
        _srcChannel: sampler2D.type,
        _channelPos: vec2,
        _channelSize: vec2,
        _uv: vec2,
        _entitySize: vec2,
        _textureSize: vec2
    ) =>
      _fillType match
        case 1 =>
          texture2D(
            _srcChannel,
            stretchedUVs(_uv, _channelPos, _channelSize)
          )

        case 2 =>
          texture2D(
            _srcChannel,
            tiledUVs(_uv, _channelPos, _channelSize, _entitySize, _textureSize)
          )

        case _ =>
          _fallback
