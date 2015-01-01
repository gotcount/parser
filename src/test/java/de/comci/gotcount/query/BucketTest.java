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
public class BucketTest {
        
    @Test
    public void shouldParseASimpleBucket() {
        Bucket instance = Bucket.fromString("myBucket:[1,10]");
        assertThat(instance.getName()).isEqualTo("myBucket");
        Predicate p = instance.getPredicate();
        assertThat(p).isEqualTo(new QueryParser.RangeCheck(1l, true, 10l, true)); // explicit cast to long here
    }
    
    @Test
    public void shouldParseABucketWithWhitespaceInTheName() {
        Bucket instance = Bucket.fromString("Children and Teenagers:[0,19]");
        assertThat(instance.getName()).isEqualTo("Children and Teenagers");
        Predicate p = instance.getPredicate();
        assertThat(p).isEqualTo(new QueryParser.RangeCheck(0l, true, 19l, true)); // explicit cast to long here
    }
    
    @Test
    public void shouldParseABucketWithDoublePointAndQuotesInTheName() {
        Bucket instance = Bucket.fromString("Bucket\\: 1:[0,19]");
        assertThat(instance.getName()).isEqualTo("Bucket: 1");
        Predicate p = instance.getPredicate();
        assertThat(p).isEqualTo(new QueryParser.RangeCheck(0l, true, 19l, true)); // explicit cast to long here
    }
    
}
