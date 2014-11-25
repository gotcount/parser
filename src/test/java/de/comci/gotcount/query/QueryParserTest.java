/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.gotcount.query;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import static org.fest.assertions.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class QueryParserTest {

    private QueryParser parser;
    private ReportingParseRunner<Object> reportingParseRunner;

    @Before
    public void setUp() {
        parser = Parboiled.createParser(QueryParser.class);
        reportingParseRunner = new ReportingParseRunner<>(parser.Query());
    }

    private Map<String, Predicate> defaultSuccessChecks(ParsingResult<Object> result) {
        assertThat(result.matched).isTrue();
        assertThat(result.resultValue).isInstanceOf(Map.class);
        return (Map) result.resultValue;
    }

    private ParsingResult<Object> run(String input) {
        ParsingResult<Object> result = reportingParseRunner.run(input);
        return result;
    }

    @Test
    public void singleCharDimension() {

        String input = "d:123";

        ParsingResult<Object> result = run(input);
        defaultSuccessChecks(result);
        final Map<String, Predicate> mapped = (Map) result.resultValue;
        assertThat(mapped.size()).isEqualTo(1);
        assertThat(mapped.containsKey("d")).isTrue();
        assertThat(mapped.get("d").test(123)).isTrue();
        assertThat(mapped.get("d").test(12)).isFalse();

    }

    @Test
    public void multipleCharDimension() {

        String dimension = "dimension";
        int filter = 123;
        String input = dimension + ":" + filter;

        ParsingResult<Object> result = run(input);
        defaultSuccessChecks(result);
        final Map<String, Predicate> mapped = (Map) result.resultValue;
        assertThat(mapped.size()).isEqualTo(1);
        assertThat(mapped.containsKey(dimension)).isTrue();
        assertThat(mapped.get(dimension).test(filter)).isTrue();
        assertThat(mapped.get(dimension).test(12)).isFalse();

    }

    @Test
    public void dimensionWithAllAllowedChars() {

        String dimension = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_";
        int filter = 123;
        String input = dimension + ":" + filter;

        ParsingResult<Object> result = run(input);
        final Map<String, Predicate> mapped = defaultSuccessChecks(result);

        assertThat(mapped.size()).isEqualTo(1);
        assertThat(mapped.containsKey(dimension)).isTrue();
        assertThat(mapped.get(dimension).test(filter)).isTrue();
        assertThat(mapped.get(dimension).test(12)).isFalse();

    }

    @Test
    public void dimensionWithInvalidChars0() {
        String dimension = "a?b";
        int filter = 123;
        String input = dimension + ":" + filter;

        ParsingResult<Object> result = run(input);
        assertThat(result.matched).isFalse();
    }

    @Test
    public void escapedCharsInFilter() {
        String input = "d0:ab\\:cd;d1:a\\(b\\)c;d2:a\\[b\\]c;d3:a\\;b;d4:a\\!bc";

        ParsingResult<Object> result = run(input);
        Map<String, Predicate> mapped = defaultSuccessChecks(result);

        assertThat(mapped.keySet()).containsOnly("d0", "d1", "d2", "d3", "d4");
        assertThat(mapped.get("d0").test("ab:cd")).isTrue();
        assertThat(mapped.get("d1").test("a(b)c")).isTrue();
        assertThat(mapped.get("d2").test("a[b]c")).isTrue();
        assertThat(mapped.get("d3").test("a;b")).isTrue();
        assertThat(mapped.get("d4").test("a!bc")).isTrue();
    }

    @Test
    public void escapedExclamationMarkAtBeginning() {
        String input = "d:\\!abc";
        ParsingResult<Object> result = run(input);
        Map<String, Predicate> mapped = defaultSuccessChecks(result);

        assertThat(mapped.keySet()).containsOnly("d");
        assertThat(mapped.get("d").test("!abc")).isTrue();
    }

    @Test
    public void twoFilters() {

        String input = "d0:0;d1:1";

        ParsingResult<Object> result = run(input);
        Map<String, Predicate> mapped = defaultSuccessChecks(result);

        assertThat(mapped.keySet()).containsOnly("d0", "d1");
        assertThat(mapped.get("d0").test(0)).isTrue();
        assertThat(mapped.get("d1").test(1)).isTrue();
    }

    @Test
    public void multipleFiltersIncludingTime() {

        String input = "d0:12:13;d1:2001-01-01;d2:aString";

        ParsingResult<Object> result = run(input);
        Map<String, Predicate> mapped = defaultSuccessChecks(result);

        assertThat(mapped.keySet()).containsOnly("d0", "d1", "d2");
        assertThat(mapped.get("d0").test(getDate(1970, 1, 1, 12, 13, 0))).isTrue();
        assertThat(mapped.get("d1").test(getDate(2001, 01, 01, 0, 0, 0))).isTrue();
        assertThat(mapped.get("d2").test("aString")).isTrue();
    }

    @Test
    public void negation() {

        String input = "d0:!0;d1:!abc";

        ParsingResult<Object> result = run(input);
        Map<String, Predicate> mapped = defaultSuccessChecks(result);

        assertThat(mapped.keySet()).containsOnly("d0", "d1");
        assertThat(mapped.get("d0").test(0)).isFalse();
        assertThat(mapped.get("d0").test(1)).isTrue();

        assertThat(mapped.get("d1").test("ab")).isTrue();
        assertThat(mapped.get("d1").test("abc")).isFalse();

    }
    
    @Test
    public void negationDate() {
        
        String input = "d1:!2012-12-24";
        
        ParsingResult<Object> result = run(input);
        Map<String, Predicate> mapped = defaultSuccessChecks(result);

        assertThat(mapped.keySet()).containsOnly("d1");
        assertThat(mapped.get("d1").test(getDate(2012, 12, 24, 0, 0, 0))).isFalse();
        assertThat(mapped.get("d1").test(getDate(2012, 12, 25, 0, 0, 0))).isTrue();
    }

    @Test
    public void simpleIsoDateRule() {
        testISODateRule("2000-01-01", 2000, 1, 1, 0, 0, 0);
        testISODateRule("2012-02-28", 2012, 2, 28, 0, 0, 0);
        testISODateRule("2004-02-29", 2004, 2, 29, 0, 0, 0);
        testISODateRule("2015-12-31", 2015, 12, 31, 0, 0, 0);
        testISODateRule("2005-02-29", 2005, 3, 1, 0, 0, 0);
    }

    private void testISODateRule(String dateToParse, int year, int month, int day, int hour, int min, int sec) {
        String input = dateToParse;

        QueryParser p = Parboiled.createParser(QueryParser.class);
        ReportingParseRunner<Object> runner = new ReportingParseRunner<>(p.Date());
        ParsingResult<Object> run = runner.run(input);

        Date expected = getDate(year, month, day, hour, min, sec);

        assertThat(run.matched).isTrue();
        assertThat(run.resultValue).isEqualTo(expected);
    }

    private Date getDate(int year, int month, int day, int hour, int min, int sec) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);
        c.set(year, month - 1, day, hour, min, sec);
        Date expected = c.getTime();
        return expected;
    }

    @Test
    public void simpleTimeRule() {
        testTimeRule("00:00", 0, 0, 0);
        testTimeRule("23:59", 23, 59, 0);
        testTimeRule("12:46", 12, 46, 0);
        testTimeRule("00:00:00", 0, 0, 0);
        testTimeRule("00:00:01", 0, 0, 1);
        testTimeRule("23:59:59", 23, 59, 59);
    }

    private void testTimeRule(String timeToParse, int hour, int min, int sec) {

        QueryParser p = Parboiled.createParser(QueryParser.class);
        ReportingParseRunner<Object> runner = new ReportingParseRunner<>(p.Time());
        ParsingResult<Object> run = runner.run(timeToParse);

        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);
        c.set(1970, 0, 1, hour, min, sec);
        Date expected = c.getTime();

        assertThat(run.matched).isTrue();
        assertThat(run.resultValue).isEqualTo(expected);

    }

    @Test
    public void isoDateValue() {

        String input = "d0:2012-02-28";

        ParsingResult<Object> result = run(input);
        Map<String, Predicate> mapped = defaultSuccessChecks(result);

        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);
        c.set(2012, 1, 28, 0, 0, 0);
        Date expected = c.getTime();

        assertThat(mapped.keySet()).containsOnly("d0");
        assertThat(mapped.get("d0").test(expected)).isTrue();

    }

    @Test
    public void timeValue() {

        String input = "d0:12:46";

        ParsingResult<Object> result = run(input);
        Map<String, Predicate> mapped = defaultSuccessChecks(result);

        Calendar c = Calendar.getInstance();
        c.set(Calendar.MILLISECOND, 0);
        c.set(1970, 0, 1, 12, 46, 0);
        Date expected = c.getTime();

        assertThat(mapped.keySet()).containsOnly("d0");
        assertThat(mapped.get("d0").test(expected)).isTrue();

    }

    @Test
    public void inStringList() {

        String input = "d0:{a,befg,c,d}";

        ParsingResult<Object> result = run(input);
        Map<String, Predicate> mapped = defaultSuccessChecks(result);

        assertThat(mapped.keySet()).containsOnly("d0");
        assertThat(mapped.get("d0").test("a")).isTrue();
        assertThat(mapped.get("d0").test("befg")).isTrue();
        assertThat(mapped.get("d0").test("c")).isTrue();
        assertThat(mapped.get("d0").test("d")).isTrue();
        assertThat(mapped.get("d0").test("e")).isFalse();
        assertThat(mapped.get("d0").test("b")).isFalse();

    }
    
    @Test
    @Ignore // does not work yet
    public void ipAsString() {

        String input = "d0:10.10.";

        ParsingResult<Object> result = run(input);
        
        System.out.println(ParseTreeUtils.printNodeTree(result));
        
        Map<String, Predicate> mapped = defaultSuccessChecks(result);

        assertThat(mapped.keySet()).containsOnly("d0");
        
        System.out.println(mapped.get("d0").toString());
        
        assertThat(mapped.get("d0")).isEqualTo(new QueryParser.DefaultCheck<>("10.10.10.10", false, String.class));

    }
    
    @Test
    public void betweenExclusiveInt() {

        String input = "d0:(1,5)";

        ParsingResult<Object> result = run(input);
        Map<String, Predicate> mapped = defaultSuccessChecks(result);

        assertThat(mapped.keySet()).containsOnly("d0");
        
        assertThat(mapped.get("d0")).isEqualTo(new QueryParser.RangeCheck(1l, false, 5l, false));

    }
    
    @Test
    public void betweenInExclusiveInt() {

        String input = "d0:[1,5)";

        ParsingResult<Object> result = run(input);
        Map<String, Predicate> mapped = defaultSuccessChecks(result);

        assertThat(mapped.keySet()).containsOnly("d0");
        
        assertThat(mapped.get("d0")).isEqualTo(new QueryParser.RangeCheck(1l, true, 5l, false));

    }
    
    @Test
    public void betweenExInclusiveInt() {

        String input = "d0:(1,5]";

        ParsingResult<Object> result = run(input);
        Map<String, Predicate> mapped = defaultSuccessChecks(result);

        assertThat(mapped.keySet()).containsOnly("d0");
        
        assertThat(mapped.get("d0")).isEqualTo(new QueryParser.RangeCheck(1l, false, 5l, true));

    }
    
    @Test
    public void betweenExInclusiveDate() {

        String input = "d0:(2010-01-01,2013-12-31)";

        ParsingResult<Object> result = run(input);
        Map<String, Predicate> mapped = defaultSuccessChecks(result);

        assertThat(mapped.keySet()).containsOnly("d0");
        
        assertThat(mapped.get("d0").test(new Date(109,11,31))).isFalse();
        assertThat(mapped.get("d0").test(new Date(110,0,1))).isFalse();
        assertThat(mapped.get("d0").test(new Date(110,0,2))).isTrue();
        assertThat(mapped.get("d0").test(new Date(113,11,30))).isTrue();
        assertThat(mapped.get("d0").test(new Date(113,11,31))).isFalse();
        assertThat(mapped.get("d0").test(new Date(114,0,1))).isFalse();

    }
    
    @Test
    public void betweenInclusiveDate() {

        String input = "d0:[2010-01-01,2013-12-31]";

        ParsingResult<Object> result = run(input);
        Map<String, Predicate> mapped = defaultSuccessChecks(result);

        assertThat(mapped.keySet()).containsOnly("d0");
        
        assertThat(mapped.get("d0").test(new Date(109,11,31))).isFalse();
        assertThat(mapped.get("d0").test(new Date(110,0,1))).isTrue();
        assertThat(mapped.get("d0").test(new Date(110,0,2))).isTrue();
        assertThat(mapped.get("d0").test(new Date(113,11,30))).isTrue();
        assertThat(mapped.get("d0").test(new Date(113,11,31))).isTrue();
        assertThat(mapped.get("d0").test(new Date(114,0,1))).isFalse();

    }
    
    @Test
    @Ignore // does not work yet
    public void negatedDoubleList() {
        
        String input = "d0:!{0.1,0.4,65}";
        
        ParsingResult<Object> result = run(input);
        Map<String, Predicate> mapped = defaultSuccessChecks(result);

        assertThat(mapped.keySet()).containsOnly("d0");
        assertThat(mapped.get("d0").test(0.1)).isFalse();
        assertThat(mapped.get("d0").test(0.4)).isFalse();
        assertThat(mapped.get("d0").test(65)).isFalse();
        assertThat(mapped.get("d0").test(0)).isTrue();
        assertThat(mapped.get("d0").test(Double.MIN_VALUE)).isTrue();
        assertThat(mapped.get("d0").test(Double.MAX_VALUE)).isTrue();
    }
    
    @Test
    public void testListPredicate() {
        QueryParser p = Parboiled.createParser(QueryParser.class);
        ReportingParseRunner<Object> runner = new ReportingParseRunner<>(p.ListPredicate());
        ParsingResult<Object> result = runner.run("{1,23,4}");
        
        Predicate actual = (Predicate) result.resultValue;
        assertThat(actual).isNotNull();
        assertThat(actual.test(23l)).isTrue();
        
    }
    
    @Test
    public void doubleNumber() {
        QueryParser p = Parboiled.createParser(QueryParser.class);
        ReportingParseRunner<Object> runner = new ReportingParseRunner<>(p.Double());
        ParsingResult<Object> result = runner.run("0.1");
        assertThat(result.resultValue).isEqualTo(0.1);
    }
    
    @Test
    public void rangeCheckInclusive() {
        
        QueryParser.RangeCheck rc = new QueryParser.RangeCheck(1, true, 3, true);
        
        assertThat(rc.test(0)).isFalse();
        assertThat(rc.test(1)).isTrue();
        assertThat(rc.test(2)).isTrue();
        assertThat(rc.test(3)).isTrue();        
        assertThat(rc.test(4)).isFalse();
        
    }
    
    @Test
    public void rangeCheckExclusive() {
        
        QueryParser.RangeCheck rc = new QueryParser.RangeCheck(1, false, 3, false);
        
        assertThat(rc.test(0)).isFalse();
        assertThat(rc.test(1)).isFalse();
        assertThat(rc.test(2)).isTrue();
        assertThat(rc.test(3)).isFalse();        
        assertThat(rc.test(4)).isFalse();
        
    }
    
    @Test
    public void rangeCheckInExclusive() {
        
        QueryParser.RangeCheck rc = new QueryParser.RangeCheck(1, true, 3, false);
        
        assertThat(rc.test(0)).isFalse();
        assertThat(rc.test(1)).isTrue();
        assertThat(rc.test(2)).isTrue();
        assertThat(rc.test(3)).isFalse();        
        assertThat(rc.test(4)).isFalse();
        
    }
    
    @Test
    public void rangeCheckExInclusive() {
        
        QueryParser.RangeCheck rc = new QueryParser.RangeCheck(1, false, 3, true);
        
        assertThat(rc.test(0)).isFalse();
        assertThat(rc.test(1)).isFalse();
        assertThat(rc.test(2)).isTrue();
        assertThat(rc.test(3)).isTrue();        
        assertThat(rc.test(4)).isFalse();
        
    }
    
    @Test
    public void switchedBordersInRangeCheck() {
        
        assertThat(new QueryParser.RangeCheck(5, true, 1, true)).isEqualTo(new QueryParser.RangeCheck(1, true, 5, true));
        
    }

}
