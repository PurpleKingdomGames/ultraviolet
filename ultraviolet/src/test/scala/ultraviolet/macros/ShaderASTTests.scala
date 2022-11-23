package ultraviolet.macros

import ultraviolet.DebugAST
import ultraviolet.syntax.*

class ShaderASTTests extends munit.FunSuite {

  @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
  case class FragEnv(UV: vec2, var COLOR: vec4)

  test("Simple conversion to GLSL") {
    inline def fragment =
      Shader {
        vec4(1.0f, 1.0f, 0.0f, 1.0f)
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |vec4(1.0,1.0,0.0,1.0);
      |""".stripMargin.trim
    )
  }

  test("Inlined external val") {

    inline def alpha: Float = 1.0f

    inline def fragment: Shader[FragEnv, vec4] =
      Shader { _ =>
        vec4(1.0f, 1.0f, 0.0f, alpha)
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)
    // println(ShaderMacros.toAST(fragment))

    assert(clue(actual) == clue("vec4(1.0,1.0,0.0,1.0);"))
  }

  test("Inlined external val (as def)") {

    inline def zw: vec2 = vec2(0.0f, 1.0f)

    inline def fragment: Shader[FragEnv, vec4] =
      Shader { _ =>
        vec4(1.0f, 1.0f, zw)
      }

    val actual =
      fragment.toGLSL[WebGL2]

    assert(clue(actual) == clue("vec4(1.0,1.0,vec2(0.0,1.0));"))
  }

  test("Inlined external function") {
    // The argument here will be ignored and inlined. Inlines are weird.
    inline def xy(v: Float): vec2 =
      vec2(v)

    inline def zw: Float => vec2 =
      alpha => vec2(0.0f, alpha)

    inline def fragment: Shader[FragEnv, vec4] =
      Shader { env =>
        vec4(xy(1.0f), zw(1.0f))
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |vec2 xy(in float val0){
      |  return vec2(1.0);
      |}
      |vec2 def0(in float alpha){
      |  return vec2(0.0,alpha);
      |}
      |vec4(xy(1.0),def0(1.0));
      |""".stripMargin.trim
    )
  }

  test("Inlined external function N args") {

    // The argument here will be ignored and inlined. Inlines are weird.
    inline def xy(red: Float, green: Float): vec2 =
      vec2(red, green)

    // Is treated like a function
    inline def zw: (Float, Float) => vec2 =
      (blue, alpha) => vec2(blue, alpha)

    inline def fragment: Shader[FragEnv, vec4] =
      Shader { env =>
        vec4(xy(1.0f, 0.25f), zw(0.5f, 1.0f))
      }

    val actual =
      fragment.toGLSL[WebGL2]

    assertEquals(
      actual,
      s"""
      |vec2 xy(in float val0,in float val1){
      |  return vec2(1.0,0.25);
      |}
      |vec2 def0(in float blue,in float alpha){
      |  return vec2(blue,alpha);
      |}
      |vec4(xy(1.0,0.25),def0(0.5,1.0));
      |""".stripMargin.trim
    )
  }

  test("Programs can use an env value like env.UV as UV") {
    inline def fragment: Shader[FragEnv, vec4] =
      Shader { env =>
        vec4(env.UV, 0.0f, 1.0f)
      }

    val actual =
      fragment.toGLSL[WebGL2]

    assertEquals(
      actual,
      s"""
      |vec4(UV,0.0,1.0);
      |""".stripMargin.trim
    )
  }

  test("Programs can use and negate env values") {
    inline def fragment: Shader[FragEnv, vec4] =
      Shader { env =>
        val a = vec2(1.0f, 2.0f)
        val b = -a
        val c = -a.x
        val d = -env.UV.y
        vec4(env.UV, 0.0f, 1.0f)
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)

    assertEquals(
      actual,
      s"""
      |vec2 a=vec2(1.0,2.0);
      |vec2 b=-a;
      |float c=-a.x;
      |float d=-UV.y;
      |vec4(UV,0.0,1.0);
      |""".stripMargin.trim
    )
  }

  test("swizzling") {
    inline def fragment1: Shader[FragEnv, vec4] =
      Shader { _ =>
        vec4(1.0f, 2.0f, 3.0f, 4.0f).wzyx
      }

    val actual1 =
      fragment1.toGLSL[WebGL2]

    assertEquals(
      actual1,
      s"""
      |vec4(1.0,2.0,3.0,4.0).wzyx;
      |""".stripMargin.trim
    )

    inline def fragment2: Shader[FragEnv, vec3] =
      Shader { _ =>
        vec3(1.0f, 2.0f, 3.0f).xxy
      }

    val actual2 =
      fragment2.toGLSL[WebGL2]

    assertEquals(
      actual2,
      s"""
      |vec3(1.0,2.0,3.0).xxy;
      |""".stripMargin.trim
    )

    inline def fragment3: Shader[FragEnv, vec3] =
      Shader { _ =>
        val fill = vec3(1.0f, 2.0f, 3.0f)
        fill.xyz
      }

    val actual3 =
      fragment3.toGLSL[WebGL2]

    assertEquals(
      actual3,
      s"""
      |vec3 fill=vec3(1.0,2.0,3.0);
      |fill.xyz;
      |""".stripMargin.trim
    )
  }

  test("can call a native function 1") {

    inline def circleSdf(p: vec2, r: Float): Float =
      length(p) - r

    inline def circleShader =
      Shader[FragEnv, Float] { env =>
        circleSdf(vec2(1.0, 2.0), 3.0)
      }

    val actual1 =
      circleShader.toGLSL[WebGL2]

    // println(ShaderMacros.toAST(circleShader))

    assertEquals(
      actual1,
      s"""
      |float circleSdf(in vec2 val0,in float val1){
      |  return length(val0)-3.0;
      |}
      |circleSdf(vec2(1.0,2.0),3.0);
      |""".stripMargin.trim
    )
  }

  test("can call a native function 2") {

    inline def circleShader2: Shader[FragEnv, Float] =
      Shader { env =>
        def circleSdf(p: vec2, r: Float): Float =
          length(p) - r

        circleSdf(env.UV, 3.0)
      }

    val actual2 =
      circleShader2.toGLSL[WebGL2]

    // println(ShaderMacros.toAST(circleShader2))

    assertEquals(
      actual2,
      s"""
      |float circleSdf(in vec2 p,in float r){
      |  return length(p)-r;
      |}
      |circleSdf(UV,3.0);
      |""".stripMargin.trim
    )
  }

  test("can build a multi-statement function") {

    inline def shader: Shader[FragEnv, vec4] =
      Shader { env =>
        def calculateColour(uv: vec2, sdf: Float): vec4 =
          val fill       = vec4(uv, 0.0f, 1.0f)
          val fillAmount = (1.0f - step(0.0f, sdf)) * fill.w
          vec4(fill.xyz * fillAmount, fillAmount)

        calculateColour(env.UV, 3.0)
      }

    val actual =
      shader.toGLSL[WebGL2]

    assertEquals(
      actual,
      s"""
      |vec4 calculateColour(in vec2 uv,in float sdf){
      |  vec4 fill=vec4(uv,0.0,1.0);
      |  float fillAmount=(1.0-step(0.0,sdf))*fill.w;
      |  return vec4(fill.xyz*fillAmount,fillAmount);
      |}
      |calculateColour(UV,3.0);
      |""".stripMargin.trim
    )
  }

  test("Small procedural shader") {

    inline def fragment: Shader[FragEnv, vec4] =
      Shader { env =>
        def circleSdf(p: vec2, r: Float): Float =
          length(p) - r

        def calculateColour(uv: vec2, sdf: Float): vec4 =
          val fill       = vec4(uv, 0.0f, 1.0f)
          val fillAmount = (1.0f - step(0.0f, sdf)) * fill.w
          vec4(fill.xyz * fillAmount, fillAmount)

        val sdf = circleSdf(env.UV - 0.5f, 0.5f)

        calculateColour(env.UV, sdf)
      }

    val actual =
      fragment.toGLSL[WebGL2]

    assertEquals(
      actual,
      s"""
      |float circleSdf(in vec2 p,in float r){
      |  return length(p)-r;
      |}
      |vec4 calculateColour(in vec2 uv,in float sdf){
      |  vec4 fill=vec4(uv,0.0,1.0);
      |  float fillAmount=(1.0-step(0.0,sdf))*fill.w;
      |  return vec4(fill.xyz*fillAmount,fillAmount);
      |}
      |float sdf=circleSdf(UV-0.5,0.5);
      |calculateColour(UV,sdf);
      |""".stripMargin.trim
    )
  }

  test("Output a color / Assign") {

    inline def fragment =
      Shader[FragEnv] { env =>
        env.COLOR = vec4(1.0f, 0.0f, 0.0f, 1.0f)
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |COLOR=vec4(1.0,0.0,0.0,1.0);
      |""".stripMargin.trim
    )
  }

  test("Small procedural shader with fragment function") {

    inline def fragment =
      Shader[FragEnv] { env =>
        def circleSdf(p: vec2, r: Float): Float =
          length(p) - r

        def calculateColour(uv: vec2, sdf: Float): vec4 =
          val fill       = vec4(uv, 0.0f, 1.0f)
          val fillAmount = (1.0f - step(0.0f, sdf)) * fill.w
          vec4(fill.xyz * fillAmount, fillAmount)

        def fragment: Unit =
          val sdf = circleSdf(env.UV - 0.5f, 0.5f)
          env.COLOR = calculateColour(env.UV, sdf)
      }

    val actual =
      fragment.toGLSL[WebGL2]

    val expected =
      s"""
      |float circleSdf(in vec2 p,in float r){
      |  return length(p)-r;
      |}
      |vec4 calculateColour(in vec2 uv,in float sdf){
      |  vec4 fill=vec4(uv,0.0,1.0);
      |  float fillAmount=(1.0-step(0.0,sdf))*fill.w;
      |  return vec4(fill.xyz*fillAmount,fillAmount);
      |}
      |void fragment(){
      |  float sdf=circleSdf(UV-0.5,0.5);
      |  COLOR=calculateColour(UV,sdf);
      |}
      |""".stripMargin.trim

    assertEquals(
      actual,
      expected
    )
  }

  test("if else statements") {
    inline def fragment: Shader[FragEnv, vec4] =
      Shader { _ =>
        val red    = vec4(1.0, 0.0, 0.0, 1.0)
        val green  = vec4(0.0, 1.0, 0.0, 1.0)
        val blue   = vec4(0.0, 0.0, 1.0, 1.0)
        val x: Int = 1

        if x <= 0 then red
        else if x == 1 then blue
        else green
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |vec4 red=vec4(1.0,0.0,0.0,1.0);
      |vec4 green=vec4(0.0,1.0,0.0,1.0);
      |vec4 blue=vec4(0.0,0.0,1.0,1.0);
      |int x=1;
      |if(x<=0){
      |  red;
      |}else{
      |  if(x==1){
      |    blue;
      |  }else{
      |    green;
      |  }
      |}
      |""".stripMargin.trim
    )
  }

  test("if (no else) statements") {
    @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
    inline def fragment =
      Shader[FragEnv] { _ =>
        var x: Int = 1
        if x <= 10 then x = 15
        x
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |int x=1;
      |if(x<=10){
      |  x=15;
      |}
      |x;
      |""".stripMargin.trim
    )
  }

  test("if statements can be used to set vals") {
    inline def fragment =
      Shader[FragEnv, Int] { _ =>
        val x: Int = 1
        val y      = if x <= 10 then 15 else 20
        val z =
          val amount = 15
          if x <= 10 then amount else 20
        y + z
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |int x=1;
      |int y;
      |if(x<=10){
      |  y=15;
      |}else{
      |  y=20;
      |}
      |int amount=15;
      |int z;
      |if(x<=10){
      |  z=amount;
      |}else{
      |  z=20;
      |}
      |y+z;
      |""".stripMargin.trim
    )
  }

  test("if statements can be used to return functions") {
    inline def fragment =
      Shader[FragEnv, Int] { _ =>
        def p1(x: Int): Int =
          if x <= 10 then 15 else 20
        p1(1)

        def p2(x: Int): Int =
          val amount = 10 // Forces a Block in the function body.
          if x <= amount then 15 else 20
        p2(1)
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |int p1(in int x){
      |  int val0;
      |  if(x<=10){
      |    val0=15;
      |  }else{
      |    val0=20;
      |  }
      |  return val0;
      |}
      |p1(1);
      |int p2(in int x){
      |  int amount=10;
      |  int val1;
      |  if(x<=amount){
      |    val1=15;
      |  }else{
      |    val1=20;
      |  }
      |  return val1;
      |}
      |p2(1);
      |""".stripMargin.trim
    )
  }

  test("casting") {
    inline def fragment: Shader[FragEnv, Float] =
      Shader { _ =>
        val x  = 1.0f.toInt
        val y  = 1.toFloat
        val z  = y.toInt
        val w1 = 2
        val w2 = (1 + w1).toFloat
        x + y
      }

    val actual =
      fragment.toGLSL[WebGL2]

    assertEquals(
      actual,
      s"""
      |int x=int(1.0);
      |float y=float(1);
      |int z=int(y);
      |int w1=2;
      |float w2=float(1+w1);
      |x+y;
      |""".stripMargin.trim
    )
  }

  test("switch statements / pattern matching") {

    @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
    inline def fragment =
      Shader[FragEnv] { _ =>
        val flag: Int = 2

        var res: Int = -1

        flag match
          case 0 => res = 10
          case 1 => res = 20
          case 2 => res = 30
          case _ => res = -100

        res
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |int flag=2;
      |int res=-1;
      |switch(flag){
      |  case 0:
      |    res=10;
      |    break;
      |  case 1:
      |    res=20;
      |    break;
      |  case 2:
      |    res=30;
      |    break;
      |  default:
      |    res=-100;
      |    break;
      |}
      |res;
      |""".stripMargin.trim
    )
  }

  test("pattern matching can set a val") {

    inline def fragment =
      Shader[FragEnv, Int] { _ =>
        val flag: Int = 2

        val res1: Int = flag match
          case 0 => 10
          case 1 => 20
          case 2 => 30
          case _ => -100

        val res2: Int = flag match
          case 0 => 10
          case 1 => 20
          case 2 =>
            val amount = 30
            amount + 1
          case _ => -100

        val res3: Int =
          val zeroRes = 10
          flag match
            case 0 => zeroRes
            case 1 => 20
            case 2 =>
              val amount = 30
              amount + 1
            case _ => -100

        res1 + res2 + res3
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |int flag=2;
      |int res1;
      |switch(flag){
      |  case 0:
      |    res1=10;
      |    break;
      |  case 1:
      |    res1=20;
      |    break;
      |  case 2:
      |    res1=30;
      |    break;
      |  default:
      |    res1=-100;
      |    break;
      |}
      |int res2;
      |switch(flag){
      |  case 0:
      |    res2=10;
      |    break;
      |  case 1:
      |    res2=20;
      |    break;
      |  case 2:
      |    int amount=30;
      |    res2=amount+1;
      |    break;
      |  default:
      |    res2=-100;
      |    break;
      |}
      |int zeroRes=10;
      |int res3;
      |switch(flag){
      |  case 0:
      |    res3=zeroRes;
      |    break;
      |  case 1:
      |    res3=20;
      |    break;
      |  case 2:
      |    int amount=30;
      |    res3=amount+1;
      |    break;
      |  default:
      |    res3=-100;
      |    break;
      |}
      |(res1+res2)+res3;
      |""".stripMargin.trim
    )
  }

  test("while loops") {

    @SuppressWarnings(Array("scalafix:DisableSyntax.var", "scalafix:DisableSyntax.while"))
    inline def fragment: Shader[FragEnv, Float] =
      Shader { _ =>
        var i = 0.0f
        while i < 4.0f do i += 1.0f
        i
      }

    val actual =
      fragment.toGLSL[WebGL2]

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

  test("for loops") {

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
      fragment.toGLSL[WebGL2]

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

  test("imports") {
    import Importable.*

    inline def fragment: Shader[FragEnv, Int] =
      Shader { _ =>
        addOne(10)
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |int addOne(in int val0){
      |  return 11;
      |}
      |addOne(10);
      |""".stripMargin.trim
    )
  }

  test("local unary lambda function (val)") {
    inline def fragment: Shader[FragEnv, vec3] =
      Shader { _ =>
        val f: Float => vec3 = r => vec3(r, 0.0f, 0.0f)
        val g                = (b: Float) => vec3(0.0f, 0.0f, b)
        f(1.0f) + g(2.0)
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |vec3 def0(in float r){
      |  return vec3(r,0.0,0.0);
      |}
      |vec3 def1(in float b){
      |  return vec3(0.0,0.0,b);
      |}
      |def0(1.0)+def1(2.0);
      |""".stripMargin.trim
    )
  }

  test("local unary lambda function (def)") {
    inline def fragment: Shader[FragEnv, vec3] =
      Shader { _ =>
        def f: Float => vec3 = r => vec3(r, 0.0f, 0.0f)
        def g                = (b: Float) => vec3(0.0f, 0.0f, b)
        f(1.0f) + g(2.0)
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |vec3 def0(in float r){
      |  return vec3(r,0.0,0.0);
      |}
      |vec3 def1(in float b){
      |  return vec3(0.0,0.0,b);
      |}
      |def0(1.0)+def1(2.0);
      |""".stripMargin.trim
    )
  }

  test("compose (Function1)") {
    inline def fragment: Shader[FragEnv, vec4] =
      Shader { _ =>
        val f: Float => vec3 = r => vec3(r, 0.0f, 0.0f)
        val g: vec3 => vec4  = val3 => vec4(val3, 0.5f)
        val h: Float => vec4 = g compose f

        h(1.0f)
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |vec3 def0(in float r){
      |  return vec3(r,0.0,0.0);
      |}
      |vec4 def1(in vec3 val3){
      |  return vec4(val3,0.5);
      |}
      |vec4 def2(in float val0){
      |  return def1(def0(val0));
      |}
      |def2(1.0);
      |""".stripMargin.trim
    )
  }

  test("andThen (Function1)") {
    inline def fragment: Shader[FragEnv, vec4] =
      Shader { _ =>
        val f: Float => vec3 = r => vec3(r, 0.0f, 0.0f)
        val g: vec3 => vec4  = val3 => vec4(val3, 0.5f)
        val h: Float => vec4 = f andThen g

        h(1.0f)
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |vec3 def0(in float r){
      |  return vec3(r,0.0,0.0);
      |}
      |vec4 def1(in vec3 val3){
      |  return vec4(val3,0.5);
      |}
      |vec4 def2(in float val0){
      |  return def1(def0(val0));
      |}
      |def2(1.0);
      |""".stripMargin.trim
    )
  }

  test("scoping is respected") {
    inline def fragment: Shader[FragEnv, vec3] =
      Shader { _ =>
        def foo: vec3 =
          val f: Float => vec3 = r => vec3(r, 0.0f, 0.0f)
          f(1.0)

        def bar: vec3 =
          val f: vec2 => vec3 = rg => vec3(rg, 0.0f)
          f(vec2(0.5))
        foo + bar
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |vec3 def0(in float r){
      |  return vec3(r,0.0,0.0);
      |}
      |vec3 def1(in vec2 rg){
      |  return vec3(rg,0.0);
      |}
      |vec3 foo(){
      |  return def0(1.0);
      |}
      |vec3 bar(){
      |  return def1(vec2(0.5));
      |}
      |foo()+bar();
      |""".stripMargin.trim
    )
  }

  test("can embed raw GLSL") {
    inline def fragment: Shader[FragEnv, RawGLSL] =
      Shader { _ =>
        RawGLSL("float v = 1.0;")
        raw("COLOR = vec4(v, v, v, 0.5);")
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |float v = 1.0;
      |COLOR = vec4(v, v, v, 0.5);
      |""".stripMargin.trim
    )
  }

  test("Can define a UBO struct") {

    case class UBO1(TIME: highp[Float], val VIEWPORT_SIZE: vec2)
    case class UBO2(customColor: vec4, pos: lowp[vec3])

    inline def fragment =
      Shader[UBO1 & UBO2 & FragEnv, Unit](
        GLSLHeader.Version300ES,
        GLSLHeader.PrecisionHighPFloat
      ) { env =>
        ubo[UBO1]
        ubo[UBO2]
        env.COLOR = vec4(env.UV, env.TIME, 1.0f)
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |#version 300 es
      |precision highp float;
      |layout (std140) uniform UBO1 {
      |  highp float TIME;
      |  vec2 VIEWPORT_SIZE;
      |};
      |layout (std140) uniform UBO2 {
      |  vec4 customColor;
      |  lowp vec3 pos;
      |};
      |COLOR=vec4(UV,TIME,1.0);
      |""".stripMargin.trim
    )

  }

  test("Annotated variables render correctly") {

    @SuppressWarnings(Array("scalafix:DisableSyntax.null", "scalafix:DisableSyntax.var"))
    inline def fragment =
      Shader {
        // @attribute var a: Float = 0.0f // Scala doesn't allow for primitives. // WebGL 1.0 only
        @const var b: vec2 = null
        // @in var c: vec3         = null // WebGL 2.0 only
        // @out var d: vec4        = null // WebGL 2.0 only
        @uniform var e: Float = 0.0f
        // @varying var f: Float   = 0.0f // WebGL 1.0 only
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |const vec2 b;
      |uniform float e;
      |""".stripMargin.trim
    )

  }

  test("Supports samplers") {

    @SuppressWarnings(Array("scalafix:DisableSyntax.var"))
    case class Env(var COLOR: vec4)

    @SuppressWarnings(Array("scalafix:DisableSyntax.null"))
    inline def fragment =
      Shader[Env, Unit] { env =>
        @in val v_texcoord: vec2   = null
        @in val v_normal: vec3     = null
        @uniform val u_texture2d   = sampler2D
        @uniform val u_textureCube = samplerCube

        val c: vec4 = texture2D(u_texture2d, v_texcoord);
        env.COLOR = textureCube(u_textureCube, normalize(v_normal)) * c
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |in vec2 v_texcoord;
      |in vec3 v_normal;
      |uniform sampler2D u_texture2d;
      |uniform samplerCube u_textureCube;
      |vec4 c=texture(u_texture2d,v_texcoord);
      |COLOR=texture(u_textureCube,normalize(v_normal))*c;
      |""".stripMargin.trim
    )

  }

  test("arrays - initialise and check length") {

    @SuppressWarnings(Array("scalafix:DisableSyntax.null"))
    inline def fragment =
      Shader {
        val x: array[Float, 12] = null
        val y                   = x.length
        y
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |float[12] x;
      |int y=x.length();
      |y;
      |""".stripMargin.trim
    )
  }

  test("arrays - more complicated example") {

    case class Env(
        VERTICES: array[vec2, 16],
        COUNT: Float,
        SIZE: vec2
    )

    @SuppressWarnings(
      Array("scalafix:DisableSyntax.null", "scalafix:DisableSyntax.var", "scalafix:DisableSyntax.while")
    )
    inline def fragment =
      Shader[Env, Unit] { env =>
        @const val MAX_VERTICES: 16 = 16

        def toUvSpace(count: Int, v: array[vec2, MAX_VERTICES.type]): array[vec2, MAX_VERTICES.type] =
          val polygon: array[vec2, MAX_VERTICES.type] = null

          var i = 0
          while i < count do
            polygon(i) = v(i) / env.SIZE;
            i += 1

          polygon

        val iCount: Int                             = env.COUNT.toInt;
        val polygon: array[vec2, MAX_VERTICES.type] = toUvSpace(iCount, env.VERTICES);
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |const int MAX_VERTICES=16;
      |vec2[MAX_VERTICES] toUvSpace(in int count,in vec2[MAX_VERTICES] v){
      |  vec2[MAX_VERTICES] polygon;
      |  int i=0;
      |  while(i<count){
      |    polygon[i]=(v(i)/SIZE);
      |    i=i+1;
      |  }
      |  return polygon;
      |}
      |int iCount=int(COUNT);
      |vec2[MAX_VERTICES] polygon=toUvSpace(iCount,VERTICES);
      |""".stripMargin.trim
    )
  }

  test("modulus") {

    inline def fragment =
      Shader {
        val x = mod(10.0f, 2.0f)
        val y = 2.0f
        val z = 10.0f % y
        val w = 10.0f % 3.0f
      }

    val actual =
      fragment.toGLSL[WebGL2]

    // DebugAST.toAST(fragment)
    // println(actual)

    assertEquals(
      actual,
      s"""
      |float x=mod(10.0,2.0);
      |float y=2.0;
      |float z=mod(10.0,y);
      |float w=1.0;
      |""".stripMargin.trim
    )
  }

}

object Importable:

  inline def addOne(i: Int): Int = i + 1
