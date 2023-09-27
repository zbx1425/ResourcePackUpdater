package cn.zbx1425.resourcepackupdater.io.network;

import cn.zbx1425.resourcepackupdater.io.ProgressReceiver;

import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class DownloadDispatcher {

    private final ProgressReceiver progressReceiver;

    public long totalBytes;
    public long downloadedBytes;
    public AtomicLong newlyDownloadedBytes = new AtomicLong(0);

    private long lastSummaryTime = -1;
    private long lastSummaryBytes = 0;
    public long summaryBytesPerSecond = 0;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    public ConcurrentLinkedQueue<DownloadTask> runningTasks = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<DownloadTask> incompleteTasks = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Runnable> delayedProgresses = new ConcurrentLinkedQueue<>();

    public DownloadDispatcher(ProgressReceiver progressReceiver) {
        this.progressReceiver = progressReceiver;
    }

    private final int MAX_RETRIES = 3;

    public void dispatch(DownloadTask task, Supplier<OutputStream> target) {
        totalBytes += task.expectedSize;
        incompleteTasks.add(task);
        executor.submit(() -> {
            runningTasks.add(task);
            try {
                while (true) {
                    try {
                        task.runBlocking(target.get());
                        break;
                    } catch (Exception ex) {
                        if (task.failedAttempts < MAX_RETRIES) {
                            delayedProgresses.add(() -> {
                                progressReceiver.printLog(ex.toString());
                                progressReceiver.printLog(String.format("Retried (%d/%d) %s ...",
                                        task.failedAttempts, MAX_RETRIES, task.fileName));
                            });
                        } else {
                            throw ex;
                        }
                    }
                }
            } catch (Exception e) {
                delayedProgresses.add(() -> progressReceiver.setException(e));
                executor.shutdown();
                runningTasks.clear();
                incompleteTasks.clear();
            } finally {
                runningTasks.remove(task);
                incompleteTasks.remove(task);
            }
        });
    }

    public void updateSummary() {
        while (!delayedProgresses.isEmpty()) delayedProgresses.poll().run();
        long newBytes = newlyDownloadedBytes.getAndSet(0);
        downloadedBytes += newBytes;
        if (lastSummaryTime == -1) {
            lastSummaryTime = System.currentTimeMillis();
            lastSummaryBytes = downloadedBytes;
        } else {
            long currentTime = System.currentTimeMillis();
            long deltaTime = currentTime - lastSummaryTime;
            if (deltaTime > 1000) {
                summaryBytesPerSecond = (downloadedBytes - lastSummaryBytes) * 1000 / deltaTime;
                lastSummaryTime = currentTime;
                lastSummaryBytes = downloadedBytes;
            }
        }
        String message = String.format(": %5d KiB / %5d KiB; %5d KiB/s",
                downloadedBytes / 1024, totalBytes / 1024, summaryBytesPerSecond / 1024);
        progressReceiver.setProgress(downloadedBytes * 1f / totalBytes, 0);

        String runningProgress = incompleteTasks.size() + " Files Remaining\n" +
                String.join(";  ", runningTasks.stream()
                .map(task -> task.fileName + ":" + (
                        task.totalBytes == 0 ? "WAIT" :
                        String.format("%.1f%%", task.downloadedBytes * 100f / task.totalBytes)
                ))
                .toList());
        progressReceiver.setInfo(runningProgress, message);
    }

    public boolean tasksFinished() {
        return incompleteTasks.isEmpty();
    }

    protected void onDownloadProgress(long deltaBytes) {
        newlyDownloadedBytes.addAndGet(deltaBytes);
    }

    public void close() {
        executor.shutdown();
    }
}
