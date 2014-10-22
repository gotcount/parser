/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.gotcount.query;

/**
 * Replacement for JDK8 Predicate
 * To be replaced when switching to Java 8
 * 
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public interface Predicate<T> {
    
    boolean test(T test);
    
}
