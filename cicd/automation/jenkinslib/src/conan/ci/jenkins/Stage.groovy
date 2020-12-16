package conan.ci.jenkins

import org.jenkinsci.plugins.workflow.cps.CpsScript

import java.util.concurrent.LinkedBlockingDeque

class Stage {
    static def parallelLimitedBranches(
            CpsScript currentJob,
            List<String> items,
            Integer maxConcurrentBranches,
            Boolean failFast = false,
            Closure body) {

        def branches = [:]
        Deque latch = new LinkedBlockingDeque(maxConcurrentBranches)
        maxConcurrentBranches.times {
            latch.offer("$it")
        }

        items.each {
            branches["${it}"] = {
                def queueSlot = null
                while (true) {
                    queueSlot = latch.pollFirst()
                    if (queueSlot != null) {
                        break
                    }
                }
                try {
                    body(it)
                }
                finally {
                    latch.offer(queueSlot)
                }
            }
        }

        currentJob.parallel(branches)
    }

}
