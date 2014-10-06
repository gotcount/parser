/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.gotcount.query;

import java.util.Collections;
import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
@BuildParseTree
public class SimpleParser extends BaseParser<Object> {

    Rule Term() {
        return Sequence(
                Number(),
                Operation(),
                Number(),
                push("" + pop(2) + pop(1) + pop())
        );
    }

    Rule Number() {
        return Sequence(
                OneOrMore(Digit()),
                push(Integer.parseInt(matchOrDefault("0")))
        );
    }

    Rule Digit() {
        return CharRange('0', '9');
    }

    Rule Operation() {
        return FirstOf(
                String("+"),
                String("-"),
                String("*"),
                String("/")
        );
    }

    public static void main(String[] args) {
        String input = "2*5642";
        SimpleParser parser = Parboiled.createParser(SimpleParser.class);
        ReportingParseRunner<Object> reportingParseRunner = new ReportingParseRunner<>(parser.Term());
        ParsingResult<Object> result = reportingParseRunner.run(input);
        String printNodeTree = ParseTreeUtils.printNodeTree(result);
        System.out.println(printNodeTree);
        System.out.println(result.resultValue);
    }

}
