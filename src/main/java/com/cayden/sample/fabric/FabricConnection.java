package com.cayden.sample.fabric;

import com.cayden.sample.common.FabricType;
import com.webank.wecross.stub.Connection;
import com.webank.wecross.stub.Request;
import com.webank.wecross.stub.ResourceInfo;
import com.webank.wecross.stub.Response;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.TransactionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.cayden.sample.utils.FabricUtils.bytesToLong;
import static com.cayden.sample.utils.FabricUtils.longToBytes;

public class FabricConnection implements Connection {
    private Logger logger = LoggerFactory.getLogger(FabricConnection.class);
    private Channel channel;
    private Map<String, ChaincodeConnection> chaincodeMap;
    private long latestBlockNumber = 0;
    private ThreadPoolTaskExecutor threadPool;
    private String blockListenerHandler;

    public FabricConnection(Channel channel, Map<String, ChaincodeConnection> chaincodeMap) {
        this.channel = channel;
        this.chaincodeMap = chaincodeMap;
        this.threadPool = new ThreadPoolTaskExecutor();
        this.threadPool.setCorePoolSize(200);
        this.threadPool.setMaxPoolSize(500);
        this.threadPool.setQueueCapacity(5000);
    }

    public void start() throws Exception {

        this.blockListenerHandler =
                channel.registerBlockListener(
                        blockEvent -> {
                            long currentBlockNumber = blockEvent.getBlockNumber();
                            if (this.latestBlockNumber < currentBlockNumber) {
                                this.latestBlockNumber = currentBlockNumber;
                            }
                        });

        channel.initialize();

        threadPool.initialize();
    }

    @Override
    public Response send(Request request) {
        switch (request.getType()) {
            case FabricType.ConnectionMessage.FABRIC_CALL:
                return handleCall(request);

            case FabricType.ConnectionMessage.FABRIC_SENDTRANSACTION_ENDORSER:
                return handleSendTransactionEndorser(request);

            case FabricType.ConnectionMessage.FABRIC_SENDTRANSACTION_ORDERER:
                return handleSendTransactionOrderer(request);

            case FabricType.ConnectionMessage.FABRIC_GET_BLOCK_NUMBER:
                return handleGetBlockNumber(request);

            case FabricType.ConnectionMessage.FABRIC_GET_BLOCK_HEADER:
                return handleGetBlockHeader(request);

            case FabricType.ConnectionMessage.FABRIC_GET_TRANSACTION:
                return handleGetTransaction(request);

            default:
                return FabricConnectionResponse.build()
                        .errorCode(FabricType.TransactionResponseStatus.RESOURCE_NOT_FOUND)
                        .errorMessage(
                                "Resource not found, name: " + request.getResourceInfo().getName());
        }
    }

    @Override
    public void asyncSend(Request request, Connection.Callback callback) {
        switch (request.getType()) {
            case FabricType.ConnectionMessage.FABRIC_CALL:
                handleAsyncCall(request, callback);
                break;

            case FabricType.ConnectionMessage.FABRIC_SENDTRANSACTION_ENDORSER:
                handleAsyncSendTransactionEndorser(request, callback);
                break;

            case FabricType.ConnectionMessage.FABRIC_SENDTRANSACTION_ORDERER:
                handleAsyncSendTransactionOrderer(request, callback);
                break;

            default:
                callback.onResponse(send(request));
        }
    }

    @Override
    public List<ResourceInfo> getResources() {
        List<ResourceInfo> resourceInfoList = new LinkedList<>();
        for (ChaincodeConnection chaincodeConnection : chaincodeMap.values()) {
            resourceInfoList.add(chaincodeConnection.getResourceInfo());
        }

        return resourceInfoList;
    }

    private Response handleCall(Request request) {
        ChaincodeConnection chaincodeConnection =
                chaincodeMap.get(request.getResourceInfo().getName());
        if (chaincodeConnection != null) {
            return chaincodeConnection.call(request);
        } else {
            return FabricConnectionResponse.build()
                    .errorCode(FabricType.TransactionResponseStatus.RESOURCE_NOT_FOUND)
                    .errorMessage(
                            "Resource not found, name: " + request.getResourceInfo().getName());
        }
    }

    private void handleAsyncCall(Request request, Connection.Callback callback) {
        // chaincode call is sync, use thread pool to simulate async for better performance
        threadPool.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        callback.onResponse(handleCall(request));
                    }
                });
    }

    private Response handleSendTransactionEndorser(Request request) {
        ChaincodeConnection chaincodeConnection =
                chaincodeMap.get(request.getResourceInfo().getName());
        if (chaincodeConnection != null) {
            return chaincodeConnection.sendTransactionEndorser(request);
        } else {
            return FabricConnectionResponse.build()
                    .errorCode(FabricType.TransactionResponseStatus.RESOURCE_NOT_FOUND)
                    .errorMessage(
                            "Resource not found, name: " + request.getResourceInfo().getName());
        }
    }

    private void handleAsyncSendTransactionEndorser(Request request, Connection.Callback callback) {
        // chaincode call is sync, use thread pool to simulate async for better performance
        threadPool.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        callback.onResponse(handleSendTransactionEndorser(request));
                    }
                });
    }

    private Response handleSendTransactionOrderer(Request request) {
        CompletableFuture<Response> responseFuture = new CompletableFuture<>();

        handleAsyncSendTransactionOrderer(
                request,
                new Callback() {
                    @Override
                    public void onResponse(Response response) {
                        responseFuture.complete(response);
                    }
                });

        try {
            return responseFuture.get(5000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return FabricConnectionResponse.build()
                    .errorCode(FabricType.TransactionResponseStatus.INTERNAL_ERROR)
                    .errorMessage("handleSendTransactionOrderer exception: " + e.getMessage());
        }
    }

    private void handleAsyncSendTransactionOrderer(Request request, Connection.Callback callback) {
        ChaincodeConnection chaincodeConnection =
                chaincodeMap.get(request.getResourceInfo().getName());
        if (chaincodeConnection != null) {
            chaincodeConnection.asyncSendTransactionOrderer(
                    request,
                    new SendTransactionOrdererCallback() {
                        @Override
                        public void onResponse(Response response) {
                            callback.onResponse(response);
                        }
                    });

        } else {
            callback.onResponse(
                    FabricConnectionResponse.build()
                            .errorCode(FabricType.TransactionResponseStatus.RESOURCE_NOT_FOUND)
                            .errorMessage(
                                    "Resource not found, name: "
                                            + request.getResourceInfo().getName()));
        }
    }

    private Response handleGetBlockNumber(Request request) {
        byte[] numberBytes = longToBytes(latestBlockNumber);

        return FabricConnectionResponse.build()
                .errorCode(FabricType.TransactionResponseStatus.SUCCESS)
                .errorMessage("Success")
                .data(numberBytes);
    }

    private Response handleGetBlockHeader(Request request) {

        Response response;
        try {
            long blockNumber = bytesToLong(request.getData());

            // Fabric Just return block
            BlockInfo blockInfo = channel.queryBlockByNumber(blockNumber);
            byte[] blockBytes = blockInfo.getBlock().toByteArray();

            response =
                    FabricConnectionResponse.build()
                            .errorCode(FabricType.TransactionResponseStatus.SUCCESS)
                            .errorMessage("Success")
                            .data(blockBytes);

        } catch (Exception e) {
            response =
                    FabricConnectionResponse.build()
                            .errorCode(FabricType.TransactionResponseStatus.INTERNAL_ERROR)
                            .errorMessage("Get block exception: " + e);
        }
        return response;
    }

    public Response handleGetTransaction(Request request) {
        Response response;
        try {
            String txID = new String(request.getData());
            TransactionInfo transactionInfo = channel.queryTransactionByID(txID);
            response =
                    FabricConnectionResponse.build()
                            .errorCode(FabricType.TransactionResponseStatus.SUCCESS)
                            .errorMessage("Success")
                            .data(transactionInfo.getEnvelope().toByteArray());

        } catch (Exception e) {
            response =
                    FabricConnectionResponse.build()
                            .errorCode(FabricType.TransactionResponseStatus.INTERNAL_ERROR)
                            .errorMessage("Get transaction exception: " + e);
        }
        return response;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public Map<String, ChaincodeConnection> getChaincodeMap() {
        return chaincodeMap;
    }
}
