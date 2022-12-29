package ultraviolet.macros

import ultraviolet.datatypes.ShaderAST
import ultraviolet.datatypes.ShaderError
import ultraviolet.datatypes.UBODef
import ultraviolet.datatypes.UBOField

import scala.collection.mutable.ListBuffer
import scala.quoted.Quotes

@SuppressWarnings(Array("scalafix:DisableSyntax.throw"))
class CreateShaderAST[Q <: Quotes](using val qq: Q) extends ShaderMacroUtils:
  import qq.reflect.*

  val uboUtils                               = new ExtractUBOUtils[qq.type](using qq)
  val proxies                                = new ProxyManager
  val shaderDefs: ListBuffer[FunctionLookup] = new ListBuffer()
  val structRegister: ListBuffer[String]     = new ListBuffer()

  def extractInferredType(typ: TypeTree): Option[String] =
    def mapName(name: Option[String]): Option[String] =
      name
        .map {
          case "Boolean"      => "bool"
          case "Float"        => "float"
          case "Int"          => "int"
          case "vec2"         => "vec2"
          case "vec3"         => "vec3"
          case "vec4"         => "vec4"
          case "bvec2"        => "bvec2"
          case "bvec3"        => "bvec3"
          case "bvec4"        => "bvec4"
          case "ivec2"        => "ivec2"
          case "ivec3"        => "ivec3"
          case "ivec4"        => "ivec4"
          case "mat2"         => "mat2"
          case "mat3"         => "mat3"
          case "mat4"         => "mat4"
          case "sampler2D$"   => "sampler2D"
          case "samplerCube$" => "samplerCube"
          case n              => n
        }
        .filterNot {
          case "Unit" | "array" =>
            true
          case n if n.startsWith("Function") =>
            true
          case _ =>
            false
        }

    typ match
      case Applied(TypeIdent("array"), List(Singleton(Literal(IntConstant(size))), TypeIdent(typeName))) =>
        mapName(Option(typeName)).map(_ + s"[${size.toString()}]")

      case Applied(TypeIdent("array"), List(Singleton(Ident(varName)), TypeIdent(typeName))) =>
        mapName(Option(typeName)).map(_ + s"[$varName]")

      case _ =>
        mapName(typ.tpe.classSymbol.map(_.name))

  def extractInferredTypeParam(typ: TypeTree): Option[String] =
    def extract(t: Tree): Option[String] =
      t match
        case Applied(TypeIdent("&"), List(t1, t2)) =>
          for {
            a <- extract(t1)
            b <- extract(t2)
          } yield s"""$a & $b"""

        case TypeIdent(typeName) =>
          Option(typeName)

        case _ =>
          None

    typ match
      case Applied(TypeIdent("&"), List(t1, t2)) =>
        for {
          a <- extract(t1)
          b <- extract(t2)
        } yield s"""$a & $b"""

      case TypeIdent(typeName) =>
        Option(typeName)

      case x =>
        typ.tpe.classSymbol.map(_.name)

  def isOneLineLambda(args: List[Statement]): Boolean =
    args.nonEmpty &&
      args.forall {
        case ValDef(name, _, Some(Literal(_))) if name.contains("$proxy") =>
          true

        case ValDef(name, _, Some(Ident(_))) if name.contains("$proxy") =>
          true

        case _ =>
          false
      }

  def assignToLast(lhs: ShaderAST): ShaderAST => ShaderAST = {
    case ShaderAST.Block(statements :+ last) =>
      ShaderAST.Block(statements :+ ShaderAST.Assign(lhs, last))

    case last =>
      ShaderAST.Assign(lhs, last)
  }

  def recursivelyAssignIf(assignTo: ShaderAST.DataTypes.ident, maybeIf: ShaderAST): ShaderAST =
    maybeIf match
      case ShaderAST.If(cond, thn, Some(els @ ShaderAST.If(_, _, Some(_)))) =>
        ShaderAST.If(
          cond,
          assignToLast(assignTo)(recursivelyAssignIf(assignTo, thn)),
          Option(recursivelyAssignIf(assignTo, els))
        )

      case ShaderAST.If(cond, thn, Some(ShaderAST.Block(statements :+ (els @ ShaderAST.If(_, _, Some(_)))))) =>
        ShaderAST.If(
          cond,
          assignToLast(assignTo)(recursivelyAssignIf(assignTo, thn)),
          Option(
            ShaderAST.Block(
              statements :+ recursivelyAssignIf(assignTo, els)
            )
          )
        )

      case ShaderAST.If(cond, thn, Some(els)) =>
        ShaderAST.If(
          cond,
          assignToLast(assignTo)(recursivelyAssignIf(assignTo, thn)),
          Option(assignToLast(assignTo)(recursivelyAssignIf(assignTo, els)))
        )

      case x =>
        x

  def walkStatement(s: Statement, envVarName: Option[String]): ShaderAST =
    s match
      case Import(_, _) =>
        ShaderAST.Empty()

      case Export(_, _) =>
        throw ShaderError.Unsupported("Shaders do not support exports.")

      case ClassDef(name, _, _, _, _) if name.endsWith("$") =>
        throw ShaderError.Unsupported(
          "Looks like you're trying to use a case class. Shaders only support simple, flat classes."
        )

      case ClassDef(_, DefDef("<init>", List(TermParamClause(Nil)), _, None), _, _, _) =>
        throw ShaderError.Unsupported(
          "Looks like you're trying to use a trait or a class with no members. Shaders only support simple, flat classes with members."
        )

      case ClassDef(name, DefDef("<init>", List(TermParamClause(params)), _, None), _, _, _) =>
        structRegister += name
        ShaderAST.Struct(name, params.map(p => walkTree(p, envVarName)))

      case ClassDef(_, _, _, _, _) =>
        throw ShaderError.Unsupported("Shaders only support simple, flat classes.")

      case TypeDef(_, _) =>
        throw ShaderError.Unsupported("Shaders do not support fancy types. :-)")

      case ValDef(name, _, _) if isGLSLReservedWord(name) =>
        throw ShaderError.GLSLReservedWord(name)

      case v @ ValDef(name, typ, Some(term)) =>
        val body = walkTerm(term, envVarName)

        val typeOf: Option[ShaderAST] =
          extractInferredType(typ)
            .map(s => ShaderAST.DataTypes.ident(s))
            .orElse {
              body match
                case ShaderAST.CallFunction(name, _, _, _) =>
                  shaderDefs.find(_.fn.id == name).flatMap(_.fn.returnType)

                case _ =>
                  None
            }

        val maybeAnnotation: Option[(ShaderAST.DataTypes.ident, ShaderAST)] =
          v.symbol.annotations.headOption.map(p => walkTerm(p, envVarName)).flatMap {
            case a: ShaderAST.DataTypes.ident =>
              Option((a, ShaderAST.Empty()))

            case ShaderAST.New(anno, List(annoTerm)) =>
              Option((ShaderAST.DataTypes.ident(anno), annoTerm))

            case _ =>
              None
          }

        body match
          case ShaderAST.FunctionRef(fnName, fnArg, fnOutType) =>
            proxies.add(name, fnName, fnArg, fnOutType)
            ShaderAST.Empty()

          case ShaderAST.Block(List(r @ ShaderAST.FunctionRef(id, arg, rt))) =>
            proxies.add(name, id, arg, rt)
            ShaderAST.Empty()

          case ShaderAST.Block(statements :+ (ifs @ ShaderAST.If(_, _, Some(_)))) =>
            val resVal: ShaderAST.DataTypes.ident = ShaderAST.DataTypes.ident(name)
            ShaderAST.Block(
              statements ++ List(
                ShaderAST.Val(name, ShaderAST.Empty(), typeOf),
                recursivelyAssignIf(resVal, ifs)
              )
            )

          case ShaderAST.Block(statements :+ ShaderAST.Switch(on, cases)) =>
            val resVal = ShaderAST.DataTypes.ident(name)
            ShaderAST.Block(
              statements ++ List(
                ShaderAST.Val(name, ShaderAST.Empty(), typeOf),
                ShaderAST.Switch(
                  on,
                  cases.map { case (i, c) =>
                    i -> assignToLast(resVal)(c)
                  }
                )
              )
            )

          case ifs @ ShaderAST.If(_, _, Some(_)) =>
            val resVal: ShaderAST.DataTypes.ident = ShaderAST.DataTypes.ident(name)
            ShaderAST.Block(
              List(
                ShaderAST.Val(name, ShaderAST.Empty(), typeOf),
                recursivelyAssignIf(resVal, ifs)
              )
            )

          case ShaderAST.Switch(on, cases) =>
            val resVal = ShaderAST.DataTypes.ident(name)
            ShaderAST.Block(
              List(
                ShaderAST.Val(name, ShaderAST.Empty(), typeOf),
                ShaderAST.Switch(
                  on,
                  cases.map { case (i, c) =>
                    i -> assignToLast(resVal)(c)
                  }
                )
              )
            )

          case _ =>
            val v =
              body match
                case arr @ ShaderAST.DataTypes.array(size, args, typeOfArray) =>
                  typeOfArray match
                    case None =>
                      throw ShaderError.Unsupported("Shader arrays must be fully typed")

                    case Some(tOf) =>
                      val (tName, tSize) = tOf.splitAt(tOf.indexOf("["))

                      ShaderAST.Val(
                        name + tSize,
                        body,
                        Option(ShaderAST.DataTypes.ident(tName))
                      )

                case _ =>
                  ShaderAST.Val(name, body, typeOf)

            maybeAnnotation match
              case None =>
                v

              case Some((label, param)) =>
                ShaderAST.Annotated(label, param, v)

      case v @ ValDef(name, typ, None) =>
        val typeOf = extractInferredType(typ).map(s => ShaderAST.DataTypes.ident(s))

        val maybeAnnotation: Option[(ShaderAST.DataTypes.ident, ShaderAST)] =
          v.symbol.annotations.headOption.map(p => walkTerm(p, envVarName)).flatMap {
            case a: ShaderAST.DataTypes.ident =>
              Option((a, ShaderAST.Empty()))

            case ShaderAST.New(anno, List(annoTerm)) =>
              Option((ShaderAST.DataTypes.ident(anno), annoTerm))

            case _ =>
              None
          }

        val vv = ShaderAST.Val(name, ShaderAST.Empty(), typeOf)

        maybeAnnotation match
          case None =>
            vv

          case Some((label, param)) =>
            ShaderAST.Annotated(label, param, vv)

      case DefDef(name, _, _, _) if isGLSLReservedWord(name) =>
        throw ShaderError.GLSLReservedWord(name)

      case d @ DefDef(fnName, args, rt, Some(term)) =>
        val argNamesTypes =
          args
            .collect { case TermParamClause(ps) => ps }
            .flatten
            .collect { case ValDef(name, typ, _) =>
              val typeOf = extractInferredType(typ)
              (typeOf.getOrElse("void"), name)
            }

        val isAnon = fnName == "$anonfun"
        val fn     = if isAnon then proxies.makeDefName else fnName
        val body   = walkTerm(term, envVarName)

        val returnType =
          extractInferredType(rt)
            .map(s => ShaderAST.DataTypes.ident(s))
            .orElse {
              rt match
                case rtt @ TypeIdent(_) =>
                  Option(walkTree(rtt, envVarName))

                case _ =>
                  findReturnType(body)
            }

        def register(fnDef: ShaderAST.Function, isAnon: Boolean): ShaderAST =
          shaderDefs += FunctionLookup(fnDef, !isAnon)

          if isAnon then ShaderAST.FunctionRef(fn, fnDef.args.map(_._1), returnType)
          else fnDef

        body match
          case ShaderAST.Block(List(ShaderAST.FunctionRef(id, arg, rt))) =>
            proxies.add(fn, id, arg, rt)
            ShaderAST.Empty()

          case ShaderAST.Block(statements :+ (ifs @ ShaderAST.If(_, _, Some(_)))) =>
            val name                              = proxies.makeVarName
            val resVal: ShaderAST.DataTypes.ident = ShaderAST.DataTypes.ident(name)
            val typeOf                            = extractInferredType(rt).map(s => ShaderAST.DataTypes.ident(s))
            val fnBody =
              ShaderAST.Block(
                List(
                  ShaderAST.Val(name, ShaderAST.Empty(), typeOf),
                  recursivelyAssignIf(resVal, ifs),
                  resVal
                )
              )

            register(
              ShaderAST.Function(
                fn,
                argNamesTypes.map(p => ShaderAST.DataTypes.ident(p._1) -> p._2),
                ShaderAST.Block(statements ++ fnBody.statements),
                returnType
              ),
              isAnon
            )

          case ShaderAST.Block(statements :+ ShaderAST.Switch(on, cases))
              if returnType.isDefined && !returnType.contains(ShaderAST.DataTypes.ident("void")) =>
            val name   = proxies.makeVarName
            val resVal = ShaderAST.DataTypes.ident(name)
            val typeOf = extractInferredType(rt).map(s => ShaderAST.DataTypes.ident(s))
            val fnBody =
              ShaderAST.Block(
                List(
                  ShaderAST.Val(name, ShaderAST.Empty(), typeOf),
                  ShaderAST.Switch(
                    on,
                    cases.map { case (i, c) =>
                      i -> assignToLast(resVal)(c)
                    }
                  ),
                  resVal
                )
              )

            register(
              ShaderAST.Function(
                fn,
                argNamesTypes.map(p => ShaderAST.DataTypes.ident(p._1) -> p._2),
                ShaderAST.Block(statements ++ fnBody.statements),
                returnType
              ),
              isAnon
            )

          case ifs @ ShaderAST.If(_, _, Some(_)) =>
            val name                              = proxies.makeVarName
            val resVal: ShaderAST.DataTypes.ident = ShaderAST.DataTypes.ident(name)
            val typeOf                            = extractInferredType(rt).map(s => ShaderAST.DataTypes.ident(s))
            val fnBody =
              ShaderAST.Block(
                List(
                  ShaderAST.Val(name, ShaderAST.Empty(), typeOf),
                  recursivelyAssignIf(resVal, ifs),
                  resVal
                )
              )

            register(
              ShaderAST.Function(
                fn,
                argNamesTypes.map(p => ShaderAST.DataTypes.ident(p._1) -> p._2),
                fnBody,
                returnType
              ),
              isAnon
            )

          case ShaderAST.Switch(on, cases)
              if returnType.isDefined && !returnType.contains(ShaderAST.DataTypes.ident("void")) =>
            val name   = proxies.makeVarName
            val resVal = ShaderAST.DataTypes.ident(name)
            val typeOf = extractInferredType(rt).map(s => ShaderAST.DataTypes.ident(s))
            val fnBody =
              ShaderAST.Block(
                List(
                  ShaderAST.Val(name, ShaderAST.Empty(), typeOf),
                  ShaderAST.Switch(
                    on,
                    cases.map { case (i, c) =>
                      i -> assignToLast(resVal)(c)
                    }
                  ),
                  resVal
                )
              )

            register(
              ShaderAST.Function(
                fn,
                argNamesTypes.map(p => ShaderAST.DataTypes.ident(p._1) -> p._2),
                fnBody,
                returnType
              ),
              isAnon
            )

          case b =>
            register(
              ShaderAST.Function(
                fn,
                argNamesTypes.map(p => ShaderAST.DataTypes.ident(p._1) -> p._2),
                body,
                returnType
              ),
              isAnon
            )

      case DefDef(_, _, _, _) =>
        throw ShaderError.UnexpectedConstruction("Unexpected def construction")

      case t: Term =>
        walkTerm(t, envVarName)

      case x =>
        val sample = Printer.TreeStructure.show(x).take(100)
        throw ShaderError.UnexpectedConstruction("Unexpected Statement: " + sample + "(..)")

  def walkTree(t: Tree, envVarName: Option[String]): ShaderAST =
    t match
      case TypeIdent("Unit") =>
        ShaderAST.DataTypes.ident("void")

      case TypeIdent("Boolean") =>
        ShaderAST.DataTypes.ident("bool")

      case TypeIdent("Float") =>
        ShaderAST.DataTypes.ident("float")

      case TypeIdent("Int") =>
        ShaderAST.DataTypes.ident("int")

      case TypeIdent(name) =>
        ShaderAST.DataTypes.ident(name)

      case PackageClause(_, _) =>
        throw ShaderError.Unsupported("Shaders do not support packages.")

      case s: Statement =>
        walkStatement(s, envVarName)

      case Applied(TypeIdent("Shader"), _) =>
        ShaderAST.DataTypes.ident("void")

      case x =>
        val sample = Printer.TreeStructure.show(x).take(100)
        throw ShaderError.UnexpectedConstruction("Unexpected Tree: " + sample + "(..)")

  def walkTerm(t: Term, envVarName: Option[String]): ShaderAST =
    t match

      // Specific hooks we care about

      // Entry point 'from file'
      case Inlined(
            Some(
              Apply(Select(Ident("Shader"), "fromFile"), List(Literal(StringConstant(_))))
            ),
            Nil,
            Typed(
              Inlined(
                Some(
                  Apply(
                    Select(Ident("Shader"), "apply"),
                    List(
                      Inlined(
                        Some(_),
                        Nil,
                        Typed(
                          Inlined(
                            Some(TypeIdent(_)),
                            Nil,
                            term
                          ),
                          _
                        )
                      )
                    )
                  )
                ),
                _,
                _
              ),
              _
            )
          ) =>
        walkTree(term, None)

      // Entry point (with type params, no headers)
      case Apply(
            TypeApply(Select(Ident("Shader"), "apply"), types),
            List(
              Block(
                Nil,
                Block(
                  List(
                    DefDef(
                      "$anonfun",
                      List(TermParamClause(List(ValDef(env, Inferred(), None)))),
                      Inferred(),
                      Some(term)
                    )
                  ),
                  Closure(Ident("$anonfun"), None)
                )
              )
            )
          ) =>
        val e          = Option(env)
        val statements = List(walkTerm(term, e))

        types.map(extractInferredTypeParam) match
          case List(in, out) =>
            ShaderAST.ShaderBlock(in, out, e, Nil, statements)

          case List(in) =>
            ShaderAST.ShaderBlock(in, None, e, Nil, statements)

          case _ =>
            ShaderAST.ShaderBlock(None, None, e, Nil, statements)

      // Entry point (with type params, with headers)
      case Apply(
            Apply(
              TypeApply(Select(Ident("Shader"), "apply"), types),
              headers
            ),
            List(
              Block(
                Nil,
                Block(
                  List(
                    DefDef(
                      "$anonfun",
                      List(TermParamClause(List(ValDef(env, Inferred(), None)))),
                      Inferred(),
                      Some(term)
                    )
                  ),
                  Closure(Ident("$anonfun"), None)
                )
              )
            )
          ) =>
        val e                = Option(env)
        val headerStatements = headers.map(p => walkTerm(p, e))
        val statements       = List(walkTerm(term, e))

        types.map(extractInferredTypeParam) match
          case List(in, out) =>
            ShaderAST.ShaderBlock(in, out, e, headerStatements, statements)

          case List(in) =>
            ShaderAST.ShaderBlock(in, None, e, headerStatements, statements)

          case _ =>
            ShaderAST.ShaderBlock(None, None, e, headerStatements, statements)

      // Entry point (no type params, no headers)
      case Apply(Select(Ident("Shader"), "apply"), args) =>
        ShaderAST.ShaderBlock(None, None, None, Nil, args.map(p => walkTerm(p, envVarName)))

      // Entry point (no type params, with headers)
      case Apply(Apply(Select(Ident("Shader"), "apply"), headers), args) =>
        ShaderAST.ShaderBlock(
          None,
          None,
          None,
          headers.map(p => walkTerm(p, envVarName)),
          args.map(p => walkTerm(p, envVarName))
        )

      case Apply(Select(Ident("RawGLSL"), "apply"), List(term)) =>
        walkTerm(term, envVarName)

      // For loops

      case Apply(
            Apply(
              TypeApply(Ident(forLoopName), _),
              List(
                initial,
                Block(
                  List(DefDef("$anonfun", _, _, Some(condition))),
                  Closure(Ident("$anonfun"), None)
                ),
                Block(
                  List(DefDef("$anonfun", _, _, Some(next))),
                  Closure(Ident("$anonfun"), None)
                )
              )
            ),
            List(
              Block(
                Nil,
                Block(
                  List(DefDef("$anonfun", _, _, Some(body))),
                  _
                )
              )
            )
          ) if forLoopName == "cfor" || forLoopName == "_for" =>
        val varName = proxies.makeVarName
        val init    = walkTerm(initial, envVarName)
        val i = ShaderAST.Val(
          varName,
          init,
          findReturnType(init)
        )

        val c = walkTerm(condition, envVarName).traverse {
          case ShaderAST.DataTypes.ident(id) if id.startsWith("_") =>
            ShaderAST.DataTypes.ident(varName)

          case ShaderAST.Infix(op, left, right, rt) =>
            ShaderAST.Infix(op, left, right, Option(ShaderAST.DataTypes.ident("int")))

          case x =>
            x
        }

        def replaceName: PartialFunction[ShaderAST, ShaderAST] = {
          case ShaderAST.DataTypes.ident(id) if id.startsWith("_") =>
            ShaderAST.DataTypes.ident(varName)
        }

        val n = ShaderAST.Assign(
          ShaderAST.DataTypes.ident(varName),
          walkTerm(next, envVarName).traverse(replaceName)
        )

        val b = walkTerm(body, envVarName).traverse(replaceName)

        ShaderAST.For(i, c, n, b)

      // Primitives

      case Apply(Select(Ident("vec2"), "apply"), args) =>
        args match
          case List(Typed(Repeated(args2, _), _)) =>
            ShaderAST.DataTypes.vec2(args2.map(p => walkTerm(p, envVarName)))
          case _ =>
            ShaderAST.DataTypes.vec2(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Ident("vec3"), "apply"), args) =>
        args match
          case List(Typed(Repeated(args2, _), _)) =>
            ShaderAST.DataTypes.vec3(args2.map(p => walkTerm(p, envVarName)))
          case _ =>
            ShaderAST.DataTypes.vec3(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Ident("vec4"), "apply"), args) =>
        args match
          case List(Typed(Repeated(args2, _), _)) =>
            ShaderAST.DataTypes.vec4(args2.map(p => walkTerm(p, envVarName)))
          case _ =>
            ShaderAST.DataTypes.vec4(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Ident("bvec2"), "apply"), args) =>
        args match
          case List(Typed(Repeated(args2, _), _)) =>
            ShaderAST.DataTypes.bvec2(args2.map(p => walkTerm(p, envVarName)))
          case _ =>
            ShaderAST.DataTypes.bvec2(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Ident("bvec3"), "apply"), args) =>
        args match
          case List(Typed(Repeated(args2, _), _)) =>
            ShaderAST.DataTypes.bvec3(args2.map(p => walkTerm(p, envVarName)))
          case _ =>
            ShaderAST.DataTypes.bvec3(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Ident("bvec4"), "apply"), args) =>
        args match
          case List(Typed(Repeated(args2, _), _)) =>
            ShaderAST.DataTypes.bvec4(args2.map(p => walkTerm(p, envVarName)))
          case _ =>
            ShaderAST.DataTypes.bvec4(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Ident("ivec2"), "apply"), args) =>
        args match
          case List(Typed(Repeated(args2, _), _)) =>
            ShaderAST.DataTypes.ivec2(args2.map(p => walkTerm(p, envVarName)))
          case _ =>
            ShaderAST.DataTypes.ivec2(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Ident("ivec3"), "apply"), args) =>
        args match
          case List(Typed(Repeated(args2, _), _)) =>
            ShaderAST.DataTypes.ivec3(args2.map(p => walkTerm(p, envVarName)))
          case _ =>
            ShaderAST.DataTypes.ivec3(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Ident("ivec4"), "apply"), args) =>
        args match
          case List(Typed(Repeated(args2, _), _)) =>
            ShaderAST.DataTypes.ivec4(args2.map(p => walkTerm(p, envVarName)))
          case _ =>
            ShaderAST.DataTypes.ivec4(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Ident("mat2"), "apply"), args) =>
        args match
          case List(Typed(Repeated(args2, _), _)) =>
            ShaderAST.DataTypes.mat2(args2.map(p => walkTerm(p, envVarName)))
          case _ =>
            ShaderAST.DataTypes.mat2(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Ident("mat3"), "apply"), args) =>
        args match
          case List(Typed(Repeated(args2, _), _)) =>
            ShaderAST.DataTypes.mat3(args2.map(p => walkTerm(p, envVarName)))
          case _ =>
            ShaderAST.DataTypes.mat3(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Ident("mat4"), "apply"), args) =>
        args match
          case List(Typed(Repeated(args2, _), _)) =>
            ShaderAST.DataTypes.mat4(args2.map(p => walkTerm(p, envVarName)))
          case _ =>
            ShaderAST.DataTypes.mat4(args.map(p => walkTerm(p, envVarName)))

      //

      case Apply(Select(Ident(id), "apply"), args) =>
        val proxy = proxies.lookUp(id, Proxy(id, Nil, Option(ShaderAST.DataTypes.ident("void"))))
        ShaderAST.CallFunction(proxy.name, args.map(x => walkTerm(x, envVarName)), Nil, proxy.returnType)

      case Apply(TypeApply(Select(g, op), _), List(f)) if op == "compose" || op == "andThen" =>
        def toProxy(t: Term): Proxy =
          t match
            case Ident(name) =>
              proxies.lookUp(name)

            case x =>
              walkTerm(x, envVarName) match
                case r: ShaderAST.FunctionRef =>
                  Proxy(r.id, r.arg, r.returnType)

                case ShaderAST.Block(List(r @ ShaderAST.FunctionRef(_, _, _))) =>
                  Proxy(r.id, r.arg, r.returnType)

                case ShaderAST.CallFunction(id, args, _, returnType) =>
                  Proxy(id, args, returnType)

                case _ =>
                  throw ShaderError.UnexpectedConstruction(
                    "You appear to be composing something other that a function."
                  )

        val gg: Proxy = toProxy(g)
        val ff: Proxy = toProxy(f)

        val gProxy = if op == "compose" then gg else ff
        val fProxy = if op == "compose" then ff else gg

        val fnName = proxies.makeDefName
        val vName  = proxies.makeVarName
        val fnInType =
          fProxy.argType.headOption
            .getOrElse(ShaderAST.DataTypes.ident("void"))

        val body =
          ShaderAST.CallFunction(
            id = gProxy.name,
            args = List(
              ShaderAST.CallFunction(
                id = fProxy.name,
                args = List(ShaderAST.DataTypes.ident(vName)),
                argNames = Nil,
                returnType = fProxy.returnType
              )
            ),
            argNames = Nil,
            returnType = gProxy.returnType
          )

        shaderDefs += FunctionLookup(
          ShaderAST.Function(fnName, List(fnInType -> vName), body, gProxy.returnType),
          false
        )
        ShaderAST.FunctionRef(fnName, gProxy.argType, gProxy.returnType)

      // Generally walking the tree

      case Apply(TypeApply(term, _), List(x)) =>
        walkTerm(x, envVarName)

      // Extension method applies...
      case Apply(Select(Select(Inlined(_, _, _), "vec2"), "apply"), args) =>
        ShaderAST.DataTypes.vec2(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Select(Inlined(_, _, _), "vec3"), "apply"), args) =>
        ShaderAST.DataTypes.vec3(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Select(Inlined(_, _, _), "vec4"), "apply"), args) =>
        ShaderAST.DataTypes.vec4(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Select(Inlined(_, _, _), "bvec2"), "apply"), args) =>
        ShaderAST.DataTypes.bvec2(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Select(Inlined(_, _, _), "bvec3"), "apply"), args) =>
        ShaderAST.DataTypes.bvec3(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Select(Inlined(_, _, _), "bvec4"), "apply"), args) =>
        ShaderAST.DataTypes.bvec4(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Select(Inlined(_, _, _), "ivec2"), "apply"), args) =>
        ShaderAST.DataTypes.ivec2(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Select(Inlined(_, _, _), "ivec3"), "apply"), args) =>
        ShaderAST.DataTypes.ivec3(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Select(Inlined(_, _, _), "ivec4"), "apply"), args) =>
        ShaderAST.DataTypes.ivec4(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Select(Inlined(_, _, _), "mat2"), "apply"), args) =>
        ShaderAST.DataTypes.mat2(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Select(Inlined(_, _, _), "mat3"), "apply"), args) =>
        ShaderAST.DataTypes.mat3(args.map(p => walkTerm(p, envVarName)))

      case Apply(Select(Select(Inlined(_, _, _), "mat4"), "apply"), args) =>
        ShaderAST.DataTypes.mat4(args.map(p => walkTerm(p, envVarName)))

      // Casting

      case Select(term, "toInt") =>
        ShaderAST.Cast(walkTerm(term, envVarName), "int")

      case Select(term, "toFloat") =>
        ShaderAST.Cast(walkTerm(term, envVarName), "float")

      case Select(term, "toBoolean") =>
        ShaderAST.Cast(walkTerm(term, envVarName), "bool")

      case Apply(Ident("toInt"), List(term)) =>
        ShaderAST.Cast(walkTerm(term, envVarName), "int")

      case Apply(Ident("toFloat"), List(term)) =>
        ShaderAST.Cast(walkTerm(term, envVarName), "float")

      case Apply(Ident("toBoolean"), List(term)) =>
        ShaderAST.Cast(walkTerm(term, envVarName), "bool")

      // Read a field

      case Select(Inlined(None, Nil, Ident(obj)), fieldName) =>
        ShaderAST.DataTypes.ident(s"$obj.$fieldName")

      case Select(Ident(name), "unary_-") =>
        ShaderAST.DataTypes.ident(s"-$name")

      case Select(Ident(namespace), name) =>
        envVarName match
          case Some(value) if value == namespace =>
            ShaderAST.DataTypes.ident(s"$name")

          case _ =>
            ShaderAST.DataTypes.ident(s"$namespace.$name")

      // Read a field - but of something namespaced, negated, e.g. -position.x
      case Select(Select(Ident(namespace), name), "unary_-") =>
        envVarName match
          case Some(value) if value == namespace =>
            ShaderAST.DataTypes.ident(s"-$name")

          case _ =>
            ShaderAST.DataTypes.ident(s"-$namespace.$name")

      // Read a field - but of something namespaced, e.g. env.Position.x
      case Select(Select(Ident(namespace), name), field) =>
        envVarName match
          case Some(value) if value == namespace =>
            ShaderAST.DataTypes.ident(s"$name.$field")

          case _ =>
            ShaderAST.DataTypes.ident(s"$namespace.$name.$field")

      // Read a field - but of something namespaced, negated, e.g. -env.Position.x
      case Select(Select(Select(Ident(namespace), name), field), "unary_-") =>
        envVarName match
          case Some(value) if value == namespace =>
            ShaderAST.DataTypes.ident(s"-$name.$field")

          case _ =>
            ShaderAST.DataTypes.ident(s"-$namespace.$name.$field")

      // Read a component of an array
      case Select(
            term @ Apply(
              Apply(TypeApply(Select(Ident("array"), "apply"), _), List(_)),
              List(Literal(IntConstant(_)))
            ),
            component
          ) =>
        ShaderAST.Field(
          walkTerm(term, envVarName),
          ShaderAST.DataTypes.ident(component)
        )

      // Native method call.
      case Apply(Ident(name), List(Inlined(None, Nil, Ident(defRef)))) =>
        val proxy = proxies.lookUp(defRef)
        val args: List[ShaderAST] =
          proxies.lookUpInlineReplace(proxy.name) match
            case Some(value) =>
              List(value)

            case None =>
              List(ShaderAST.DataTypes.ident(proxy.name))

        ShaderAST.CallFunction(name, args, args, None)

      case Apply(Select(term, "apply"), xs) =>
        val body = walkTerm(term, envVarName)

        body.find {
          case ShaderAST.CallFunction(_, _, _, _) => true
          case ShaderAST.FunctionRef(_, _, _)     => true
          case _                                  => false
        } match
          case Some(ShaderAST.CallFunction(id, Nil, Nil, rt)) =>
            ShaderAST.CallFunction(id, xs.map(tt => walkTerm(tt, envVarName)), Nil, rt)

          case Some(ShaderAST.CallFunction(id, args, argNames, rt)) =>
            ShaderAST.CallFunction(id, xs.map(tt => walkTerm(tt, envVarName)), argNames, rt)

          case Some(ShaderAST.FunctionRef(id, _, rt)) =>
            ShaderAST.CallFunction(id, xs.map(tt => walkTerm(tt, envVarName)), Nil, rt)

          case _ =>
            (body, xs) match
              case (ShaderAST.DataTypes.ident(name), List(arg)) =>
                ShaderAST.CallFunction(name, List(walkTerm(arg, envVarName)), Nil, None)

              case _ =>
                ShaderAST.Block(xs.map(tt => walkTerm(tt, envVarName)))

      case Apply(Select(Ident(maybeEnv), funcName), args) if envVarName.isDefined && maybeEnv == envVarName.get =>
        ShaderAST.CallFunction(funcName, args.map(tt => walkTerm(tt, envVarName)), Nil, None)

      //

      case Select(term, "unary_-") =>
        ShaderAST.Neg(walkTerm(term, envVarName))

      // Annotations

      case Apply(Select(New(tree), _), List()) =>
        walkTree(tree, envVarName)

      //

      // Infix operations

      case Apply(
            Select(New(TypeIdent(name)), "<init>"),
            args
          ) =>
        // 'New' is allowed for layout annotations specifically (as they have arguments) or
        // for any struct that has been previously registered.
        if name == "layout" || structRegister.contains(name) then
          ShaderAST.New(name, args.map(a => walkTerm(a, envVarName)))
        else
          throw ShaderError.UnexpectedConstruction(
            "You cannot use classes (structs) not previously declared within the Shader body. This is either an illegal forward reference or you declared the class outside the shader."
          )

      case Apply(
            Select(Ident(id), "update"),
            List(
              index,
              rhs
            )
          ) =>
        // Update mutable collections - array's and mat's in our case.
        val idx = walkTerm(index, envVarName)
        ShaderAST.Infix(
          "=",
          ShaderAST.DataTypes.index(id, idx),
          walkTerm(rhs, envVarName),
          None
        )

      case Apply(Select(term, op), xs) =>
        op match
          case "+" | "-" | "*" | "/" | "<" | ">" | "==" | "<=" | ">=" | "&&" | "||" =>
            val lhs = walkTerm(term, envVarName)
            val rhs = xs.headOption.map(tt => walkTerm(tt, envVarName)).getOrElse(ShaderAST.Empty())
            val rt  = findReturnType(lhs)
            ShaderAST.Infix(op, lhs, rhs, rt)

          case "%" =>
            val lhs = walkTerm(term, envVarName)
            val rhs = xs.headOption.map(tt => walkTerm(tt, envVarName)).getOrElse(ShaderAST.Empty())
            val rt  = findReturnType(lhs)
            ShaderAST.CallFunction(
              "mod",
              List(lhs, rhs),
              List(
                ShaderAST.DataTypes.ident("x"),
                ShaderAST.DataTypes.ident("y")
              ),
              rt
            )

          case _ =>
            throw ShaderError.Unsupported("Shaders do not support infix operator: " + op)

      case Apply(Apply(Ident(op), List(l)), List(r)) =>
        op match
          case "+" | "-" | "*" | "/" | "<" | ">" | "==" | "<=" | ">=" | "&&" | "||" =>
            val lhs = walkTerm(l, envVarName)
            val rhs = walkTerm(r, envVarName)
            val rt  = findReturnType(lhs)
            ShaderAST.Infix(op, lhs, rhs, rt)

          case "%" =>
            val lhs = walkTerm(l, envVarName)
            val rhs = walkTerm(r, envVarName)
            val rt  = findReturnType(lhs)
            ShaderAST.CallFunction(
              "mod",
              List(lhs, rhs),
              List(
                ShaderAST.DataTypes.ident("x"),
                ShaderAST.DataTypes.ident("y")
              ),
              rt
            )

          case _ =>
            throw ShaderError.Unsupported("Shaders do not support infix operator: " + op)

      // Arrays

      // array constuctor
      case Apply(
            Apply(
              foo @ TypeApply(
                Select(Ident("array"), "apply"),
                List(Singleton(Literal(IntConstant(size))), typ)
              ),
              List(Typed(Repeated(args, _), _))
            ),
            _
          ) =>
        val typeOf = extractInferredType(typ).map(_ + s"[${size.toString()}]")
        ShaderAST.DataTypes.array(size, args.map(a => walkTerm(a, envVarName)), typeOf)

      // array component access from a env var
      case Apply(
            Apply(
              TypeApply(Select(Ident("array"), "apply"), _),
              List(Select(Ident(namespace), name))
            ),
            List(index)
          ) =>
        val idx = walkTerm(index, envVarName)
        envVarName match
          case Some(value) if value == namespace =>
            ShaderAST.DataTypes.index(name, idx)

          case _ =>
            ShaderAST.DataTypes.index(s"$namespace.$name", idx)

      // array component access
      case Apply(
            Apply(TypeApply(Select(Ident("array"), "apply"), _), List(Ident(name))),
            List(index)
          ) =>
        ShaderAST.DataTypes.index(name, walkTerm(index, envVarName))

      // array - unexpected build
      case x @ Apply(
            Apply(
              TypeApply(
                Select(Ident("array"), "apply"),
                _
              ),
              _
            ),
            _
          ) =>
        throw ShaderError.UnexpectedConstruction(
          "Shader arrays must be constructed with full type information, e.g.: array[3, Float] (where 3 is the size of the array)"
        )

      //

      case Apply(Ident(name), terms) =>
        ShaderAST.CallFunction(name, terms.map(tt => walkTerm(tt, envVarName)), Nil, None)

      case Inlined(None, _, term) =>
        walkTerm(term, envVarName)

      case Inlined(Some(Ident(_)), _, term) =>
        walkTerm(term, envVarName)

      // Raw
      case Inlined(
            Some(Select(Ident(_), _)),
            Nil,
            Typed(Apply(Select(_, _), List(Literal(StringConstant(raw)))), TypeIdent("RawGLSL"))
          ) =>
        ShaderAST.RawLiteral(raw)

      // GLSLHeader
      case Inlined(
            Some(Select(Ident(_), _)),
            Nil,
            Typed(Apply(Select(_, _), List(Literal(StringConstant(raw)))), TypeIdent("GLSLHeader"))
          ) =>
        ShaderAST.RawLiteral(raw)

      // raw
      case Inlined(Some(Apply(Ident("raw"), List(term))), _, _) =>
        walkTerm(term, envVarName)

      // Swizzle
      case Inlined(Some(Apply(Ident(name), List(id @ Select(Ident(env), varName)))), _, rt)
          if isSwizzle.matches(name) && envVarName.contains(env) =>
        ShaderAST.DataTypes.swizzle(
          walkTerm(id, envVarName),
          name,
          Option(walkTree(rt, envVarName))
        )

      case Inlined(Some(Apply(Ident(name), List(term))), _, _) if isSwizzle.matches(name) =>
        val body = walkTerm(term, envVarName)
        ShaderAST.DataTypes.swizzle(
          body,
          name,
          body.typeIdent
        )

      case Inlined(Some(Apply(Ident(name), List(Ident(id)))), _, _) if isSwizzle.matches(name) =>
        ShaderAST.DataTypes.swizzle(
          ShaderAST.DataTypes.ident(id),
          name,
          None
        )

      // Swizzle a function call
      case Select(term @ Apply(Ident(_), _), swzl) if isSwizzle.matches(swzl) =>
        ShaderAST.DataTypes.swizzle(
          walkTerm(term, envVarName),
          swzl,
          None
        )

      // Inlined external def

      case Inlined(Some(Apply(Ident(name), args)), ds, x @ Typed(term, typeTree)) =>
        ds.map(s => walkStatement(s, envVarName))
          .flatMap {
            case v @ ShaderAST.Val(proxy, value, _) =>
              List(v)
            case _ =>
              Nil
          }
          .foreach { case ShaderAST.Val(proxy, value, _) =>
            proxies.addInlineReplace(proxy, value)
          }

        walkTerm(x, envVarName)

      case Inlined(Some(Select(This(_), _)), _, term) =>
        walkTerm(term, envVarName)

      case tt @ Inlined(
            Some(
              Apply(
                TypeApply(Ident("ubo"), List(TypeIdent(_))),
                _
              )
            ),
            _,
            _
          ) =>
        ShaderAST.UBO(uboUtils.extractUBO(tt))

      // Inlined call to a method on a class

      case Inlined(
            Some(
              Select(
                Inlined(_, _, Apply(Select(New(TypeIdent(_ /*class name*/ )), "<init>"), Nil)),
                _ // method name
              )
            ),
            _,
            term
          ) =>
        walkTerm(term, envVarName)

      //

      case Inlined(Some(tree: Tree), _, _) =>
        walkTree(tree, envVarName)

      case TypeApply(term, _) =>
        walkTerm(term, envVarName)

      // Anonymous function?
      case Typed(
            Block(
              List(
                fn @ DefDef(_, args, rt, Some(term))
              ),
              Closure(Ident("$anonfun"), None)
            ),
            _
          ) =>
        walkTree(fn, envVarName)

      case Typed(term, _) =>
        walkTerm(term, envVarName)

      case Block(args, fnBody) if isOneLineLambda(args) =>
        val fnName = proxies.makeDefName

        val body = walkTerm(fnBody, envVarName)

        val arguments =
          args.collect {
            case ValDef(name, Inferred(), Some(value)) if name.contains("$proxy") =>
              walkTerm(value, envVarName) -> name.substring(0, name.indexOf("$"))
          }

        val returnType =
          findReturnType(body)

        shaderDefs += FunctionLookup(
          ShaderAST.Function(
            id = fnName,
            args = arguments.map((typ, n) => findReturnType(typ).getOrElse(typ) -> n),
            body = body,
            returnType = returnType
          ),
          false
        )

        ShaderAST.CallFunction(
          id = fnName,
          args = arguments.map(_._1),
          argNames = arguments.map(_._2).map(n => ShaderAST.DataTypes.ident(n)),
          returnType = returnType
        )

      case Block(statements, Closure(Ident("$anonfun"), None)) =>
        val ss = statements
          .map(s => walkStatement(s, envVarName))

        ShaderAST.Block(ss)

      case Block(Nil, term) =>
        walkTerm(term, envVarName)

      case Block(statements, term) =>
        val ss =
          statements.map(s => walkStatement(s, envVarName)) :+ walkTerm(term, envVarName)

        ShaderAST.Block(ss)

      // Literals

      case Literal(FloatConstant(f)) =>
        ShaderAST.DataTypes.float(f)

      case Literal(IntConstant(i)) =>
        ShaderAST.DataTypes.int(i)

      case Literal(BooleanConstant(b)) =>
        ShaderAST.DataTypes.bool(b)

      case Literal(UnitConstant()) =>
        ShaderAST.Empty()

      case Literal(NullConstant()) =>
        ShaderAST.Empty()

      case Literal(StringConstant(raw)) =>
        ShaderAST.RawLiteral(raw)

      case Literal(constant) =>
        throw ShaderError.Unsupported("Shaders do not support constant type: " + constant.show)

      // Refs

      case Ident(name) =>
        val resolvedName = proxies.lookUp(name)._1

        shaderDefs.toList.find(_.fn.id == resolvedName).map(_.fn) match
          case None =>
            ShaderAST.DataTypes.ident(resolvedName)

          case Some(ShaderAST.Function(_, _, _, rt)) =>
            ShaderAST.CallFunction(resolvedName, Nil, Nil, rt)

      case Closure(_, _) =>
        ShaderAST.Empty()

      case Wildcard() =>
        throw ShaderError.Unsupported("Shaders do not support wildcards.")

      case Select(term, _) => // term, name
        walkTerm(term, envVarName)

      // Unsupported (yet?)

      case This(_) =>
        throw new ShaderError.Unsupported("Shaders do not support references to 'this'.")

      case New(_) =>
        throw new ShaderError.Unsupported("Shaders do not support 'new' instances.")

      case NamedArg(_, _) =>
        throw new ShaderError.Unsupported("Shaders do not support named args.")

      case Super(_, _) =>
        throw new ShaderError.Unsupported("Shaders do not support calls to super.")

      case Assign(lhs, rhs) =>
        ShaderAST.Assign(
          walkTerm(lhs, envVarName),
          walkTerm(rhs, envVarName)
        )

      case If(condTerm, thenTerm, elseTerm) =>
        walkTerm(elseTerm, envVarName) match
          case ShaderAST.Empty() =>
            ShaderAST.If(
              walkTerm(condTerm, envVarName),
              walkTerm(thenTerm, envVarName),
              None
            )

          case e =>
            ShaderAST.If(
              walkTerm(condTerm, envVarName),
              walkTerm(thenTerm, envVarName),
              Option(e)
            )

      case Match(term, cases) =>
        val cs =
          cases.map {
            case CaseDef(Literal(IntConstant(i)), None, caseTerm) =>
              (Option(i), walkTerm(caseTerm, envVarName))

            case CaseDef(Wildcard(), None, caseTerm) =>
              (None, walkTerm(caseTerm, envVarName))

            case _ =>
              throw ShaderError.Unsupported("Shaders only support pattern matching on `Int` values or `_` wildcards.")
          }

        ShaderAST.Switch(walkTerm(term, envVarName), cs)

      case SummonFrom(_) =>
        throw ShaderError.Unsupported("Shaders do not support summoning.")

      case Try(_, _, _) =>
        throw ShaderError.Unsupported("Shaders do not support try blocks.")

      case Return(_, _) =>
        throw ShaderError.Unsupported("Shaders do not support return statements.")

      case Repeated(args, _) =>
        ShaderAST.Block(args.map(p => walkTerm(p, envVarName)))

      case SelectOuter(_, _, _) =>
        throw ShaderError.Unsupported("Shaders do not support outer selectors.")

      case While(cond, body) =>
        ShaderAST.While(walkTerm(cond, envVarName), walkTerm(body, envVarName))

      case x =>
        val sample = Printer.TreeStructure.show(x).take(100)
        throw ShaderError.UnexpectedConstruction("Unexpected Term: " + sample + "(..)")
