package it.unica.co2.manual.ebookstore;

public class Main {

    public static void main(String[] args) {
        new Thread(new Buyer()).start();
        new Thread(new Seller()).start();
        new Thread(new Distributor()).start();
    }

}
