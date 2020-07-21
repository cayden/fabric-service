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
            System.out.println("^^^^^^^^^"+info.getName());
            if (info.getName().equals("mcka")) {
                resourceInfo = info;
            }
        }

        blockHeaderManager = new MockBlockHeaderManager(driver, connection);
    }

    private void getBlockheight(){
        long blockNumber = driver.getBlockNumber(connection);

        System.out.println(blockNumber);
    }

    public void callTest() throws Exception {
        String[] result = new String[] {"aaaa","4444444444"};
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setMethod("invoke");
        transactionRequest.setArgs(result);

        TransactionContext<TransactionRequest> request =
                new TransactionContext<>(transactionRequest, account, resourceInfo, null);

        byte[] bytes = driver.encodeTransactionRequest(request);
        TransactionContext<TransactionRequest> requestCmp = driver.decodeTransactionRequest(bytes);

        System.out.println(requestCmp.getData().toString());
    }

    public void callInvoke() throws Exception {
        String[] result = new String[] {"aaaa","4444444444"};
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setMethod("invoke");
        transactionRequest.setArgs(result);

        TransactionContext<TransactionRequest> request =
                new TransactionContext<>(
                        transactionRequest, account, resourceInfo, blockHeaderManager);

        TransactionResponse response = driver.sendTransaction(request, connection);

        System.out.println(response.toString());
    }


    public void callGet() throws Exception {
        String[] result = new String[] {"aaaa"};
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setMethod("query");
        transactionRequest.setArgs(result);

        TransactionContext<TransactionRequest> request =
                new TransactionContext<>(
                        transactionRequest, account, resourceInfo, blockHeaderManager);

        TransactionResponse response = driver.sendTransaction(request, connection);

        System.out.println(response.toString());
    }

    private TransactionResponse sendOneTransaction() throws Exception {
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setMethod("invoke");
        transactionRequest.setArgs(new String[] {"a", "b", "10"});

        TransactionContext<TransactionRequest> request =
                new TransactionContext<>(
                        transactionRequest, account, resourceInfo, blockHeaderManager);

        TransactionResponse response = driver.sendTransaction(request, connection);
        System.out.println(response.toString());
        return response;
    }

    public static void main(String [] args) throws Exception{
        FabricTest fabricTest=new FabricTest();
        fabricTest.getBlockheight();
//        fabricTest.callInvoke();
        fabricTest.callGet();
//        fabricTest.getBlockheight();
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
