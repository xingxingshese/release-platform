package com.company.release.jenkins;

import com.company.release.jenkins.api.JenkinsProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 测试用 Fake Jenkins（agent.md §二十）：Queue → Build → 状态可控。 */
public class FakeJenkinsProvider implements JenkinsProvider {

    public record TriggeredBuild(String jobName, Map<String, String> params) {
    }

    public final List<TriggeredBuild> triggered = new ArrayList<>();
    /** 下一次 getBuild 返回的状态；null 表示保持 RUNNING。 */
    public BuildStatus nextStatus = BuildStatus.RUNNING;

    @Override
    public long buildWithParameters(String jobName, Map<String, String> parameters) {
        triggered.add(new TriggeredBuild(jobName, parameters));
        return 9000L + triggered.size(); // queueId
    }

    @Override
    public Long getBuildNumberFromQueue(long queueId) {
        return (queueId % 1000) + 500; // 模拟 buildNumber
    }

    @Override
    public BuildInfo getBuild(String jobName, long buildNumber) {
        return new BuildInfo(nextStatus == null ? BuildStatus.RUNNING : nextStatus, buildNumber,
                "https://jenkins.fake/job/" + jobName + "/" + buildNumber);
    }

    @Override
    public String getConsoleText(String jobName, long buildNumber) {
        return "fake console output";
    }

    @Override
    public void stopBuild(String jobName, long buildNumber) {
        nextStatus = BuildStatus.ABORTED;
    }
}
