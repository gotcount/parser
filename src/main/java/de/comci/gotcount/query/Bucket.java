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
public class Bucket {
    
    public static Bucket fromString(String input) {
        return new Bucket(input);
    }
    
    private final Map.Entry<String, Predicate> bucket;

    public Bucket(String input) {
        QueryParser parser = Parboiled.createParser(QueryParser.class);
        ReportingParseRunner<Object> reportingParseRunner = new ReportingParseRunner<>(parser.Filter());
        bucket = (Map.Entry)reportingParseRunner.run(input).resultValue;        
    }
    
    public String getName() {
        return bucket.getKey();
    }
    
    public Predicate getPredicate() {
        return bucket.getValue();
    }
    
}
