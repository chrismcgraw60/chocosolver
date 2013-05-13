package org.clafer.ir.compiler;

import java.util.Arrays;
import java.util.Random;
import solver.ResolutionPolicy;
import solver.Solver;
import solver.constraints.IntConstraintFactory;
import solver.explanations.ExplanationFactory;
import solver.search.loop.monitors.SearchMonitorFactory;
import solver.search.strategy.selectors.values.InDomainMin;
import solver.search.strategy.selectors.variables.FirstFail;
import solver.search.strategy.strategy.Assignment;
import solver.variables.BoolVar;
import solver.variables.IntVar;
import solver.variables.VariableFactory;
import util.tools.ArrayUtils;

/**
 * 1. optimize union if detect operands are disjoint. ie. |children| = |union|
 * 2. same with intchannel
 * 3. remove useless members and set unions
 * 4. remove useless parent pointers if already solved
 * 5. common subexpression elimination
 * 6. remove |set| <= 5 if set only contains 5 env
 * 
 * 7. Degree#0 should have #0 in its ker
        AstModel model = Ast.newModel();
        model.addTopClafer("Jimmy").withCard(2, 2).addChild("Degree").withCard(1, 2).refTo(Ast.IntType);

        ChocoSolver solver = compile(model, Scope.builder().defaultScope(5).intLow(-1).intHigh(1).toScope());
        System.out.println(solver);
        while (solver.nextSolution()) {
            System.out.println(solver.solution());
        }
        System.out.println(solver.getMeasures().getSolutionCount());
 * 
 * 
 * @author jimmy
 */
public class Feature {

    public static void main(String[] args) {
        Solver solver = new Solver();
        SearchMonitorFactory.log(solver, false, true);
        ExplanationFactory.CBJ.plugin(solver, true);
        
        IntVar objective = VariableFactory.bounded("objective", -100, 100, solver);
        BoolVar[] features = VariableFactory.boolArray("feature", 10, solver);
        Random rand = new Random();
        IntVar[] footprint = new IntVar[features.length];
        for (int i = 0; i < footprint.length; i++) {
            int val = rand.nextBoolean() ? rand.nextInt(100) : -rand.nextInt(100);
            int[] e = new int[]{0, val};
            Arrays.sort(e);
            footprint[i] = VariableFactory.enumerated("footprint#" + i, e, solver);
            solver.post(IntConstraintFactory.implies(features[i],
                    IntConstraintFactory.arithm(footprint[i], "=", VariableFactory.fixed(val, solver))));
            solver.post(IntConstraintFactory.implies(VariableFactory.not(features[i]),
                    IntConstraintFactory.arithm(footprint[i], "=", VariableFactory.fixed(0, solver))));
        }
        solver.post(IntConstraintFactory.sum(footprint, objective));
        solver.set(new Assignment(new FirstFail(ArrayUtils.append(features, footprint)), new InDomainMin()));
        
        System.out.println(solver);
        
        solver.findOptimalSolution(ResolutionPolicy.MINIMIZE, objective);
        System.out.println(solver);
    }
}