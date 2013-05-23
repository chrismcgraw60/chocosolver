package org.clafer.choco.constraint.propagator;

import java.util.Arrays;
import solver.constraints.propagators.Propagator;
import solver.constraints.propagators.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.BoolVar;
import solver.variables.EventType;
import util.ESat;

/**
 *
 * @author jimmy
 */
public class PropOne extends Propagator<BoolVar> {

    public PropOne(BoolVar[] vars) {
        super(vars, PropagatorPriority.BINARY);
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        return EventType.INSTANTIATE.mask;
    }

    private void clearAllBut(int exclude) throws ContradictionException {
        for (int i = 0; i < vars.length; i++) {
            if (i != exclude) {
                vars[i].setToFalse(aCause);
            }
        }
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        int count = -1;
        BoolVar last = null;
        for (int i = 0; i < vars.length; i++) {
            BoolVar var = vars[i];
            if (var.instantiated()) {
                if (var.getValue() == 1) {
                    clearAllBut(i);
                    return;
                }
            } else {
                count = count == -1 ? i : -2;
                last = var;
            }
        }
        // Every variable if false except for last.
        if (count >= 0) {
            last.setToTrue(aCause);
            clearAllBut(count);
        }
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        assert EventType.isInstantiate(mask);
        if (vars[idxVarInProp].getValue() == 1) {
            clearAllBut(idxVarInProp);
        } else {
            propagate(mask);
        }
    }

    @Override
    public ESat isEntailed() {
        int count = 0;
        boolean allInstantiated = true;
        for (BoolVar var : vars) {
            if (var.instantiated()) {
                if (var.getValue() == 1) {
                    count++;
                    if (count > 1) {
                        return ESat.FALSE;
                    }
                }
            } else {
                allInstantiated = false;
            }
        }
        return allInstantiated
                ? (count == 1 ? ESat.TRUE : ESat.FALSE) : ESat.UNDEFINED;
    }

    @Override
    public String toString() {
        return "one(" + Arrays.toString(vars) + ")";
    }
}
