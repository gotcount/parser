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
import org.parboiled.common.Predicate;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class Filter {
    
    public static Filter fromString(String input) {
        return new Filter(input);
    }
    
    private final Map<String, Predicate> map;

    public Filter(String input) {
        map = Parser.INSTANCE.parser(input);
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
