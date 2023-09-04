package project.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Accounts {

    private static String[] names = {"Reuven","Shimon","Levi","Yehuda","Yissachar","Zevulun",
    "Dan","Naftali","Gad","Asher","Yosef","Binyamin","Avraham","Yitzchak","Yaakov"};

    private static String[] colors = {"Red","Orange","Yellow","Green","Blue","Purple"};

    public static Account[] randomAccounts(int amount, boolean shuffle){
        List<Account> accounts = new ArrayList<>(amount);
        while(amount --> 0){
            accounts.add(new Account(select(names),new Random().nextInt(100),select(colors)));
        }
        if(shuffle){
            Collections.shuffle(accounts);
        }
        return accounts.toArray(Account[]::new);
    }

    public static Account[] randomAccounts(int amount){
        return randomAccounts(amount, false);
    }

    private static String select(String[] arr){
        return arr[new Random().nextInt(arr.length)];
    }

}
