package org.clafer.ir;

import org.clafer.common.Check;

/**
 *
 * @author jimmy
 */
public class IrWithin extends IrAbstractBool implements IrBoolExpr {

    private final IrIntExpr var;
    private final IrDomain range;

    IrWithin(IrIntExpr var, IrDomain range, IrBoolDomain domain) {
        super(domain);
        this.var = Check.notNull(var);
        this.range = Check.notNull(range);

        if (range.isEmpty()) {
            throw new IllegalArgumentException();
        }
    }

    public IrIntExpr getVar() {
        return var;
    }

    public IrDomain getRange() {
        return range;
    }

    @Override
    public IrBoolExpr negate() {
        return new IrNotWithin(var, range, getDomain().invert());
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public <A, B> B accept(IrBoolExprVisitor<A, B> visitor, A a) {
        return visitor.visit(this, a);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IrWithin) {
            IrWithin other = (IrWithin) obj;
            return var.equals(other.var) && range.equals(other.range) && super.equals(other);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return var.hashCode() ^ range.hashCode();
    }

    @Override
    public String toString() {
        return var + " ∈ " + range;
    }
}