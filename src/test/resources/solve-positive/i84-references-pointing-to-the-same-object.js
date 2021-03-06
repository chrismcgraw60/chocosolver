scope({c0_Dimension:2, c0_DimensionLevel:2, c0_belongsTo:2, c0_levels:4});
defaultScope(1);
intRange(-8, 7);
stringLength(16);

c0_Dimension = Abstract("c0_Dimension");
c0_DimensionLevel = Abstract("c0_DimensionLevel");
c0_levels = c0_Dimension.addChild("c0_levels");
c0_belongsTo = c0_DimensionLevel.addChild("c0_belongsTo").withCard(1, 1);
c0_dim1 = Clafer("c0_dim1").withCard(1, 1).extending(c0_Dimension);
c0_dim2 = Clafer("c0_dim2").withCard(1, 1).extending(c0_Dimension);
c0_dimLevel1 = Clafer("c0_dimLevel1").withCard(1, 1).extending(c0_DimensionLevel);
c0_dimLevel2 = Clafer("c0_dimLevel2").withCard(1, 1).extending(c0_DimensionLevel);
c0_levels.refToUnique(c0_DimensionLevel);
c0_belongsTo.refTo(c0_Dimension);
Constraint(some([disjDecl([dl1 = local("dl1"), dl2 = local("dl2")], global(c0_DimensionLevel))], equal(joinRef(join(dl1, c0_belongsTo)), joinRef(join(dl2, c0_belongsTo)))));
c0_Dimension.addConstraint(all([decl([dl = local("dl")], join($this(), c0_levels))], equal(joinRef(join(joinRef(dl), c0_belongsTo)), $this())));
