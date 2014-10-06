package de.comci.gotcount.query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.support.StringVar;

/**
 * Query := Filter(';' Filter) Filter := Object':'Action Action := Value /
 * UnaryOperation Value / Value BinaryOperation Value / SetOperation (Value /
 * '['Value(',' Value)*']') / SetOperation ('('/'[') Value(',' Value)* (')'/']')
 * UnaryOperation := '!' / '>' / '<' BinaryOperation := '-' SetOperation := 'IN'
 * / '!IN' Value := Number / String
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
@BuildParseTree
public class QueryParser extends BaseParser<Object> {

    Map<String, Check> map = new HashMap<>();

    Rule Query() {

        return Sequence(
                Filter(),
                addToMap((String) pop(1), (Check) pop()),
                ZeroOrMore(
                        Sequence(
                                TestNot(String("\\")),
                                String(";"),
                                Filter(),
                                addToMap((String) pop(1), (Check) pop())
                        )
                ),
                push(map)
        );
    }

    boolean addToMap(String key, Check value) {
        map.put(key, value);
        return true;
    }

    Rule Filter() {
        return Sequence(
                Dimension(),
                //TestNot(String("\\")), not necessary as dimension does not allow '\' or ':'
                String(":"),
                Condition()
        );
    }

    Rule Dimension() {
        return Sequence(
                ZeroOrMore(
                        FirstOf(
                                Char(),
                                Integer(),
                                String("-"),
                                String("_")
                        )
                ),
                push(matchOrDefault(""))
        );
    }

    Rule Condition() {
        return FirstOf(
                ListPredicate(),
                Predicate()                
        );
    }

    Rule Predicate() {
        return FirstOf(
                Sequence(
                        Not(),
                        Content(true)
                ),
                Content(false)
        );
    }

    Rule Not() {
        return Sequence(TestNot(String("\\")), String("!"));
    }

    Rule ListPredicate() {
        return FirstOf(
                Sequence(
                        Not(),
                        Sequence(String("["), AtomList(), String("]")),
                        push(new ListCheck<>(pop(), true, Object.class))
                ),
                Sequence(
                        String("("), 
                        AtomList(), 
                        String(")"), 
                        push(new ListCheck<>(pop(), true, Object.class))
                )
        );
    }
    
    Rule AtomList() {
        List l = new LinkedList();
        return Sequence(
                Atom(),
                addToList(l, pop()),
                ZeroOrMore(
                        Sequence(
                                ZeroOrMore(String(" ")),
                                String(","),
                                ZeroOrMore(String(" ")),
                                Atom(),
                                addToList(l, pop())
                        )
                ),
                push(l)
        );
    }
    
    boolean addToList(List l, Object value) {
        l.add(value);
        return true;
    }
    
    Rule Atom() {
        return FirstOf(
                Time(),Date(),Number(),String()
        );
    }

    Rule Content(boolean isNot) {
        return FirstOf(
                Sequence(
                        Time(),
                        push(new DefaultCheck<>((Date) pop(), isNot, Date.class))
                ),
                Sequence(
                        Date(),
                        push(new DefaultCheck<>((Date) pop(), isNot, Date.class))
                ),
                Sequence(
                        Number(),
                        push(new NumberCheck((Number) pop(), isNot))
                ),
                Sequence(
                        String(),
                        push(new DefaultCheck<>((String) pop(), isNot, String.class))
                )
        );
    }

    Rule Number() {
        return FirstOf(
                Integer(),
                Double()
        );
    }

    Rule Integer() {
        return Sequence(
                Digits(),
                // parse the input text matched by the preceding "Digits" rule,
                // convert it into an Integer and push it onto the value stack
                // the action uses a default string in case it is run during error recovery (resynchronization)
                push(Integer.parseInt(matchOrDefault("0")))
        );
    }

    Rule Double() {
        return Sequence(
                Integer(),
                String("."),
                Integer(),
                // push the double value to the stack removing the previous
                // 2 integer values
                push(Double.parseDouble(pop(1) + "." + pop()))
        );
    }

    @SuppressSubnodes
    public Rule Digits() {
        return OneOrMore(Digit());
    }

    Rule Digit() {
        return CharRange('0', '9');
    }

    Rule Char() {
        return FirstOf(
                CharRange('a', 'z'),
                CharRange('A', 'Z')
        );
    }

    Rule ISODateOnlyFormat() {
        return Sequence(
                Digit(),
                Digit(),
                Digit(),
                Digit(),
                String("-"),
                Digit(),
                Digit(),
                String("-"),
                Digit(),
                Digit()
        );
    }

    Rule Date() {
        return Sequence(
                ISODateOnlyFormat(),
                push(parseDate(match(), "yyyy-MM-dd"))
        );
    }

    Date parseDate(String input, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date date = null;
        try {
            date = sdf.parse(input);
        } catch (ParseException ex) {
            // ignore
        }
        return date;
    }

    Rule Hour() {
        return FirstOf(
                Sequence(
                        CharRange('0', '1'),
                        CharRange('0', '9')
                ),
                Sequence(
                        String("2"),
                        CharRange('0', '3')
                )
        );
    }

    Rule MinutesOrSeconds() {
        return Sequence(
                CharRange('0', '5'),
                CharRange('0', '9')
        );
    }

    Rule TimeWithoutSeconds() {
        return Sequence(Hour(), String(":"), MinutesOrSeconds());
    }

    Rule TimeWithSeconds() {
        return Sequence(
                TimeWithoutSeconds(),
                String(":"),
                MinutesOrSeconds()
        );
    }

    Rule Time() {
        return FirstOf(
                Sequence(TimeWithSeconds(),
                        push(parseDate(match(), "HH:mm:ss"))
                ),
                Sequence(TimeWithoutSeconds(),
                        push(parseDate(match(), "HH:mm"))
                )
        );
    }

    Rule String() {
        StringVar s = new StringVar();
        return Sequence(
                ZeroOrMore(
                        FirstOf(
                                Sequence(Char(), s.append(match())),
                                Sequence(String("\\"), AnyOfThese(":()[];!"), s.append(match()))
                        )
                ),
                push(s.get())
        );
    }

    Rule AnyOfThese(String strings) {
        String[] split = strings.split("");
        if (split[0].equals("")) {
            // because:
            // http://stackoverflow.com/questions/22718744/why-does-split-in-java-8-sometimes-remove-empty-strings-at-start-of-result-array
            split = Arrays.copyOfRange(split, 1, split.length);
        }
        return FirstOf(split);
    }

    public static class NumberCheck extends DefaultCheck<Number> {

        public NumberCheck(Number value, boolean isNot) {
            super(value, isNot, Number.class);
        }

        @Override
        public boolean test(Object value) {
            typeCheck(value);
            return ((v == value) || (v.doubleValue() == ((Number) value).doubleValue())) ^ isNot; // is in 'a xor b'
        }
    }

    public static class DefaultCheck<T> implements Check {

        final T v;
        final boolean isNot;
        private final Class<? extends T> type;

        public DefaultCheck(T value, boolean isNot, Class<? extends T> type) {
            this.v = value;
            this.isNot = isNot;
            this.type = type;
        }

        public final boolean typeCheck(Object value) {
            if (value != null && !type.isAssignableFrom(value.getClass())) {
                throw new IllegalArgumentException("not a " + type.getSimpleName());
            }
            return true;
        }

        @Override
        public boolean test(Object value) {
            typeCheck(value);
            return ((v == value) || (v != null && v.equals(value))) ^ isNot; // is in 'a xor b'
        }

    }

    public static class ListCheck<T> extends DefaultCheck<T> {

        public ListCheck(T value, boolean isNot, Class<? extends T> type) {
            super(value, isNot, type);
        }

        @Override
        public boolean test(Object value) {
            typeCheck(value);
            return ((List)v).contains(value) ^isNot;
        }
        
    }
    
}
