package com.inaos.iamj.attach;

import net.bytebuddy.agent.ByteBuddyAgent;

import java.io.File;

public class InaosAttacher {

    public static void main(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Expected an agent jar as a first argument and at least one process id");
        }

        File agent = new File(args[0]);
        for (int index = 1; index < args.length; index++) {
            ByteBuddyAgent.attach(agent, args[index]);
        }
    }
}
