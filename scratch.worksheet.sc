Inlined(
  Some(
    TypeApply(Ident("summonLabels"), List(TypeSelect(Inlined(None, Nil, Ident("x$1$proxy1")), "MirroredElemLabels")))
  ),
  Nil,
  Typed(
    Block(
      List(
        ValDef(
          "elem$2",
          Inferred(),
          Some(TypeApply(Select(Literal(StringConstant("TIME")), "asInstanceOf"), List(TypeIdent("String"))))
        )
      ),
      Apply(
        TypeApply(
          Select(
            Inlined(
              Some(TypeApply(Ident("summonLabels"), List(Inferred()))),
              Nil,
              Typed(
                Block(
                  List(
                    ValDef(
                      "elem$2",
                      Inferred(),
                      Some(
                        TypeApply(
                          Select(Literal(StringConstant("VIEWPORT_SIZE")), "asInstanceOf"),
                          List(TypeIdent("String"))
                        )
                      )
                    )
                  ),
                  Apply(
                    TypeApply(
                      Select(
                        Inlined(
                          Some(TypeApply(Ident("summonLabels"), List(Inferred()))),
                          Nil,
                          Typed(Ident("Nil"), Applied(TypeIdent("List"), List(TypeIdent("String"))))
                        ),
                        "::"
                      ),
                      List(Inferred())
                    ),
                    List(Ident("elem$2"))
                  )
                ),
                Applied(TypeIdent("List"), List(TypeIdent("String")))
              )
            ),
            "::"
          ),
          List(Inferred())
        ),
        List(Ident("elem$2"))
      )
    ),
    Applied(TypeIdent("List"), List(TypeIdent("String")))
  )
)
