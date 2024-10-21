package com.minwook.springaop.user;

public class User {
    private String name;
    private int age;

    // Constructor to initialize name and age
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // Constructor that uses three String parameters (just for illustration)
    public User(String a, String b, String c) {
        this.name = a;
        // For simplicity, setting age based on a value
        this.age = Integer.parseInt(c); // Assuming 'c' represents a number for 'age'
    }

    // Getter for 'name'
    public String getName() {
        return name;
    }

    // Getter for 'age'
    public int getAge() {
        return age;
    }
}
