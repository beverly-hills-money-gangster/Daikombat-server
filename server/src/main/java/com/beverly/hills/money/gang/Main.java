package com.beverly.hills.money.gang;

import com.beverly.hills.money.gang.runner.ServerRunner;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args) throws InterruptedException, FileNotFoundException {
        ServerRunner runner = new ServerRunner(NumberUtils.toInt(args[0]));
        runner.runServer();
    }
}
