package de.comci.gotcount.query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
class QueryParser extends BaseParser<Object> {

    Map<String, Predicate> map = new HashMap<>();

    Rule Query() {

        return Sequence(
                Filter(),
                addToMap((String) pop(1), (Predicate) pop()),
                ZeroOrMore(
                        Sequence(
                                TestNot(String("\\")),
                                String(";"),
                                Filter(),
                                addToMap((String) pop(1), (Predicate) pop())
                        )
                ),
                push(map)
        );
    }

    boolean addToMap(String key, Predicate value) {
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
                                Long(),
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
        return FirstOf(Sequence(
                        Not(),
                        Sequence(String("{"), AtomList(), String("}")),
                        push(new ListCheck<>(pop(), true, Object.class))
                ),
                Sequence(
                        Sequence(String("{"), AtomList(), String("}")),
                        push(new ListCheck<>(pop(), false, Object.class))
                ),
                Range(true, true),
                Range(true, false),
                Range(false, true),
                Range(false, false)
        );
    }

    Rule Range(final boolean fromInclusive, final boolean toInclusive) {
        
        final java.lang.String fromChar = (fromInclusive) ? "["  : "(";
        final java.lang.String toChar = (toInclusive) ? "]": ")";
        
        return Sequence(
                FirstOf(
                        Sequence(String(fromChar), Date(), String(","), Date(), String(toChar)),
                        Sequence(String(fromChar), Number(), String(","), Number(), String(toChar))
                ),
                push(new RangeCheck((Comparable) pop(1), fromInclusive, (Comparable) pop(), toInclusive))
        );
    }
    
    Rule EndOfAtom() {
        return FirstOf(
                EOI,
                String("]"),
                String("}"),
                String(")"),
                String(";"),
                String(",")
        );
    }

    Rule AtomList() {
        List l = new LinkedList();
        return Sequence(
                ListContent(),
                addToList(l, pop()),
                ZeroOrMore(
                        Sequence(
                                ZeroOrMore(String(" ")),
                                String(","),
                                ZeroOrMore(String(" ")),
                                ListContent(),
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

    Rule ListContent() {
        return FirstOf(
                Time(), Date(), Number(), String()
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
        return Sequence(FirstOf(                
            Double(),
            Long()
        ),Test(EndOfAtom()));
    }
    
    Rule Long() {
        return Sequence(
                Sequence(
                    Optional(String("-")),
                    Digits()
                ),
                // parse the input text matched by the preceding "Digits" rule,
                // convert it into an Integer and push it onto the value stack
                // the action uses a default string in case it is run during error recovery (resynchronization)
                push(Long.parseLong(matchOrDefault("0")))
        );
    }
    
    Rule PositiveLong() {
        return Sequence(
                Digits(),
                // parse the input text matched by the preceding "Digits" rule,
                // convert it into an Integer and push it onto the value stack
                // the action uses a default string in case it is run during error recovery (resynchronization)
                push(Long.parseLong(matchOrDefault("0")))
        );
    }
    
    Rule Double() {
        return Sequence(
                Sequence(
                    Optional(String("-")),
                    Long(),
                    String("."),
                    PositiveLong()
                ),
                // push the double value to the stack removing the previous
                // 2 integer values
                push(Double.parseDouble(matchOrDefault("0")))
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
                                Sequence(Digit(), s.append(match())),
                                Sequence(AnyOfThese(".+"), s.append(match())),
                                Sequence(String("\\"), AnyOfThese(":()[];!"), s.append(match()))
                        )
                ),
                Test(EndOfAtom()),
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
        public boolean test(Number value) {
            typeCheck(value);
            return ((v == value) || (v.doubleValue() == ((Number) value).doubleValue())) ^ isNot; // is in 'a xor b'
        }
    }

    public static class DefaultCheck<T> implements Predicate<T> {

        final T v;
        final boolean isNot;
        private final Class<? extends T> type;

        public DefaultCheck(T value, boolean isNot, Class<? extends T> type) {
            this.v = value;
            this.isNot = isNot;
            this.type = type;
        }

        public final boolean typeCheck(T value) {
            if (value != null && !type.isAssignableFrom(value.getClass())) {
                throw new IllegalArgumentException("not a " + type.getSimpleName());
            }
            return true;
        }

        @Override
        public boolean test(T value) {
            typeCheck(value);
            return ((v == value) || (v != null && v.equals(value))) ^ isNot; // is in 'a xor b'
        }

        @Override
        public java.lang.String toString() {
            return String.format("Check(%s,%s,%s)", v, isNot, type);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.v);
            hash = 53 * hash + (this.isNot ? 1 : 0);
            hash = 53 * hash + Objects.hashCode(this.type);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final DefaultCheck<?> other = (DefaultCheck<?>) obj;
            if (!Objects.equals(this.v, other.v)) {
                return false;
            }
            if (this.isNot != other.isNot) {
                return false;
            }
            if (!Objects.equals(this.type, other.type)) {
                return false;
            }
            return true;
        }
        
    }
    
    public static class RangeCheck implements Predicate<Comparable> {

        private final boolean toInclusive;
        private final boolean fromInclusive;
        private final Comparable to;
        private final Comparable from;

        public RangeCheck(Comparable from, boolean fromInclusive, Comparable to, boolean toInclusive) {
            if (from.compareTo(to) > 0) {
                this.from = to;
                this.to = from;
            } else {
                this.from = from;
                this.to = to;
            }
            this.fromInclusive = fromInclusive;
            this.toInclusive = toInclusive;
        }

        @Override
        public boolean test(Comparable test) {
            
            boolean lowerBorder, upperBorder;
            
            if (test.getClass().isAssignableFrom(from.getClass())) {
                lowerBorder = (fromInclusive && test.compareTo(from) >= 0) || (test.compareTo(from) > 0);
                upperBorder = (toInclusive && test.compareTo(to) <= 0) || (test.compareTo(to) < 0);
            } else if (from.getClass().isAssignableFrom(test.getClass())) {
                lowerBorder = (fromInclusive && from.compareTo(test) <= 0) || (from.compareTo(test) < 0);
                upperBorder = (toInclusive && to.compareTo(test) >= 0) || (to.compareTo(test) > 0);
            } else {
                throw new RuntimeException(String.format("types not compatible for comparison: %s <> %s", test.getClass(), from.getClass()));
            }
            
            return lowerBorder && upperBorder;

        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + (this.toInclusive ? 1 : 0);
            hash = 97 * hash + (this.fromInclusive ? 1 : 0);
            hash = 97 * hash + Objects.hashCode(this.to);
            hash = 97 * hash + Objects.hashCode(this.from);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final RangeCheck other = (RangeCheck) obj;
            if (this.toInclusive != other.toInclusive) {
                return false;
            }
            if (this.fromInclusive != other.fromInclusive) {
                return false;
            }
            if (!Objects.equals(this.to, other.to)) {
                return false;
            }
            if (!Objects.equals(this.from, other.from)) {
                return false;
            }
            return true;
        }

        @Override
        public java.lang.String toString() {
            return String.format("Range(%s,%s-%s,%s)", fromInclusive, from, to, toInclusive);
        }
        
    }

    public static class ListCheck<T> extends DefaultCheck<T> {

        public ListCheck(T value, boolean isNot, Class<? extends T> type) {
            super(value, isNot, type);
        }

        @Override
        public boolean test(T value) {
            typeCheck(value);
            return ((List) v).contains(value) ^ isNot;
        }

    }

}
