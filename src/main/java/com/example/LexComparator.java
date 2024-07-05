package com.example;

import java.util.Comparator;
import java.util.List;

public class LexComparator implements Comparator<List<Integer>> {
    @Override
    public int compare(List<Integer> list1, List<Integer> list2){
        int result = 0;
        int n = list1.size();
        int m = list2.size();
        if (n == m) {
            for (int i = 0; i <= n - 1 && result == 0; i++) {
                result = list1.get(i).compareTo(list2.get(i));
            }
        } else {
            // Handle the case when the lists are not of the same length
            result = Integer.compare(n, m);
        }
        return result;
    }
}    
    

