package com.company.release.deployment.adapter;

import com.company.release.deployment.adapter.DeploymentAdapter.DeploymentTarget;
import com.company.release.deployment.adapter.DeploymentAdapter.VerifyOutcome;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 部署验证门面：按 target.type 路由到对应 Adapter，并把逐节点结果落库
 * （release_deployment_node），供发布详情 Timeline 展开查看。
 */
@Service
public class DeploymentVerificationService {

    private final Map<String, DeploymentAdapter> adapters;
    private final ReleaseDeploymentNodeRepository nodeRepository;

    public DeploymentVerificationService(List<DeploymentAdapter> adapters,
                                         ReleaseDeploymentNodeRepository nodeRepository) {
        this.adapters = adapters.stream()
                .collect(Collectors.toMap(DeploymentAdapter::type, Function.identity()));
        this.nodeRepository = nodeRepository;
    }

    public VerifyOutcome verifyAndRecord(DeploymentTarget target, String nodeName) {
        var adapter = adapters.get(target.type());
        if (adapter == null) {
            throw new IllegalArgumentException("no DeploymentAdapter for type: " + target.type());
        }
        var outcome = adapter.verify(target);
        record(target, nodeName, outcome);
        return outcome;
    }

    private void record(DeploymentTarget target, String nodeName, VerifyOutcome o) {
        var node = new ReleaseDeploymentNodeEntity();
        node.setReleaseTaskId(target.taskId());
        node.setServiceName(target.serviceName());
        node.setNodeName(nodeName);
        node.setDeploymentType(target.type());
        node.setReplicaDesired(o.desired());
        node.setReplicaUpdated(o.updated());
        node.setReplicaReady(o.ready());
        node.setReplicaAvailable(o.available());
        node.setReplicaUnavailable(o.unavailable());
        node.setHealthPassed(o.healthPassed());
        node.setVersionExpected(o.versionExpected());
        node.setVersionActual(o.versionActual());
        node.setVersionPassed(o.versionPassed());
        node.setResult(o.result().name());
        node.setMessage(o.message());
        nodeRepository.save(node);
    }
}
