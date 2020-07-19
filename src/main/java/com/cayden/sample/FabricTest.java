package com.cayden.sample;

import com.webank.wecross.stub.*;
import com.cayden.sample.fabric.FabricDriver;
import com.cayden.sample.fabric.FabricStubFactory;

public class FabricTest {

    private FabricStubFactory fabricStubFactory;
    private FabricDriver driver;
    private Connection connection;
    private Account account;
    private ResourceInfo resourceInfo;

    private MockBlockHeaderManager blockHeaderManager;


    public FabricTest() {
        FabricStubFactory fabricStubFactory = new FabricStubFactory();
        driver = (FabricDriver) fabricStubFactory.newDriver();
        connection = fabricStubFactory.newConnection("classpath:fabric/");
        account = fabricStubFactory.newAccount("fabric_user1", "classpath:accounts/fabric_user1/");
        resourceInfo = new ResourceInfo();
        for (ResourceInfo info : connection.getResources()) {
            if (info.getName().equals("abac")) {
                resourceInfo = info;
            }
        }

        blockHeaderManager = new MockBlockHeaderManager(driver, connection);
    }

    private void getBlockheight(){
        long blockNumber = driver.getBlockNumber(connection);

        System.out.println(blockNumber);
    }

    public static void main(String [] args){
        FabricTest fabricTest=new FabricTest();
        fabricTest.getBlockheight();
    }

    public static class MockBlockHeaderManager implements BlockHeaderManager {
        private Driver driver;
        private Connection connection;

        public MockBlockHeaderManager(Driver driver, Connection connection) {
            this.driver = driver;
            this.connection = connection;
        }

        @Override
        public long getBlockNumber() {
            return driver.getBlockNumber(connection);
        }

        @Override
        public byte[] getBlockHeader(long l) {
            return driver.getBlockHeader(l, connection);
        }

        @Override
        public void asyncGetBlockHeader(long blockNumber, BlockHeaderCallback callback) {
            callback.onBlockHeader(getBlockHeader(blockNumber));
        }
    }
}
