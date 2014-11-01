/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.gotcount.query;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.ReportingParseRunner;

/**
 * Filter constructs the predicate set based on a query string
 * 
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class Filter {
    
    public static Filter fromString(String input) {
        return new Filter(input);
    }
    
    private final Map<String, Predicate> map;

    public Filter(String input) {
        QueryParser parser = Parboiled.createParser(QueryParser.class);
        ReportingParseRunner<Object> reportingParseRunner = new ReportingParseRunner<>(parser.Query());
        map = (Map)reportingParseRunner.run(input).resultValue;        
    }
    
    public Set<String> getDimensions() {
        if (map == null) {
            return new HashSet<>();
        }
        return Collections.unmodifiableSet(map.keySet());
    }
    
    public Predicate getPredicate(String dimension) {
        if (!map.containsKey(dimension)) {
            throw new NoSuchElementException();
        }
        return map.get(dimension);
    }
    
}
