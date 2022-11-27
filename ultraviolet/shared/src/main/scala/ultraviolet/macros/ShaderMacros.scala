package ultraviolet.macros

import ultraviolet.datatypes.ProceduralShader
import ultraviolet.datatypes.ShaderAST
import ultraviolet.datatypes.UBODef
import ultraviolet.datatypes.UBOField
import ultraviolet.syntax.*

import scala.annotation.tailrec
import scala.quoted.*

object ShaderMacros:

  inline def toAST[In, Out](inline expr: Shader[In, Out]): ProceduralShader = ${ toASTImpl('{ expr }) }

  private[macros] def toASTImpl[In, Out: Type](expr: Expr[Shader[In, Out]])(using q: Quotes): Expr[ProceduralShader] = {
    import q.reflect.*

    val createAST = new CreateShaderAST[q.type](using q)

    val main =
      expr.asTerm match
        // Skip the initial noise...
        case Inlined(_, _, Inlined(_, _, Inlined(_, _, term))) =>
          createAST.walkTerm(term, None)

        case term =>
          createAST.walkTerm(term, None)

    val shaderDefList = createAST.shaderDefs.toList

    Expr(ProceduralShader(shaderDefList.filterNot(_.userDefined).map(_.fn), main))
  }