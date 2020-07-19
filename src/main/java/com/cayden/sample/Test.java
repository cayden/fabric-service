package com.cayden.sample;

import com.webank.wecross.stub.Account;
import com.webank.wecross.stub.Connection;
import com.cayden.sample.fabric.FabricDriver;
import com.cayden.sample.fabric.FabricStubFactory;

public class Test {

    public static void main(String [] args){
        System.out.println("test");

        FabricStubFactory fabricStubFactory = new FabricStubFactory();
        FabricDriver  driver = (FabricDriver) fabricStubFactory.newDriver();
        Connection connection = fabricStubFactory.newConnection("classpath:fabric/");
         Account account = fabricStubFactory.newAccount("fabric_user1", "classpath:accounts/fabric_user1/");
    }
}
