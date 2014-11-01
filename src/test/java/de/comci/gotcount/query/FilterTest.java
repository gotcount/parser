/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.gotcount.query;


import static org.fest.assertions.api.Assertions.assertThat;
import org.junit.Test;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class FilterTest {

    /**
     * Test of fromString method, of class Filter.
     */
    @Test
    public void testFromString() {
        System.out.println("fromString");
        Filter f = Filter.fromString("d0:abc;d1:!4");
        assertThat(f.getPredicate("d0").test("abc")).isTrue();
        assertThat(f.getPredicate("d1").test(5)).isTrue();
    }
    
    @Test
    public void testFromEmptyString() {
        Filter f = Filter.fromString("");
        assertThat(f.getDimensions()).isEmpty();
    }

    /**
     * Test of getDimensions method, of class Filter.
     */
    @Test
    public void testGetDimensions() {
        System.out.println("getDimensions");
        Filter f = Filter.fromString("d0:abc;d1:!4");
        assertThat(f.getDimensions()).containsOnly("d0", "d1");
    }

    /**
     * Test of getPredicate method, of class Filter.
     */
    @Test
    public void testGetPredicate() {
        System.out.println("fromString");
        Filter f = Filter.fromString("d0:abc;d1:!4");
        assertThat(f.getPredicate("d0").test("abc")).isTrue();
        assertThat(f.getPredicate("d1").test(5)).isTrue();
    }
    
    @Test
    public void testList() {
        Filter f = Filter.fromString("path:{node,user}");
        assertThat(f.getPredicate("path").test("node")).isTrue();
        assertThat(f.getPredicate("path").test("user")).isTrue();
        assertThat(f.getPredicate("path").test("n")).isFalse();
        assertThat(f.getPredicate("path").test("")).isFalse();
    }

}
