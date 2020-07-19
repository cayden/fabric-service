package com.cayden.sample.fabric.performance;

import com.cayden.sample.fabric.EndorsementPolicyAnalyzer;
import com.cayden.sample.fabric.FabricConnection;
import com.cayden.sample.fabric.FabricConnectionFactory;
import org.hyperledger.fabric.sdk.*;

import java.util.Collection;

public class PureFabricCallSuite implements PerformanceSuite {
    private Channel channel;
    private HFClient hfClient;
    private Collection<Peer> peers;

    public PureFabricCallSuite(String chainPath) throws Exception {
        FabricConnection fabricConnection = FabricConnectionFactory.build(chainPath);
        fabricConnection.start();

        this.channel = fabricConnection.getChannel();

        if (!fabricConnection.getChaincodeMap().containsKey("sacc")) {
            throw new Exception(
                    "Resource sacc has not been config, please check chains/fabric/stub.toml\n"
                            + "And add this config:\n"
                            + "[[resources]]\n"
                            + "    # name cannot be repeated\n"
                            + "    name = 'sacc'\n"
                            + "    type = 'FABRIC_CONTRACT'\n"
                            + "    chainCodeName = 'sacc'\n"
                            + "    chainLanguage = 'go'\n"
                            + "    peers=['org1']");
        }

        this.hfClient = fabricConnection.getChaincodeMap().get("sacc").getHfClient();

        this.peers = fabricConnection.getChaincodeMap().get("sacc").getEndorsers();

        queryOnce();
    }

    @Override
    public String getName() {
        return "Pure Fabric Call Suite";
    }

    @Override
    public void call(PerformanceSuiteCallback callback) {
        try {
            Collection<ProposalResponse> responses = queryOnce();
            EndorsementPolicyAnalyzer analyzer = new EndorsementPolicyAnalyzer(responses);

            if (analyzer.allSuccess()) {
                callback.onSuccess("Success");
            } else {
                callback.onFailed("Failed: " + analyzer.info());
            }

        } catch (Exception e) {
            callback.onFailed("sacc query failed: " + e);
        }
    }

    private Collection<ProposalResponse> queryOnce() throws Exception {
        QueryByChaincodeRequest request = hfClient.newQueryProposalRequest();
        String cc = "sacc";
        ChaincodeID ccid = ChaincodeID.newBuilder().setName(cc).build();
        request.setChaincodeID(ccid);
        request.setFcn("query");
        request.setArgs("a");
        request.setProposalWaitTime(3000);

        Collection<ProposalResponse> responses = channel.queryByChaincode(request, peers);
        return responses;
    }
}
