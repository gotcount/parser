/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.gotcount.query;

import java.util.Map;
import org.parboiled.Parboiled;
import org.parboiled.common.Predicate;
import org.parboiled.parserunners.ReportingParseRunner;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
enum Parser {
    
    INSTANCE;
    
    private final QueryParser parser;
    private final ReportingParseRunner<Object> reportingParseRunner;
    
    Parser() {
        parser = Parboiled.createParser(QueryParser.class);
        reportingParseRunner = new ReportingParseRunner<>(parser.Query());
    }
    
    Map<String, Predicate> parser(String input) {
        return (Map) this.reportingParseRunner.run(input).resultValue;
    }
    
}
