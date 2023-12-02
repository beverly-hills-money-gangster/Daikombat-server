package com.beverly.hills.money.gang;

import com.beverly.hills.money.gang.runner.ServerRunner;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        ServerRunner serverRunner = new ServerRunner(2025);
        serverRunner.runServer();
    }
}
