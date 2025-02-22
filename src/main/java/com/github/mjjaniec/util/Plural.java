package com.github.mjjaniec.util;

public class Plural {
    public static String points(int number) {
        if (number == 1) {
            return "punkt";
        }
        if (number % 10 >= 2 && number % 10 <= 4 && !(number % 100 > 10 && number % 100 < 20)) {
            return "punkty";
        }
        return "punktów";
    }
}
