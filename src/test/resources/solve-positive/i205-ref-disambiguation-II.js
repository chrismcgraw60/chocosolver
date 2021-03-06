scope({c0_Car:4, c0_Person:4, c0_owner:4});
defaultScope(1);
intRange(-8, 7);
stringLength(16);

c0_Car = Clafer("c0_Car").withCard(4, 4);
c0_owner = c0_Car.addChild("c0_owner").withCard(1, 1);
c0_Person = Clafer("c0_Person").withCard(4, 4);
c0_owner.refTo(c0_Person);
Constraint(all([disjDecl([c1 = local("c1"), c2 = local("c2")], global(c0_Car))], notEqual(joinRef(join(c1, c0_owner)), joinRef(join(c2, c0_owner)))));
