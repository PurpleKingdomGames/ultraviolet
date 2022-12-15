package ultraviolet.datatypes

import ultraviolet.DebugAST
import ultraviolet.macros.ShaderMacros
import ultraviolet.syntax.*

import scala.collection.mutable.ListBuffer

class ShaderASTTests extends munit.FunSuite {

  test("traverse - can find all the val's") {
    inline def fragment =
      Shader {
        val h = vec2(10)
        val i = 10
      }

    val acc = new ListBuffer[String]()

    val ast = ShaderMacros.toAST(fragment).main
    
    // This doesn't seem to work.
    ast.traverse {
      case v @ ShaderAST.Val(id, _, _) =>
        acc += id
        v
    }

    // FINDS NOTHING.
    assertEquals(acc.length, 2)
    assertEquals(acc.toList, List("h", "i"))
  }

}
