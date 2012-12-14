package org.clafer;

import choco.cp.solver.search.integer.branching.AssignVar;
import javax.script.ScriptEngine;
import choco.kernel.solver.Solver;
import choco.cp.model.CPModel;
import choco.cp.solver.CPSolver;
import choco.cp.solver.search.integer.valiterator.IncreasingDomain;
import choco.cp.solver.search.set.AssignSetVar;
import choco.cp.solver.search.set.MinDomSet;
import choco.cp.solver.search.set.MinEnv;
import choco.kernel.model.Model;
import choco.kernel.model.variables.set.SetVariable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.script.ScriptEngineManager;
import static choco.Choco.*;
import choco.cp.solver.search.integer.varselector.MostConstrained;
import choco.kernel.common.logging.ChocoLogging;
import java.io.Reader;
import javax.script.Invocable;
import javax.script.ScriptException;

/**
 *
 * @author jimmy
 */
public class ChocoSolver {

    public static void main(String[] args) throws IOException, NoSuchMethodException {
        try {
            if (args.length == 0) {
                System.out.println("Expected at least one argument.");
            }
            if(args.length == 1) {
                run(false, new File(args[0]));
            } else {
                run(Boolean.parseBoolean(args[0]), new File(args[1]));
            }
        } catch (ScriptException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void run(boolean all, File in) throws IOException, ScriptException, NoSuchMethodException {

        Model m = new CPModel();
        Solver s = new CPSolver();
        
        SetVariable root = constant(new int[]{0});
        m.addConstraint(eqCard(root, 1));
        SetVariable[] __root = new SetVariable[]{root};

        ScriptEngine engine = new ScriptEngineManager().getEngineByMimeType("application/javascript");
        if (engine == null) {
            throw new IllegalStateException("Missing javascript engine.");
        }

        String header = readAll(ChocoSolver.class.getResourceAsStream("header.js"));
        String script = readAll(in);

        /**/
        engine.put("m", m);
        engine.put("s", s);
        engine.put("root", root);
        engine.put("__root", __root);

        Invocable i = (Invocable) engine;

        engine.put(ScriptEngine.FILENAME, "header.js");
        engine.eval(header);
        engine.put(ScriptEngine.FILENAME, in.getName());
        engine.eval(script);

//        ChocoLogging.toVerbose();
//        ChocoLogging.toSearch();
        s.read(m);

        s.addGoal(new AssignSetVar(new MinDomSet(s), new MinEnv()));
        s.addGoal(new AssignVar(new MostConstrained(s), new IncreasingDomain()));

        System.out.println("Solving");
        if (!Boolean.TRUE.equals(s.solve())) {
            System.err.println("No solution.");
            return;
        }
        s.checkSolution();
        i.invokeFunction("solution", s);
        System.out.println(s.solutionToString());

        while (all && (System.in.read()) != -1 && s.nextSolution()) {
            i.invokeFunction("solution", s);
        }
    }

    public static String readAll(File in) throws IOException {
        Reader reader = new FileReader(in);
        try {
            return readAll(reader);
        } finally {
            reader.close();
        }
    }

    public static String readAll(InputStream in) throws IOException {
        return readAll(new InputStreamReader(in));
    }

    public static String readAll(Reader in) throws IOException {
        StringBuilder result = new StringBuilder();
        char[] buffer = new char[1024];
        int l;
        while ((l = in.read(buffer)) != -1) {
            result.append(buffer, 0, l);
        }
        return result.toString();
    }
}