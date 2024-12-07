package ultraviolet

import ultraviolet.syntax.*

// These tests are run as JS only due to differences between JVM and JS floating point precision
class sdfTests extends munit.FunSuite {

  test("A circle SDF") {

    val p = vec2(1.0f, 0.0f)

    val actual =
      ultraviolet.sdf.circle(p, 1.0f)

    val expected =
      0.0f

    assert(closeEnough(clue(actual), clue(expected)))
  }

  test("Box SDF acceptance test") {
    inline def fragment: Shader[Unit, Float] =
      Shader { _ =>

        import ultraviolet.sdf.*

        def proxy: (vec2, vec2) => Float = (p, b) => box(p, b)

        proxy(vec2(0.5f), vec2(1.5f))
      }

    val actual =
      fragment.toGLSL[WebGL2].toOutput.code

    // DebugAST.toAST(fragment)

    assertEquals(
      actual,
      s"""
      |float def0(in vec2 p,in vec2 b){
      |  vec2 d=abs(p)-b;
      |  return length(max(d,0.0))+min(max(d.x,d.y),0.0);
      |}
      |def0(vec2(0.5),vec2(1.5));
      |""".stripMargin.trim
    )
  }

  test("Circle SDF acceptance test") {
    inline def fragment: Shader[Unit, Float] =
      Shader { _ =>

        import ultraviolet.sdf.*

        def proxy: (vec2, Float) => Float = (p, r) => circle(p, r)

        proxy(vec2(0.5f), 1.5f)
      }

    val actual =
      fragment.toGLSL[WebGL2].toOutput.code

    // DebugAST.toAST(fragment)

    assertEquals(
      actual,
      s"""
      |float def0(in vec2 p,in float r){
      |  return length(p)-r;
      |}
      |def0(vec2(0.5),1.5);
      |""".stripMargin.trim
    )
  }

  test("Hexagon SDF acceptance test") {
    inline def fragment: Shader[Unit, Float] =
      Shader { _ =>

        import ultraviolet.sdf.*

        def proxy: (vec2, Float) => Float = (p, r) => hexagon(p, r)

        proxy(vec2(0.5f), 1.5f)
      }

    val actual =
      fragment.toGLSL[WebGL2].toOutput.code

    // DebugAST.toAST(fragment)

    assertEquals(
      actual,
      s"""
      |float def0(in vec2 p,in float r){
      |  vec3 k=vec3(-0.8660253882408142,0.5,0.5773502588272095);
      |  vec2 pt=abs(p);
      |  pt=pt-((2.0*min(dot(k.xy,pt),0.0))*k.xy);
      |  pt=pt-(vec2(clamp(pt.x,(-k.z)*r,k.z*r),r));
      |  return length(pt)*sign(pt.y);
      |}
      |def0(vec2(0.5),1.5);
      |""".stripMargin.trim
    )
  }

  test("Segment SDF acceptance test") {
    inline def fragment: Shader[Unit, Float] =
      Shader { _ =>

        import ultraviolet.sdf.*

        def proxy: (vec2, vec2, vec2) => Float = (p, a, b) => segment(p, a, b)

        proxy(vec2(0.5f), vec2(0.0f), vec2(1.0f))
      }

    val actual =
      fragment.toGLSL[WebGL2].toOutput.code

    // DebugAST.toAST(fragment)

    assertEquals(
      actual,
      s"""
      |float def0(in vec2 p,in vec2 a,in vec2 b){
      |  vec2 pa=p-a;
      |  vec2 ba=b-a;
      |  float h=clamp(dot(pa,ba)/dot(ba,ba),0.0,1.0);
      |  return length(pa-(ba*h));
      |}
      |def0(vec2(0.5),vec2(0.0),vec2(1.0));
      |""".stripMargin.trim
    )
  }

  test("Star with five points SDF acceptance test") {
    inline def fragment: Shader[Unit, Float] =
      Shader { _ =>

        import ultraviolet.sdf.*

        def proxy: (vec2, Float, Float) => Float = (p, r, rf) => star(p, r, rf)

        proxy(vec2(0.5f), 1.5f, 1.0f)
      }

    val actual =
      fragment.toGLSL[WebGL2].toOutput.code

    // DebugAST.toAST(fragment)

    assertEquals(
      actual,
      s"""
      |float def0(in vec2 p,in float r,in float rf){
      |  vec2 k1=vec2(0.80901700258255,-0.5877852439880371);
      |  vec2 k2=vec2(-k1.x,k1.y);
      |  vec2 p2=vec2(abs(p.x),-p.y);
      |  p2=p2-((2.0*max(dot(k1,p2),0.0))*k1);
      |  p2=p2-((2.0*max(dot(k2,p2),0.0))*k2);
      |  p2=vec2(abs(p2.x),p2.y-r);
      |  vec2 ba=(rf*(vec2(-k1.y,k1.x)))-vec2(0.0,1.0);
      |  float h=clamp(dot(p2,ba)/dot(ba,ba),0.0,r);
      |  return (length(p2-(ba*h)))*(sign((p2.y*ba.x)-(p2.x*ba.y)));
      |}
      |def0(vec2(0.5),1.5,1.0);
      |""".stripMargin.trim
    )
  }

  test("Triangle SDF acceptance test") {
    inline def fragment: Shader[Unit, Float] =
      Shader { _ =>

        import ultraviolet.sdf.*

        def proxy: (vec2, Float) => Float = (p, r) => triangle(p, r)

        proxy(vec2(0.5f), 1.5f)
      }

    val actual =
      fragment.toGLSL[WebGL2].toOutput.code

    // println(DebugAST.toAST(fragment))

    assertEquals(
      actual,
      s"""
      |float def0(in vec2 p,in float r){
      |  float k=sqrt(3.0);
      |  vec2 pt=vec2(abs(p.x)-r,(-p.y)+(r/k));
      |  if((pt.x+(k*pt.y))>0.0){
      |    pt=(vec2(pt.x-(k*pt.y),((-k)*pt.x)-pt.y))/2.0;
      |  }
      |  pt=vec2(pt.x-(clamp(pt.x,-r,0.0)),pt.y);
      |  return (-length(pt))*sign(pt.y);
      |}
      |def0(vec2(0.5),1.5);
      |""".stripMargin.trim
    )
  }

  def closeEnough(a: Float, b: Float): Boolean =
    Math.abs(Math.abs(a) - Math.abs(b)) < 0.01f

}
