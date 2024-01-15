package com.beverly.hills.money.gang;

import com.beverly.hills.money.gang.runner.ServerRunner;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        ServerRunner runner = new ServerRunner(7777);
        runner.runServer();
    }
}
