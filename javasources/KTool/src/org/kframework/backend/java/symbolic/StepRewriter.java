package org.kframework.backend.java.symbolic;

import org.kframework.backend.java.builtins.IntToken;
import org.kframework.backend.java.kil.ConstrainedTerm;
import org.kframework.backend.java.kil.Definition;
import org.kframework.backend.java.kil.Rule;
import org.kframework.backend.java.kil.Term;
import org.kframework.backend.java.kil.Variable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.google.common.base.Stopwatch;


/**
 * @author AndreiS
 */
public class StepRewriter {

    private final Definition definition;
    private final Collection<Rule> rules;
    private final Stopwatch stopwatch = new Stopwatch();
    private Collection<ConstrainedTerm> constrainedTermResults = new ArrayList<ConstrainedTerm>();
    private Collection<Term> termResults = new ArrayList<Term>();


    public StepRewriter(Collection<Rule> rules, Definition definition) {
        this.rules = rules;
        this.definition = definition;
    }

    public Collection<Term> getAllSuccessors(Term term) {
        for (Rule rule : rules) {
            rewriteByRule(term, rule);
        }
        return termResults;
    }

    public Collection<ConstrainedTerm> getAllNarrowingSuccessors(ConstrainedTerm constrainedTerm) {
        for (Rule rule : rules) {
            narrowByRule(constrainedTerm, rule);
        }
        return constrainedTermResults;
    }

    public Term getOneSuccessor(Term term) {
        for (Rule rule : rules) {
            rewriteByRule(term, rule);
            if (!constrainedTermResults.isEmpty()) {
                return constrainedTermResults.iterator().next();
            }
        }
        return null;
    }

    public ConstrainedTerm getOneNarrowingSuccessor(ConstrainedTerm constrainedTerm) {
        for (Rule rule : rules) {
            narrowByRule(constrainedTerm, rule);
            if (!constrainedTermResults.isEmpty()) {
                return constrainedTermResults.iterator().next();
            }
        }
        return null;
    }

    private void narrowByRule(ConstrainedTerm constrainedTerm, Rule rule) {
        stopwatch.reset();
        stopwatch.start();

        constrainedTermResults = new ArrayList<ConstrainedTerm>();

        SymbolicConstraint leftHandSideConstraint = new SymbolicConstraint(definition);
        leftHandSideConstraint.addAll(rule.condition());
        for (Variable variable : rule.freshVariables()) {
            leftHandSideConstraint.add(variable, IntToken.fresh());
        }

        ConstrainedTerm leftHandSide = new ConstrainedTerm(
                rule.leftHandSide(),
                rule.lookups(),
                leftHandSideConstraint);

        for (SymbolicConstraint constraint : constrainedTerm.unify(leftHandSide, definition)) {
            /* rename rule variables in the constraints */
            Map<Variable, Variable> freshSubstitution = constraint.rename(rule.variableSet());

            Term result = rule.rightHandSide();
            /* rename rule variables in the rule RHS */
            result = result.substitute(freshSubstitution, definition);
            /* apply the constraints substitution on the rule RHS */
            result = result.substitute(constraint.substitution(), definition);
            /* evaluate pending functions in the rule RHS */
            result = result.evaluate(definition);
            /* eliminate anonymous variables */
            constraint.eliminateAnonymousVariables();

            /* compute all results */
            constrainedTermResults.add(new ConstrainedTerm(result, constraint, definition));
        }

        stopwatch.stop();
    }

    // apply rule by matching
    private void rewriteByRule(Term term, Rule rule) {
        stopwatch.reset();
        stopwatch.start();

        constrainedTermResults = new ArrayList<ConstrainedTerm>();

        ConstrainedTerm constrainedTerm = new ConstrainedTerm(term, definition);

        SymbolicConstraint leftHandSideConstraint = new SymbolicConstraint(definition);
        leftHandSideConstraint.addAll(rule.condition());
        for (Variable variable : rule.freshVariables()) {
            leftHandSideConstraint.add(variable, IntToken.fresh());
        }

        ConstrainedTerm leftHandSide = new ConstrainedTerm(
                rule.leftHandSide(),
                rule.lookups(),
                leftHandSideConstraint);

        for (SymbolicConstraint constraint : constrainedTerm.unify(leftHandSide, definition)) {
            /* check the constraint represents a match */
            if (!constraint.isSubstitution()
                    || !constraint.substitution().keySet().equals(rule.variableSet())) {
                continue;
            }

            Term result = rule.rightHandSide();
            /* apply the constraints substitution on the rule RHS */
            result = result.substitute(constraint.substitution(), definition);
            /* evaluate pending functions in the rule RHS */
            result = result.evaluate(definition);

            /* compute all results */
            termResults.add(term);
        }

        stopwatch.stop();
    }

}