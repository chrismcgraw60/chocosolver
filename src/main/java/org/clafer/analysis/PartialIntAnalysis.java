package org.clafer.analysis;

import gnu.trove.set.hash.TIntHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.clafer.Scope;
import org.clafer.Util;
import org.clafer.analysis.AbstractOffsetAnalysis.Offsets;
import org.clafer.analysis.FormatAnalysis.Format;
import org.clafer.ast.AstAbstractClafer;
import org.clafer.ast.AstBoolExpr;
import org.clafer.ast.AstClafer;
import org.clafer.ast.AstConstant;
import org.clafer.ast.AstConstraint;
import org.clafer.ast.AstEqual;
import org.clafer.ast.AstEqual.Op;
import org.clafer.ast.AstExpr;
import org.clafer.ast.AstJoin;
import org.clafer.ast.AstJoinRef;
import org.clafer.ast.AstModel;
import org.clafer.ast.AstRef;
import org.clafer.ast.AstSetExpr;
import org.clafer.ast.AstThis;
import org.clafer.ast.AstUpcast;
import org.clafer.collection.Pair;
import org.clafer.collection.Quad;
import org.clafer.collection.Triple;

/**
 *
 * @author jimmy
 */
public class PartialIntAnalysis {

    private final Map<AstExpr, AstClafer> types;
    private final Map<AstClafer, Format> formats;

    private PartialIntAnalysis(Map<AstExpr, AstClafer> types, Map<AstClafer, Format> formats) {
        this.types = types;
        this.formats = formats;
    }

    public static Pair<Map<AstRef, int[]>, Map<Pair<AstRef, Integer>, int[]>> analyze(
            AstModel model,
            Map<AstAbstractClafer, Offsets> offsets,
            Map<AstClafer, Format> formats,
            Map<AstExpr, AstClafer> types,
            Scope scope) {
        Map<AstRef, int[]> partialInts = new HashMap<AstRef, int[]>();
        Map<Pair<AstRef, Integer>, int[]> partialRefInts = new HashMap<Pair<AstRef, Integer>, int[]>();

        Map<AstRef, Pair<List<List<AstClafer>>, TIntHashSet>> subMap = new HashMap<AstRef, Pair<List<List<AstClafer>>, TIntHashSet>>();
        for (AstClafer clafer : AnalysisUtil.getClafers(model)) {
            PartialIntAnalysis analysis = new PartialIntAnalysis(types, formats);
            for (AstConstraint constraint : clafer.getConstraints()) {
                if (constraint.isSoft()) {
                    continue;
                }
                Quad<AstRef, List<AstClafer>, Integer, Boolean> quad = analysis.analyze(constraint.getExpr());
                if (quad == null) {
                    continue;
                }

                AstRef key = quad.getFst();

                if (quad.getFou()) {
                    List<AstClafer> path = quad.getSnd();
                    if (path.isEmpty()) {
                        for (int i = 0; i < scope.getScope(key.getSourceType()); i++) {
                            int[] oldValue = partialRefInts.get(new Pair<AstRef, Integer>(key, 0));
                            if (oldValue != null) {
                                partialRefInts.put(new Pair<AstRef, Integer>(key, 0), Util.cons(quad.getThd(), oldValue));
                            } else {
                                partialRefInts.put(new Pair<AstRef, Integer>(key, 0), new int[]{quad.getThd()});
                            }
                        }
                    } else {
                        int offset = 0;
                        for (AstClafer p : path) {
                            offset += offsets.get(p.getSuperClafer()).getOffset(p);
                        }
                        int s = scope.getScope(path.get(0));
                        for (int i = 0; i < s; i++) {
                            int[] oldValue = partialRefInts.get(new Pair<AstRef, Integer>(key, 0));
                            if (oldValue != null) {
                                partialRefInts.put(new Pair<AstRef, Integer>(key, 0), Util.cons(quad.getThd(), oldValue));
                            } else {
                                partialRefInts.put(new Pair<AstRef, Integer>(key, 0), new int[]{quad.getThd()});
                            }
                        }
                    }
                }

                Pair<List<List<AstClafer>>, TIntHashSet> subs = subMap.get(key);
                if (subs == null) {
                    subs = new Pair<List<List<AstClafer>>, TIntHashSet>(new ArrayList<List<AstClafer>>(), new TIntHashSet());
                    subs.getSnd().add(0);
                    subMap.put(key, subs);
                }
                subs.getFst().add(quad.getSnd());
                subs.getSnd().add(quad.getThd());
            }
        }

        for (Entry<AstRef, Pair<List<List<AstClafer>>, TIntHashSet>> entry : subMap.entrySet()) {
            if (covers(entry.getKey().getSourceType(), entry.getValue().getFst())) {
                partialInts.put(entry.getKey(), entry.getValue().getSnd().toArray());
            }
        }
        return new Pair<Map<AstRef, int[]>, Map<Pair<AstRef, Integer>, int[]>>(partialInts, partialRefInts);
    }

    private static boolean covers(AstClafer clafer, List<List<AstClafer>> paths) {
        for (List<AstClafer> path : paths) {
            if (path.isEmpty()) {
                return true;
            }
            if (clafer.equals(path.get(0))) {
                return true;
            }
        }
        AstClafer topClafer = AnalysisUtil.getTopParent(clafer);
        if (topClafer instanceof AstAbstractClafer) {
            AstAbstractClafer sup = (AstAbstractClafer) topClafer;
            for (AstClafer sub : sup.getSubs()) {
                List<List<AstClafer>> $paths = new ArrayList<List<AstClafer>>();
                for (List<AstClafer> path : paths) {
                    if (sub.equals(path.get(path.size() - 1))) {
                        $paths.add(path.subList(0, path.size() - 1));
                    }
                }
                if (!covers(sub, $paths)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private Quad<AstRef, List<AstClafer>, Integer, Boolean> analyze(AstBoolExpr exp) {
        if (exp instanceof AstEqual) {
            AstEqual compare = (AstEqual) exp;
            if (Op.Equal.equals(compare.getOp())) {
                if (compare.getLeft() instanceof AstJoinRef && compare.getRight() instanceof AstConstant) {
                    return analyzeEqual((AstJoinRef) compare.getLeft(), (AstConstant) compare.getRight());
                }
                if (compare.getRight() instanceof AstJoinRef && compare.getLeft() instanceof AstConstant) {
                    return analyzeEqual((AstJoinRef) compare.getRight(), (AstConstant) compare.getLeft());
                }
            }
        }
        return null;
    }

    private Quad<AstRef, List<AstClafer>, Integer, Boolean> analyzeEqual(AstJoinRef exp, AstConstant constant) {
        Triple<AstRef, List<AstClafer>, Boolean> expAnalysis = analyze(exp);
        if (expAnalysis == null) {
            return null;
        }
        return new Quad<AstRef, List<AstClafer>, Integer, Boolean>(
                expAnalysis.getFst(), expAnalysis.getSnd(), constant.getValue(), expAnalysis.getThd());
    }

    private Triple<AstRef, List<AstClafer>, Boolean> analyze(AstJoinRef exp) {
        Pair<List<AstClafer>, Boolean> derefAnalysis = analyze(exp.getDeref());
        if (derefAnalysis == null) {
            return null;
        }
        return new Triple<AstRef, List<AstClafer>, Boolean>(getType(exp.getDeref()).getRef(), derefAnalysis.getFst(), derefAnalysis.getSnd());
    }

    private Pair<List<AstClafer>, Boolean> analyze(AstSetExpr exp) {
        List<AstClafer> subs = new ArrayList<AstClafer>();
        boolean allParentGroup = true;
        while (exp instanceof AstUpcast || exp instanceof AstJoin) {
            if (exp instanceof AstUpcast) {
                exp = ((AstUpcast) exp).getBase();
                subs.add(getType(exp));
            } else if (exp instanceof AstJoin) {
                allParentGroup = allParentGroup && Format.ParentGroup.equals(getFormat(((AstJoin) exp).getRight()));
                exp = ((AstJoin) exp).getLeft();
            }
        }
        if (exp instanceof AstThis) {
            Collections.reverse(subs);
            return new Pair<List<AstClafer>, Boolean>(subs, allParentGroup);
        }
        return new Pair<List<AstClafer>, Boolean>(Collections.<AstClafer>emptyList(), allParentGroup);
    }

    private AstClafer getType(AstExpr exp) {
        return AnalysisUtil.notNull(exp + " type not analyzed yet", types.get(exp));
    }

    private Format getFormat(AstClafer exp) {
        return AnalysisUtil.notNull(exp + " format not analyzed yet", formats.get(exp));
    }
}