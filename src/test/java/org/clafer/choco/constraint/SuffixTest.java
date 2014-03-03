package org.clafer.choco.constraint;

import org.clafer.choco.constraint.propagator.PropUtil;
import org.clafer.collection.Pair;
import static org.clafer.ir.Irs.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;
import solver.Solver;
import solver.constraints.Constraint;
import solver.variables.IntVar;

/**
 *
 * @author jimmy
 */
public class SuffixTest extends ConstraintTest<Pair<IntVar[], IntVar[]>> {

    @Override
    protected void check(Pair<IntVar[], IntVar[]> s) {
        String prefix = PropUtil.toString(s.getFst());
        String word = PropUtil.toString(s.getSnd());
        assertThat(word, endsWith(prefix));
    }

    @Test(timeout = 60000)
    public void quickTest() {
        randomizedTest(new TestCase<Pair<IntVar[], IntVar[]>>() {
            @Override
            public Pair<Constraint, Pair<IntVar[], IntVar[]>> setup(Solver solver) {
                CStringVar suffix = toCStringVar(randString(), solver);
                CStringVar word = toCStringVar(randString(), solver);
                Constraint constraint = Constraints.suffix(
                        suffix.getChars(), suffix.getLength(),
                        word.getChars(), word.getLength());
                return pair(constraint, pair(suffix.getChars(), word.getChars()));
            }
        });
    }

    @Test(timeout = 60000)
    public void testSamePrefix() {
        randomizedTest(new TestCase<Pair<IntVar[], IntVar[]>>() {
            /*
             * import Control.Monad
             *
             * positive = do
             *     length <- [0..3]
             *     string1 <- replicateM 3 ['a', 'b', 'c']
             *     string2 <- replicateM 3 ['a', 'b', 'c']
             *     guard $ take length string1 == take length string2
             *     return (length, string1, string2)
             * negative = length (liftM3 (,,) [0..3] (replicateM 3 ['a', 'b', 'c']) (replicateM 3 ['a', 'b', 'c'])) - length positive
             */
            @PositiveSolutions(142)
            @NegativeSolutions(1458)
            @Override
            public Pair<Constraint, Pair<IntVar[], IntVar[]>> setup(Solver solver) {
                CStringVar suffix = toCStringVar(string("suffix", boundDomain(0, 3), 3), solver);
                CStringVar word = toCStringVar(string("word", boundDomain(0, 3), 3), solver);
                Constraint constraint = Constraints.suffix(
                        suffix.getChars(), suffix.getLength(),
                        word.getChars(), word.getLength());
                return pair(constraint, pair(suffix.getChars(), word.getChars()));
            }
        });
    }
}