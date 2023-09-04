package project.util;

import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.impl.DocumentImpl;

import java.net.URI;
import java.util.UUID;

public record Account(String name, int age, String favoriteColor, int id) {

    private static int nextId = 1;

    public Account(String name, int age, String favoriteColor){
        this(name, age, favoriteColor, nextId++);
    }

    @Override
    public String toString(){
        return "Name: " + name + "\n " +
                "Age: " + age + "\n " +
                "Favorite Color: " + favoriteColor;
    }

    public URI getURI(){
        return URI.create("social_media/accounts/" + id());
    }

    public Document toDocument(){
        return new DocumentImpl(getURI(), toString(),null);
    }

    public Document toBinaryDocument(){
        return new DocumentImpl(getURI(),toString().getBytes());
    }
}
